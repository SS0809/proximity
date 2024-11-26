const { handler } = require('./index');

// Test data for storing road points
const storePointEvents = [
    {
        body: JSON.stringify({
            action: 'storeRoadPoint',
            roadName: 'Main Street Delhi',
            latitude: 28.7041,
            longitude: 77.1025,
            distance: 5.2
        })
    },
    {
        body: JSON.stringify({
            action: 'storeRoadPoint',
            roadName: 'Park Avenue Bhopal',
            latitude: 23.2599,
            longitude: 77.4126,
            distance: 3.7
        })
    },
    {
        body: JSON.stringify({
            action: 'storeRoadPoint',
            roadName: 'River Road Kolkata',
            latitude: 22.5726,
            longitude: 88.3639,
            distance: 2.5
        })
    }
];

// Distance calculation event
const distanceEvent = {  
    body: JSON.stringify({
        action: 'calculateDistance',
        latitude1: 22.5726, 
        longitude1: 88.3639, 
        latitude2: 28.7041, 
        longitude2: 77.1025
    })
};

// Nearby points check event
const nearbyEvent = {  
    body: JSON.stringify({
        action: 'checkNearby',
        latitude: 23.25204561436272, 
        longitude: 77.48521347885162
    })
};

// Async function to run tests sequentially
async function runTests() {
    console.log("===== STORING ROAD POINTS =====");
    for (const event of storePointEvents) {
        const response = await handler(event);
        console.log("Store Point Response:", response);
    }

    console.log("\n===== CALCULATE DISTANCE =====");
    const distanceResponse = await handler(distanceEvent);
    console.log("Distance Response:", distanceResponse);

    console.log("\n===== CHECK NEARBY POINTS =====");
    const nearbyResponse = await handler(nearbyEvent);
    console.log("Nearby Points Response:", JSON.stringify(nearbyResponse, null, 2));
}

// Run the tests
runTests().catch(console.error);