const { calculateDistance } = require('./distanceCalculator');
const { createClient } = require('redis');

// Global Redis client variable
let redisClient;

// Function to get or create Redis client
const getRedisClient = async () => {
    if (!redisClient) {
        redisClient = createClient({
            password: 'KQDUcWErbcAbX2kcpvrhz1Bp7zNwPsyb',
            socket: {
                host: 'redis-16274.c57.us-east-1-4.ec2.redns.redis-cloud.com',
                port: 16274,
            },
        });

        redisClient.on('error', (err) => console.error('Redis Client Error', err));

        await redisClient.connect();
    }
    return redisClient;
};

exports.handler = async (event) => {
    try {
        // Initialize Redis client
        const client = await getRedisClient();

        const { action, latitude, longitude } = JSON.parse(event.body);

        if (!action) {
            return {
                statusCode: 400,
                body: JSON.stringify({ error: 'Missing action parameter' }),
            };
        }

        if (action === 'calculateDistance') {
            // Parse input coordinates for distance calculation
            const { latitude1, longitude1, latitude2, longitude2 } = JSON.parse(event.body);

            if (
                latitude1 === undefined || longitude1 === undefined ||
                latitude2 === undefined || longitude2 === undefined
            ) {
                return {
                    statusCode: 400,
                    body: JSON.stringify({ error: 'Missing required parameters' }),
                };
            }

            // Calculate distance
            const distance = calculateDistance(latitude1, longitude1, latitude2, longitude2);

            // Return the response
            return {
                statusCode: 200,
                body: JSON.stringify({
                    distance: distance.toFixed(2), // Distance in kilometers rounded to 2 decimal places
                    unit: 'km',
                }),
            };
        } else if (action === 'checkNearby') {
            if (latitude === undefined || longitude === undefined) {
                return {
                    statusCode: 400,
                    body: JSON.stringify({ error: 'Missing latitude or longitude' }),
                };
            }

            // Retrieve stored points from Redis
            const storedPoints = await client.lRange('storedPoints', 0, -1); // Assuming stored points are in a list
            if (!storedPoints || storedPoints.length === 0) {
                return {
                    statusCode: 404,
                    body: JSON.stringify({ error: 'No stored points found' }),
                };
            }

            // Parse and check distances
            const withinRadius = [];
            const userPoint = { latitude: parseFloat(latitude), longitude: parseFloat(longitude) };

            for (const point of storedPoints) {
                const parsedPoint = JSON.parse(point); // Assuming points are stored as JSON strings
                const distance = calculateDistance(
                    userPoint.latitude,
                    userPoint.longitude,
                    parsedPoint.latitude,
                    parsedPoint.longitude
                );

                if (distance <= 2) {
                    withinRadius.push(parsedPoint);
                }
            }

            // Return points within 2 km
            return {
                statusCode: 200,
                body: JSON.stringify({
                    nearbyPoints: withinRadius,
                    count: withinRadius.length,
                }),
            };
        } else {
            return {
                statusCode: 400,
                body: JSON.stringify({ error: 'Invalid action' }),
            };
        }
    } catch (error) {
        console.error('Error:', error);
        return {
            statusCode: 500,
            body: JSON.stringify({ error: 'Internal Server Error' }),
        };
    }
};
