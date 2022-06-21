package com.example.afinal;

import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
class http{
    public String makeServiceCall(String reqUrl) {
        String response = null;
        try {
            URL url = new URL(reqUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            // read the response
            InputStream in = new BufferedInputStream(conn.getInputStream());
            response = convertStreamToString(in);
        } catch (MalformedURLException e) {
        } catch (ProtocolException e) {
        } catch (IOException e) {
        } catch (Exception e) {
        }
        return response;
    }

    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
public class MainActivity extends AppCompatActivity {
    public static String url;
    public static String ApiKey = "";//google map api key
    private static String TAG = "MainActivity";//Log
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    public FusedLocationProviderClient locaClient;
    public Location currLoc;
    private double lat = 0;//latitude
    private double lng = 0;//longitude
    private RecyclerViewAdapter ListAdapter;
    private ArrayList<HashMap<String, String>> datalist;
    private RecyclerView listdisplay;
    private Button searchbtn;
    private EditText searchInfo;
    private boolean locationPermissionGranted;
    private boolean locDone;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //layout
        listdisplay = findViewById(R.id.loc_result);
        searchbtn = findViewById(R.id.search_btn);
        searchInfo = findViewById(R.id.search);
        //Set button listener
        searchbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(searchInfo.getText().toString() != null) {
                    datalist.clear();
                    setURL(searchInfo.getText().toString());
                }else{
                    setURL();
                }
            }
        });
        //parameter  setting
        locationPermissionGranted = false;
        datalist = new ArrayList<>();
        ListAdapter = new RecyclerViewAdapter();
        locaClient = LocationServices.getFusedLocationProviderClient(this);
        locDone = false;
    }
    public void onResume() {
        super.onResume();
        //claer current data
        datalist.clear();
        //start funtion
        getLastLocation();
    }

    @SuppressWarnings("MissingPermission")
    public void getLastLocation() {
        locaClient.getLastLocation()
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            currLoc = task.getResult();
                            lat = currLoc.getLatitude();
                            lng = currLoc.getLongitude();
                            //to avoid the async problem therefore call next function here
                            setURL();
                        } else {
                            getLocationPermission();
                            Log.w(TAG, "locatoin:exception", task.getException());
                        }
                    }
                });

    }
    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
            }
        }
    }
    //if user have not input specify word use this function to set the url
    private void setURL(){
        url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + lat + "," + lng + "&rankby=distance&key=" + ApiKey;
        getjson json = new getjson();
        Log.i(TAG, "URL " + url);
        json.execute();
    }
    //if user input specify word to search will call this function to set the url
    private void setURL(String search){
        url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?radius=1500&keyword=" + search + "&location=" + lat + "," + lng + "6&radius=10000&key=" + ApiKey;
        getjson json = new getjson();
        Log.i(TAG, "URL " + url);
        json.execute();
    }
    //when json have next page token it will call this function to set the url;
    private void nextpage(String pagetoken){
        url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?pagetoken=" + pagetoken + "&key=" + ApiKey;
        getjson json = new getjson();
        Log.i(TAG, "URL " + url);
        json.execute();

    }
    //send the request and get the json string from http respone.
    private class getjson extends AsyncTask<Void, Void, String>{
        @Override
        protected void onPostExecute(String s) {
            try {
                if(s != null) {
                    loadtolist(s);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        @Override
        protected String doInBackground(Void... voids) {
            http connector = new http();
            String js = connector.makeServiceCall(url);
            //Log.i(TAG, "Json" + js);
            return js;
        }
    }
    private void loadtolist(String Json) throws JSONException {
        //Log.i(TAG, "Json" + Json);
        JSONObject header = new JSONObject(Json);
        JSONArray result = header.getJSONArray("results");
        for(int i=0;i<result.length();i++){
            HashMap<String, String> data = new HashMap<>();
            JSONObject index = result.getJSONObject(i);
            JSONObject geometry = index.getJSONObject("geometry");
            JSONObject location = geometry.getJSONObject("location");
            double latitude = location.getDouble("lat");
            double longitude = location.getDouble("lng");
            String name = index.getString("name");
            String address = index.getString("vicinity");;

            data.put("latitude",Double.toString(latitude));
            data.put("longitude",Double.toString(longitude));
            data.put("name",name);
            data.put("address",address);
            datalist.add(data);
        }
        //when next page token is not null , it will call the nextpage function and send request to get next result
        //otherwise will set the setadapter and show the result to user.
        if(!header.isNull("next_page_token")){
            nextpage(header.getString("next_page_token"));
        }else {
            listdisplay.setLayoutManager(new LinearLayoutManager(this));
            listdisplay.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
            listdisplay.setAdapter(ListAdapter);
        }
    }
    private class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{
        class ViewHolder extends RecyclerView.ViewHolder{
            private TextView nameTEXT,addrTEXT;
            private Button show_loc, path_to_loc;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                nameTEXT = itemView.findViewById(R.id.nameText);
                addrTEXT = itemView.findViewById(R.id.addrText);
                show_loc = itemView.findViewById(R.id.show_loc);
                path_to_loc = itemView.findViewById(R.id.path_to_loc);
            }
        }
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item,parent,false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.nameTEXT.setText(datalist.get(position).get("name"));
            holder.addrTEXT.setText(datalist.get(position).get("address"));

            holder.show_loc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Log.i(TAG, "geo:" + datalist.get(position).get("latitude") + "," + datalist.get(position).get("longitude"));
                    String geo = "geo:" + datalist.get(position).get("latitude") + "," + datalist.get(position).get("longitude") + "?z=10&q=" + datalist.get(position).get("name") + datalist.get(position).get("address");
                    Uri gmmIntentUri = Uri.parse(geo);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);

                }
            });
            holder.path_to_loc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //String geo = "google.navigation:q=" + datalist.get(position).get("latitude") + "," + datalist.get(position).get("longitude");
                    String geo = "google.navigation:q=" + datalist.get(position).get("name") + datalist.get(position).get("address");
                    Log.i(TAG, "path " + geo);
                    Uri gmmIntentUri = Uri.parse(geo);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);
                }
            });
        }
        @Override
        public int getItemCount() {
            return datalist.size();
        }
    }
}