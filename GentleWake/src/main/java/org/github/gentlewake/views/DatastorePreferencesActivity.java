/* TODO: license */
package org.github.gentlewake.views;

import android.app.Activity;
import android.os.Bundle;

/**
 * @author lorenz.fischer@gmail.com
 */
public class DatastorePreferencesActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new DatastorePreferences()).commit();
    }

}