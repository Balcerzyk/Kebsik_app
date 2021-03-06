package com.example.myapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<ListItem> listItems;
    ArrayList<String> names = new ArrayList<>();
    ArrayList<String> addresses = new ArrayList<>();

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    String URL_DATA;
    int searchDistance = 1000;
    Location location = null;

    boolean mLocationPermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        listItems = new ArrayList<>();

        setButtonsClickListener();

        if (checkPermissions()) {
            setLink();
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if (resultCode == CheckActivity.RESULT_OK) {
                setLink();
            }
        }
    }


    public void setButtonsClickListener() {
        final Button mapButton = findViewById(R.id.mapButton);

        View.OnClickListener myClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.mapButton:
                        if(location != null) {
                            Intent intentMap = new Intent(MainActivity.this, MapsActivity.class);
                            intentMap.putStringArrayListExtra("names", names);
                            intentMap.putStringArrayListExtra("addresses", addresses);
                            startActivity(intentMap);
                        }
                        else Toast.makeText(getApplicationContext(), "Check your internet connection and GPS", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
            }
        };

        mapButton.setOnClickListener(myClickListener);
    }

    private void setLink() {
        getLocationPermission();

        location = getLocation();
        if(location != null) URL_DATA = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + Double.toString(location.getLatitude()) + "," + Double.toString(location.getLongitude()) + "&radius=" + searchDistance + "&type=restaurant&keyword=kebap&key=AIzaSyCeqtVQj1BrhCyNWb4esPOzop43lc5fYIY";
        else URL_DATA = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=52.404252,16.949703&radius=" + searchDistance + "&type=restaurant&keyword=kebap&key=AIzaSyCeqtVQj1BrhCyNWb4esPOzop43lc5fYIY";
        // if something wrong

        loadRecyclerViewData();
    }

    private void loadRecyclerViewData() {
        final ProgressDialog dialog;
        dialog = new ProgressDialog(this);
        dialog.setMessage("Loading....");
        dialog.show();
        StringRequest request = new StringRequest(URL_DATA, new Response.Listener<String>() {
            @Override
            public void onResponse(String string) {
                try {
                    JSONObject object = new JSONObject(string);
                    JSONArray places = object.getJSONArray("results");

                    for (int i = 0; i < places.length(); ++i) {
                        JSONObject pl = places.getJSONObject(i);
                        ListItem item = new ListItem(
                                pl.getString("name"),
                                pl.getString("vicinity"),
                                isOpenFun(pl.getString("opening_hours")),
                                ""
                        );
                        listItems.add(item);
                        names.add(pl.getString("name"));
                        addresses.add(pl.getString("vicinity"));

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(getApplicationContext(), "Some error occurred!!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        RequestQueue rQueue = Volley.newRequestQueue(MainActivity.this);
        rQueue.add(request);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                RecyclerView.Adapter adapter = new ListAdapter(listItems, getApplicationContext());
                recyclerView.setAdapter(adapter);
                dialog.dismiss();

            }
        }, 3000);



    }

    public String isOpenFun(String isOpen) {
        char letter = isOpen.charAt(12);
        if (letter == 't') {
            return "Otwarte";
        } else {
            return "Zamknięte";
        }
    }

    private boolean checkPermissions() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || !isNetworkAvailable()) {
            Intent intentCheck = new Intent(MainActivity.this, CheckActivity.class);
            startActivityForResult(intentCheck, 1);
            return false;
        }
        return true;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void getLocationPermission() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    public Location getLocation() {Button but = findViewById(R.id.mapButton);
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            Location lastKnownLocationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocationGPS != null) {
                return lastKnownLocationGPS;
            } else {
                Location loc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                return loc;
            }
        } else {
            return null;
        }
    }

    public void distance(MenuItem item) {
        listItems.clear();
        names.clear();
        addresses.clear();

        switch (item.getItemId()) {
            case R.id.km1:
                searchDistance = 1000;
                break;
            case R.id.km2:
                searchDistance = 2000;
                break;
            case R.id.km3:
                searchDistance = 3000;
            default:
        }
        setLink();
    }

}
