### Documentation for **`MainActivity` Class**

---

#### **Package**
`com.saurabh.proximity`

#### **Overview**
`MainActivity` is the main entry point of the application that demonstrates:
1. Location tracking and updates using Google's Fused Location Provider.
2. Integration with OpenStreetMap (OSMDroid) to display a map and mark user locations.
3. Backend communication using AWS Lambda API Gateway for location-based calculations.
4. Road proximity analysis with road point markers displayed on the map.

---

### **Features**
- **Real-time location tracking**: Updates the user’s location in real-time.
- **Speed calculation**: Displays the user’s current speed in km/h.
- **Map integration**: Displays and updates a map with the user’s location using OSMDroid.
- **Nearest road finder**: Identifies and displays the nearest road and points within a 1km radius.
- **AWS Lambda integration**: Sends location data to a backend server for further processing.

---

### **Dependencies**
- **AndroidX Libraries**: Core Android features and backward compatibility.
- **Google Play Services**: Fused Location Provider for location tracking.
- **OSMDroid**: OpenStreetMap integration.
- **OkHttp**: For API calls.
- **AWS API Gateway**: Endpoint for Lambda integration.

---

### **Permissions Required**
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

---

### **Key Class Variables**
| Variable                          | Type                | Description                                                                          |
|-----------------------------------|---------------------|--------------------------------------------------------------------------------------|
| `PERMISSION_CODE`                 | `int`               | Code used for location permission requests.                                         |
| `mapView`                         | `MapView`           | OSMDroid map view for displaying maps.                                              |
| `locationTextView`                | `TextView`          | UI element for displaying the user's current location.                              |
| `speedTextView`                   | `TextView`          | UI element for displaying the user's current speed.                                 |
| `fusedLocationClient`             | `FusedLocationProviderClient` | Client for accessing location updates.                                          |
| `locationCallback`                | `LocationCallback`  | Callback for receiving location updates.                                            |
| `httpClient`                      | `OkHttpClient`      | HTTP client for API calls to the AWS Lambda endpoint.                               |
| `roadPointFinder`                 | `RoadPointFinder`   | Utility for finding nearby road points.                                            |

---

### **Lifecycle Methods**
1. **`onCreate(Bundle savedInstanceState)`**
    - Initializes the OSMDroid map.
    - Configures views and sets up location services and callbacks.
    - Observes live location updates using `MutableLiveData`.

2. **`onResume()`**
    - Starts location updates when the activity resumes.

3. **`onPause()`**
    - Stops location updates to save resources when the activity is paused.

---

### **Core Methods**
#### **OSMDroid Integration**
1. **`initializeOSMDroid()`**
    - Configures OSMDroid settings, including cache paths and user agent.
    - Prevents potential banning from OSM servers by properly identifying the application.

2. **`maploader(Location location)`**
    - Loads and displays the map centered at the provided location.
    - Configures map settings like zoom, tile source, and user interaction.

---

#### **Location Tracking**
1. **`setupLocationServices()`**
    - Initializes `FusedLocationProviderClient` for location services.

2. **`setupLocationCallback()`**
    - Configures the callback to receive and process location updates.

3. **`startLocationUpdates()`**
    - Requests periodic location updates using the `FusedLocationProviderClient`.

4. **`stopLocationUpdates()`**
    - Stops receiving location updates.

5. **`getCurrentLocation()`**
    - Fetches the user's current location once.

---

#### **Road Proximity**
1. **`findNearestRoad(double latitude, double longitude)`**
    - Fetches the nearest road and nearby points within a 1km radius.
    - Displays the road name and marks points on the map.

---

#### **AWS Lambda Integration**
1. **`invokeLambdaFunction()`**
    - Constructs a JSON payload with the current location.
    - Sends a POST request to an AWS Lambda endpoint using `OkHttp`.
    - Updates the UI with the response.

2. **`sendPostRequest(String jsonBody)`**
    - Sends a POST request to the backend API Gateway.
    - Handles success and failure responses.

---

#### **UI and Error Handling**
1. **`showError(String message)`**
    - Displays error messages in a toast.

2. **`updateSpeed(Location location)`**
    - Converts speed from m/s to km/h and updates the UI.

3. **`updateLocationUI(Location location)`**
    - Displays the user's latitude and longitude in the UI.

---

### **Usage Example**
1. Add required permissions in `AndroidManifest.xml`.
2. Configure AWS Lambda API Gateway URL in `API_ENDPOINT`.
3. Update `mainactivity.xml` to include required views (e.g., `TextView`, `Button`, `MapView`).
4. Launch the app and grant location permissions.
5. Observe real-time location and map updates.

---

### **Sample Lambda JSON Request**
```json
{
  "action": "calculateDistance",
  "latitude1": 23.252060,
  "longitude1": 77.485380,
  "latitude2": 23.256371,
  "longitude2": 77.486690
}
```

---

### **Potential Improvements**
1. Add a retry mechanism for failed API calls.
2. Enable offline map caching for better user experience in low-connectivity areas.
3. Optimize location update intervals based on user activity.

---

