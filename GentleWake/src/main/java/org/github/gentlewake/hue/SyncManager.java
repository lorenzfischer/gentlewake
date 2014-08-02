/* TODO: license */
package org.github.gentlewake.hue;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.philips.lighting.hue.listener.PHGroupListener;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResourcesCache;
import com.philips.lighting.model.PHGroup;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import com.philips.lighting.model.PHSchedule;

import org.github.gentlewake.R;
import org.github.gentlewake.data.ApplicationPreferences;
import org.github.gentlewake.util.AlarmUtils;
import org.github.gentlewake.util.Callback;
import org.github.gentlewake.util.ValueCallback;

import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

/**
 * This class deals with setting the schedules to sync the alarm of the phone with the Hue system. In each
 * synchronization action there are three schedules set:
 * <ol>
 * <li>Schedule On: This schedule turns the light on and sets it to the lowest brightness.</li>
 * <li>Schedule Brighten: This schedule increases the brightness slowly to its maximum, so that the maximum
 * is reached, when the alarm clock of the phone goes off.</li>
 * <li>Schedule Off: This schedule turns the Hue lights off.</li>
 * </ol>
 *
 * @author lorenz.fischer@gmail.com
 */
public class SyncManager {

    private static final String TAG = "GentleWake.SyncManager";

    private Context mCtx;

    /** the object that can be used to communicate with the Hue bridge. */
    private PHBridge mHueBridge;

    /** a reference to the preferences object that contains all the configuration of Gentlewake. */
    private ApplicationPreferences mPrefs;

    /**
     * @param ctx    the context that can be used to retrieve resources.
     * @param bridge an object that the sync manager can use to configure the Hue bridge.
     */
    public SyncManager(Context ctx, PHBridge bridge) {
        this.mCtx = ctx;
        this.mHueBridge = bridge;
        this.mPrefs = ApplicationPreferences.getInstance(ctx);
    }

    /**
     * Checks the cached configuration of the bridge for the  existence of a group with the provided name.
     *
     * @param alarmLightGroupName the name of the light group whose existence in the bridge configuration we should verify.
     * @return true if a group with that name is configured on the bridge, false otherwise.
     */
    public boolean isGroupExistsOnBridge(String alarmLightGroupName) {
        PHGroup alarmGroup;
        PHBridgeResourcesCache bridgeInfos;


        bridgeInfos = this.mHueBridge.getResourceCache();
        alarmGroup = null;

        for (PHGroup group : bridgeInfos.getAllGroups()) {
            if (group.getName().equals(alarmLightGroupName)) {
                alarmGroup = group;
                break;
            }
        }

        return alarmGroup != null;
    }

