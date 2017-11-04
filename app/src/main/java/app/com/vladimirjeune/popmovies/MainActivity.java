package app.com.vladimirjeune.popmovies;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private String mTMDKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        obtainTMDKey();

    }

    /**
     * OBTAINTMDKEY - Reads the apiTMD.key file in the assets directory so that we can
     * access: <a href="https://www.themoviedb.org/">https://www.themoviedb.org/</a> with
     * the assigned key.  Keys can be acquired at the site.  Then string can be placed in
     * a file.
     */
    private void obtainTMDKey() {
        // In order for the movie requests to work we must obtain key from file in assets
        try {

            AssetManager assetManager = getAssets();

            Scanner scanner = new Scanner(assetManager.open("apiTmd.key"));

            mTMDKey = scanner.next();

//            Toast.makeText(this, mTMDKey, Toast.LENGTH_LONG).show();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException io ) {
            io.printStackTrace();
        }
    }
}
