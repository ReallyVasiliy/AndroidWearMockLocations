package com.ustwo.mocklocation;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MainActivity extends WearableActivity {

    public final static String MOCK_LOCATION_PROVIDER = LocationManager.GPS_PROVIDER;
    public final static double MOCK_LOCATION_STEP = 0.00005;
    public final static long MOCK_LOCATION_INTERVAL_MILLIS = 150;
    private final Handler mHandler = new Handler();
    private ScheduledExecutorService mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> mUpdateFuture;
    private double mLastLong = -48.671337;
    private double mLastLat = -26.902038;
    private TextView mTextView;

    private Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updateLocation();
        }
    };

    private Runnable mOutputRunnable = new Runnable() {
        @Override
        public void run() {
            mTextView.setText(String.format(Locale.US, "%.6f, %.6f", mLastLat, mLastLong));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = findViewById(R.id.text);

        // Enables Always-on
        setAmbientEnabled();
    }

    private void disableMockLocations() {
        if (mUpdateFuture == null || mUpdateFuture.isCancelled()) {
            Toast.makeText(this, "Mocking is already disabled...", Toast.LENGTH_LONG)
                    .show();
            return;
        }
        mUpdateFuture.cancel(false);
        mTextView.setText("");
    }

    private void enableMockLocations() {

        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy( Criteria.ACCURACY_FINE );

        try {
            lm.addTestProvider(MOCK_LOCATION_PROVIDER, false, false, false, false, true, true, true, 0, 5);
            lm.setTestProviderEnabled(MOCK_LOCATION_PROVIDER, true);
            mUpdateFuture = mScheduledExecutorService.scheduleAtFixedRate(mUpdateRunnable,
                    0, MOCK_LOCATION_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        } catch (SecurityException se) {

            String command = String.format("adb shell appops set %s android:mock_location allow",
                    getApplicationContext().getPackageName());
            Toast.makeText(this,
                    String.format("Not allowed to mock locations. Did you run \"%s\"?", command),
                    Toast.LENGTH_LONG)
                    .show();
        } catch (IllegalArgumentException iae) {
            Toast.makeText(this, "Can't mock again.", Toast.LENGTH_LONG)
                    .show();
        }

    }

    private void updateLocation() {
        mLastLat += MOCK_LOCATION_STEP;
        mLastLong += MOCK_LOCATION_STEP;
        updateLocation(mLastLat, mLastLong);
        mHandler.post(mOutputRunnable);
    }

    private void updateLocation(double lat, double lon) {
        Location loc = new Location(LocationManager.GPS_PROVIDER);
        Location mockLocation = new Location(LocationManager.GPS_PROVIDER); // a string
        mockLocation.setLatitude(lat);  // double
        mockLocation.setLongitude(lon);
        mockLocation.setAltitude(loc.getAltitude());
        mockLocation.setTime(System.currentTimeMillis());
        mockLocation.setAccuracy(1);
        mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        lm.setTestProviderLocation( LocationManager.GPS_PROVIDER, mockLocation);
    }

    public void onEnableClicked(View view) {
        enableMockLocations();
    }

    public void onDisableClicked(View view) {
        disableMockLocations();
    }
}
