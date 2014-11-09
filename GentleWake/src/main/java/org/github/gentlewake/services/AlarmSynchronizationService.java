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
import com.philips.lighting.model.PHBridge;

import org.github.gentlewake.data.ApplicationPreferences;
import org.github.gentlewake.hue.DefaultPHSDKListener;
import org.github.gentlewake.hue.SyncManager;
import org.github.gentlewake.util.ValueCallback;

import java.net.InetAddress;

/**
 * @author lorenz.fischer@gmail.com
 */
public class AlarmSynchronizationService extends Service {

    private static final String TAG = "GentleWake.SyncService";

    /** application datastore_preferences. */
    private ApplicationPreferences mPrefs;

    /** A handler used to show the toast message (in the case of a successful sync) on the UI thread. */
    private Handler mHandler;

    /** The sdk object used to communicate with the hue system. We need to destroy this after we are done using it. **/
    private PHHueSDK mSdk = PHHueSDK.getInstance();

    @Override
    public void onCreate() {
        mPrefs = ApplicationPreferences.getInstance(this);
        mHandler = new Handler();
        mSdk = PHHueSDK.getInstance();
        mSdk.setDeviceName(mPrefs.getBridgeDeviceName()); // the device name is the "password"
    }

    @Override
    public void onDestroy() {

        if (mSdk.getSelectedBridge() != null) {
            // disconnect again
            mSdk.disableHeartbeat(mSdk.getSelectedBridge());
            mSdk.disconnect(mSdk.getSelectedBridge());
        }

        //mSdk.destroySDK(); // free up resources
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Starting sync service");
        }

        /*
         * do the syncing in a background activity, as this method is run on the setup thread.
         * See http://developer.android.com/reference/android/app/Service.html
         */
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                String lastIpAddress;
                lastIpAddress = mPrefs.getLastConnectedIPAddress();

                if (lastIpAddress != null && isBridgeReachable(lastIpAddress)) {
                    PHAccessPoint lastAccessPoint;
                    String lastUsername;

                    lastUsername = mPrefs.getUsername();
                    lastAccessPoint = new PHAccessPoint();
                    lastAccessPoint.setIpAddress(lastIpAddress);
                    lastAccessPoint.setUsername(lastUsername);

                    if (!mSdk.isAccessPointConnected(lastAccessPoint)) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "Bridge wasn't connected. Trying to connect to bridge ...");
                        }

                        mSdk.getNotificationManager().registerSDKListener(new DefaultPHSDKListener() {
                            @Override
                            public void onBridgeConnected(PHBridge bridge) {
                                initiateSync(AlarmSynchronizationService.this, bridge);
                            }

                            @Override
                            public void onConnectionResumed(PHBridge bridge) {
                                initiateSync(AlarmSynchronizationService.this, bridge);
                            }
                        });

                        try {
                            mSdk.connect(lastAccessPoint);
                        } catch (PHHueException e) {
                            Log.e(TAG, "Error while connecting to the Hue bridge. ", e);
                        }

                    } else {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "bridge was connected already, so we just do the syncing ...");
                        }

                        if (mSdk.getSelectedBridge() == null) {
                            if (Log.isLoggable(TAG, Log.DEBUG)) {
                                Log.d(TAG, "No bridge selected, trying to select the first one we can find.");
                            }
                            mSdk.setSelectedBridge(mSdk.getAllBridges().get(0));
                        }

                        initiateSync(AlarmSynchronizationService.this, mSdk.getSelectedBridge());
                    }

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
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);

        return Service.START_REDELIVER_INTENT; // make sure we get restarted, if we were cancelled
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /**
     * This method initiates the the synchronization process.
     *
     * @param ctx    the android context used to read out the configured alarms.
     * @param bridge the bridge to configure the light schedule on.
     */
    private void initiateSync(final Context ctx, PHBridge bridge) {
        SyncManager manager;

        // now we should be connected
        manager = new SyncManager(ctx, bridge);
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
                        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
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
            result = InetAddress.getByName(bridgeIp).isReachable(10 * 1000); // this sends a ping to the bridge
        } catch (Exception e) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "Exception while trying to connect to the Hue bridge.", e);
            }
        }

        return result;
    }
}
