package com.l2p.oauth.access;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.l2p.oauth.access.httpclient.L2PHttpClient;
import com.l2p.oauth.access.httpclient.OkL2PHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class OAuthActivity extends ActionBarActivity {

    private Button initiateOAuthButton;
    private TextView refreshingTokensTextView;
    private ProgressBar refreshingTokensProgress;
    private L2PHttpClient httpClient;
    private ClientIdHandler clientIdHandler;
    private String deviceCode; // returned by the OAuth service
    private String accessToken; //returned by the OAuth service
    private String refreshToken; //returned by the OAuth service
    private int intervalForPollingOAuthTokens; // returned by the OAuth service
    private boolean authorizationInProgress;
    private static final String TAG = OAuthActivity.class.getName();
    public static final String SHARED_PREFERENCES_FILE_KEY = "com.l2p.oauth.access.shared_pref";
    private SharedPreferences sharedPreferences; // local storage of the application, for saving access and refresh tokens

    private enum TaskType {
        AUTHENTICATION_TASK,
        TOKEN_VALIDATION_TASK,
        TOKEN_FETCH_TASK
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oauth);
        httpClient = new OkL2PHttpClient();
        clientIdHandler = new ClientIdHandler(this);
        sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_FILE_KEY, Context.MODE_PRIVATE);
        initiateOAuthButton = (Button) findViewById(R.id.button_initiateOauth);
        refreshingTokensProgress = (ProgressBar) findViewById(R.id.refreshTokenProgressBar);
        refreshingTokensTextView = (TextView) findViewById(R.id.refreshTokenTextView);
        authorizationInProgress = false;
        initiateOAuthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sharedPreferences.getString(getString(R.string.oauth_refresh_token),"").isEmpty()) {
                    //fire up the oauth authentication task
                    startAuthorizationProcess();
                }else{
                   finishAuthorizationProcess();
                }
            }
        });
        startAuthorizationProcess();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_oauth, menu);
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
    protected void onPause() {
        super.onPause();
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
    protected void onResume() {
        // so the user came back after preferably authorizing our application
        // lets check if the user actually tried to authorize the application
        if(authorizationInProgress  && sharedPreferences.getString(getString(R.string.oauth_refresh_token),"").isEmpty())
            new OAuthTask(this,TaskType.TOKEN_FETCH_TASK).execute();
        super.onResume();
    }

    private void finishAuthorizationProcess(){
        finish();
    }

    private void startAuthorizationProcess(){
        new OAuthTask(this,TaskType.AUTHENTICATION_TASK).execute();
    }

    private class OAuthTask extends AsyncTask<Void, Void, Void> {

        private Activity activityReference;
        private TaskType taskType;

        OAuthTask(Activity activity, TaskType taskType){
            this.activityReference = activity;
            this.taskType = taskType;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String clientId = clientIdHandler.getApiClientID();
            if(!clientId.isEmpty()){
                try{
                    switch(taskType){
                        case AUTHENTICATION_TASK:
                            authorizeApplication(clientId);
                            break;
                        case TOKEN_VALIDATION_TASK:
                            if(!validateAccessToken(clientId)) {
                                // invalid access token received from Oauth server
                                accessToken = null;
                            }
                            break;
                        case TOKEN_FETCH_TASK:
                            if(!obtainTokensFromOAuthServer(clientId)) // the oauth server did not return an access token,
                                accessToken = null;
                            break;
                        default:
                            break;
                    }
                }catch (IOException|JSONException exception){
                    System.out.println(exception.getMessage());
                    authorizationInProgress = false;
                    accessToken = null;
                    Log.i(TAG,"OAuth was not successful, want to try again?");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(OAuthActivity.this, "OAuth was not successful, want to try again?", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }else{
                Log.i(TAG,"The client id in the text file is empty!");
            }
            return null;
        }
        @Override
        protected void onPreExecute() {
            switch (taskType){
                case AUTHENTICATION_TASK:
                    refreshingTokensProgress.setVisibility(View.VISIBLE);
                    refreshingTokensTextView.setText("OAuth in Progress...");
                    refreshingTokensTextView.setVisibility(View.VISIBLE);
                    break;
                case TOKEN_FETCH_TASK:
                    refreshingTokensProgress.setVisibility(View.VISIBLE);
                    refreshingTokensTextView.setText("Obtaining Tokens...");
                    refreshingTokensTextView.setVisibility(View.VISIBLE);
                    break;
                case TOKEN_VALIDATION_TASK:
                    refreshingTokensProgress.setVisibility(View.VISIBLE);
                    refreshingTokensTextView.setText("Validating Tokens...");
                    refreshingTokensTextView.setVisibility(View.VISIBLE);
                    break;
                default:
                    break;
            }
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            refreshingTokensProgress.setVisibility(View.INVISIBLE);
            refreshingTokensTextView.setVisibility(View.INVISIBLE);
            switch (taskType){
                case AUTHENTICATION_TASK:
                    break;
                case TOKEN_FETCH_TASK:
                    // update the tokens text view with the values
                    if(accessToken == null)
                        (Toast.makeText(activityReference, "Looks like your day got even worse, got no tokens from OAuth server!", Toast.LENGTH_SHORT)).show();
                    else
                        new OAuthTask(activityReference,TaskType.TOKEN_VALIDATION_TASK).execute(); // launch the validation task
                    break;
                case TOKEN_VALIDATION_TASK:
                    // access token validated message
                    if(accessToken!=null) { // if the validation of access token was successful then store it in our app's local storage
                        // store the tokens in the shared preferences for later use
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(getString(R.string.oauth_access_token), accessToken);
                        editor.putString(getString(R.string.oauth_refresh_token), refreshToken);
                        editor.commit();
                        finishAuthorizationProcess(); // finish off the authorization process, OAUTH has been successful and the tokens stored
                    }
                    break;
                default:
                    break;
            }
            super.onPostExecute(aVoid);
        }
    }

    private void authorizeApplication(String clientId) throws IOException,JSONException {
        Map<String,String> postBodyParameters = new HashMap<>();
        postBodyParameters.put(getString(R.string.oauth_client_id),clientId);
        postBodyParameters.put(getString(R.string.oauth_scope_of_acccess),getString(R.string.scope_of_access));
        JSONObject responseJSON = new JSONObject(httpClient.makePOSTRequest(postBodyParameters,getString(R.string.oauth_endpoint)));
        Log.i(TAG, responseJSON.toString());
        deviceCode = responseJSON.getString("device_code");
        intervalForPollingOAuthTokens = responseJSON.getInt(getString(R.string.oauth_polling_interval));
        Uri uri = Uri.parse(responseJSON.getString(getString(R.string.oauth_verification_url)) + "?q=verify&d=" + responseJSON.getString(getString(R.string.oauth_user_code))); // encode the verification url
        // along with the user code for more details look at the OAuth documentation of the ITC RWTH Aachen
        authorizationInProgress=true;
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    private boolean obtainTokensFromOAuthServer(String clientId) throws IOException, JSONException{
        Map<String,String> postBodyParameters = new HashMap<>();
        postBodyParameters.put(getString(R.string.oauth_client_id),clientId);
        postBodyParameters.put(getString(R.string.oauth_device_code),deviceCode);
        postBodyParameters.put(getString(R.string.oauth_grant_type),getString(R.string.oauth_device_grant_type));
        JSONObject responseJSON = new JSONObject(httpClient.makePOSTRequest(postBodyParameters,getString(R.string.oauth_token_endpoint)));
        Log.i(TAG, responseJSON.toString());
        if(!responseJSON.optString(getString(R.string.oauth_access_token)).isEmpty()) {
            accessToken = responseJSON.getString(getString(R.string.oauth_access_token));
            refreshToken = responseJSON.getString(getString(R.string.oauth_refresh_token));
            return true;
        }
        else{
            return false;
        }
    }

    private boolean validateAccessToken(String clientId)throws IOException,JSONException{
        if(accessToken == null)
            return false;
        Map<String,String> postBodyParameters = new HashMap<>();
        postBodyParameters.put(getString(R.string.oauth_client_id),clientId);
        postBodyParameters.put(getString(R.string.oauth_access_token),accessToken);
        JSONObject responseJSON = new JSONObject(httpClient.makePOSTRequest(postBodyParameters,getString(R.string.oauth_token_validation_endpoint)));
        Log.i(TAG, responseJSON.toString());
        if(responseJSON.getString("status").equals("ok") && responseJSON.getString("state").equals("valid"))
            return true;
        else
            return false;
    }
}
