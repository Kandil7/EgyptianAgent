package com.egyptian.agent.utils;

import android.content.Context;
import android.location.Location;

/**
 * Interface for Location Service as defined in the SRD
 */
public interface LocationServiceInterface {
    Location getLastKnownLocation(Context context);
    void requestLocationUpdates(LocationService.LocationCallback callback);
    void stopLocationUpdates();

    interface LocationCallback {
        void onLocationChanged(Location location);
        void onLocationError(String error);
    }
}