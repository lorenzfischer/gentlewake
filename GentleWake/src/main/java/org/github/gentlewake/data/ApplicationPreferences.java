package org.github.gentlewake.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.provider.Settings;

import com.philips.lighting.hue.sdk.connection.impl.PHBridgeInternal;

import org.github.gentlewake.R;

import java.util.UUID;

/**
 * This class manages all settings of the application. It stores and retrieves all values from a shared datastore_preferences
 * object.
 */
public class ApplicationPreferences {

    private static final String SHARED_PREFERENCES_STORE = "GentlewakeSharedPrefs";
    private static final String BRIDGE_DEVICE_NAME = "LastConnectedUsername";
    private static final String LAST_CONNECTED_USERNAME = "LastConnectedUsername";
    private static final String LAST_CONNECTED_IP = "LastConnectedIP";
    private static final String LIGHT_GROUP_NAME = "LightGroupName";
    private static final String BASE_SCHEDULE_NAME = "BaseScheduleName";
    private static final String SCHEDULE_ID_ON = "ScheduleIdOn";
    private static final String SCHEDULE_ID_BRIGHTEN = "ScheduleIdBrighten";
    private static final String SCHEDULE_ID_OFF = "ScheduleIdOff";
    private static final String TRANSITION_MINUTES = "TransitionMinutes";
    private static ApplicationPreferences instance = null;

    /** This context is used to read default values from the system. */
    private final Context mCtx;

    private SharedPreferences mSharedPreferences = null;

    private Editor mSharedPreferencesEditor = null;


    public void create() {

    }

    public static ApplicationPreferences getInstance(Context ctx) {
        if (instance == null) {
            instance = new ApplicationPreferences(ctx);
        }
        return instance;
    }

    private ApplicationPreferences(Context appContext) {
        mCtx = appContext;
        mSharedPreferences = appContext.getSharedPreferences(SHARED_PREFERENCES_STORE, 0); // 0 - for private mode
        mSharedPreferencesEditor = mSharedPreferences.edit();
    }


    public String getUsername() {
        String username = mSharedPreferences.getString(LAST_CONNECTED_USERNAME, "");
        if (username == null || username.equals("")) {
            username = PHBridgeInternal.generateUniqueKey();
            setUsername(username);  // Persist the username in the shared prefs
        }
        return username;
    }

    public boolean setUsername(String username) {
        mSharedPreferencesEditor.putString(LAST_CONNECTED_USERNAME, username);
        return (mSharedPreferencesEditor.commit());
    }

    /**
     * @return the IP address of the last bridge this app was connected to or <code>null</code> if this application
     * has never been connected to a bridge.
     */
    public String getLastConnectedIPAddress() {
        return mSharedPreferences.getString(LAST_CONNECTED_IP, null);
    }

    public boolean setLastConnectedIPAddress(String ipAddress) {
        mSharedPreferencesEditor.putString(LAST_CONNECTED_IP, ipAddress);
        return (mSharedPreferencesEditor.commit());
    }

    /**
     * @return the name of the "light group" that is stored in the settings of this app. If everything worked correctly
     * the bridge will have a group with this name configured.
     */
    public String getLightGroupName() {
        String result;

        result = mSharedPreferences.getString(LIGHT_GROUP_NAME, null);
        if (result == null) {
            result = mCtx.getString(R.string.app_name);
            setLightGroupName(result);
        }

        return result;
    }

    /**
     * @param groupName sets the name of the light group that we use when scheduling alarms. If this field
     *                  is set, the group is assumed to exist on the bridge and we won't create one during each
     *                  start of the application.
     */
    public void setLightGroupName(String groupName) {
        // todo: check if the old value exists on the bridge and implement some handling code to delete it (after
        // the user has been consulted.
        mSharedPreferencesEditor.putString(LIGHT_GROUP_NAME, groupName);
        mSharedPreferencesEditor.apply();
    }

    /**
     * @return the id that should be used for the schedule turning the Hue lights on.
     */
    public String getScheduleIdOn() {
        return mSharedPreferences.getString(SCHEDULE_ID_ON, null);
    }

    /**
     * @return the id that should be used for the schedule that increases the brightness of the hue lights.
     */
    public String getScheduleIdBrighten() {
        return mSharedPreferences.getString(SCHEDULE_ID_BRIGHTEN, null);
    }

    /**
     * @return the id that should be used for the schedule turning the Hue lights off.
     */
    public String getScheduleIdOff() {
        return mSharedPreferences.getString(SCHEDULE_ID_OFF, null);
    }

