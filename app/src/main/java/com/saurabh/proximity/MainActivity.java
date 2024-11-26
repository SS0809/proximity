package com.saurabh.proximity;

import static com.saurabh.proximity.RoadPointFinder.filterPointsByDistance;
import static com.saurabh.proximity.RoadPointFinder.getPointsForRoad;
import static com.saurabh.proximity.RoadPointFinder.updateMapWithPoints;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnTokenCanceledListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.osmdroid.config.Configuration;
import android.content.Context;
import android.os.Environment;
import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_CODE = 100;
    private MapView mapView;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final String API_ENDPOINT = "https://cp0yi7o5hg.execute-api.us-east-1.amazonaws.com/default/location";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final int TIMEOUT_SECONDS = 30;
    private static final long LOCATION_UPDATE_INTERVAL = 1000; // 1 second

    private TextView locationTextView;
    private TextView resultTextView;
    private TextView resultTextView2;
    private TextView speedTextView; // New TextView for speed
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final String USER_AGENT = "com.saurabh.proximity"; // Your app's package name
    private final MutableLiveData<Location> locationLiveData = new MutableLiveData<>();
    private final OkHttpClient httpClient;
    private RoadPointFinder roadPointFinder;
    private Location lastLocation;

    public MainActivity() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainactivity);
        initializeOSMDroid();
        initializeViews();
        setupLocationServices();
        setupLocationCallback();
        setupObservers();

    }
    private void initializeOSMDroid() {
        Context ctx = getApplicationContext();
        // Configuration.getInstance().load(ctx, androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx));

        // Set user agent to prevent getting banned from the OSM servers
        Configuration.getInstance().setUserAgentValue(USER_AGENT);

        // Set the tile cache location
        File basePath = new File(ctx.getCacheDir().getAbsolutePath(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(basePath);
        File tileCache = new File(Configuration.getInstance().getOsmdroidBasePath().getAbsolutePath(), "tile");
        Configuration.getInstance().setOsmdroidTileCache(tileCache);
    }
    private void initializeViews() {
        locationTextView = findViewById(R.id.locationTextView);
        resultTextView = findViewById(R.id.resultTextView);
        resultTextView2 = findViewById(R.id.resultTextView2);
        speedTextView = findViewById(R.id.speedTextView); // Make sure to add this to your layout

        Button invokeButton = findViewById(R.id.invokeButton);
        invokeButton.setOnClickListener(v -> invokeLambdaFunction());

    }

    private void setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void setupObservers() {
        locationLiveData.observe(this, location -> {
            if (location != null) {
                updateLocationUI(location);
                maploader(location);
            }
        });
    }
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    updateSpeed(location);
                    locationLiveData.setValue(location);
                    lastLocation = location;
                } else {
                    Log.w("LocationCallback", "Received null location");
                }
            }
        };
    }
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 2000; // 2 seconds

    private void updateSpeed(Location location) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL) {
            return; // Skip update
        }
        lastUpdateTime = currentTime;

        if (location == null) {
            runOnUiThread(() -> speedTextView.setText("Speed unavailable"));
            return;
        }

        if (location.hasSpeed()) {
            float speedMS = location.getSpeed();
            float speedKMH = speedMS * 3.6f;
            runOnUiThread(() -> {
                String speedText = String.format("Current Speed: %.1f km/h", speedKMH);
                speedTextView.setText(speedText);
            });
        } else {
            runOnUiThread(() -> speedTextView.setText("Speed data not available"));
        }
    }



    private boolean hasLocationPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                PERMISSION_CODE
        );
    }

    private void getCurrentLocation() {
        if (!hasLocationPermissions()) {
            return;
        }

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationToken() {
                        @Override
                        public boolean isCancellationRequested() {
                            return false;
                        }

                        @Override
                        public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
                            return this;
                        }
                    })
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            locationLiveData.setValue(location);
                            System.out.println("getlocation setted");
                        } else {
                            showError("Unable to fetch location. Please try again.");
                        }
                    })
                    .addOnFailureListener(e -> showError("Failed to get location: " + e.getMessage()));
        } catch (SecurityException e) {
            showError("Location permission required");
        }
    }

    private void updateLocationUI(Location location) {
        // TODO HARD CODED STRINGS
        //  location.setLatitude(23.25204561436272);
        // location.setLongitude(77.48521347885162);
        String locationText = getString(R.string.location_format,
                location.getLatitude(),
                location.getLongitude());
        locationTextView.setText(locationText);
    }
    private void startLocationUpdates() {
        if (!hasLocationPermissions()) {
            requestLocationPermissions();
            return;
        }

        try {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                    .setIntervalMillis(LOCATION_UPDATE_INTERVAL)
                    .setMinUpdateIntervalMillis(LOCATION_UPDATE_INTERVAL)
                    .build();

            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    getMainLooper());
        } catch (SecurityException e) {
            showError("Location permission required");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }
    private void findNearestRoad(double latitude, double longitude) {
        RoadPointFinder roadFinder = new RoadPointFinder();
        roadFinder.getNearestRoad(latitude, longitude, new RoadPointFinder.RoadFinderCallback() {


            @Override
            public void onSuccess(String roadName) {
                if (roadName == null) {
                    runOnUiThread(() -> showError("No road found nearby"));
                    return;
                }

                executorService.execute(() -> {
                    // Perform network operation in a background thread
                    List<Point> nearbyPoints = new ArrayList<>();
                    String jsonBody = String.format(
                            "{ \"action\": \"checkNearby\", \"latitude\": %.6f, \"longitude\": %.6f }",
                           latitude,longitude
                    );
                    // HARD CODED
                    // 23.2563714, 77.48669

                    try {
                        String response = checknearbypointsbyredis(jsonBody);
                        JSONObject jsonResponse = new JSONObject(response);
                        JSONArray pointsArray = jsonResponse.getJSONArray("nearbyPoints");
                        for (int i = 0; i < pointsArray.length(); i++) {
                            JSONObject pointObject = pointsArray.getJSONObject(i);
                            double latitude = pointObject.getDouble("latitude");
                            double longitude = pointObject.getDouble("longitude");
                            String roadName1 = pointObject.getString("roadName");
                            double distance = pointObject.getDouble("distance");// TODO unused
                            nearbyPoints.add(new Point(roadName1, latitude, longitude, distance));
                        }
                        String locationText = "Latitude: " + nearbyPoints.get(0).latitude + "\nLongitude: " + nearbyPoints.get(0).longitude;
                        locationTextView.setText(locationText);
                        sendPostRequest(String.format(
                                "{ \"action\": \"calculateDistance\", \"latitude1\": %.6f, \"longitude1\": %.6f, \"latitude2\": "+latitude+", \"longitude2\":"+longitude+" }",
                                nearbyPoints.get(0).latitude, nearbyPoints.get(0).longitude
                        ));
                        runOnUiThread(() -> {
                            updateMapWithPoints(mapView, nearbyPoints);
                            resultTextView2.setText("Nearest Road: " + roadName);
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> showError("Network error: " + e.getMessage()));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> showError("Parsing error: " + e.getMessage()));
                    }
                });
            }


            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> showError("Failed to find road: " + error));
            }
        });
    }
    private void invokeLambdaFunction() {
        //HARD CODED
        Location currentLocation = locationLiveData.getValue();

        if (currentLocation == null) {
            showError("No location available. Please get location first.");
            return;
        }

        // Ensure permissions are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        //findNearestRoad(23.252060147348807, 77.48537967398161);
        findNearestRoad(currentLocation.getLatitude(), currentLocation.getLongitude());
    }


    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
    private void sendPostRequest(String jsonBody) {
        OkHttpClient client = new OkHttpClient();

        // Define MediaType for JSON
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        // Create request body
        RequestBody body = RequestBody.create(jsonBody, JSON);

        // Create POST request
        Request request = new Request.Builder()
                .url(API_ENDPOINT) // Replace with your API Gateway URL
                .post(body)
                .build();

        // Execute the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to connect: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    JSONObject jsonResponse = null;
                    try {
                        jsonResponse = new JSONObject(responseBody);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    String distance = jsonResponse.optString("distance", "N/A");
                    String unit = jsonResponse.optString("unit", "");
                    runOnUiThread(() -> resultTextView.setText("Distance: "+distance+" "+unit));
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
    private String checknearbypointsbyredis(String jsonBody) throws IOException {
        OkHttpClient client = new OkHttpClient();

        // Define MediaType for JSON
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        // Create request body
        RequestBody body = RequestBody.create(jsonBody, JSON);

        // Create POST request
        Request request = new Request.Builder()
                .url(API_ENDPOINT) // Replace with your API Gateway URL
                .post(body)
                .build();

        // Execute synchronously
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string(); // Get the response as a string

                // Parse the body if needed (e.g., JSON parsing)
                return responseBody;
            } else {
                throw new IOException("Error: " + response.code());
            }
        }
    }



    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();
    private void showError(String message) {
        runOnUiThread(() ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                showError("Location permission required for this feature");
            }
        }
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void maploader(Location location) {
        if (location == null) {
            showError("Location not available for map.");
            return;
        }

        try {
            // Configure OsmDroid map settings
            if (mapView == null) {
                mapView = findViewById(R.id.mapView);
                mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
                mapView.setBuiltInZoomControls(true);
                mapView.setMultiTouchControls(true);

                // Enable hardware acceleration
                mapView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null);

                // Enable tile downloading
                mapView.setUseDataConnection(true);
            }

            // Set the map center and zoom level
            GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            mapView.getController().setZoom(15.0);
            mapView.getController().setCenter(startPoint);

            // Add a marker for the current location
            Marker marker = new Marker(mapView);
            marker.setPosition(startPoint);
            marker.setTitle("My Location");
            mapView.getOverlays().add(marker);

            // Force a refresh of the map
            mapView.invalidate();

        } catch (Exception e) {
            showError("Error loading map: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
}