/* TODO: license */
package org.github.gentlewake.views;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import org.github.gentlewake.R;

/**
 * @author lorenz.fischer@gmail.com
 */
public class DatastorePreferences extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the datastore_preferences from an XML resource
        addPreferencesFromResource(R.xml.datastore_preferences);
    }
}
