/* TODO: license */
package org.github.gentlewake.activities;

import android.app.Activity;
import android.os.Bundle;

/**
 * @author lorenz.fischer@gmail.com
 */
public class PreferencesActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new Preferences()).commit();
    }

}