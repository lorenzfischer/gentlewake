package org.github.gentlewake.views;

import android.app.Activity;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHMessageType;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHHueError;

import org.github.gentlewake.R;
import org.github.gentlewake.data.ApplicationPreferences;

import java.util.List;


public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        PHSDKListener {

    public static final String TAG = "GentleWake.MainActy";

    /** We need this reference */
    private PHHueSDK mHueSdk;

    /** We store all settings in the preferences framework using this object. */
    private ApplicationPreferences mPrefs;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String lastIpAddress;
        String lastUsername;
        DrawerLayout drawerLayout;

        mPrefs = ApplicationPreferences.getInstance(this);

        // setup communication channels to hue bridge
        mHueSdk = PHHueSDK.create();
        mHueSdk.getNotificationManager().registerSDKListener(this); // start listening to callbacks from the SDK

        // setup view
        setContentView(R.layout.activity_main);
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, drawerLayout);

        drawerLayout.closeDrawer(findViewById(R.id.navigation_drawer));

        // Automatically try to connect to the last connected IP Address.
        // For multiple bridge support a different implementation is required.
        lastIpAddress = mPrefs.getLastConnectedIPAddress();
        lastUsername = mPrefs.getUsername();
        if (lastIpAddress != null && !lastIpAddress.equals("")) {
            PHAccessPoint lastAccessPoint = new PHAccessPoint();
            lastAccessPoint.setIpAddress(lastIpAddress);
            lastAccessPoint.setUsername(lastUsername);

            if (!mHueSdk.isAccessPointConnected(lastAccessPoint)) {
                PHWizardAlertDialog.getInstance().showProgressDialog(R.string.connecting, this);
                mHueSdk.connect(lastAccessPoint);
            }
        }
//        else {
//            showBridgeListFragment();
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mHueSdk.getSelectedBridge() != null) {
            showSetupFragment();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHueSdk != null) {  // this should not be possible
            mHueSdk.getNotificationManager().unregisterSDKListener(this);
            mHueSdk.disableAllHeartbeat();
            //mHueSdk.destroySDK();
        }
    }


    @Override
    public void onNavigationDrawerItemSelected(int position) {
        switch (position) {
            case 0:
                showBridgeListFragment();
                break;
            case 1:
                showSetupFragment();
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.global, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            getFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new DatastorePreferences()).commit();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showBridgeListFragment() {
        FragmentTransaction transaction;

        transaction = getFragmentManager().beginTransaction();
        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, new BridgeListFragment());
        // Starting the setup activity this way, prevents the PushLink Activity being shown when pressing the back button.
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();

        mTitle = getString(R.string.title_bridge_list);
    }


    public void showSetupFragment() {
        FragmentTransaction transaction;

        transaction = getFragmentManager().beginTransaction();
        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, new SetupFragment());
        // Starting the setup activity this way, prevents the PushLink Activity being shown when pressing the back button.
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();

        mTitle = getString(R.string.title_setup);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
        // see http://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-h
    }




    /* These are all the functions that we need to implement the PHSDKListener interface. */

    @Override
    public void onAccessPointsFound(List<PHAccessPoint> accessPoint) {}

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
        Intent startMainActy;

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "onBridgeConnected");
        }
        mHueSdk.setSelectedBridge(b);
        mHueSdk.enableHeartbeat(b, PHHueSDK.HB_INTERVAL);
        mHueSdk.getLastHeartbeat().put(b.getResourceCache().getBridgeConfiguration().getIpAddress(), System.currentTimeMillis());
        mPrefs.setLastConnectedIPAddress(b.getResourceCache().getBridgeConfiguration().getIpAddress());
        PHWizardAlertDialog.getInstance().closeProgressDialog();

        startMainActy = new Intent(this, MainActivity.class);
        startMainActy.setFlags(startMainActy.getFlags() & Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(startMainActy);
    }

    @Override
    public void onAuthenticationRequired(PHAccessPoint accessPoint) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Authentication Required.");
        }
        mHueSdk.startPushlinkAuthentication(accessPoint);
        startActivity(new Intent(this, PHPushlinkActivity.class));
    }

    @Override
    public void onConnectionResumed(PHBridge bridge) {
        if (MainActivity.this.isFinishing())
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
            Log.e(TAG, "Authentication failed or code 1158");
            PHWizardAlertDialog.getInstance().showErrorDialog(MainActivity.this, message, R.string.btn_ok);
        } else if (code == PHHueError.BRIDGE_NOT_RESPONDING) {
            Log.w(TAG, "Bridge Not Responding . . . ");
            PHWizardAlertDialog.getInstance().closeProgressDialog();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    PHWizardAlertDialog.showErrorDialog(MainActivity.this, message, R.string.btn_ok);
                }
            });

        } else if (code == PHMessageType.BRIDGE_NOT_FOUND) {
            PHWizardAlertDialog.getInstance().closeProgressDialog();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    PHWizardAlertDialog.showErrorDialog(MainActivity.this, message, R.string.btn_ok);
                }
            });
        }
    }


}
