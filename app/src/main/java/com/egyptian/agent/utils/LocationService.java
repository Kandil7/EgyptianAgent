package com.egyptian.agent.utils;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * Location service implementation as defined in the SRD
 */
public class LocationService implements LocationServiceInterface {
    private static final String TAG = "LocationService";
    private Context context;
    private LocationManager locationManager;
    private Location currentLocation;

    public LocationService() {
        // Default constructor
    }

    @Override
    public Location getLastKnownLocation(Context context) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        try {
            // Get location from GPS or Network provider
            Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            // Choose the most accurate location
            if (gpsLocation != null && networkLocation != null) {
                if (gpsLocation.getAccuracy() < networkLocation.getAccuracy()) {
                    currentLocation = gpsLocation;
                } else {
                    currentLocation = networkLocation;
                }
            } else if (gpsLocation != null) {
                currentLocation = gpsLocation;
            } else if (networkLocation != null) {
                currentLocation = networkLocation;
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted", e);
            CrashLogger.logError(context, e);
        }

        return currentLocation;
    }

    @Override
    public void requestLocationUpdates(LocationCallback callback) {
        if (locationManager != null) {
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000, // 5 seconds
                    10,   // 10 meters
                    new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            callback.onLocationChanged(location);
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {
                            // Not implemented
                        }

                        @Override
                        public void onProviderEnabled(String provider) {
                            // Not implemented
                        }

                        @Override
                        public void onProviderDisabled(String provider) {
                            callback.onLocationError("Location provider disabled: " + provider);
                        }
                    }
                );
            } catch (SecurityException e) {
                Log.e(TAG, "Location permission not granted", e);
                callback.onLocationError("Permission denied: " + e.getMessage());
                CrashLogger.logError(context, e);
            }
        }
    }

    @Override
    public void stopLocationUpdates() {
        if (locationManager != null) {
            locationManager.removeUpdates(new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {}

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}
            });
        }
    }
}