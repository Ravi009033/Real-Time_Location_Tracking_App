package com.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap, mMap2;
    private DatabaseReference databaseReference, databaseReference2;
    private LocationListener locationListener;
    private LocationManager locationManager;
    private EditText editTextLatitude, editTextLongitude, editTextLocality;
    FusedLocationProviderClient fusedLocationProviderClient;
    private Polyline currentPolyline;
    Button getDirection;
    private MarkerOptions place1, place2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if(mapFragment != null) mapFragment.getMapAsync(this);

        //Request location permission from user
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION},
                PackageManager.PERMISSION_GRANTED);

        //Initializing text editor to view location
        editTextLatitude = findViewById(R.id.editText);
        editTextLongitude = findViewById(R.id.editText2);
        editTextLocality = findViewById(R.id.editText3);

        //Search location when user presses the search action on keyboard
        editTextLocality.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    searchLocation();
                    return true;
                }
                return false;
            }
        });

        locationEnabled();  // Prompts user to enable location if disabled
        getLocation();      // Gets current device location

        //set up Firebase database listener
        databaseReference = FirebaseDatabase.getInstance().getReference("Location");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                try {
                    //store local value to database
                    String databaseLatitudeString = snapshot.child("latitude").getValue().toString().substring(1, snapshot.child("latitude").getValue().toString().length() - 1);
                    String databaseLongitudeString = snapshot.child("longitude").getValue().toString().substring(1, snapshot.child("longitude").getValue().toString().length() - 1);

                    String[] stringLat = databaseLatitudeString.split(", ");
                    Arrays.sort(stringLat);
                    String[] stringLong = databaseLongitudeString.split(", ");
                    Arrays.sort(stringLong);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    //called when map is ready
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap2 = googleMap;
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                try {
                    updateLocation(location);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        };
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // User taps map to add a marker and update address fields
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                mMap.clear();
                // Animating to the touched position
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.addMarker(markerOptions);

                setTextvalue(latLng.latitude,latLng.longitude);
                try {
                    Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

                    if (addresses != null) {
                        editTextLocality.setText(addresses.get(0).getAddressLine(0));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void shareButtonOnclick(View view) {

        HashMap hashMap = new HashMap();
        hashMap.put("latitude", editTextLatitude.getText().toString());
        hashMap.put("longitude", editTextLongitude.getText().toString());
        databaseReference.updateChildren(hashMap);
        Toast.makeText(this, "Share Location successful", Toast.LENGTH_SHORT).show();

    }

    public void updateLocationOnclick(View view) {
        getLocation();

    }

    private void locationEnabled() {
        LocationManager lm = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!gps_enabled && !network_enabled) {
            new AlertDialog.Builder(MapsActivity.this)
                    .setMessage("GPS Enable")
                    .setPositiveButton("Settings", new
                            DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                }
                            })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

    }

    // Uses FusedLocationProvider to get last known location
    private void getLocation() {
        locationEnabled();
        //intialise fusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                Location location = task.getResult();
                if (location != null) {
                    try {
                        Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                       updateLocation(location);

                        Toast.makeText(MapsActivity.this, "You are in " + addresses.get(0).getLocality(), Toast.LENGTH_SHORT).show();
                        editTextLocality.setText(addresses.get(0).getAddressLine(0));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            }

        });
    }
    private void updateLocation(Location location) {

        setTextvalue(location.getLatitude(),location.getLongitude());
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        marker_display(latLng);

    }
    private void setTextvalue(double x,double y)
    {
        String lat = Double.toString(x);
        String lon = Double.toString(y);
        editTextLatitude.setText(lat);
        editTextLongitude.setText(lon);
    }
    private void marker_display(LatLng latLng)
    {
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(latLng));
        CameraPosition myPosition = new CameraPosition.Builder()
                .target(latLng).zoom(10).build();
        mMap.animateCamera(
                CameraUpdateFactory.newCameraPosition(myPosition));
    }

    // Searches address entered in locality EditText and places marker
    public void searchLocation() {
        String location = editTextLocality.getText().toString();
        List<Address> addressList = null;

        if (location != null || !location.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);

            } catch (IOException e) {
                e.printStackTrace();
            }
            Address address = addressList.get(0);

           LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            setTextvalue(address.getLatitude(),address.getLongitude());
            marker_display(latLng);
        }
    }

    // Handles "second location" feature, displays a blue marker
     public void secondLocationOnclick(View view) {
         Toast.makeText(MapsActivity.this, "Clicked", Toast.LENGTH_SHORT).show();
         databaseReference2 = FirebaseDatabase.getInstance().getReference().child("Location2");
         //update everytime when data get updated
         databaseReference2.addValueEventListener(new ValueEventListener() {
             @Override
             public void onDataChange(@NonNull DataSnapshot snapshot) {
                 try {
                     String databaseLatitudeString2 = snapshot.child("latitude").getValue().toString();
                     String databaseLongitudedeString2 = snapshot.child("longitude").getValue().toString();

                     LatLng latLng = new LatLng(Double.parseDouble(databaseLatitudeString2), Double.parseDouble(databaseLongitudedeString2));

                     mMap2.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                     CameraPosition myPosition = new CameraPosition.Builder()
                             .target(latLng).zoom(10).build();
                     mMap2.animateCamera(
                             CameraUpdateFactory.newCameraPosition(myPosition));

                 } catch (Exception e) {
                     e.printStackTrace();
                 }
             }
             @Override
             public void onCancelled(@NonNull DatabaseError error) {

             }
         });
     }

    // Updates Firebase "Track" node with a specific value
     private void Trackfind(int val){
         databaseReference = FirebaseDatabase.getInstance().getReference("Track");
         HashMap hashMap = new HashMap();
         hashMap.put("key", val);

         databaseReference.updateChildren(hashMap);

     }



}




