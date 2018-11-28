package com.example.caroline.airportnav;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.caroline.airportnav.Bean.Message;
import com.example.caroline.airportnav.configuration.Config;
import com.example.caroline.airportnav.utilities.TextToSpeech;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Modules.Route;
import ai.api.android.AIConfiguration;
import ai.api.android.GsonFactory;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import ai.api.ui.AIButton;

import Modules.DirectionFinderListener;
import Modules.Route;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class VoiceAssistantActivity extends AppCompatActivity implements AIButton.AIButtonListener,OnMapReadyCallback, DirectionFinderListener  {

    public static final String TAG = VoiceAssistantActivity.class.getName();

    private AIButton aiButton;
    private Gson gson = GsonFactory.getGson();
    private TextView resultTextView;
    private int MY_RECORD_AUDIO_PERMISSION = 5;
    private static TextToSpeech textToSpeech;
    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageAdapter;
    private List<Message> messageList = new ArrayList<>();
    protected LinearLayoutManager mLayoutManager;
    public static final String MyPREFERENCES = "MyPrefs" ;
    public static final String AIRLINES = "airlines";
    public static final String GATE = "gate";
    public static final String DEPARTURE_TIME = "departTime";
    public static final String TERMINAL = "terminal";
    public static final String FLIGHT_NUMBER = "flightNumber";
    SharedPreferences sharedpreferences;
    /******************************maps*************************************/

    private GoogleMap mMap;
    private Button btnFindPath;
    private EditText etOrigin;
    private EditText etDestination;
    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();
    private ProgressDialog progressDialog;
    ArrayList<LatLng> listpoints;
    /*******************************maps*************************************/

    /******************************navigate***********************************/

    private Button navigateButton;
    private FusedLocationProviderClient mFusedLocationClient;
    private static final int REQUEST_LOCATION = 2;
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    private String currentAction = null;
    private String locString = null;
    private String destString = null;
    private static final String CHECK_IN = "checkin";
    private static final String SECURITY_CHECK = "security_check";
    private static final String BOARDING = "boarding";


    /******************************navigate end*******************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_assistant);

        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

        /* APN: Temporary hardcoding shared preferences
           For testing

        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(AIRLINES,"Delta Air Lines");
        editor.putString(GATE,"40");
        editor.putString(DEPARTURE_TIME,"2018-11-29T06:00:00.000");
        editor.putString(TERMINAL,"1");
        editor.putString(FLIGHT_NUMBER,"1210");
        editor.commit();*/

        /***********************************from maps******************************/

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        listpoints = new ArrayList<>();
        /**********************************from maps end*****************/


        /********************************navigate intent*******************/

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        getCurrentLocation();
        navigateButton = (Button) findViewById(R.id.navigateButton);
        navigateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openNavigation(getApplicationContext());
            }
        });
        /*********************************navigate intent end**************/

        resultTextView = (TextView) findViewById(R.id.resultTextView);
        aiButton = (AIButton) findViewById(R.id.imageButton);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED) {
                // put your code for Version>=Marshmallow
            } else {
                if (shouldShowRequestPermissionRationale(android.Manifest.permission.RECORD_AUDIO)) {
                    Toast.makeText(this,
                            "App required access to audio", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO
                },MY_RECORD_AUDIO_PERMISSION);
            }

        } else {

        }

        final AIConfiguration config = new AIConfiguration(Config.ACCESS_TOKEN,
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        config.setRecognizerStartSound(getResources().openRawResourceFd(R.raw.test_start));
        config.setRecognizerStopSound(getResources().openRawResourceFd(R.raw.test_stop));
        config.setRecognizerCancelSound(getResources().openRawResourceFd(R.raw.test_cancel));

        aiButton.initialize(config);
        aiButton.setResultsListener(this);

        mMessageRecycler = (RecyclerView) findViewById(R.id.reyclerview_message_list);
        mMessageAdapter = new MessageListAdapter(this, messageList);

        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mMessageRecycler.setLayoutManager(mLayoutManager);
        mMessageRecycler.setAdapter(mMessageAdapter);
        
        String sessionData = sharedpreferences.getString(GATE,"defaultGate");
        resultTextView.setText(sessionData);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_RECORD_AUDIO_PERMISSION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "Application will not have audio on record", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResult(AIResponse response) {
        Result result = response.getResult();
        String parameterString = "";
        if (result.getParameters() != null && !result.getParameters().isEmpty()) {
            for (final Map.Entry<String, JsonElement> entry : result.getParameters().entrySet()) {
                parameterString += "(" + entry.getKey() + ", " + entry.getValue() + ") ";
            }
        }

        // Show results in TextView.
        //String text = "Query:" + result.getResolvedQuery() + "\n Action: " + result.getAction() + "\n Parameters: " + parameterString;
        String Query = result.getResolvedQuery();
        String action = "";
        if(result!= null && result.getContexts() != null && result.getContexts().get(0)!=null) {
            Map<String, JsonElement> context = result.getContexts().get(0).getParameters();
            if (context.containsKey("action")) {
                action = context.get("action").toString();
            }
            resultTextView.setText(action);

            /* APN:
                To add below code to replot map based on action
                currentAction=action;
                getCurrentLocation();
                drawMap();
            */
        }

        String speech = response.getResult().getFulfillment().getSpeech();
        messageList.add(new Message(Query,"User"));
        messageList.add(new Message(speech,"Agent"));
        mMessageAdapter.notifyDataSetChanged();
        mMessageRecycler.scrollToPosition(mMessageAdapter.getItemCount() - 1);
        TextToSpeech.speak(speech);


    }

    @Override
    public void onError(final AIError error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onError");
                resultTextView.setText(error.toString());
            }
        });

    }

    @Override
    public void onCancelled() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onCancelled");
                resultTextView.setText("");
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        aiButton.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        aiButton.resume();
    }
    public class MessageListAdapter extends RecyclerView.Adapter {
        private Context mContext;
        private List<Message> mMessageList;
        private static final int VIEW_TYPE_MESSAGE_SENT = 1;
        private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

        public MessageListAdapter(Context context, List<Message> messageList) {
            mContext = context;
            mMessageList = messageList;
        }

        @Override
        public int getItemViewType(int position) {
            Message message = (Message) mMessageList.get(position);

            if (message!= null && message.getSender()!=null && !message.getSender().equals("Agent")){
                // If the current user is the sender of the message
                return VIEW_TYPE_MESSAGE_SENT;
            } else {
                // If some other user sent the message
                return VIEW_TYPE_MESSAGE_RECEIVED;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            View view;

            if (viewType == VIEW_TYPE_MESSAGE_SENT) {
                view = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.item_message_sent,viewGroup,false);
                return new SentMessageHolder(view);
            } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
                view = LayoutInflater.from(VoiceAssistantActivity.this)
                        .inflate(R.layout.item_message_received, viewGroup, false);
                return new ReceivedMessageHolder(view);
            }

            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Message message = mMessageList.get(position);

            switch (holder.getItemViewType()) {
                case VIEW_TYPE_MESSAGE_SENT:
                    ((SentMessageHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_MESSAGE_RECEIVED:
                    ((ReceivedMessageHolder) holder).bind(message);
            }
        }

        @Override
        public int getItemCount() {
            return mMessageList.size();
        }
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            messageText = (TextView) itemView.findViewById(R.id.text_message_body);

        }
        void bind(Message message) {
            messageText.setText(message.getMessage());
        }
    }
    private class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        SentMessageHolder(View itemView) {
            super(itemView);
            messageText = (TextView) itemView.findViewById(R.id.text_message_body);

        }
        void bind(Message message) {
            messageText.setText(message.getMessage());
        }
    }

    /********************************************Maps functions**********************************************/
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker for the user's current location.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        int i=0;
        while(locString == null){
            Log.d("APN", "locstring is null");
            getCurrentLocation();
            i++;
            if(i>=5) break;
        }
    }


    private String getRequestUrl(LatLng origin, LatLng destination) {
        // value of origin
        String string_org = "origin=" + origin.latitude +","+origin.longitude;
        // value of destination
        String string_dest = "destination=" + destination.latitude +","+destination.longitude;
        // value of enabling the sensor
        String sensor = "sensor=false";
        //mode for direction
        String mode = "mode=walking";
        String key = "";
        String param = string_org + "&"+ string_dest + "&" + sensor + "&" + mode + "&key="+key;
        String output= "json";
        String url = "https://maps.googleapis.com/maps/api/directions/" + output +"?"+ param;

        return url;
    }


    private String requestDirection(String reqUrl) throws IOException {
        String responseString = "";
        InputStream inputStream = null;
        HttpURLConnection httpURLConnection = null;
        try{
            URL url = new URL(reqUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();

            //Get the response result
            inputStream = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuffer stringBuffer = new StringBuffer();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }

            responseString = stringBuffer.toString();
            bufferedReader.close();
            inputStreamReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            httpURLConnection.disconnect();
        }
        return responseString;
    }

    @Override
    public void onDirectionFinderStart() {
        progressDialog = ProgressDialog.show(this, "Please wait.",
                "Finding direction..!", true);

        if (originMarkers != null) {
            for (Marker marker : originMarkers) {
                marker.remove();
            }
        }

        if (destinationMarkers != null) {
            for (Marker marker : destinationMarkers) {
                marker.remove();
            }
        }

        if (polylinePaths != null) {
            for (Polyline polyline:polylinePaths ) {
                polyline.remove();
            }
        }
    }

    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {
        progressDialog.dismiss();
        polylinePaths = new ArrayList<>();
        originMarkers = new ArrayList<>();
        destinationMarkers = new ArrayList<>();

        for (Route route : routes) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 16));

            originMarkers.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.common_google_signin_btn_icon_dark))
                    .title(route.startAddress)
                    .position(route.startLocation)));
            destinationMarkers.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.common_google_signin_btn_icon_dark))
                    .title(route.endAddress)
                    .position(route.endLocation)));

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(Color.BLUE).
                    width(10);

            for (int i = 0; i < route.points.size(); i++)
                polylineOptions.add(route.points.get(i));

            polylinePaths.add(mMap.addPolyline(polylineOptions));
        }
    }



    public class TaskRequestDirections extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... strings) {

            String responseString = "";

            try {
                responseString = requestDirection(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return  responseString;


        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //json parsing
            VoiceAssistantActivity.TaskParser taskParser = new VoiceAssistantActivity.TaskParser();
            taskParser.execute(s);

        }
    }

    public class TaskParser extends AsyncTask<String, Void, List<List<HashMap<String, String>>> > {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jsonObject = null;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jsonObject = new JSONObject(strings[0]);
                DirectionsParser directionsParser = new DirectionsParser();
                routes = directionsParser.parse(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            //Get list route and display it into the map

            ArrayList points = null;

            PolylineOptions polylineOptions = null;

            for (List<HashMap<String, String>> path : lists) {
                points = new ArrayList();
                polylineOptions = new PolylineOptions();

                for (HashMap<String, String> point : path) {
                    double lat = Double.parseDouble(point.get("lat"));
                    double lon = Double.parseDouble(point.get("lon"));

                    points.add(new LatLng(lat,lon));
                }

                polylineOptions.addAll(points);
                polylineOptions.width(15);
                polylineOptions.color(Color.BLUE);
                polylineOptions.geodesic(true);
            }

            if (polylineOptions!=null) {
                mMap.addPolyline(polylineOptions);
            } else {
                Toast.makeText(getApplicationContext(), "Direction not found!", Toast.LENGTH_SHORT).show();
            }

        }
    }
/*MapsActivity end******************************************************************************************************/

    /*****************************************************navigate*************************************************/

    protected void createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(VoiceAssistantActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }
    private boolean getCurrentLocation(){
        createLocationRequest();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("APN", "Inside if");
            // Check Permissions Now

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        } else {
            Log.d("APN", "inside the else");
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            Log.d("APN", "Inside onSuccess");
                            if (location != null) {
                                // Logic to handle location object
                                Log.d("APN", "location is not null");
                                locString = Location.convert(location.getLatitude(), Location.FORMAT_DEGREES);
                                locString += "," + Location.convert(location.getLongitude(), Location.FORMAT_DEGREES);
                                Log.d("APN", "locstring is: "+locString);
                                drawMap();

                            }
                        }
                    });
        }
        if (locString == null){
            Log.d("APN", "The location is null");
            return false;
        }
        Log.d("APN", "This is locString" + locString);
        return true;
    }

    protected void drawMap(){

        Log.d("APN", "Got locstring");
        String[] coordinates = locString.split(",");
        LatLng origin = new LatLng(Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1]) );

        /*APN
          Hardcoding currentAction
          For testing

        currentAction = BOARDING; */

        if(currentAction == null) return;
        String address = "Terminal " +sharedpreferences.getString(TERMINAL,"1") +", San Francisco International Airport ";
        if(currentAction.equals(CHECK_IN)){
            //destination is airline name
            //address += sharedpreferences.getString(AIRLINES,"airlines");
        } else if(currentAction.equals(BOARDING)){
            //destination is gate
            address += "Gate " + sharedpreferences.getString(GATE,"1");
        } else if(currentAction.equals(SECURITY_CHECK)){
            //improvise
        } else{

            return;
        }
        convertAddressToCoordinates(address, getApplicationContext());
        String[] destCoordinates = destString.split(",");
        LatLng destination = new LatLng(Double.parseDouble(destCoordinates[0]), Double.parseDouble(destCoordinates[1]) );

        listpoints.add(origin);
        listpoints.add(destination);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(origin);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

        MarkerOptions markerOptions2 = new MarkerOptions();
        markerOptions2.position(destination);
        markerOptions2.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        mMap.addMarker(markerOptions);
        mMap.addMarker(markerOptions2);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origin,16));
        //Create the URL to get request from first marker to second marker
        String url = getRequestUrl(listpoints.get(0), listpoints.get(1));
        VoiceAssistantActivity.TaskRequestDirections taskRequestDirections = new VoiceAssistantActivity.TaskRequestDirections();
        taskRequestDirections.execute(url);
    }

    protected void openNavigation(Context ctx)
    {if(currentAction == null) return;
        String address = "Terminal+" +sharedpreferences.getString(TERMINAL,"1") +",+San+Francisco+International+Airport";
        if(currentAction.equals(CHECK_IN)){
            //destination is airline name
           // address += "+"+sharedpreferences.getString(AIRLINES,"airlines");
        } else if(currentAction.equals(BOARDING)){
            //destination is gate
            address += "+Gate+" + sharedpreferences.getString(GATE,"1");
        } else if(currentAction.equals(SECURITY_CHECK)){
            //APN: to do
        } else {
            /*APN
             Pop up that action is not set or something
             */
            return;
        }
            String uri = "google.navigation:q="+address+"&mode=w";
            Log.d("APN", "the uri: "+uri);
            Uri gmmIntentUri = Uri.parse(uri);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
    }

    /* APN: currently, openMaps is not used
    protected void openMaps(){
        if(getCurrentLocation()) {
            // Create a Uri from an intent string. Use the result to create an Intent.
            Uri gmmIntentUri = Uri.parse("google.streetview:cbll="+locString);

            // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            // Make the Intent explicit by setting the Google Maps package
            mapIntent.setPackage("com.google.android.apps.maps");

            // Attempt to start an activity that can handle the Intent
            startActivity(mapIntent);
        }
    }*/

    public boolean convertAddressToCoordinates(String strAddress, Context ctx)
    {
        Log.d("APN", "inside convertAddress. Addr is: " + strAddress);
        Geocoder coder = new Geocoder(ctx);
        List<Address> address;

        try {
            address = coder.getFromLocationName(strAddress, 1);
            if (address == null) {
                Log.d("APN", "address is null");
                return false;
            }
            Address location = address.get(0);
            destString = Location.convert(location.getLatitude(), Location.FORMAT_DEGREES);
            destString += ",";
            destString += Location.convert(location.getLongitude(), Location.FORMAT_DEGREES);

            Log.d("APN", "deststring converted is: " + locString);
            if(destString == null) {
                Log.d("APN", "destString is null");
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /************************************************navigate end**********************************************/
}