    /**
     * This method ensures that there is a registered "light group" configured on the hue bridge that we can use
     * when setting alarms and returns it, so we can set alarms.
     *
     * @param alarmLightGroupName the name of the light group whose existence in the bridge configuration we should verify.
     * @param callback            as we have to communicate with the bridge in order to make sure the light group exists, this
     *                            method may take a while to complete. By passing a callback method you can make sure you get
     *                            called when we have the group ready. This parameter can be <code>null</code>. In this case
     *                            the callback will be ignored.
     */
    public void ensureAlarmLightGroup(String alarmLightGroupName, final Callback callback) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Checking for existence of light group...");
        }

        if (callback != null) {
            if (isGroupExistsOnBridge(alarmLightGroupName)) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Group '" + alarmLightGroupName + "' existed");
                }
                callback.go();
            } else { // There was no alarm group configured, yet. So we do that now ...
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Group '" + alarmLightGroupName + "' did not exist. Creating it...");
                }

                PHBridgeResourcesCache bridgeInfos;
                List<PHLight> allLights;
                String[] allLightIds;
                int idx;

                bridgeInfos = this.mHueBridge.getResourceCache();
                allLights = bridgeInfos.getAllLights();
                allLightIds = new String[allLights.size()];
                idx = 0;
                for (PHLight light : allLights) {
                    allLightIds[idx++] = light.getIdentifier();
                }
                this.mHueBridge.createGroup(alarmLightGroupName, allLightIds, new PHGroupListener() {
                    @Override
                    public void onCreated(PHGroup group) {
                        super.onCreated(group);
                        callback.go(); // pass the group on to the callback
                    }

                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError(int i, String s) {
                        if (Log.isLoggable(TAG, Log.ERROR)) {
                            Log.e(TAG, "Error while creating group. Error code = " + Integer.toString(i) +
                                    " Error msg = '" + s + "'");
                        }
                    }

                    @Override
                    public void onStateUpdate(Hashtable<String, String> stringStringHashtable,
                                              List<PHHueError> phHueErrors) {

                    }
                });
            }
        }
    }

    /**
     * This method sets the time for Hue to turn on the light group with the supplied name.
     * <p/>
     * This method makes sure that the configured light group exist on the bridge.
     *
     * @param primaryMessageCallback   this callback will be called with a message for the user after the schedule that
     *                                 turns the Hue lights "on" has been changed or if there is some general information
     *                                 that is important for the user.
     * @param secondaryMessageCallback this callback will be called with messages for the user log that are not
     *                                 primarily important, but that may still be useful to show in a user log.
     */
    public void syncAlarm(final ValueCallback<String> primaryMessageCallback, final ValueCallback<String> secondaryMessageCallback) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Syncing alarms");
        }
        String lightGroupName;
        lightGroupName = this.mPrefs.getLightGroupName();

        if (!isGroupExistsOnBridge(lightGroupName)) {
            ensureAlarmLightGroup(lightGroupName, new Callback() {
                @Override
                public void go() {
                    // todo: should I prevent a stack overflow, here?
                    syncAlarm(primaryMessageCallback); // self-call, but this time the group should exist
                }
            });
        } else {
            PHBridge bridge;
            Calendar scheduleOnCalendar;
            String scheduleIdOn;
            String scheduleNameOn;
            Calendar scheduleBrightenCalendar;
            String scheduleIdBrighten;
            String scheduleNameBrighten;
            Calendar scheduleOffCalendar;
            String scheduleIdOff;
            String scheduleNameOff;
            Date nextAlarm;

            nextAlarm = AlarmUtils.getNextAlarm(mCtx);

            if (nextAlarm != null) {

                // the first schedule turns the light on
                scheduleOnCalendar = Calendar.getInstance();
                scheduleOnCalendar.setTime(nextAlarm);
                scheduleOnCalendar.add(Calendar.MINUTE, mPrefs.getTransitionMinutes() * -1); // n mins before alarm
                scheduleIdOn = mPrefs.getScheduleIdOn();
                scheduleNameOn = mPrefs.getScheduleNameOn();
                createUpdateSchedule(scheduleIdOn, scheduleNameOn, lightGroupName,
                        scheduleOnCalendar.getTime(), createLightStateOn(), primaryMessageCallback,
                        new ValueCallback<PHSchedule>() {
                            @Override
                            public void go(PHSchedule createdSchedule) {
                                mPrefs.setScheduleIdOn(createdSchedule.getIdentifier());
                            }
                        }
                );

                // the second schedule brightens the light slowly over the course of
                scheduleBrightenCalendar = Calendar.getInstance();
                scheduleBrightenCalendar.setTime(scheduleOnCalendar.getTime());
                scheduleBrightenCalendar.add(Calendar.SECOND, 10); // start 10 seconds after turning the light on
                scheduleIdBrighten = mPrefs.getScheduleIdBrighten();
                scheduleNameBrighten = mPrefs.getScheduleNameBrighten();
                createUpdateSchedule(scheduleIdBrighten, scheduleNameBrighten, lightGroupName,
                        scheduleBrightenCalendar.getTime(),
                        // technically, this will result in the light reaching its brightest setting 10 seconds after
                        // the alarm of the phone goes off, as we will start the brighten process 10 seconds after
                        // the turn-on schedule.
                        createLightStateBrighten(),
                        secondaryMessageCallback,
                        new ValueCallback<PHSchedule>() {
                            @Override
                            public void go(PHSchedule createdSchedule) {
                                mPrefs.setScheduleIdBrighten(createdSchedule.getIdentifier());
                            }
                        }
                );

                // todo: make the "one hour" configurable
                // the last schedule will turn the light off after one hour
                scheduleOffCalendar = Calendar.getInstance();
                scheduleOffCalendar.setTime(nextAlarm);
                scheduleOffCalendar.add(Calendar.HOUR, 1);  // turn the light off one hour after the alarm went off
                scheduleIdOff = mPrefs.getScheduleIdOff();
                scheduleNameOff = mPrefs.getScheduleNameOff();
                createUpdateSchedule(scheduleIdOff, scheduleNameOff, lightGroupName, scheduleOffCalendar.getTime(),
                        createLightStateOff(), secondaryMessageCallback,
                        new ValueCallback<PHSchedule>() {
                            @Override
                            public void go(PHSchedule createdSchedule) {
                                mPrefs.setScheduleIdOff(createdSchedule.getIdentifier());
                            }
                        }
                );

            } // end if (nextAlarm != null)
        }
    }

    /**
     * This method sets the time for Hue to turn on the light group with the supplied name.
     * <p/>
     * This method makes sure that the configured light group exist on the bridge.
     *
     * @param messageCallback this callback will be called with a message for the user. It will contain information
     *                        about whether Hue's schedules were synchronized with the alarm settings of this phone.
     */
    public void syncAlarm(final ValueCallback<String> messageCallback) {
        // sync the alarm using the same callback for both schedules.
        syncAlarm(messageCallback, messageCallback);
    }

    /**
     * Checks if there is a schedule with the given id configured. If yes, the method checks if the light group is set
     * correctly and sets the date and the light state on the schedule. The success/failure of the operation will
     * be communicated ot the callback.
     *
     * @param scheduleId       the id of the schedule to find.
     * @param scheduleName     the name of the schedule that will be set, should the schedule be created newly.
     * @param lightGroupName   the light group that should be used.
     * @param scheduleDate     the time for the schedule to be executed at.
     * @param lightState       the light state to set on the schedule.
     * @param messageCallback  the callback to inform about the success/failure of the operation. If this value is
     *                         <code>null</code> it will be ignored.
     * @param scheduleCallback in case of the schedule not existing on the bridge and a new schedule has to be created,
     *                         this method will be called with the new schedule as an argument.
     */
    private void createUpdateSchedule(String scheduleId,
                                      String scheduleName,
                                      String lightGroupName,
                                      Date scheduleDate,
                                      PHLightState lightState,
                                      final ValueCallback<String> messageCallback,
                                      final ValueCallback<PHSchedule> scheduleCallback) {
        final PHSchedule schedule;

        if (scheduleId != null) {
            schedule = this.mHueBridge.getResourceCache().getSchedules().get(scheduleId);
        } else {
            schedule = null;
        }

        if (schedule == null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Schedule with id '" + scheduleId + "' did not exist. Creating it...");
            }

            final PHSchedule newSchedule;

            newSchedule = new PHSchedule(scheduleName);
            newSchedule.setGroupIdentifier(lightGroupName);
            newSchedule.setDescription(this.mCtx.getString(R.string.schedule_description));
            newSchedule.setLightState(lightState);
            newSchedule.setDate(scheduleDate);

            // creating schedule on bridge
            this.mHueBridge.createSchedule(newSchedule, new DefaultPHScheduleListener() {
                @Override
                public void onCreated(PHSchedule createdSchedule) {
                    String msg = "New schedule '" + createdSchedule.getName() + "' created for "
                            + createdSchedule.getDate();
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, msg);
                    }

                    if (messageCallback != null) {
                        messageCallback.go(msg);
                    }

                    scheduleCallback.go(createdSchedule);
                }

                @Override
                public void onError(int i, String s) {
                    if (Log.isLoggable(TAG, Log.WARN)) {
                        Log.w(TAG, "Error while creating Schedule '" + newSchedule.getName() + "'. Error code " + i + ": " + s);
                    }

                    // todo: handle the case when the light group does not exist on the bridge (e.g. if it was renamed
                    // in the settings)
                }
            });
        } else {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Schedule existed. Checking if scheduling of Hue is necessary");
            }

            String scheduleLightGroupId;

            scheduleLightGroupId = schedule.getGroupIdentifier();

            if (!lightGroupName.equals(scheduleLightGroupId)) {
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, "Light group identifier on schedule '" + schedule.getName() + "' was set to '" +
                            scheduleLightGroupId + "'. Overwriting with new value '" + lightGroupName + "'");
                }
                schedule.setGroupIdentifier(lightGroupName);
            }

            // updating schedule on bridge if necessary
            schedule.setLightState(lightState);
            schedule.setDate(scheduleDate);
            this.mHueBridge.updateSchedule(schedule, new DefaultPHScheduleListener() {
                @Override
                public void onSuccess() {
                    String msg = "Existing Hue schedule '" + schedule.getName() +
                            "' updated for time " + schedule.getDate();
                    if (messageCallback != null) {
                        messageCallback.go(msg);
                    } else {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, msg);
                        }
                    }
                }

                @Override
                public void onError(int i, String s) {
                    if (Log.isLoggable(TAG, Log.WARN)) {
                        Log.w(TAG, "Error code " + i + ":" + s);
                    }
                }
            });
        }
    }

    /**
     * @return a light state to turn the hue on.
     */
    private PHLightState createLightStateOn() {
        PHLightState scheduleLightState;
        scheduleLightState = new PHLightState();
        scheduleLightState.setBrightness(1);
        scheduleLightState.setOn(true);
        return scheduleLightState;
    }

    /**
     * Creates a hue lights state object that transitions from the lights current state to full brightness within
     * the time configured in {@link org.github.gentlewake.data.ApplicationPreferences#getTransitionMinutes()}.
     *
     * @return a light state to turn the hue on.
     */
    private PHLightState createLightStateBrighten() {
        PHLightState scheduleLightState;
        scheduleLightState = new PHLightState();
        // the transition time is measured in 100ms, so to get from minutes to 100ms we need * 60 * 10
        scheduleLightState.setTransitionTime(this.mPrefs.getTransitionMinutes() * 60 * 10);
        scheduleLightState.setBrightness(255);
        return scheduleLightState;
    }

    /**
     * @return a light state to turn the hue on.
     */
    private PHLightState createLightStateOff() {
        PHLightState scheduleLightState;
        scheduleLightState = new PHLightState();
        scheduleLightState.setOn(false);
        return scheduleLightState;
    }

}
