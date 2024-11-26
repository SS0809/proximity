const { calculateDistance } = require('./distanceCalculator');
const { createClient } = require('redis');
const fs = require('fs');
const path = require('path');

// Configuration constants
const CONFIG = {
    REDIS: {
        HOST:  'redis-16274.c57.us-east-1-4.ec2.redns.redis-cloud.com',
        PORT:16274,
        PASSWORD: 'KQDUcWErbcAbX2kcpvrhz1Bp7zNwPsyb',
        connectTimeout: 10000 ,
    },
    DEFAULTS: {
        SEARCH_RADIUS_KM: 2,
        COORDINATE_PRECISION: 6
    }
};

// Redis client singleton
let redisClient;

// Validation functions
const validateRequestBody = (body) => {
    if (!body) {
        throw new Error('Request body is empty');
    }

    let parsed;
    try {
        parsed = typeof body === 'string' ? JSON.parse(body) : body;
    } catch (e) {
        throw new Error(`Invalid JSON format: ${e.message}`);
    }

    if (!parsed.action) {
        throw new Error('Missing required field: action');
    }

    return parsed;
};

const isValidCoordinate = (coord) => {
    return typeof coord === 'number' && 
           !isNaN(coord) && 
           isFinite(coord) &&
           Math.abs(coord) <= 180;
};

const validateCoordinates = (lat, lon) => {
    if (!isValidCoordinate(lat) || !isValidCoordinate(lon)) {
        throw new Error('Invalid coordinates provided');
    }
    if (Math.abs(lat) > 90) {
        throw new Error('Latitude must be between -90 and 90 degrees');
    }
};

// Redis client initialization
const getRedisClient = async () => {
    if (!redisClient) {
        redisClient = createClient({
            password: CONFIG.REDIS.PASSWORD,
            socket: {
                host: CONFIG.REDIS.HOST,
                port: CONFIG.REDIS.PORT,
            },
            retry_strategy: (options) => {
                if (options.total_retry_time > 1000 * 60 * 60) {
                    return new Error('Retry time exhausted');
                }
                return Math.min(options.attempt * 100, 3000);
            }
        });

        redisClient.on('error', (err) => {
            console.error('Redis Client Error:', err);
            redisClient = null;
        });

        await redisClient.connect();
    }
    return redisClient;
};

// Action handlers
const handleCalculateDistance = async (params) => {
    const { latitude1, longitude1, latitude2, longitude2 } = params;
    
    // Convert string coordinates to numbers
    const coords = {
        lat1: parseFloat(latitude1),
        lon1: parseFloat(longitude1),
        lat2: parseFloat(latitude2),
        lon2: parseFloat(longitude2)
    };

    // Validate coordinates
    [
        [coords.lat1, coords.lon1],
        [coords.lat2, coords.lon2]
    ].forEach(([lat, lon]) => validateCoordinates(lat, lon));

    const distance = calculateDistance(
        coords.lat1, 
        coords.lon1, 
        coords.lat2, 
        coords.lon2
    );
    
    return {
        distance: Number(distance.toFixed(CONFIG.DEFAULTS.COORDINATE_PRECISION)),
        unit: 'km'
    };
};

const handleCheckNearby = async (params, client) => {
    const lat = parseFloat(params.latitude);
    const lon = parseFloat(params.longitude);
    
    validateCoordinates(lat, lon);

    const storedPoints = await client.lRange('storedPoints', 0, -1);
    if (!storedPoints?.length) {
        return {
            nearbyPoints: [],
            count: 0
        };
    }

    const withinRadius = storedPoints
        .map(point => {
            try {
                return JSON.parse(point);
            } catch (e) {
                console.warn('Invalid point data in Redis:', point);
                return null;
            }
        })
        .filter(point => point !== null)
        .filter(point => {
            const distance = calculateDistance(
                lat,
                lon,
                parseFloat(point.latitude),
                parseFloat(point.longitude)
            );
            return distance <= CONFIG.DEFAULTS.SEARCH_RADIUS_KM;
        });

    return {
        nearbyPoints: withinRadius,
        count: withinRadius.length
    };
};
const handleStoreRoadPoint = async (params, client) => {
    const { roadName, latitude, longitude, distance } = params;
    
    // Validate coordinates
    [
        [parseFloat(latitude), parseFloat(longitude)]
    ].forEach(([lat, lon]) => validateCoordinates(lat, lon));

    // Create road point object
    const roadPoint = {
        roadName,
        latitude: parseFloat(latitude).toFixed(CONFIG.DEFAULTS.COORDINATE_PRECISION),
        longitude: parseFloat(longitude).toFixed(CONFIG.DEFAULTS.COORDINATE_PRECISION),
        distance: parseFloat(distance).toFixed(1),
        timestamp: new Date().toISOString()
    };

    // Store the point in Redis list
    await client.lPush('storedPoints', JSON.stringify(roadPoint));

    return {
        message: 'Road point stored successfully',
        point: roadPoint
    };
};

// Main handler
exports.handler = async (event) => {
        if (event.httpMethod === 'GET') {
            const indexHTML = fs.readFileSync(path.join(__dirname, 'index.html'), 'utf-8');
            return {
                statusCode: 200,
                headers: {
                    'Content-Type': 'text/html',
                    'Cache-Control': 'no-store'
                },
                body: indexHTML
            };
        }
    try {
        // Log incoming request for debugging
        console.log('Incoming request:', {
            body: event.body,
            headers: event.headers
        });

        // Validate and parse request body
        const params = validateRequestBody(event.body);
        
        const client = await getRedisClient();
        let result;

        switch (params.action) {
            case 'calculateDistance':
                result = await handleCalculateDistance(params);
                break;
            case 'checkNearby':
                result = await handleCheckNearby(params, client);
                break;
            case 'storeRoadPoint':
                result = await handleStoreRoadPoint(params, client);
                break;
            default:
                throw new Error(`Invalid action: ${params.action}`);
        }

        return {
            statusCode: 200,
            headers: {
                'Content-Type': 'application/json',
                'Cache-Control': 'no-store'
            },
            body: JSON.stringify(result)
        };

    } catch (error) {
        console.error('Error details:', {
            message: error.message,
            stack: error.stack
        });
        
        const statusCode = error.message.includes('Invalid') ? 400 : 500;
        
        return {
            statusCode,
            headers: {
                'Content-Type': 'application/json',
                'Cache-Control': 'no-store'
            },
            body: JSON.stringify({
                error: statusCode === 400 ? error.message : 'Internal Server Error',
                details: process.env.NODE_ENV === 'development' ? error.stack : undefined
            })
        };
    }
};