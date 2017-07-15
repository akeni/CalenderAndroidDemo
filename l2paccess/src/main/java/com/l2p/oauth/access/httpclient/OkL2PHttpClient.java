package com.l2p.oauth.access.httpclient;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.Map;

/**
 * A very basic concrete implementation of the L2PHttpClient interface using the OkHttpClient library.
 * @see <a href="http://square.github.io/okhttp/">OkHttpClient</a>
 */
public class OkL2PHttpClient implements L2PHttpClient {

    private OkHttpClient okHttpClient;

    public OkL2PHttpClient(){
        okHttpClient = new OkHttpClient();
    }

    @Override
    public String makeGETRequest(Map<String, String> params, String requestURL) throws IOException{
        String completeRequestURL = requestURL;
        int count = 0;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (count == 0)
                completeRequestURL += "?" + entry.getKey() + "=" + entry.getValue();
            else
                completeRequestURL += "&" + entry.getKey() + "=" + entry.getValue();
            count++;
        }
        Request request  = new Request.Builder().url(completeRequestURL).build();
        Response response = okHttpClient.newCall(request).execute();
        //return completeRequestURL;
        return response.body().string();
    }

    @Override
    public String makePOSTRequest(Map<String, String> params, String requestURL) throws IOException{
        RequestBody formBody;
        FormEncodingBuilder builder = new FormEncodingBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }

        formBody = builder.build();
        Request request = new Request.Builder()
                .url(requestURL)
                .post(formBody)
                .build();
        Response response = okHttpClient.newCall(request).execute();
        return response.body().string();
    }
}
