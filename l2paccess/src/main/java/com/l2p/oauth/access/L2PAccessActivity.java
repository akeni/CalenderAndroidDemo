package com.l2p.oauth.access;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.l2p.oauth.access.httpclient.L2PHttpClient;
import com.l2p.oauth.access.httpclient.OkL2PHttpClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A sample activity which illustrates the binding to the Token service and calling L2P API
 * functions
 */
public class L2PAccessActivity extends ActionBarActivity {

    private Button accessL2PButton;
    private TextView accessL2PApiTextView;
    private TextView accessL2PApiResultTextView;
    private ProgressBar accessingL2PApiProgress;
    private L2PHttpClient l2PHttpClient;
    private Spinner apiSpinner;
    private static final String L2P_API_ENDPOINT = "https://www3.elearning.rwth-aachen.de/_vti_bin/l2pservices/api.svc/v1/";
    private static final String L2P_API_VIEW_ALL_COURSE_INFO = "viewAllCourseInfoAllCourseInfo";
    private static final String L2P_API_PING = "Ping";
    private static final String TAG = L2PAccessActivity.class.getName();
    private OAuthTokenService tokenService;
    private boolean tokenServiceBound = false;
    private String accessToken;

    private ServiceConnection oauthTokenServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            OAuthTokenService.OAuthTokenServiceBinder binder = (OAuthTokenService.OAuthTokenServiceBinder) service;
            tokenService = binder.getService();
            tokenServiceBound = true;
            Log.i(TAG, "Bound to Token service");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            tokenServiceBound = false;
            Log.i(TAG, "UnBound from Token service");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_l2p);
        accessL2PButton = (Button) findViewById(R.id.button_accessL2P);
        accessingL2PApiProgress = (ProgressBar) findViewById(R.id.accessL2PAPIProgressBar);
        accessL2PApiTextView = (TextView) findViewById(R.id.accessL2PAPItext);
        accessL2PApiResultTextView = (TextView) findViewById(R.id.accessL2PApiResult);
        accessL2PApiResultTextView.setMovementMethod(new ScrollingMovementMethod());
        apiSpinner = (Spinner) findViewById(R.id.api_spinner);
        accessL2PButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tokenServiceBound) {
                    accessToken = tokenService.getL2PAccessToken();
                    if (apiSpinner.getSelectedItem() != null)
                        startAccessL2PTask();
                    else
                        Log.i(TAG, "No API function call selected");
                } else {
                    Log.i(TAG, "Not yet bound to token service!");
                }
            }
        });
        l2PHttpClient = new OkL2PHttpClient();
        // bind to the token service
        if(!tokenServiceBound) {
            Log.i(TAG,"Binding to token service");
            Intent intent = new Intent(this, OAuthTokenService.class);
            bindService(intent, oauthTokenServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
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

    private void startAccessL2PTask() {
        new L2PAccessTask().execute();
    }

    private class L2PAccessTask extends AsyncTask<Void, Void, Void> {

        private String apiFunctionName;
        private String apiFunctionCallResult;

        L2PAccessTask() {
            this.apiFunctionName = "";
            this.apiFunctionCallResult = "...";
        }

        @Override
        protected void onPreExecute() {
            if (String.valueOf(apiSpinner.getSelectedItem()).equals("View All Course Info"))
                apiFunctionName = L2P_API_VIEW_ALL_COURSE_INFO;
            else
                apiFunctionName = L2P_API_PING;
            accessingL2PApiProgress.setVisibility(View.VISIBLE);
            accessL2PApiTextView.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            apiFunctionCallResult = accessL2PApi(apiFunctionName);
            Log.i(TAG, apiFunctionCallResult);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
           accessingL2PApiProgress.setVisibility(View.INVISIBLE);
           accessL2PApiTextView.setVisibility(View.INVISIBLE);
           accessL2PApiResultTextView.setText(apiFunctionCallResult);
           super.onPostExecute(aVoid);
        }
    }

    private String accessL2PApi(String apiFunctionName) {
        try {
            if (accessToken.isEmpty())
                return "You need to authorize your application!";
            String requestURL = L2P_API_ENDPOINT + (apiFunctionName.equals(L2P_API_VIEW_ALL_COURSE_INFO) ? L2P_API_VIEW_ALL_COURSE_INFO : L2P_API_PING);
            Map<String, String> postBodyParameters = new HashMap<>();
            postBodyParameters.put("accessToken", accessToken);
            if (!apiFunctionName.equals(L2P_API_VIEW_ALL_COURSE_INFO))
                postBodyParameters.put("p", "TestingApi");
            return l2PHttpClient.makeGETRequest(postBodyParameters, requestURL);
        }catch(IOException exception){
            return "Looks like L2P is being very friendly at the moment. No Result!";
        }
    }
}