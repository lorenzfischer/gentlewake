package org.github.gentlewake.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResourcesCache;
import com.philips.lighting.model.PHSchedule;

import org.github.gentlewake.R;
import org.github.gentlewake.data.ApplicationPreferences;
import org.github.gentlewake.hue.DefaultPHSDKListener;
import org.github.gentlewake.util.AlarmUtils;
import org.github.gentlewake.hue.SyncManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.regex.Pattern;

/**
 * MainApplicationActivity - The starting point for creating your own Hue App.
 * Currently contains a simple view with a button to change your lights to random colours.  Remove this and add your own app implementation here! Have fun!
 *
 * @author SteveyO
 */
public class MainApplicationActivity extends Activity {

    private static final String TAG = "GentleWake.MainAppActy";

    /** The maximum number of lines that should be shown in the message log text field. */
    public static final int MAX_LOG_LINES = 50;

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

    /** The button for issueing sync commands. */
    private Button mBtnSync;

    /** The button for issuing a disconnect from the bridge. */
    private Button mDisconnectButton;

    //private static final int MAX_HUE = 65535;

    /** An object which contains global settings of the app. */
    private ApplicationPreferences mPrefs;

    /** We use this task to read from logcat. */
    private AsyncTask<Void, String, Void> mLogcatReaderTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.app_name);
        setContentView(R.layout.activity_main);

        this.mDateFmt = new SimpleDateFormat();
        this.mHueSdk = PHHueSDK.create();
        this.mPrefs = ApplicationPreferences.getInstance(this);
        this.mTxtvLog = (TextView) findViewById(R.id.log);
        this.mScrollvLog = (ScrollView) findViewById(R.id.scrollViewLog);

        mBtnSync = (Button) findViewById(R.id.buttonSync);
        mBtnSync.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mSyncManager.syncAlarm(null);
            }

        });
        mBtnSync.setEnabled(false);

        mDisconnectButton = (Button) findViewById(R.id.buttonDisconnect);
        mDisconnectButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ApplicationPreferences prefs = ApplicationPreferences.getInstance(getApplicationContext());
                prefs.setLastConnectedIPAddress(null);

                // Starting home activity again (access point selection)
                // prevent the PushLink Activity being shown when pressing the back button.
                Intent intent = new Intent(getApplicationContext(), BridgeListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    // equal to Intent.FLAG_ACTIVITY_CLEAR_TASK which is only available from API level 11
                    intent.addFlags(0x8000);
                }
                startActivity(intent);

            }

        });
        mDisconnectButton.setEnabled(false);

        // ... otherwise we will update the ui as soon as we receive a bridge object
        this.mSdkListener = new DefaultPHSDKListener() {
            @Override
            public void onCacheUpdated(int i, final PHBridge bridge) {
                mSyncManager = new SyncManager(MainApplicationActivity.this, bridge);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBtnSync.setEnabled(mSyncManager != null);
                        mDisconnectButton.setEnabled(mSyncManager != null);
                        updateUi(bridge);
                    }
                });
            }
        };

    }

    /**
     * This method updates all the ui components, so they contain the current configured alarm, as well as the
     * current configuration of the Hue bridge (Hue on/off).
     *
     * @param selectedBridge the bridge to read the data from, if null, nothing will be updated.
     */
    private void updateUi(PHBridge selectedBridge) {
        if (selectedBridge != null) {
            PHBridgeResourcesCache resourceCache;
            Date nextAlarm;
            PHSchedule hueOn;
            PHSchedule hueOff;
            Hashtable<String, PHSchedule> schedules;

            resourceCache = selectedBridge.getResourceCache();
            schedules = resourceCache.getSchedules();
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
                ((TextView) findViewById(R.id.txtvCurrentAlarm)).setText("Not Set");
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
    }

    /**
     * Writes a message to the log view so the user knows what's going on.
     *
     * @param lines the messages to add to the log view.
     */
    public void logMessage(String... lines) {
        StringBuilder formattedMessage;
        int linesToRemove;

        formattedMessage = new StringBuilder();
        for (String line : lines) { // I'm so looking forward to join()
            formattedMessage.append(line).append("\n");
        }

        // add new line
        mTxtvLog.append(formattedMessage);
        // remove old lines if necessary
        // copied from here: http://stackoverflow.com/questions/5078058/how-to-delete-the-old-lines-of-a-textview
        linesToRemove = mTxtvLog.getLineCount() - MAX_LOG_LINES;
        if (linesToRemove > 0) {
            for (int i = 0; i < linesToRemove; i++) {
                Editable text;
                Layout layout;

                text = mTxtvLog.getEditableText();
                layout = mTxtvLog.getLayout();
                text.delete(layout.getLineStart(0), layout.getLineEnd(0));
            }
        }

        // scroll to bottom of text field
        mScrollvLog.fullScroll(ScrollView.FOCUS_DOWN);
    }

    @Override
    protected void onStart() {
        super.onStart();

        PHBridge selectedBridge;
        this.mHueSdk.getNotificationManager().registerSDKListener(this.mSdkListener);

        // test to see if we have a connection to the bridge already
        selectedBridge = this.mHueSdk.getSelectedBridge();
        if (selectedBridge != null) {
            this.mSyncManager = new SyncManager(this, selectedBridge);  // the sync-button relies on this
            mBtnSync.setEnabled(this.mSyncManager != null);
            mDisconnectButton.setEnabled(this.mSyncManager != null);
            updateUi(selectedBridge);                 // show current alarm and hue configuration in ui
        }

        // start reading the logcat log in a background thread
        if (mLogcatReaderTask != null) {
            mLogcatReaderTask.cancel(true);
        }
        mLogcatReaderTask = new AsyncTask<Void, String, Void>() {
            private Pattern pattern = Pattern.compile("“");

            @Override
            protected Void doInBackground(Void... args) {
                Process process;
                BufferedReader reader;

                try {
                    process = Runtime.getRuntime().exec("logcat -v time "+SyncManager.TAG+":D *:S");
                    reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));

                    while (true) { // we do this eternally
                        String line;

                        line = reader.readLine();
                        if (line == null) {
                            // try again in 2 seconds
                            Thread.sleep(2 * 1000);
                        } else {
                            // the log line contains way too much information, strip away some of it.

                            publishProgress(line);
                        }
                    }
                } catch (IOException e) {
                    // post the exception to the log
                    publishProgress(e.getMessage());
                } catch (InterruptedException e) {
                    // this happens if this object gets killed while we're waiting.
                    publishProgress("Exiting");
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(String... values) {
                logMessage(values);
            }

        };
        mLogcatReaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.mHueSdk.getNotificationManager().unregisterSDKListener(this.mSdkListener);
        mLogcatReaderTask.cancel(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PHBridge bridge = mHueSdk.getSelectedBridge();
        if (bridge != null) {

            if (mHueSdk.isHeartbeatEnabled(bridge)) {
                mHueSdk.disableHeartbeat(bridge);
            }

            mHueSdk.disconnect(bridge);
        }
    }
}
