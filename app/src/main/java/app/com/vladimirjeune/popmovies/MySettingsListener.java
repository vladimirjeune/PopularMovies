package app.com.vladimirjeune.popmovies;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.View;

/**
 * This is a listener for the SnackBar that shows up when there is no internet connection.
 * It will go to the Android Internet Settings Activity so the user can rectify the situation
 * if the cause is that they turned on connectivity.
 * Created by vladimirjeune on 1/3/18.
 */

class MySettingsListener implements View.OnClickListener {
    @Override
    public void onClick(View view) {
        Context context = view.getContext();

        // Going to Settings because the Internet Activities either went to Airplane Mode
        // Or Wireless ON/OFF, not both.
        Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);

        if (settingsIntent.resolveActivity(context.getPackageManager()) != null ) {
            context.startActivity(settingsIntent);
        }
    }
}
