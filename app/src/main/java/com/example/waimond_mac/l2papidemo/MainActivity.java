package com.example.waimond_mac.l2papidemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.alamkanak.weekview.DateTimeInterpreter;
import com.alamkanak.weekview.WeekView;
import com.alamkanak.weekview.WeekViewEvent;
import com.l2p.oauth.access.OAuthTokenService;
import com.l2p.oauth.access.httpclient.L2PHttpClient;
import com.l2p.oauth.access.httpclient.OkL2PHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class MainActivity extends ActionBarActivity implements  AsyncResponse {

    //"https://demo2.elearning.rwth-aachen.de/_vti_bin/l2pservices/api.svc/v1/";//
    private static final String L2P_API_ENDPOINT = "https://www3.elearning.rwth-aachen.de/_vti_bin/l2pservices/api.svc/v1/";
    private static final String L2P_API_VIEW_ALL_COURSE_INFO = "viewAllCourseInfo";
    private static final String L2P_API_VIEW_ALL_COURSE_EVENTS = "viewAllCourseEvents";
    private static final String L2P_API_VIEW_COURSE_EVENTS = "viewCourseEvents";
    private static final String L2P_API_ADD_COURSE_EVENTS = "addCourseEvents";
    private static final String L2P_API_DELETE_COURSE_EVENTS = "deleteCourseEvent";
    private static final String L2P_API_PING = "Ping";


    private String TAG = MainActivity.class.getName();
    private L2PHttpClient l2PHttpClient;
    private OAuthTokenService tokenService;
    private boolean tokenServiceBound = false;
    private String accessToken;
    L2PAccessTask asyncTask =new L2PAccessTask();
    private Button getData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        asyncTask.delegate = this;
        getData = (Button) findViewById(R.id.button);
        l2PHttpClient = new OkL2PHttpClient();
        // bind to the token service
        if(!tokenServiceBound) {
            Log.i(TAG, "Binding to token service");
            Intent intent = new Intent(this, OAuthTokenService.class);
            bindService(intent, oauthTokenServiceConnection, Context.BIND_AUTO_CREATE);
        }



        getData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tokenServiceBound) {
                    accessToken = tokenService.getL2PAccessToken();
                    Log.d(TAG,"accessToken: "+accessToken);
                    startAccessL2PTask(L2P_API_VIEW_ALL_COURSE_EVENTS);
                }
            }
        });


    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy called service bound : " + tokenServiceBound);
        if (tokenServiceBound) {
            unbindService(oauthTokenServiceConnection);
            tokenServiceBound = false;
        }
    }

    private ServiceConnection oauthTokenServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            OAuthTokenService.OAuthTokenServiceBinder binder = (OAuthTokenService.OAuthTokenServiceBinder) service;
            tokenService = binder.getService();
            tokenServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            tokenServiceBound = false;
            Log.i(TAG, "UnBound from Token service");
        }
    };
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void startAccessL2PTask(String command) {
        asyncTask.execute(L2P_API_VIEW_ALL_COURSE_EVENTS);

    }


    private class L2PAccessTask extends AsyncTask<String, Void, String> {

        public AsyncResponse delegate=null;
        private String apiFunctionName;
        private String apiFunctionCallResult;

        L2PAccessTask() {
            this.apiFunctionName = "";
            this.apiFunctionCallResult = "...";
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG,"In preExecute");
        }

        @Override
        protected String doInBackground(String... command) {
            apiFunctionCallResult = accessL2PApi(command[0]);
            Log.d(TAG, "Do in background finished: " + apiFunctionCallResult);
            return apiFunctionCallResult;
        }

        @Override
        protected void onPostExecute(String aVoid) {
            super.onPostExecute(aVoid);
            Log.d(TAG,"onPostExecute");
            delegate.processFinish(aVoid);
        }


    }

    public void processFinish(String output){
        //this you will received result fired from async class of onPostExecute(result) method.
        Intent intent = new Intent(this, CalendarView.class);
        intent.putExtra("data",output);
        startActivity(intent);
    }

    private String accessL2PApi(String apiFunctionName) {
        try {
            if (accessToken.isEmpty())
                return "You need to authorize your application!";
            String requestURL;
            Map<String, String> postBodyParameters = new HashMap<>();
            switch (apiFunctionName) {
                case L2P_API_VIEW_ALL_COURSE_INFO:
                    requestURL = L2P_API_ENDPOINT + L2P_API_VIEW_ALL_COURSE_INFO;
                    postBodyParameters.put("accessToken", accessToken);
                    break;
                case L2P_API_VIEW_ALL_COURSE_EVENTS:
                    requestURL = L2P_API_ENDPOINT + L2P_API_VIEW_ALL_COURSE_EVENTS;
                    postBodyParameters.put("accessToken", accessToken);
                    break;
                default:
                    requestURL = L2P_API_ENDPOINT + L2P_API_PING;
                    postBodyParameters.put("p", "TestingApi");
            }
            Log.d(TAG,"returning getRequest");
            return l2PHttpClient.makeGETRequest(postBodyParameters, requestURL);
        }catch(IOException exception){
            return "Looks like L2P is being very friendly at the moment. No Result!";
        }
    }

}


