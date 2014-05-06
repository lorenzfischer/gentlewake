/* TODO: license */
package org.github.gentlewake.hue;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;

import java.util.List;

/**
 * Default implementation of {@link PHSDKListener} that has empty implementations of all methods of the interface.
 *
 * @author lorenz.fischer@gmail.com
 */
public class DefaultPHSDKListener implements PHSDKListener {
    @Override
    public void onCacheUpdated(int i, PHBridge bridge) {

    }

    @Override
    public void onBridgeConnected(PHBridge bridge) {

    }

    @Override
    public void onAuthenticationRequired(PHAccessPoint phAccessPoint) {

    }

    @Override
    public void onAccessPointsFound(List<PHAccessPoint> phAccessPoints) {

    }

    @Override
    public void onError(int i, String s) {

    }

    @Override
    public void onConnectionResumed(PHBridge bridge) {

    }

    @Override
    public void onConnectionLost(PHAccessPoint phAccessPoint) {

    }
}
