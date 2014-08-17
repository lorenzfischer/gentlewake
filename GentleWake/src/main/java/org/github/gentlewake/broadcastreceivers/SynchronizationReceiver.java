/* TODO: license */
package org.github.gentlewake.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.github.gentlewake.services.AlarmSynchronizationService;

/**
 * This BroadcastReceiver listens for events that should trigger the synchronization of the Hue schedule with the
 * alarm clock. Currently this is when either the WiFi is freshly connected (when returning home) or when the
 * alarm clock gets set to a new value.
 *
 * @author lorenz.fischer@gmail.com
 */
public class SynchronizationReceiver extends BroadcastReceiver {

    private static final String TAG = "GentleWake.SyncReceiver";


    @Override
    public void onReceive(final Context context, Intent intent) {
        boolean doSync = false;

        if (WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION.equals(intent.getAction())) {
            if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false) == true) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "A wifi connection has just been established");
                }
                doSync = true;

                // introduce artificial 5 second lag, to give the wifi connection a bit more time to initialize
                try {
                    Thread.sleep(5 * 1000);
                } catch (InterruptedException e) {
                    // it doesn't matter if we were interrupted while waiting, so we don't do anything here
                }
            }
        } else {
            ConnectivityManager connManager;
            NetworkInfo mWifi;

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "We received an intent that tells us to do alarm syncing. Checking for a WiFi connection..");
            }

            connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (mWifi.isConnected()) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Wifi is connected.");
                }
                doSync = true;
            } else {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Wifi not connected.");
                }
            }
        }

        if (doSync) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "initiating alarm sync service.");
            }

            context.startService(new Intent(context, AlarmSynchronizationService.class));
        }

    }

}
