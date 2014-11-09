package org.github.gentlewake.views;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHMessageType;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHHueError;

import org.github.gentlewake.R;
import org.github.gentlewake.data.AccessPointListAdapter;
import org.github.gentlewake.data.ApplicationPreferences;

import java.util.List;

/**
 * BridgeListActivity - The starting point in your own Hue App.
 * <p/>
 * For first time use, a Bridge search (UPNP) is performed and a list of all available bridges is displayed (and clicking one of them shows the PushLink dialog allowing authentication).
 * The last connected Bridge IP Address and Username are stored in SharedPreferences.
 * <p/>
 * For subsequent usage the app automatically connects to the last connected bridge.
 * When connected the MainApplicationActivity Activity is started.  This is where you should start implementing your Hue App!  Have fun!
 * <p/>
 * For explanation on key concepts visit: https://github.com/PhilipsHue/PhilipsHueSDK-Java-MultiPlatform-Android
 *
 * @author SteveyO
 */
public class BridgeListFragment extends Fragment implements OnItemClickListener, PHSDKListener {

    private PHHueSDK mHueSdk;
    public static final String TAG = "GentleWake.ListFgmt";
    private ApplicationPreferences mPrefs;
    private AccessPointListAdapter mListAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result;

        mPrefs = ApplicationPreferences.getInstance(getActivity());

        // Gets an instance of the Hue SDK.
        mHueSdk = PHHueSDK.create();

        // Set the Device Name (name of your app). This will be stored in your bridge whitelist entry.
        mHueSdk.setDeviceName(mPrefs.getBridgeDeviceName());

        mListAdapter = new AccessPointListAdapter(getActivity(), mHueSdk.getAccessPointsFound());

        result = inflater.inflate(R.layout.fragment_bridgelist, container, false);

        ListView accessPointList = (ListView) result.findViewById(R.id.bridge_list);
        accessPointList.setOnItemClickListener(this);
        accessPointList.setAdapter(mListAdapter);

        setHasOptionsMenu(true); // tell the super activity that we have a menu button that we want to be shown

        // TODO: remove this code from here and place it into the MainActivity.
        // Try to automatically connect to the last known bridge.  For first time use this will be empty so a bridge search is automatically started.
        mPrefs = ApplicationPreferences.getInstance(getActivity());
        String lastIpAddress = mPrefs.getLastConnectedIPAddress();
        String lastUsername = mPrefs.getUsername();

        // Automatically try to connect to the last connected IP Address.  For multiple bridge support a different implementation is required.
        if (lastIpAddress != null && !lastIpAddress.equals("")) {
            PHAccessPoint lastAccessPoint = new PHAccessPoint();
            lastAccessPoint.setIpAddress(lastIpAddress);
            lastAccessPoint.setUsername(lastUsername);

            if (!mHueSdk.isAccessPointConnected(lastAccessPoint)) {
                PHWizardAlertDialog.getInstance().showProgressDialog(R.string.connecting, getActivity());
                mHueSdk.connect(lastAccessPoint);
            }
        } else {
            doBridgeSearch();
        }

