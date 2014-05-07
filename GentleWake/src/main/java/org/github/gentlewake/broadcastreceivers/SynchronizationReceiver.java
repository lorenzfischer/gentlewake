/* TODO: license */
package org.github.gentlewake.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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

    private static final String TAG = "GentleWake";


    @Override
    public void onReceive(final Context context, Intent intent) {
        boolean doSync = false;

        if (WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION.equals(intent.getAction()) &&
                intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false) == true) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "a wifi connection has just been established");
            }
            doSync = true;
        }

        if ("android.intent.action.ALARM_CHANGED".equals(intent.getAction())) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "an alarm has just been changed");
            }
            doSync = true;
        }

        if (doSync) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "initiating alarm sync service.");
            }

            context.startService(new Intent(context, AlarmSynchronizationService.class));
        }

    }

}
