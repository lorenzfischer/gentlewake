/* TODO: license */
package org.github.gentlewake.hue;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.github.gentlewake.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * @author lorenz.fischer@gmail.com
 */
public class RetrieveBridgeIpTask extends AsyncTask<Void, Integer, String> {

    private static final String TAG = "GentleWake.Util";

    /** @see http://developers.meethue.com/gettingstarted.html */
    public static final String HUE_BROKER_URL = "http://www.meethue.com/api/nupnp";

    /** Used to access the shared preferences of the app. */
    private final Context context;

    /** @param context the context will be used to store the IP address of the bridge into the shared preferences. */
    public RetrieveBridgeIpTask(Context context) {
        this.context = context;
    }

    protected String doInBackground(Void... params) {
        String result = null;

        HttpClient client = HttpClientBuilder.create().build();

        try {
            HttpResponse response = null;
            BridgeInfo[] bridges;
            ObjectMapper mapper;
            JsonFactory jsonFctry;
            JsonParser jparser;

            response = client.execute(RequestBuilder.get().
                    setUri(HUE_BROKER_URL).build());

            mapper = new ObjectMapper();
            jsonFctry = new JsonFactory();
            jparser = jsonFctry.createParser(response.getEntity().getContent());
            bridges = mapper.readValue(jparser, BridgeInfo[].class);

            if (bridges.length > 0) {
                // for now we only support one bridge, so we just try to get the first one
                result = bridges[0].getInternalipaddress();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    protected void onProgressUpdate(Integer... progress) {
    }

    protected void onPostExecute(String bridgeIpAddress) {

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Answer: " + bridgeIpAddress);
        }

        if (bridgeIpAddress != null) {
            SharedPreferences prefs;
            SharedPreferences.Editor editor;


            prefs = this.context.getSharedPreferences("pref_bridge_config", Context.MODE_MULTI_PROCESS);
            editor = prefs.edit();
            editor.putString("prefBridgeIpAddress", bridgeIpAddress);
            editor.commit();
        }
    }
}