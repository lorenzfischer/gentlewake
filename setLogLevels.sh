#!/bin/bash

adb shell setprop  log.tag.GentleWake             DEBUG
adb shell setprop  log.tag.GentleWake.AlarmUtils  DEBUG
adb shell setprop  log.tag.GentleWake.HueUtils    DEBUG
adb shell setprop  log.tag.GentleWake.SyncManager DEBUG