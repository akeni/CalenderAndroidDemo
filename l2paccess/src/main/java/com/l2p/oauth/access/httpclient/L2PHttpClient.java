package com.l2p.oauth.access.httpclient;

import java.io.IOException;
import java.util.Map;

/**
 * An interface which states the methods that concrete http client implementations should provide.
 */
public interface L2PHttpClient {

    /**
     * @param params The query parameters
     * @param requestURL The url of the server to make the GET request to
     * @return The response body from the Server
     * */
    public String makeGETRequest(Map<String,String> params, String requestURL) throws IOException;

    /**
     * @param params - The query parameters
     * @param requestURL - The url of the server to make the GET request to
     * @return The response body from the Server
     * */
    public String makePOSTRequest(Map<String,String> params, String requestURL) throws IOException;
}
