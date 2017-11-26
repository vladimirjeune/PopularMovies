package app.com.vladimirjeune.popmovies;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;

/**
 * Created by vladimirjeune on 11/23/17.
 */

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_main);  // Generates a prefscreen for the fragment

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();  // Note how different than Activity
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        int count = getPreferenceScreen().getPreferenceCount();


    }
}
