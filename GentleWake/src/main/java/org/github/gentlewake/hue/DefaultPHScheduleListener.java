/* TODO: license */
package org.github.gentlewake.hue;

import com.philips.lighting.hue.listener.PHScheduleListener;
import com.philips.lighting.model.PHHueError;

import java.util.Hashtable;
import java.util.List;

/**
 * This listener has empty implementations for all methods. This makes anonymous implementations a bit less verbose.
 *
 * @author lorenz.fischer@gmail.com
 */
public class DefaultPHScheduleListener extends PHScheduleListener {

    @Override
    public void onSuccess() {
    }

    @Override
    public void onError(int i, String s) {
    }

    @Override
    public void onStateUpdate(Hashtable<String, String> stringStringHashtable, List<PHHueError> phHueErrors) {
    }
}
