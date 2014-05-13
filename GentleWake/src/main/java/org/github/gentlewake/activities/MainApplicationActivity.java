package org.github.gentlewake.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHSchedule;

import org.github.gentlewake.R;
import org.github.gentlewake.data.ApplicationPreferences;
import org.github.gentlewake.hue.DefaultPHSDKListener;
import org.github.gentlewake.util.AlarmUtils;
import org.github.gentlewake.hue.SyncManager;
import org.github.gentlewake.util.ValueCallback;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

/**
 * MainApplicationActivity - The starting point for creating your own Hue App.
 * Currently contains a simple view with a button to change your lights to random colours.  Remove this and add your own app implementation here! Have fun!
 *
 * @author SteveyO
 */
public class MainApplicationActivity extends Activity {

    private static final String TAG = "GentleWake";

    private PHHueSDK mHueSdk;

    /** We use this object to store global settings about the app. */
    private SyncManager mSyncManager;

    /** This formatter is used to format all date values in this view. */
    private DateFormat mDateFmt;

    /** We use this text view to log messages, so the user knows what was going on. */
    private TextView mTxtvLog;

    /** The scroll view containing the log text view. */
    private ScrollView mScrollvLog;

    /** This listener will be informed about updates to the Hue. */
    private PHSDKListener mSdkListener;

    private static final int MAX_HUE = 65535;

    /** An object which contains global settings of the app. */
    private ApplicationPreferences mPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Button btnSync;
        Button disconnectButton;
        final ValueCallback<String> logMessageCallback;

        setTitle(R.string.app_name);
        setContentView(R.layout.activity_main);
        this.mHueSdk = PHHueSDK.create();
        this.mPrefs = ApplicationPreferences.getInstance(this);
        this.mSyncManager = new SyncManager(this, this.mHueSdk.getSelectedBridge());
        this.mTxtvLog = (TextView) findViewById(R.id.log);
        this.mScrollvLog = (ScrollView) findViewById(R.id.scrollViewLog);
        this.mDateFmt = new SimpleDateFormat();

        this.mSdkListener = new DefaultPHSDKListener() {
            @Override
            public void onCacheUpdated(int i, final PHBridge bridge) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateUi(bridge);
                    }
                });
            }
        };

        // this callback writes everything into the log text view
        logMessageCallback = new ValueCallback<String>() {
            @Override
            public void go(String message) {
                logMessage(message);
            }
        };

        btnSync = (Button) findViewById(R.id.buttonSync);
        btnSync.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mSyncManager.syncAlarm(logMessageCallback);
            }

        });

        disconnectButton = (Button) findViewById(R.id.buttonDisconnect);
        disconnectButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ApplicationPreferences prefs = ApplicationPreferences.getInstance(getApplicationContext());
                prefs.setLastConnectedIPAddress(null);

                // Starting home activity again (access point selection)
                // prevent the PushLink Activity being shown when pressing the back button.
                Intent intent = new Intent(getApplicationContext(), PHHomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    // equal to Intent.FLAG_ACTIVITY_CLEAR_TASK which is only available from API level 11
                    intent.addFlags(0x8000);
                }
                startActivity(intent);

            }

        });

        // show current alarm and hue configuration in ui
        updateUi(this.mHueSdk.getSelectedBridge());
    }

    /**
     * This method updates all the ui components, so they contain the current configured alarm, as well as the
     * current configuration of the Hue bridge (Hue on/off).
     *
     * @param selectedBridge the bridge to read the data from.
     */
    private void updateUi(PHBridge selectedBridge) {
        Date nextAlarm;
        PHSchedule hueOn;
        PHSchedule hueOff;
        Hashtable<String, PHSchedule> schedules;

        schedules = this.mHueSdk.getSelectedBridge().getResourceCache().getSchedules();
        nextAlarm = AlarmUtils.getNextAlarm(this);
        hueOn = null;
        hueOff = null;

        if (this.mPrefs.getScheduleIdOn() != null) {
            hueOn = schedules.get(this.mPrefs.getScheduleIdOn());
        }
        if (this.mPrefs.getScheduleIdOff() != null) {
            hueOff = schedules.get(this.mPrefs.getScheduleIdOff());
        }

        if (nextAlarm == null) {
            ((TextView) findViewById(R.id.txtvCurrentAlarm)).setText(this.mDateFmt.format("Not Set"));
        } else {
            ((TextView) findViewById(R.id.txtvCurrentAlarm)).setText(this.mDateFmt.format(nextAlarm));
        }

        if (hueOn == null) {
            ((TextView) findViewById(R.id.txtvHueOn)).setText(R.string.txt_not_set);
        } else {
            ((TextView) findViewById(R.id.txtvHueOn)).setText(this.mDateFmt.format(hueOn.getDate()));
        }

        if (hueOff == null) {
            ((TextView) findViewById(R.id.txtvHueOff)).setText(R.string.txt_not_set);
        } else {
            ((TextView) findViewById(R.id.txtvHueOff)).setText(this.mDateFmt.format(hueOff.getDate()));
        }

    }

    /**
     * Writes a message to the log view so the user knows what's going on.
     *
     * @param msg the message to add to the log view.
     */
    public void logMessage(String msg) {
        final String formattedMessage;

        formattedMessage = String.format("%s: %s\n", mDateFmt.format(new Date()), msg);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, formattedMessage);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTxtvLog.append(formattedMessage);
                mScrollvLog.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        this.mHueSdk.getNotificationManager().registerSDKListener(this.mSdkListener);
        updateUi(this.mHueSdk.getSelectedBridge());
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.mHueSdk.getNotificationManager().unregisterSDKListener(this.mSdkListener);
    }

    @Override
    protected void onDestroy() {
        PHBridge bridge = mHueSdk.getSelectedBridge();
        if (bridge != null) {

            if (mHueSdk.isHeartbeatEnabled(bridge)) {
                mHueSdk.disableHeartbeat(bridge);
            }

            mHueSdk.disconnect(bridge);
            super.onDestroy();
        }
    }
}
