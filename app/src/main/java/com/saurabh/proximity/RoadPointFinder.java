package com.saurabh.proximity;

import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
public class RoadPointFinder {
    private static final String TAG = "RoadFinder";
    private static final List<Point> predefinedPoints = new ArrayList<>();
    // HARD CODED
    static {
        predefinedPoints.add(new Point("Raisen Road diff", 23.251858252142124, 77.48453767393227, 0.0));
    }
    private static final String OSM_API_URL = "https://nominatim.openstreetmap.org/reverse";
    private static final String USER_AGENT = "YourAppName/1.0"; // Replace with your app name
    private static final int TIMEOUT_SECONDS = 10;

    private final OkHttpClient client;

    public RoadPointFinder() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .header("User-Agent", USER_AGENT)
                            .method(original.method(), original.body())
                            .build();
                    return chain.proceed(request);
                })
                .build();
    }

    public interface RoadFinderCallback {
        void onSuccess(String roadName);
        void onFailure(String error);
    }

    public void getNearestRoad(double latitude, double longitude, RoadFinderCallback callback) {
        String url = String.format("%s?format=json&lat=%f&lon=%f", OSM_API_URL, latitude, longitude);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "API call failed", e);
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) {
                try {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response code: " + response);
                    }

                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        throw new IOException("Empty response from server");
                    }

                    String roadName = extractRoadName(responseBody.string());
                    if (roadName != null) {
                        callback.onSuccess(roadName);
                    } else {
                        callback.onFailure("Road name not found in response");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error processing response", e);
                    callback.onFailure("Error processing response: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    private String extractRoadName(String responseJson) throws JSONException {
        JSONObject json = new JSONObject(responseJson);
        Log.d(TAG, "Response: " + responseJson); // Log the full response for debugging

        if (!json.has("address")) {
            Log.w(TAG, "No address object in response");
            return null;
        }

        JSONObject address = json.getJSONObject("address");
        if (!address.has("road")) {
            // Try alternative fields if "road" is not available
            String[] possibleFields = {"street", "pedestrian", "path", "footway", "highway"};
            for (String field : possibleFields) {
                if (address.has(field)) {
                    return address.getString(field);
                }
            }
            Log.w(TAG, "No road name found in address object");
            return null;
        }

        return address.getString("road");
    }// Method to get points for a specific road
    static List<Point> getPointsForRoad(String roadName) {
        List<Point> points = new ArrayList<>();
        for (Point point : predefinedPoints) {
            points.add(point);
        }
        return points;
    }

    // Method to filter points within a certain distance
    public static List<Point> filterPointsByDistance(double lat1, double lon1, List<Point> points, double maxDistanceMeters) {
        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException("Point list is null or empty");
        }

        List<Point> nearbyPoints = new ArrayList<>();
        for (Point point : points) {
            if (point == null) continue; // Skip null points
            double distance = calculateDistance(lat1, lon1, point.latitude, point.longitude);
            if (distance <= maxDistanceMeters) {
                point.distance = distance;
                nearbyPoints.add(point);
            }
        }
        return nearbyPoints;
    }
    public static void updateMapWithPoints(MapView mapView, List<Point> points) {
        if (mapView == null) {
            throw new IllegalArgumentException("MapView is null");
        }

        // Add markers for each point
        for (Point point : points) {
            if (point == null) continue;
            GeoPoint geoPoint = new GeoPoint(point.latitude, point.longitude);
            Marker marker = new Marker(mapView);
            marker.setPosition(geoPoint);
            marker.setTitle(String.format("Lat: %.6f, Lon: %.6f", point.latitude, point.longitude));
            mapView.getOverlays().add(marker);
        }

        // Adjust map to show all points if needed
        if (!points.isEmpty()) {
            GeoPoint firstPoint = new GeoPoint(points.get(0).latitude, points.get(0).longitude);
            mapView.getController().setZoom(15.0);
            mapView.getController().setCenter(firstPoint);
        }
    }

    // Haversine formula to calculate the distance between two points
    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS = 6371000.0; // Earth's radius in meters
        if (Double.isNaN(lat1) || Double.isNaN(lon1) || Double.isNaN(lat2) || Double.isNaN(lon2)) {
            throw new IllegalArgumentException("Latitude or Longitude values are invalid");
        }

        // Convert degrees to radians
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        // Apply Haversine formula
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c; // Distance in meters
    }

}

// Class to represent a point
class Point {
    String roadName;
    double latitude;
    double longitude;

    double distance;

    Point(String roadName, double latitude, double longitude,  double distance) {
        this.roadName = roadName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distance = distance;
    }
}