package com.logger.app;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import org.json.*;

public class GpsTrackerActivity extends ActionBarActivity implements LocationListener, GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private static final String TAG = "GpsTrackerActivity";
    private static TextView longitudeTextView;
    private static TextView latitudeTextView;
    private static TextView accuracyTextView;
    private static TextView providerTextView;
    private static TextView timeStampTextView;
    private static TextView phoneNumberTextView;

    private LocationRequest locationRequest;
    private LocationClient locationClient;
    private Location previousLocation;
    private float totalDistanceInMeters = 0.0f;
    private boolean firstTimeGettingPosition = true;
    private boolean currentlyTracking = false;
    private String sessionID;
    private String shortSessionID;
    private String phoneNumber = "androidUser";

    private LocationDatabaseHelper mDatabaseHelper;

    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpstracker);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        int response = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(response == ConnectionResult.SUCCESS){
            locationClient = new LocationClient(this,this,this);
            locationClient.connect();
        }
        else{
            Log.e(TAG, "google play service error: " + response);
        }

        mDatabaseHelper = new LocationDatabaseHelper(getApplicationContext());
        queue = Volley.newRequestQueue(this);
    }

    // called when startTrackingButton is tapped
    public void trackLocation(View v) {
        if (currentlyTracking) {
            ((Button) v).setText("start tracking");
            stopTracking();
            currentlyTracking = false;
        } else {
            ((Button) v).setText("stop tracking");
            startTracking();
            currentlyTracking = true;
        }
    }

    protected void startTracking() {
        sessionID = UUID.randomUUID().toString();
        shortSessionID = sessionID.substring(0,5);
        phoneNumberTextView.setText("phoneNumber: " + phoneNumber + "-" + shortSessionID);

        totalDistanceInMeters = 0.0f;
        int intervalInSeconds = 5; // one minute
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(intervalInSeconds * 1000);
        locationRequest.setFastestInterval(intervalInSeconds * 1000); // the fastest rate in milliseconds at which your app can handle location updates
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationClient.requestLocationUpdates(locationRequest, this);
    }

    protected void stopTracking() {
        phoneNumberTextView.setText("phoneNumber: ");
        Log.e(TAG,"stopping");

        if (locationClient != null && locationClient.isConnected()) {
            locationClient.removeLocationUpdates(this);
//            locationClient.disconnect();
            Log.e(TAG,"disconnected location client");
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            displayLocationData(location);
            insertLocation(location);
            postLocation(location);
        }
    }

    public void insertLocation(Location location){
        mDatabaseHelper.insertLocation(location);
    }

    public void postLocation(final Location location){
        if(location == null)
            return;

        final String url = "http://localhost:8080";

        final String timestamp = Long.toString(location.getTime());
        final String latitude = Double.toString(location.getLatitude());
        final String longitude = Double.toString(location.getLongitude());
        final String speed = Float.toString(location.getSpeed());
        final String heading = Float.toString(location.getBearing());

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("uuid", "test");
        params.put("gps_timestamp", timestamp);
        params.put("gps_latitude", latitude);
        params.put("gps_longitude", longitude);
        params.put("gps_speed", speed);
        params.put("gps_heading", heading);

        JsonObjectRequest postRequest = new JsonObjectRequest(url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            VolleyLog.v("Response:%n %s", response.toString(4));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Error: ", error.getMessage());
            }
        });

        queue.add(postRequest);
    }

    protected void displayLocationData(Location location) {
        DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");
        dateFormat.setTimeZone(TimeZone.getDefault());
        Date date = new Date(location.getTime());

        longitudeTextView.setText("longitude: " + location.getLongitude());
        latitudeTextView.setText("latitude: " + location.getLatitude());
        accuracyTextView.setText("accuracy: " + location.getAccuracy());
        providerTextView.setText("provider: " + location.getProvider());
        timeStampTextView.setText("timeStamp: " + dateFormat.format(date));

        Log.e(TAG, dateFormat.format(date) + " accuracy: " + location.getAccuracy());
    }

    @Override
    protected void onStart() {
        Log.e(TAG, "onStart");
        super.onStart();
        // Connect the client.
//        locationClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "onStop");

        if (locationClient != null && locationClient.isConnected()) {
            locationClient.removeLocationUpdates(this);
            locationClient.disconnect();
        }
    }

    /**
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle bundle) {
        Log.e(TAG, "onConnected");
    }

    /**
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        Log.e(TAG, "onDisconnected");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gps_tracker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class PlaceholderFragment extends Fragment {
        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_gpstracker, container, false);
            longitudeTextView = (TextView)rootView.findViewById(R.id.longitudeTextView);
            latitudeTextView = (TextView)rootView.findViewById(R.id.latitudeTextView);
            accuracyTextView = (TextView)rootView.findViewById(R.id.accuracyTextView);
            providerTextView = (TextView)rootView.findViewById(R.id.providerTextView);
            timeStampTextView = (TextView)rootView.findViewById(R.id.timeStampTextView);
            phoneNumberTextView = (TextView)rootView.findViewById(R.id.phoneNumberTextView);
            return rootView;
        }
    }
}
