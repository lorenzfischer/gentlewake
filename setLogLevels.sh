#!/bin/bash

adb shell setprop  log.tag.GentleWake                DEBUG
adb shell setprop  log.tag.GentleWake.AlarmUtils     DEBUG
adb shell setprop  log.tag.GentleWake.HueUtils       DEBUG

adb shell setprop  log.tag.GentleWake.SyncReceiver   DEBUG
adb shell setprop  log.tag.GentleWake.SyncManager    DEBUG
adb shell setprop  log.tag.GentleWake.SyncService    DEBUG

adb shell setprop  log.tag.GentleWake.ListActy       DEBUG
adb shell setprop  log.tag.GentleWake.MainAppActy    DEBUG
