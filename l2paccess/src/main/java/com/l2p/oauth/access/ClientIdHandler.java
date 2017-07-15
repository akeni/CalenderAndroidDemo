package com.l2p.oauth.access;

import android.content.Context;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.IOException;

/**
 * Provides convenience method to load the clientId from the file present in the assets folder.
 */
public class ClientIdHandler {
    private Context context;
    private static String APPID_FILE_NAME = "apiclientID.txt"; // the name of file where the client Id for OAUTH is stored
    private String clientID;

    public ClientIdHandler(Context context){
        this.context = context;
        this.clientID ="";
    }

    /**
     * Loads the client Id from the file located in the assets folder and returns the
     * contents
     * @return The client Id if the file containing it exists, else an empty string
     * */
    public String getApiClientID(){
        try {
            if (clientID.isEmpty())
                clientID = IOUtils.toString(context.getResources().getAssets().open(APPID_FILE_NAME));
        }catch(IOException|NullPointerException exception){
            Log.e(ClientIdHandler.class.getName(),exception.getLocalizedMessage());
            return "";
        }
        return clientID;
    }
}
