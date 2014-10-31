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
 * 
 * For first time use, a Bridge search (UPNP) is performed and a list of all available bridges is displayed (and clicking one of them shows the PushLink dialog allowing authentication).
 * The last connected Bridge IP Address and Username are stored in SharedPreferences.
 * 
 * For subsequent usage the app automatically connects to the last connected bridge.
 * When connected the MainApplicationActivity Activity is started.  This is where you should start implementing your Hue App!  Have fun!
 * 
 * For explanation on key concepts visit: https://github.com/PhilipsHue/PhilipsHueSDK-Java-MultiPlatform-Android
 * 
 * @author SteveyO
 *
 */
public class BridgeListFragment extends Fragment implements OnItemClickListener {

    private PHHueSDK mHueSdk;
    public static final String TAG = "GentleWake.ListActy";
    private ApplicationPreferences mPrefs;
    private AccessPointListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result;

        mPrefs = ApplicationPreferences.getInstance(getActivity());

        // Gets an instance of the Hue SDK.
        mHueSdk = PHHueSDK.create();

        // Set the Device Name (name of your app). This will be stored in your bridge whitelist entry.
        mHueSdk.setDeviceName(mPrefs.getBridgeDeviceName());

        // Register the PHSDKListener to receive callbacks from the bridge.
        mHueSdk.getNotificationManager().registerSDKListener(listener);

        adapter = new AccessPointListAdapter(getActivity(), mHueSdk.getAccessPointsFound());

        result = inflater.inflate(R.layout.fragment_bridgelist, container, false);

        ListView accessPointList = (ListView) result.findViewById(R.id.bridge_list);
        accessPointList.setOnItemClickListener(this);
        accessPointList.setAdapter(adapter);

        // Try to automatically connect to the last known bridge.  For first time use this will be empty so a bridge search is automatically started.
        mPrefs = ApplicationPreferences.getInstance(getActivity());
        String lastIpAddress   = mPrefs.getLastConnectedIPAddress();
        String lastUsername    = mPrefs.getUsername();

        // Automatically try to connect to the last connected IP Address.  For multiple bridge support a different implementation is required.
        if (lastIpAddress !=null && !lastIpAddress.equals("")) {
            PHAccessPoint lastAccessPoint = new PHAccessPoint();
            lastAccessPoint.setIpAddress(lastIpAddress);
            lastAccessPoint.setUsername(lastUsername);

            if (!mHueSdk.isAccessPointConnected(lastAccessPoint)) {
                PHWizardAlertDialog.getInstance().showProgressDialog(R.string.connecting, getActivity());
                mHueSdk.connect(lastAccessPoint);
            }
        }
        else {  // First time use, so perform a bridge search.
            doBridgeSearch();
        }

        setHasOptionsMenu(true); // tell the super activity that we have a menu button that we want to be shown

        return result;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.w(TAG, "Inflating bridgelist menu");
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

    // Local SDK Listener
    private PHSDKListener listener = new PHSDKListener() {

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
                            adapter.updateData(mHueSdk.getAccessPointsFound());
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
            Log.w(TAG, "On CacheUpdated");

        }

        @Override
        public void onBridgeConnected(PHBridge b) {
            mHueSdk.setSelectedBridge(b);
            mHueSdk.enableHeartbeat(b, PHHueSDK.HB_INTERVAL);
            mHueSdk.getLastHeartbeat().put(b.getResourceCache().getBridgeConfiguration() .getIpAddress(), System.currentTimeMillis());
            mPrefs.setLastConnectedIPAddress(b.getResourceCache().getBridgeConfiguration().getIpAddress());
            mPrefs.setUsername(mPrefs.getUsername());
            PHWizardAlertDialog.getInstance().closeProgressDialog();     
            showSetupFragment();
        }

        @Override
        public void onAuthenticationRequired(PHAccessPoint accessPoint) {
            Log.w(TAG, "Authentication Required.");
            mHueSdk.startPushlinkAuthentication(accessPoint);
            startActivity(new Intent(getActivity(), PHPushlinkActivity.class));
           
        }

        @Override
        public void onConnectionResumed(PHBridge bridge) {
            if (BridgeListFragment.this.isRemoving()) // todo: this was Activity.isFinishing().. is this the correct replacement?
                return;
            
            Log.v(TAG, "onConnectionResumed on ip " + bridge.getResourceCache().getBridgeConfiguration().getIpAddress());
            mHueSdk.getLastHeartbeat().put(bridge.getResourceCache().getBridgeConfiguration().getIpAddress(),  System.currentTimeMillis());
            for (int i = 0; i < mHueSdk.getDisconnectedAccessPoint().size(); i++) {

                if (mHueSdk.getDisconnectedAccessPoint().get(i).getIpAddress().equals(bridge.getResourceCache().getBridgeConfiguration().getIpAddress())) {
                    mHueSdk.getDisconnectedAccessPoint().remove(i);
                }
            }

        }

        @Override
        public void onConnectionLost(PHAccessPoint accessPoint) {
            Log.v(TAG, "onConnectionLost : " + accessPoint.getIpAddress());
            if (!mHueSdk.getDisconnectedAccessPoint().contains(accessPoint)) {
                mHueSdk.getDisconnectedAccessPoint().add(accessPoint);
            }
        }
        
        @Override
        public void onError(int code, final String message) {
            Log.e(TAG, "on Error Called : " + code + ":" + message);

            if (code == PHHueError.NO_CONNECTION) {
                Log.w(TAG, "On No Connection");
            } 
            else if (code == PHHueError.AUTHENTICATION_FAILED || code==1158) {
                PHWizardAlertDialog.getInstance().closeProgressDialog();
            } 
            else if (code == PHHueError.BRIDGE_NOT_RESPONDING) {
                Log.w(TAG, "Bridge Not Responding . . . ");
                PHWizardAlertDialog.getInstance().closeProgressDialog();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PHWizardAlertDialog.showErrorDialog(getActivity(), message, R.string.btn_ok);
                    }
                }); 

            } 
            else if (code == PHMessageType.BRIDGE_NOT_FOUND) {
                PHWizardAlertDialog.getInstance().closeProgressDialog();

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PHWizardAlertDialog.showErrorDialog(getActivity(), message, R.string.btn_ok);
                    }
                });                
            }
        }
    };


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (listener !=null) {
            mHueSdk.getNotificationManager().unregisterSDKListener(listener);
        }
        mHueSdk.disableAllHeartbeat();
        //mHueSdk.destroySDK();
    }
        
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        ApplicationPreferences prefs = ApplicationPreferences.getInstance(getActivity());
        PHAccessPoint accessPoint = (PHAccessPoint) adapter.getItem(position);
        accessPoint.setUsername(prefs.getUsername());
        
        PHBridge connectedBridge = mHueSdk.getSelectedBridge();

        if (connectedBridge != null) {
            String connectedIP = connectedBridge.getResourceCache().getBridgeConfiguration().getIpAddress();
            if (connectedIP != null) {   // We are already connected here:-
                mHueSdk.disableHeartbeat(connectedBridge);
                mHueSdk.disconnect(connectedBridge);
            }
        }
        PHWizardAlertDialog.getInstance().showProgressDialog(R.string.connecting, getActivity());
        mHueSdk.connect(accessPoint);
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
    
}