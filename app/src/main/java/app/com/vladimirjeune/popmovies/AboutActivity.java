package app.com.vladimirjeune.popmovies;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Shows user that home selects one level up
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * ONOPTIONITEMSELECTED - Since we want to navigate to the previous screen the user came
     * from when the up button was clicked, instead of a single designated activity we have to
     * go through a different process to get the up button functioning.  We called Activity.
     * getSupportActionBar().setDisplayHomeAsUpEnabled(true) in onCreate() capture the home button
     * and call onBackPressed().  The Manifest was not modified.
     * @param item - MenuItem that was pressed
     * @return - Whether we found what we were searching for
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;  // We found what we were looking for
        }

        return super.onOptionsItemSelected(item);  // Keep looking
    }

}
