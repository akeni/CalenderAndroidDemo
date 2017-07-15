package com.l2p.oauth.access;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.l2p.oauth.access.httpclient.L2PHttpClient;
import com.l2p.oauth.access.httpclient.OkL2PHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * This service is responsible for refreshing the access token for L2P. Any activity which wants to access L2P should
 * bind to this service and request an access token from it!
 */
public class OAuthTokenService extends Service {

    private final IBinder serviceBinder = new OAuthTokenServiceBinder();
    private SharedPreferences sharedPreferences;
    private L2PHttpClient httpClient;
    private ClientIdHandler clientIdHandler;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture scheduledFutureTask;
    private int accessTokenRefreshDelay;
    private volatile String accessToken;
    private String refreshToken;
    private String clientKey;
    private static String TAG = OAuthTokenService.class.getName();

    public class OAuthTokenServiceBinder extends Binder {
        public OAuthTokenService getService() {
            // Return this instance of OAuthTokenService so clients can call public methods
            return OAuthTokenService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    private void startService() {
        Log.i(TAG, "Starting the token service...");
        clientIdHandler = new ClientIdHandler(this);
        httpClient = new OkL2PHttpClient();
        accessToken = sharedPreferences.getString(getString(R.string.oauth_access_token),"");
        refreshToken = sharedPreferences.getString(getString(R.string.oauth_refresh_token),"");
        accessTokenRefreshDelay = 0;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduledFutureTask = scheduler.schedule(new RefreshAccessTokenTask(),0, TimeUnit.SECONDS); // immediately fire off the task to refresh the access token upon starting of the service
    }

    private class RefreshAccessTokenTask implements Runnable {
        public void run() {
            try {
                Log.i(TAG, "Refreshing access token...");
                // get the refresh token from L2P
                if (clientKey.isEmpty()) {
                    clientKey = clientIdHandler.getApiClientID();
                }
                if(validateAccessToken(clientKey)) { //if the access token is still valid, do not update it
                    Log.i(TAG,"Access token is valid. Next task scheduled to be run after: "+accessTokenRefreshDelay+" seconds");
                    scheduledFutureTask = scheduler.schedule(new RefreshAccessTokenTask(), accessTokenRefreshDelay, TimeUnit.SECONDS);
                }
                else{
                    if(refreshAccessToken(clientKey)) { // if the refreshing of the access token did not return an error then schedule another
                        Log.i(TAG,"Access token is valid. Next task scheduled to be run after: "+accessTokenRefreshDelay+" seconds");
                        scheduledFutureTask = scheduler.schedule(new RefreshAccessTokenTask(), accessTokenRefreshDelay, TimeUnit.SECONDS);
                    }else{ //clear the local variables of their values
                        accessToken="";
                        refreshToken="";
                    }
                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    }

    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences(OAuthActivity.SHARED_PREFERENCES_FILE_KEY, Context.MODE_PRIVATE);
        accessToken = "";
        refreshToken = "";
        clientKey = "";
        if (!sharedPreferences.getString(getString(R.string.oauth_refresh_token),"").isEmpty()) // if the refresh token exists in the shared preferences
            startService(); // start off the task of refreshing the access token
        else {
            Log.i(TAG, "Service not started, the refresh token does not exist...");
            //fire off the authentication activity
            Intent intent_oauth = new Intent(getBaseContext(), OAuthActivity.class);
            intent_oauth.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getApplication().startActivity(intent_oauth);
        }
    }

    @Override
    public void onDestroy() {
        if (scheduledFutureTask != null)
            scheduledFutureTask.cancel(true); //cancel the task, forcefully if necessary
        if(scheduler != null)
            scheduler.shutdown(); // shutdown the scheduler
        if (!accessToken.isEmpty() && !refreshToken.isEmpty()) // make sure that we do not commit empty tokens.
            persistTokens(); // save the tokens before shutting down the service
        super.onDestroy();
    }

    /**
     * @return The actual access token. If there was an error fetching the access token then an empty string
     */
    public String getL2PAccessToken() {
        //so the authorization process was completed but the thread for refresh the token has not been fired off
        if(!sharedPreferences.getString(getString(R.string.oauth_refresh_token),"").isEmpty()&&scheduledFutureTask==null&&accessToken.isEmpty())
            startService();
        return accessToken;
    }

    private void persistTokens(){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getString(R.string.oauth_access_token),accessToken);
        editor.putString(getString(R.string.oauth_refresh_token),refreshToken);
        editor.commit();
    }

    private boolean validateAccessToken(String clientId)throws IOException,JSONException{
        if(accessToken.isEmpty() || clientId.isEmpty())
            return false;
        Map<String,String> postBodyParameters = new HashMap<>();
        postBodyParameters.put(getString(R.string.oauth_client_id),clientId);
        postBodyParameters.put(getString(R.string.oauth_access_token),accessToken);
        JSONObject responseJSON = new JSONObject(httpClient.makePOSTRequest(postBodyParameters,getString(R.string.oauth_token_validation_endpoint)));
        Log.i(TAG, responseJSON.toString());
        if(responseJSON.getString("status").equals("ok") && responseJSON.getString("state").equals("valid")) {
            accessTokenRefreshDelay = responseJSON.getInt(getString(R.string.oauth_token_expire)); // get the remaining time before the access token will expire
            // the next execution of the refresh task will be scheduled according to this delay
            return true;
        }
        else
            return false;
    }

    private boolean refreshAccessToken(String clientId) throws IOException,JSONException{
        Map<String,String> postBodyParameters = new HashMap<>();
        postBodyParameters.put(getString(R.string.oauth_client_id),clientId);
        postBodyParameters.put(getString(R.string.oauth_refresh_token),refreshToken);
        postBodyParameters.put(getString(R.string.oauth_grant_type),getString(R.string.oauth_token_refresh_grant_type));
        JSONObject responseJSON = new JSONObject(httpClient.makePOSTRequest(postBodyParameters,getString(R.string.oauth_token_endpoint)));
        Log.i(TAG,"Access token refresh response:");
        Log.i(TAG,responseJSON.toString());
        if(!responseJSON.optString(getString(R.string.oauth_access_token)).isEmpty()) {
            accessToken = responseJSON.getString(getString(R.string.oauth_access_token));
            accessTokenRefreshDelay = responseJSON.getInt(getString(R.string.oauth_token_expire));
            return true;
        }else{
            // an error was probably returned. According to the documentation only one type of error can be returned i.e. refresh token not valid anymore
            //https://oauth.campus.rwth-aachen.de/doc/
            if (responseJSON.has("error")) {
                //clear the tokens from the app's local storage because the whole authorization process needs to be performed again
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove(getString(R.string.oauth_access_token));
                editor.remove(getString(R.string.oauth_refresh_token));
                editor.commit();
                return false;
            }
            return true;
        }
    }
}
