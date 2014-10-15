Revision History
================
Version-Code    Version-Name    Change-Log
5               "1.1.2"         - Prevent NullPointerExceptions in the UI, when the "selected bridge" is null.
                                - Prevent NullPointerExceptions in the syncing service by implementing an sdk listener.
                                - Don't use PHHueSDK.destroySDK()
                                - Require permission android.permission.ACCESS_NETWORK_STATE in order to prevent a sync if the wifi is not connected
                                - Wait for 5 seconds when receiving the wifi-connection intent, to give the connection a chance to finish initializing