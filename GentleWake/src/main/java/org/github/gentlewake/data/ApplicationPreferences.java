package org.github.gentlewake.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.philips.lighting.hue.sdk.connection.impl.PHBridgeInternal;

import org.github.gentlewake.R;

/**
 * This class manages all settings of the application. It stores and retrieves all values from a shared datastore_preferences
 * object.
 */
public class ApplicationPreferences {

    /** Ths singleton */
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

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        mSharedPreferencesEditor = mSharedPreferences.edit();

        // I used to have this.
        //appContext.getSharedPreferences(SHARED_PREFERENCES_STORE, Context.MODE_PRIVATE);
    }


    public String getUsername() {
        String username = mSharedPreferences.getString(mCtx.getString(R.string.pref_key_username), null);
        if (username == null || username.equals("")) {
            username = PHBridgeInternal.generateUniqueKey();
            setUsername(username);  // Persist the username in the shared prefs
        }
        return username;
    }

    public boolean setUsername(String username) {
        mSharedPreferencesEditor.putString(mCtx.getString(R.string.pref_key_username), username);
        return (mSharedPreferencesEditor.commit());
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

        result = mSharedPreferences.getString(mCtx.getString(R.string.pref_key_device_name), null);
        if (result == null) {
            // don't put a space or an underscore.. I don't know why, but this causes trouble when connecting.
            result = mCtx.getString(R.string.app_name) + "-" + PHBridgeInternal.generateUniqueKey();
            setBridgeDeviceName(result);
        }

        return result;
    }

    /**
     * @param bridgeDeviceName a new device name
     * @see #getBridgeDeviceName()
     */
    public void setBridgeDeviceName(String bridgeDeviceName) {
        mSharedPreferencesEditor.putString(mCtx.getString(R.string.pref_key_device_name), bridgeDeviceName);
        mSharedPreferencesEditor.apply();
    }


    /**
     * @return the IP address of the last bridge this app was connected to or <code>null</code> if this application
     * has never been connected to a bridge.
     */
    public String getLastConnectedIPAddress() {
        return mSharedPreferences.getString(mCtx.getString(R.string.pref_key_bridge_ip), null);
    }

    public boolean setLastConnectedIPAddress(String ipAddress) {
        mSharedPreferencesEditor.putString(mCtx.getString(R.string.pref_key_bridge_ip), ipAddress);
        return (mSharedPreferencesEditor.commit());
    }

    /**
     * @return the name of the "light group" that is stored in the settings of this app. If everything worked correctly
     * the bridge will have a group with this name configured.
     */
    public String getLightGroupName() {
        String result;

        result = mSharedPreferences.getString(mCtx.getString(R.string.pref_key_light_group), null);
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
        mSharedPreferencesEditor.putString(mCtx.getString(R.string.pref_key_light_group), groupName);
        mSharedPreferencesEditor.apply();
    }

    /**
     * @return the id that should be used for the schedule turning the Hue lights on.
     */
    public String getScheduleIdOn() {
        return mSharedPreferences.getString(mCtx.getString(R.string.pref_key_schedule_on), null);
    }

    /**
     * @return the id that should be used for the schedule that increases the brightness of the hue lights.
     */
    public String getScheduleIdBrighten() {
        return mSharedPreferences.getString(mCtx.getString(R.string.pref_key_schedule_brighten), null);
    }

    /**
     * @return the id that should be used for the schedule turning the Hue lights off.
     */
    public String getScheduleIdOff() {
        return mSharedPreferences.getString(mCtx.getString(R.string.pref_key_schedule_off), null);
    }

    /**
     * Sets the schedule id for the schedule that turns the HUE on.
     *
     * @param scheduleId the new schedule id to use by the app.
     */
    public void setScheduleIdOn(String scheduleId) {
        mSharedPreferencesEditor.putString(mCtx.getString(R.string.pref_key_schedule_on), scheduleId);
        mSharedPreferencesEditor.apply();
    }

    /**
     * Sets the schedule id for the schedule that turns the brightness up.
     *
     * @param scheduleId the new schedule id to use by the app.
     */
    public void setScheduleIdBrighten(String scheduleId) {
        mSharedPreferencesEditor.putString(mCtx.getString(R.string.pref_key_schedule_brighten), scheduleId);
        mSharedPreferencesEditor.apply();
    }

    /**
     * Sets the schedule id for the schedule that turns the HUE on.
     *
     * @param scheduleId the new schedule id to use by the app.
     */
    public void setScheduleIdOff(String scheduleId) {
        mSharedPreferencesEditor.putString(mCtx.getString(R.string.pref_key_schedule_off), scheduleId);
        mSharedPreferencesEditor.apply();
    }

    /**
     * @return the name prefix that will be used for the alarm clock schedules.
     */
    private String getBaseScheduleName() {
        return mCtx.getString(R.string.app_name) + " " + Build.MODEL;
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
     * @return the number of minutes, the Hue lights should use to transition into the "on" state.
     */
    public int getTransitionMinutes() {
        String result;

        result = mSharedPreferences.getString(mCtx.getString(R.string.pref_key_transition_minutes), null);

        if (result == null) {
            result = "10";
            setTransitionMinutes(10);
        }


        return Integer.parseInt(result);
    }


    /**
     * Sets the the number of minutes, the Hue lights should use to transition into the "on" state.
     *
     * @param minutes
     */
    public void setTransitionMinutes(int minutes) {
        mSharedPreferencesEditor.putString(mCtx.getString(R.string.pref_key_transition_minutes),
                                           Integer.toString(minutes));
        mSharedPreferencesEditor.apply();
    }

}
