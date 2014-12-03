package org.github.gentlewake.views;

import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResourcesCache;
import com.philips.lighting.model.PHSchedule;

import org.github.gentlewake.R;
import org.github.gentlewake.broadcastreceivers.SynchronizationReceiver;
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
public class SetupFragment extends Fragment {

    private static final String TAG = "GentleWake.SetupFgmt";

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

    //private static final int MAX_HUE = 65535;

    /** An object which contains global settings of the app. */
    private ApplicationPreferences mPrefs;

    /** We use this task to read from logcat. */
    private AsyncTask<Void, String, Void> mLogcatReaderTask;

    /** We keep a reference to this container, so we can find views outside of the onCreateView method. */
    private ViewGroup mContainer;  // todo: is this clean

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result;

        result = inflater.inflate(R.layout.fragment_setup, container, false);

        this.mContainer = container;
        this.mDateFmt = new SimpleDateFormat();
        this.mHueSdk = PHHueSDK.create();
        this.mPrefs = ApplicationPreferences.getInstance(getActivity());
        this.mTxtvLog = (TextView) result.findViewById(R.id.log);
        this.mScrollvLog = (ScrollView) result.findViewById(R.id.scrollViewLog);

        // ... otherwise we will update the ui as soon as we receive a bridge object
        this.mSdkListener = new DefaultPHSDKListener() {
            @Override
            public void onCacheUpdated(int i, final PHBridge bridge) {
                mSyncManager = new SyncManager(getActivity(), bridge);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateUi(bridge);
                    }
                });
            }
        };

        // tell the super activity that we have a menu button that we want to be shown
        setHasOptionsMenu(true);

        // tmp
        logMessage(android.os.Build.MODEL);

        return result;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Inflating setup menu");
        }
        inflater.inflate(R.menu.setup, menu);
    }

    /**
     * Called when option is selected.
     *
     * @param item the MenuItem object.
     * @return boolean Return false to allow normal menu processing to proceed,  true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sync_alarm:
                mSyncManager.syncAlarm(null);
                break;
        }
        return true;
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
            nextAlarm = AlarmUtils.getNextAlarm(getActivity());
            hueOn = null;
            hueOff = null;

            if (this.mPrefs.getScheduleIdOn() != null) {
                hueOn = schedules.get(this.mPrefs.getScheduleIdOn());
            }
            if (this.mPrefs.getScheduleIdOff() != null) {
                hueOff = schedules.get(this.mPrefs.getScheduleIdOff());
            }

            if (nextAlarm == null) {
                ((TextView) mContainer.findViewById(R.id.txtvCurrentAlarm)).setText("Not Set");
            } else {
                ((TextView) mContainer.findViewById(R.id.txtvCurrentAlarm)).setText(this.mDateFmt.format(nextAlarm));
            }

            if (hueOn == null) {
                ((TextView) mContainer.findViewById(R.id.txtvHueOn)).setText(R.string.txt_not_set);
            } else {
                ((TextView) mContainer.findViewById(R.id.txtvHueOn)).setText(this.mDateFmt.format(hueOn.getDate()));
            }

            if (hueOff == null) {
                ((TextView) mContainer.findViewById(R.id.txtvHueOff)).setText(R.string.txt_not_set);
            } else {
                ((TextView) mContainer.findViewById(R.id.txtvHueOff)).setText(this.mDateFmt.format(hueOff.getDate()));
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
    public void onResume() {
        super.onResume();

        PHBridge selectedBridge;
        this.mHueSdk.getNotificationManager().registerSDKListener(this.mSdkListener);

        // test to see if we have a connection to the bridge already
        selectedBridge = this.mHueSdk.getSelectedBridge();
        if (selectedBridge != null) {
            this.mSyncManager = new SyncManager(getActivity(), selectedBridge);  // the sync-button relies on this
            updateUi(selectedBridge);                 // show current alarm and hue configuration in ui
        }

        // start reading the logcat log in a background thread
        if (mLogcatReaderTask != null) {
            mLogcatReaderTask.cancel(true);
        }
        mLogcatReaderTask = new AsyncTask<Void, String, Void>() {
            private static final String PATTERN = "^[^\\s]+\\s([0-9][0-9]:[0-9][0-9]:[0-9][0-9])[^:]+:\\s(.*)$";
            @Override
            protected Void doInBackground(Void... args) {
                Process process;
                BufferedReader reader;

                try {
                    // http://developer.android.com/tools/debugging/debugging-log.html#outputFormat
                    // logcat -v time GentleWake.SyncManager:D GentleWake.SyncReceiver:D *:W | grep GentleWake
                    // todo: replace the "GentleWake" string
                    process = Runtime.getRuntime().exec("logcat -v time "+  // show the "time format"
                            SyncManager.TAG+":D "+                          // debug for the sync manager
                            SynchronizationReceiver.TAG+":D "+              // debug for the sync receiver
                            "*:E "+                                         // show all errors
                            "| grep GentleWake");                           // ... for TAGs containing "GentleWake"
                    reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));

                    while (true) { // we do this eternally
                        String line;

                        line = reader.readLine();
                        if (line == null) {
                            // try again in 2 seconds
                            Thread.sleep(2 * 1000);
                        } else {
                            /* The log line contains way too much information. Example:
                               11-09 16:28:31.996 I/GentleWake.SyncManager(19703): Syncing alarms
                             */
                            line = line.replaceAll(PATTERN, "$1: $2");

                            publishProgress(line);
                        }
                    }
                } catch (IOException e) {
                    // post the exception to the log
                    publishProgress("Error while reading logcat: " + e.getMessage());
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
    public void onPause() {
        super.onPause();
        this.mHueSdk.getNotificationManager().unregisterSDKListener(this.mSdkListener);
        mLogcatReaderTask.cancel(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mHueSdk != null) {  // I have not yet found out how this is possible
            PHBridge bridge = mHueSdk.getSelectedBridge();
            if (bridge != null) {

                if (mHueSdk.isHeartbeatEnabled(bridge)) {
                    mHueSdk.disableHeartbeat(bridge);
                }

                mHueSdk.disconnect(bridge);
            }
        }
    }
}
