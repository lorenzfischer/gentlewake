/* TODO: license */
package org.github.gentlewake.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.exception.PHHueException;

import org.github.gentlewake.data.ApplicationPreferences;
import org.github.gentlewake.hue.SyncManager;
import org.github.gentlewake.util.ValueCallback;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * @author lorenz.fischer@gmail.com
 */
public class AlarmSynchronizationService extends Service {

    private static final String TAG = "GentleWake";

    /** application preferences. */
    private ApplicationPreferences mPrefs;

    /** A handler used to show the toas message (in the case of a successful sync) on the UI thread. */
    private Handler mHandler;

    @Override
    public void onCreate() {
        mPrefs = ApplicationPreferences.getInstance(this);
        mHandler = new Handler();
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "starting sync service");
        }

        final Context context = this;

        /*
         * do the syncing in a background activity, as this method is run on the main thread.
         * See http://developer.android.com/reference/android/app/Service.html
         */
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                String lastIpAddress;
                lastIpAddress = mPrefs.getLastConnectedIPAddress();

                if (lastIpAddress != null && isBridgeReachable(lastIpAddress)) {
                    PHHueSDK sdk;
                    PHAccessPoint lastAccessPoint;
                    String lastUsername;
                    SyncManager manager;

                    sdk = PHHueSDK.create();
                    sdk.setDeviceName(mPrefs.getBridgeDeviceName());
                    lastUsername = mPrefs.getUsername();
                    lastAccessPoint = new PHAccessPoint();
                    lastAccessPoint.setIpAddress(lastIpAddress);
                    lastAccessPoint.setUsername(lastUsername);

                    if (!sdk.isAccessPointConnected(lastAccessPoint)) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "all necessary information available. trying to connect to bridge ...");
                        }

                        try {
                            sdk.connect(lastAccessPoint);
                        } catch (PHHueException e) {
                            Log.e(TAG, "Error while connecting to the Hue bridge. ", e);
                        }
                    }

                    // now we should be connected
                    manager = new SyncManager(context, sdk.getSelectedBridge());
                    manager.syncAlarm(new ValueCallback<String>() {
                        @Override
                        public void go(final String message) {
                            if (Log.isLoggable(TAG, Log.DEBUG)) {
                                Log.d(TAG, message);
                            }

                            // show message about the alarm having been set
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                                }
                            });

                        }
                    }, null); // disable the off-callback

                    // disconnect again
                    sdk.disconnect(sdk.getSelectedBridge());
                } else {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "bridge not reachable. IP: " + lastIpAddress);
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                stopSelf(startId); // tell the service that we were able to run to completion and don't need to be restarted
            }
        }.execute(null, null, null);

        return Service.START_REDELIVER_INTENT; // make sure we get restarted, if we were cancelled
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Tests if the bridge with the given ip is reachable. This method does not try to connect, but only tries to
     * establish a socket connection on port 80 of the given ip.
     *
     * @param bridgeIp the ip of the bridge to test for reachability.
     * @return true if the bridge can be reached, false otherwise.
     */
    private static boolean isBridgeReachable(String bridgeIp) {
        boolean result = false;

        try {
            result = InetAddress.getByName(bridgeIp).isReachable(2000); // this sends a ping to the bridge
            result = true;
        } catch (Exception e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Exception while trying to connect to the Hue bridge.", e);
            }
        }

        return result;
    }
}
