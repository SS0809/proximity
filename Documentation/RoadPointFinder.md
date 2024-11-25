### Documentation for **`RoadPointFinder` Class**

---

#### **Package**
`com.saurabh.proximity`

---

### **Overview**
The `RoadPointFinder` class is a utility for finding nearby road points within a specific radius and determining the closest road to a given geographical location. It integrates with location services and a backend server (AWS Lambda or another API) to fetch road data and perform calculations.

---

### **Features**
- **Nearest road calculation**: Identifies the closest road to the given latitude and longitude.
- **Proximity point fetching**: Retrieves and displays points of interest (POIs) or road markers within a configurable radius.
- **Backend integration**: Sends location data to an external server for road-related computations.
- **Map marker support**: Adds markers for identified road points on an OSMDroid map.

---

### **Dependencies**
- **OkHttp**: For making HTTP requests to the backend server.
- **OSMDroid**: For adding markers and interacting with maps.
- **Gson**: For JSON parsing (optional).

---

### **Key Class Variables**
| Variable           | Type           | Description                                                                           |
|--------------------|----------------|---------------------------------------------------------------------------------------|
| `radius`           | `double`       | Radius (in meters) within which road points are fetched.                             |
| `roadDataEndpoint` | `String`       | API endpoint for fetching road data.                                                 |
| `httpClient`       | `OkHttpClient` | HTTP client for making API requests.                                                 |
| `mapView`          | `MapView`      | Reference to the OSMDroid map where markers will be displayed.                       |

---

### **Constructor**
#### **`RoadPointFinder(MapView mapView, String roadDataEndpoint)`**
- **Parameters**:
    - `mapView`: The OSMDroid map where road markers will be displayed.
    - `roadDataEndpoint`: URL of the API endpoint for road data retrieval.
- **Description**:
  Initializes the `RoadPointFinder` instance with the map reference and API endpoint.

---

### **Public Methods**

#### **`void findNearestRoad(double latitude, double longitude)`**
- **Parameters**:
    - `latitude`: The latitude of the current location.
    - `longitude`: The longitude of the current location.
- **Description**:
    - Fetches the nearest road and surrounding points within the specified radius.
    - Updates the map with road markers and displays the road name in the UI.
- **Implementation Steps**:
    1. Constructs a JSON payload with the user's coordinates and radius.
    2. Sends a POST request to the `roadDataEndpoint`.
    3. Parses the server response to extract road and point data.
    4. Displays the nearest road name in the UI.
    5. Adds markers for road points on the OSMDroid map.

---

#### **`void setRadius(double radius)`**
- **Parameters**:
    - `radius`: The search radius in meters.
- **Description**:
  Updates the radius used for fetching nearby road points.

---

#### **`void addMarkersToMap(List<LocationPoint> points)`**
- **Parameters**:
    - `points`: A list of `LocationPoint` objects representing road points.
- **Description**:
    - Clears existing markers from the map.
    - Adds new markers for each point in the provided list.

---

#### **`void handleApiResponse(String response)`**
- **Parameters**:
    - `response`: JSON response string from the API.
- **Description**:
    - Parses the JSON response to extract road and point data.
    - Updates the UI and map with the received data.

---

### **Helper Classes**

#### **`LocationPoint`**
Represents a point of interest or road marker on the map.
- **Attributes**:
    - `latitude`: Latitude of the point.
    - `longitude`: Longitude of the point.
    - `name`: Optional name or description of the point.

#### **Example**
```java
class LocationPoint {
    double latitude;
    double longitude;
    String name;
}
```

---

### **Usage Example**

```java
// Initialize RoadPointFinder
RoadPointFinder roadPointFinder = new RoadPointFinder(mapView, "https://api.example.com/road-data");

// Set search radius
roadPointFinder.setRadius(1000);

// Find the nearest road and display points
roadPointFinder.findNearestRoad(23.252060, 77.485380);
```

---

### **Sample JSON Response**
```json
{
  "nearestRoad": "MG Road",
  "points": [
    { "latitude": 23.252500, "longitude": 77.485700, "name": "Point A" },
    { "latitude": 23.253000, "longitude": 77.486200, "name": "Point B" }
  ]
}
```

---

### **Error Handling**
- **Timeouts**: Uses `OkHttp`'s built-in timeout handling to ensure API calls do not hang indefinitely.
- **Invalid Data**: Validates server responses to handle missing or malformed data.
- **UI Updates**: Displays a toast or dialog in case of API failures or connectivity issues.

---

### **Potential Improvements**
1. Add caching for road data to reduce API calls.
2. Enable offline functionality by storing road data locally.
3. Support for dynamic radius adjustments based on user preferences.

---
