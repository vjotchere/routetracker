package com.vjotchere.gps_thing;

// To Do: Stop location services when stop menu option is selected

import android.Manifest;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

//public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private LocationManager locationManager = null;

    private double latitude = 0;
    private double longitude = 0;
    private boolean valid = false;

    private SharedPreferences settings = null;

    private boolean browse = false;
    private boolean circle = true;

    private Menu mMenu = null;

    private ActiveListener activeListener = new ActiveListener();

    private GoogleMap mMap;

    private ArrayList<Circle> circles = new ArrayList<Circle>();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        menu.findItem(R.id.cont).setVisible(false);
        menu.findItem(R.id.end).setVisible(true);
        //super.onCreateOptionsMenu(menu, inflater);

        mMenu = menu;

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.cont:
                circle = true;
                mMenu.findItem(R.id.cont).setVisible(false);
                mMenu.findItem(R.id.end).setVisible(true);
                return true;

            case R.id.end:
                circle = false;
                mMenu.findItem(R.id.cont).setVisible(true);
                mMenu.findItem(R.id.end).setVisible(false);
                return true;

            case R.id.help:
                HelpDlg helpDlg = new HelpDlg();
                helpDlg.show(getSupportFragmentManager(), "help");
                return true;
        }

        return  true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        //Location permission requesting code
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }

        // Get the location manager
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        // Force the screen to stay on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    /**
     * Called when this application becomes foreground again.
     */
    @Override
    protected void onResume() {
        super.onResume();

        registerListeners();
    }

    /**
     * Called when this application is no longer the foreground application.
     */
    @Override
    protected void onPause() {
        unregisterListeners();

        super.onPause();
    }

    private class ActiveListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            onLocation(location);
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {
            registerListeners();
        }
    };

    private void registerListeners() {
        unregisterListeners();

        // Create a Criteria object
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        criteria.setAltitudeRequired(true);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(false);

        String bestAvailable = locationManager.getBestProvider(criteria, true);

        if(bestAvailable != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(bestAvailable, 500, 1, activeListener);
            Location location = locationManager.getLastKnownLocation(bestAvailable);
            onLocation(location);
        }
    }

    private void unregisterListeners() {
        locationManager.removeUpdates(activeListener);
    }

    private void onLocation(Location location) {
        if(location == null || mMap == null) {
            return;
        }

        latitude = location.getLatitude();
        longitude = location.getLongitude();
        valid = true;

        // Add a circle on current location
        LatLng latLng = new LatLng(latitude, longitude);

        if(circle){
            Circle circle = mMap.addCircle(new CircleOptions()
                    .center(latLng)
                    .radius(5)
                    .strokeColor(Color.TRANSPARENT)
                    .fillColor(0x1AFF0000));
            circles.add(circle);

            // Snap the camera if browse is set
            if(!browse){
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(18));
            }
        }

        TextView tvlat = (TextView) findViewById(R.id.latitude);
        TextView tvlong = (TextView) findViewById(R.id.longitude);

        tvlat.setText(Double.toString(latitude));
        tvlong.setText(Double.toString(longitude));
    }

    public void onBrowse(View view){
        browse = !browse;

        if(browse){
            Toast.makeText(MapsActivity.this, "Browse mode on. The camera will not snap to your location",
                    Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(MapsActivity.this, "Browse mode turned off. The camera will snap to your location.",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void onReset(View view){
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }
}