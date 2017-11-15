package app.com.vladimirjeune.popmovies;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;

import app.com.vladimirjeune.popmovies.utilities.NetworkUtils;
import app.com.vladimirjeune.popmovies.utilities.OpenTMDJsonUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private String mTMDKey;

    private RecyclerView mRecyclerView;
    private MovieAdapter mMovieAdapter;

    private final int mNumberOfFakeMovies = 20;

    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find RecyclerView from XML
        mRecyclerView = findViewById(R.id.rv_grid_movies);

        boolean reverseLayoutForGridView = false;
        int spanCount = 2;
        GridLayoutManager gridLayoutManager
                = new GridLayoutManager(this, spanCount, GridLayoutManager.VERTICAL, reverseLayoutForGridView);

        mRecyclerView.setLayoutManager(gridLayoutManager);

        // Creates efficiency because no need to unnecessarily measure
        mRecyclerView.setHasFixedSize(true);

        // Adapter links our poster data with views that will show the posters
        mMovieAdapter = new MovieAdapter(this, mNumberOfFakeMovies);

        // Attaches Adapter to RecyclerView in our layout
        mRecyclerView.setAdapter(mMovieAdapter);

        // Test to read JSON list object for most of Movie data
        // Maybe doing too much at once.  Just see if you can get the JSON
        MovieData[] tempMovies;
//        String json = NetworkUtils.obtainTempJSON(this);

//        Log.i(TAG, "onCreate: " + json);
        try {
            String receivedJSON = NetworkUtils.obtainTempJSON(this, NetworkUtils.TEST_MOVIE_LIST);
            Log.i(TAG, "onCreate: >>>" + receivedJSON + "<<<");

            tempMovies = OpenTMDJsonUtils
                    .getPopularOrTopJSON(this, receivedJSON);

            for (int i = 0; i < 20; i++) {
                Log.i(TAG, "onCreate: " + tempMovies[i] + "\n");

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }


        // Test we can work with single movie JSON to get at the runtime information
        int movieRuntime = 0;
        try {
            String receivedSingleMovieJSON = NetworkUtils.obtainTempJSON(this, NetworkUtils.TEST_SINGLE_MOVIE);
            Log.i(TAG, "onCreate: >>>" + receivedSingleMovieJSON + "<<<");

            movieRuntime = OpenTMDJsonUtils
                    .getRuntimeOfSingleMovie(this, receivedSingleMovieJSON);

                Log.i(TAG, "onCreate: Runtime: " + movieRuntime + "\n");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Ctrl shift space files in the ()

        int posterDataSize = 20;
        Drawable[] posterData = new Drawable[posterDataSize];

        for (int i = 0; i < posterDataSize; i++) {

            posterData[i] = ContextCompat.getDrawable(this, R.drawable.tmd_placeholder_poster);
        }

        mMovieAdapter.setPosterData(posterData);

    }

    // DBG: KEEPING UNTIL SURE THE NEW WAY WORKS.
//    /**
//     * OBTAINTMDKEY - Reads the apiTMD.key file in the assets directory so that we can
//     * access: <a href="https://www.themoviedb.org/">https://www.themoviedb.org/</a> with
//     * the assigned key.  Keys can be acquired at the site.  Then string can be placed in
//     * a file.
//     */
//    private void obtainTMDKey() {
//        // In order for the movie requests to work we must obtain key from file in assets
//        try {
//
//            AssetManager assetManager = getAssets();
//
//            Scanner scanner = new Scanner(assetManager.open("apiTmd.key"));
//
//            mTMDKey = scanner.next();
//
////            Toast.makeText(this, mTMDKey, Toast.LENGTH_LONG).show();
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException io ) {
//            io.printStackTrace();
//        }
//    }
}