    /**
     * Sets the schedule id for the schedule that turns the HUE on.
     *
     * @param scheduleId the new schedule id to use by the app.
     */
    public void setScheduleIdOn(String scheduleId) {
        mSharedPreferencesEditor.putString(SCHEDULE_ID_ON, scheduleId);
        mSharedPreferencesEditor.apply();
    }

    /**
     * Sets the schedule id for the schedule that turns the brightness up.
     *
     * @param scheduleId the new schedule id to use by the app.
     */
    public void setScheduleIdBrighten(String scheduleId) {
        mSharedPreferencesEditor.putString(SCHEDULE_ID_BRIGHTEN, scheduleId);
        mSharedPreferencesEditor.apply();
    }

    /**
     * Sets the schedule id for the schedule that turns the HUE on.
     *
     * @param scheduleId the new schedule id to use by the app.
     */
    public void setScheduleIdOff(String scheduleId) {
        mSharedPreferencesEditor.putString(SCHEDULE_ID_OFF, scheduleId);
        mSharedPreferencesEditor.apply();
    }

    /**
     * @return the name that will be used for the alarm clock schedule. This name will be extended by both the
     * {@link #getScheduleNameOn()} and {@link #getScheduleNameOff()} methods to create the names that will be used
     * on the bridge.
     */
    private String getBaseScheduleName() {
        String result;

        result = mSharedPreferences.getString(BASE_SCHEDULE_NAME, null);
        if (result == null) {
            String phoneModel;  // e.g. "Nexus 4"
            String scheduleNameFmt;

            phoneModel = Build.MODEL;
            scheduleNameFmt = "%s %s";
            result = String.format(scheduleNameFmt, mCtx.getString(R.string.app_name), phoneModel);
            setBaseScheduleName(result);
        }

        return result;
    }

    /**
     * @return the name that should be used for the schedule turning the Hue lights on.
     */
    public String getScheduleNameOn() {
        return getBaseScheduleName() + " On";
    }

    /**
     * @return the name that should be used for the schedule that increases the brightness of the hue lights.
     */
    public String getScheduleNameBrighten() {
        return getBaseScheduleName() + " Brighten";
    }

    /**
     * @return the name that should be used for the schedule turning the Hue lights off.
     */
    public String getScheduleNameOff() {
        return getBaseScheduleName() + " Off";
    }

    /**
     * Sets the schedule name in the datastore_preferences of the app.
     *
     * @param scheduleName the new schedule name to use by the app.
     * @see #getScheduleNameOn()
     * @see #getScheduleNameOff()
     */
    public void setBaseScheduleName(String scheduleName) {
        mSharedPreferencesEditor.putString(BASE_SCHEDULE_NAME, scheduleName);
        mSharedPreferencesEditor.apply();
    }

    /**
     * @return the number of minutes, the Hue lights should use to transition into the "on" state.
     */
    public int getTransitionMinutes() {
        return mSharedPreferences.getInt(TRANSITION_MINUTES, 10);
        //return 3;
    }


    /**
     * Sets the the number of minutes, the Hue lights should use to transition into the "on" state.
     *
     * @param minutes
     */
    public void setTransitionMinutes(int minutes) {
        mSharedPreferencesEditor.putInt(TRANSITION_MINUTES, minutes);
        mSharedPreferencesEditor.apply();
    }

    /**
     * This property contains a unique id, which this android device uses when connecting to the bridge. For every
     * new device that tries to connect to the bridge, the user has to physically go up to the bridge and push
     * the "connect" button. The bridge stores all device names, who have been authenticated like this. For this
     * reason, the "device name" can be thought of as an automatically generated password.
     *
     * @return the device name generated for this device.
     */
    public String getBridgeDeviceName() {
        String result;

        result = mSharedPreferences.getString(BRIDGE_DEVICE_NAME, null);
        if (result == null) {
            // don't put a space or an underscore.. I don't know why, but this causes trouble when connecting.
            result = mCtx.getString(R.string.app_name) + "-" +
                    Settings.Secure.getString(mCtx.getContentResolver(), Settings.Secure.ANDROID_ID);
            setBridgeDeviceName(result);
        }

        return result;
    }

    /**
     * @param bridgeDeviceName a new device name
     * @see #getBridgeDeviceName()
     */
    public void setBridgeDeviceName(String bridgeDeviceName) {
        mSharedPreferencesEditor.putString(BRIDGE_DEVICE_NAME, bridgeDeviceName);
        mSharedPreferencesEditor.apply();
    }
}
