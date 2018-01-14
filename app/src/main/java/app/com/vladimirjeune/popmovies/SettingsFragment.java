package app.com.vladimirjeune.popmovies;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;

import app.com.vladimirjeune.popmovies.data.MovieContract;

/** Actually the PreferenceFragmentCompat
 * Created by vladimirjeune on 11/23/17.
 */

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final String TAG = SettingsFragment.class.getSimpleName();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_main);  // Generates a prefscreen for the fragment

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();  // Note, different than Activity
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        int count = getPreferenceScreen().getPreferenceCount();

        // Loop through preferences to find ones you want to set their summaries
        for (int i = 0; i < count; i++) {
            Preference preference = preferenceScreen.getPreference(i);

            if (!(preference instanceof CheckBoxPreference)) {  // Checkboxes would error because uses boolean, not String
                // "" will be kicked out of next called function
                String value = sharedPreferences.getString(getString(R.string.pref_sort_key), "");
                setPreferenceSummary(preference, value);
            }
        }

    }


    /**
     * SETPREFERENCESUMMARY - Take a preference and find the label of the ListPreference item that
     * you need to set the summary for.
     * @param preference - Preference from PreferenceScreen that you probably want to set summary for.
     *                   Should NOT be CheckboxPreference
     * @param value - Value corresponding to the index of the label you want to set as a summary for the ListPreference
     */
    public void setPreferenceSummary(Preference preference, String value) {
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;

            int prefIndex = listPreference.findIndexOfValue(value);
            if (prefIndex >= 0) {
                listPreference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();  // Note, difference to Activity
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();  // Note, difference from Activity
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }


    /**
     * ONSHAREDPREFERENCECHANGED - Called when preference is changed so changes take place immediately, without
     * having to go through onCreate().
     * @param sharedPreferences - Reference to SharedPreferences
     * @param key - Key to find the preference you want
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Activity activity = getActivity();
        Preference preference = findPreference(key);

        // Sort has changed, update the list
        if (key.equals(getString(R.string.pref_sort_key))) {
            activity.getContentResolver().notifyChange(MovieContract.MovieEntry.CONTENT_URI, null);  // TODO:  Just added 12/21
        }

        if (preference != null) {
            if (!(preference instanceof CheckBoxPreference)) {
                String value = sharedPreferences.getString(preference.getKey(), "");  // Just making sure correct key.
                setPreferenceSummary(preference, value);
            }
        }
    }

}
