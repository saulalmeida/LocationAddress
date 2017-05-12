package com.dd.exa.sam.locationaddress;

import android.content.Intent;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener{

    protected static final String TAG = "main-activity";
    protected final String ADDRESS_REQUESTED_KEY =  "address-request-pending";
    protected static String LOCATION_ADDRESS_KEY = "location-address";
    protected  GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;
    protected boolean mAdressRequested;
    protected String mAdressOutput;
    private AddressResultReceiver mResultReceiver;
    protected TextView mLocationAddressTextView;
    ProgressBar mProgressBar;
    Button mFetchAdressButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mResultReceiver = new AddressResultReceiver(new Handler());

        mLocationAddressTextView = (TextView)findViewById(R.id.location_address_view);

        mProgressBar = (ProgressBar)findViewById(R.id.progress_bar);
        mFetchAdressButton= (Button)findViewById(R.id.fetch_address_button);

        mAdressRequested = false;
        mAdressOutput = "";
        updatesValuesFromBundle(savedInstanceState);
        updateUIWidgets();
        buildGoogleApiClient();

    }

    private void updatesValuesFromBundle(Bundle savedInstanceState){
        if (savedInstanceState != null){
            if (savedInstanceState.keySet().contains(ADDRESS_REQUESTED_KEY)){
                mAdressRequested = savedInstanceState.getBoolean(ADDRESS_REQUESTED_KEY);
            }

            if (savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)){
                mAdressOutput = savedInstanceState.getString(LOCATION_ADDRESS_KEY);
                displayAddressOutput();
            }
        }
    }

    protected synchronized  void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    public void fetchAddressButtonHandler(View view){
        if (mGoogleApiClient.isConnected()&& mLastLocation != null){
            startIntentService();
        }
        mAdressRequested = true;
        updateUIWidgets();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation!= null){
            if (!Geocoder.isPresent()){
                Toast.makeText(this,R.string.no_geocoder_available,Toast.LENGTH_LONG).show();
                return;
            }

            if (mAdressRequested){
                startIntentService();

            }
        }


    }

    protected void startIntentService(){
        Intent intent = new Intent(this,FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA,mLastLocation);
        startService(intent);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG,"onConnetion Suspended");
        mGoogleApiClient.connect();

    }

    protected void displayAddressOutput(){
        mLocationAddressTextView.setText(mAdressOutput);

    }
    private void updateUIWidgets(){
        if (mAdressRequested){
            mProgressBar.setVisibility(ProgressBar.VISIBLE);
            mFetchAdressButton.setEnabled(false);
        }else {
            mProgressBar.setVisibility(ProgressBar.GONE);
            mFetchAdressButton.setEnabled(true);
        }
    }


    public void showToast(String text){
        Toast.makeText(this, text,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(ADDRESS_REQUESTED_KEY,mAdressRequested);
        savedInstanceState.putString(LOCATION_ADDRESS_KEY,mAdressOutput);
        super.onSaveInstanceState(savedInstanceState);
    }


    class AddressResultReceiver extends ResultReceiver{
        public AddressResultReceiver(Handler handler){
            super(handler);
        }

        protected void onReceiveResult(int resultCode, Bundle resultData){
            mAdressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            displayAddressOutput();
            if (resultCode == Constants.SUCCES_RESULT){
                showToast(getString(R.string.address_found));
            }
            mAdressRequested =false;
            updateUIWidgets();
        }
    }



    @Override
    public void onConnectionFailed( ConnectionResult connectionResult) {
    }



}
