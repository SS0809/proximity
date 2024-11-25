const { handler } = require('./index');


const event = {  "body": "{ \"action\": \"checkNearby\",\"latitude\": 23.25204561436272, \"longitude\": 77.48521347885162}" };
const event1 = {  "body": "{ \"action\": \"calculateDistance\",\"latitude1\": 22.5726, \"longitude1\": 88.3639, \"latitude2\": 28.7041, \"longitude2\": 77.1025}" };
handler(event1).then((response) => {
    console.log("Response:", response);
});


handler(event).then((response) => {
    console.log("Response:", response);
});