        return result;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Inflating bridgelist menu");
        }
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.bridgelist, menu);
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
            case R.id.find_new_bridge:
                doBridgeSearch();
                break;
        }
        return true;
    }


    @Override
    public void onResume() {
        super.onResume();

        // start listening to callbacks from the SDK
        mHueSdk.getNotificationManager().registerSDKListener(this);
    }

    @Override
    public void onPause() {
        super.onDestroy();
        mHueSdk.getNotificationManager().unregisterSDKListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHueSdk.disableAllHeartbeat();
        //mHueSdk.destroySDK();
    }

    public void doBridgeSearch() {
        PHWizardAlertDialog.getInstance().showProgressDialog(R.string.search_progress, getActivity());
        PHBridgeSearchManager sm = (PHBridgeSearchManager) mHueSdk.getSDKService(PHHueSDK.SEARCH_BRIDGE);
        // Start the UPNP Searching of local bridges.
        sm.search(true, true);
    }

    // Starting the setup activity this way, prevents the PushLink Activity being shown when pressing the back button.
    public void showSetupFragment() {
        FragmentTransaction transaction;

        transaction = getFragmentManager().beginTransaction();
        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, new SetupFragment());
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }

    /* We need to implement this method, so we can react when the user selects a bridge. */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ApplicationPreferences prefs;
        PHAccessPoint selectedAccessPoint;
        PHBridge connectedBridge;

        // if we are currently connected to a bridge, disconnect first
        connectedBridge = mHueSdk.getSelectedBridge();
        if (connectedBridge != null) {
            String connectedIP = connectedBridge.getResourceCache().getBridgeConfiguration().getIpAddress();
            if (connectedIP != null) {
                mHueSdk.disableHeartbeat(connectedBridge);
                mHueSdk.disconnect(connectedBridge);
            }
        }

        // show a dialog while we wait for the connection to happen
        PHWizardAlertDialog.getInstance().showProgressDialog(R.string.connecting, getActivity());

        // set the configured username
        prefs = ApplicationPreferences.getInstance(getActivity());
        selectedAccessPoint = (PHAccessPoint) mListAdapter.getItem(position);
        selectedAccessPoint.setUsername(prefs.getUsername());

        // connect to the newly selected bridge
        mHueSdk.connect(selectedAccessPoint);
    }


    /* These are all the functions that we need to implement the PHSDKListener interface. */

    @Override
    public void onAccessPointsFound(List<PHAccessPoint> accessPoint) {
        Log.w(TAG, "Access Points Found. " + accessPoint.size());

        PHWizardAlertDialog.getInstance().closeProgressDialog();
        if (accessPoint != null && accessPoint.size() > 0) {
            mHueSdk.getAccessPointsFound().clear();
            mHueSdk.getAccessPointsFound().addAll(accessPoint);

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mListAdapter.updateData(mHueSdk.getAccessPointsFound());
                }
            });

        } else {
            // FallBack Mechanism.  If a UPNP Search returns no results then perform an IP Scan. Of course it could fail as the user has disconnected their bridge, connected to a wrong network or disabled Network Discovery on their router so it is not guaranteed to work.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                PHWizardAlertDialog.getInstance().showProgressDialog(R.string.search_progress, getActivity());
                PHBridgeSearchManager sm = (PHBridgeSearchManager) mHueSdk.getSDKService(PHHueSDK.SEARCH_BRIDGE);
                // Start the IP Scan Search if the UPNP and NPNP return 0 results.
                sm.search(false, false, true);
            }
        }

    }

    @Override
    public void onCacheUpdated(int flags, PHBridge bridge) {
        // Here you receive notifications that the BridgeResource Cache was updated. Use the PHMessageType to
        // check which cache was updated, e.g.
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "CacheUpdated");
        }
    }

    @Override
    public void onBridgeConnected(PHBridge b) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "onBridgeConnected");
        }
        mHueSdk.setSelectedBridge(b);
        mHueSdk.enableHeartbeat(b, PHHueSDK.HB_INTERVAL);
        mHueSdk.getLastHeartbeat().put(b.getResourceCache().getBridgeConfiguration().getIpAddress(), System.currentTimeMillis());
        mPrefs.setLastConnectedIPAddress(b.getResourceCache().getBridgeConfiguration().getIpAddress());
        //mPrefs.setUsername(mPrefs.getUsername()); // why did I put this?
        PHWizardAlertDialog.getInstance().closeProgressDialog();
        showSetupFragment();
    }

    @Override
    public void onAuthenticationRequired(PHAccessPoint accessPoint) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Authentication Required.");
        }
        mHueSdk.startPushlinkAuthentication(accessPoint);
        startActivity(new Intent(getActivity(), PHPushlinkActivity.class));

    }

    @Override
    public void onConnectionResumed(PHBridge bridge) {
        if (BridgeListFragment.this.isRemoving()) // todo: this was Activity.isFinishing().. is this the correct replacement?
            return;

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "onConnectionResumed on ip " + bridge.getResourceCache().getBridgeConfiguration().getIpAddress());
        }
        mHueSdk.getLastHeartbeat().put(bridge.getResourceCache().getBridgeConfiguration().getIpAddress(), System.currentTimeMillis());
        for (int i = 0; i < mHueSdk.getDisconnectedAccessPoint().size(); i++) {

            if (mHueSdk.getDisconnectedAccessPoint().get(i).getIpAddress().equals(bridge.getResourceCache().getBridgeConfiguration().getIpAddress())) {
                mHueSdk.getDisconnectedAccessPoint().remove(i);
            }
        }

    }

    @Override
    public void onConnectionLost(PHAccessPoint accessPoint) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "onConnectionLost : " + accessPoint.getIpAddress());
        }
        if (!mHueSdk.getDisconnectedAccessPoint().contains(accessPoint)) {
            mHueSdk.getDisconnectedAccessPoint().add(accessPoint);
        }
    }

    @Override
    public void onError(int code, final String message) {
        if (Log.isLoggable(TAG, Log.ERROR)) {
            Log.e(TAG, "on Error Called : " + code + ":" + message);
        }

        if (code == PHHueError.NO_CONNECTION) {
            Log.w(TAG, "On No Connection");
        } else if (code == PHHueError.AUTHENTICATION_FAILED || code == 1158) {
            PHWizardAlertDialog.getInstance().closeProgressDialog();
        } else if (code == PHHueError.BRIDGE_NOT_RESPONDING) {
            Log.w(TAG, "Bridge Not Responding . . . ");
            PHWizardAlertDialog.getInstance().closeProgressDialog();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    PHWizardAlertDialog.showErrorDialog(getActivity(), message, R.string.btn_ok);
                }
            });

        } else if (code == PHMessageType.BRIDGE_NOT_FOUND) {
            PHWizardAlertDialog.getInstance().closeProgressDialog();

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    PHWizardAlertDialog.showErrorDialog(getActivity(), message, R.string.btn_ok);
                }
            });
        }
    }

}