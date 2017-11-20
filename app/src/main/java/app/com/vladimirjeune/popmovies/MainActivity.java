package app.com.vladimirjeune.popmovies;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.net.URL;

import app.com.vladimirjeune.popmovies.utilities.NetworkUtils;
import app.com.vladimirjeune.popmovies.utilities.OpenTMDJsonUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private String mTMDKey;

    private RecyclerView mRecyclerView;
    private MovieAdapter mMovieAdapter;

    private final int mNumberOfFakeMovies = 20;

    // Test to read JSON list object for most of Movie data
    // Maybe doing too much at once.  Just see if you can get the JSON
    private MovieData[] tempMovies;

    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "BEGIN::onCreate: ");

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


//        String json = NetworkUtils.obtainTempJSON(this);

//        Log.i(TAG, "onCreate: " + json);
//        try {
//            String receivedJSON = NetworkUtils.obtainTempJSON(this, NetworkUtils.TEST_MOVIE_LIST);
//            Log.i(TAG, "onCreate: >>>" + receivedJSON + "<<<");
//
//            tempMovies = OpenTMDJsonUtils
//                    .getPopularOrTopJSON(this, receivedJSON);
//
//            getRuntimesForMoviesInList();
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }

        // Ctrl shift space files in the ()
//        int posterDataSize = 20;
//        Drawable[] posterData = new Drawable[posterDataSize];
//
//        for (int i = 0; i < posterDataSize; i++) {
//
//            posterData[i] = ContextCompat.getDrawable(this, R.drawable.tmd_placeholder_poster);
//        }
//
//        mMovieAdapter.setMoviesData(posterData);

        loadPreferredMovieList();  // Calls AsyncTask and gets posters for MainPage
        Log.d(TAG, "END::onCreate: ");
    }

    /**
     * GETRUNTIMESFORMOVIESINLIST - Add the runtimes to the Movies that were just created from
     * JSON call that should preceed this one.
     * Note: Makes network call.
     */
    private void getRuntimesForMoviesInList() {
        Log.d(TAG, "BEGIN::getRuntimesForMoviesInList: ");
        // Get runtime for found movies and place in correct Movie Objects.
        if (tempMovies != null) {
            for (int i = 0; i < tempMovies.length; i++) {
                tempMovies[i]
                        .setRuntime(getSingleMovieRuntimeFromTMDB("" + tempMovies[i].getMovieId()));
                Log.i(TAG, "getRuntimesForMoviesInList: " + (i+1) + ":] " + tempMovies[i] + "\n");

            }
        }
        Log.d(TAG, "END::getRuntimesForMoviesInList: ");
    }

    /**
     * GETSINGLEMOVIERUNTIMEFROMTMDB - Get the runtime for the movie with the given movieId.
     * Note: Makes network call.
     * @param aMovieId - Movie Id for movie we are getting the runtime for
     * @return int - Runtime of movie
     */
    private int getSingleMovieRuntimeFromTMDB(String aMovieId) {
        int movieRuntime = 0;
        Log.d(TAG, "BEGIN::getSingleMovieRuntimeFromTMDB: ");
        try {
            String receivedSingleMovieJSON = NetworkUtils
                    .getResponseFromHttpUrl(NetworkUtils
                            .buildUrlForSingleMovie(this, aMovieId));
            Log.i(TAG, "getSingleMovieRuntimeFromTMDB: >>>" + receivedSingleMovieJSON + "<<<");

            movieRuntime = OpenTMDJsonUtils
                    .getRuntimeOfSingleMovie(this, receivedSingleMovieJSON);

            Log.i(TAG, "getSingleMovieRuntimeFromTMDB: Runtime: " + movieRuntime + "\n");

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {  // There was a problem with the network
            e.printStackTrace();
        }
        Log.d(TAG, "END::getSingleMovieRuntimeFromTMDB: ");
        return movieRuntime;
    }

    /**
     * LOADPREFERREDMOVIELIST - Loads the movie list that the user has set in SharedPreferences
     */
    private void loadPreferredMovieList() {
        // Get whether the user wants Popular or Top-Rated from SharedPreferences
        // Then have if to use a Popular URL, or Top-Rated URL call the AsyncTask
        Log.d(TAG, "BEGIN::loadPreferredMovieList: ");
        new TMDBQueryTask().execute(NetworkUtils.buildUrlForPopularOrTopRated(this));
        Log.d(TAG, "END::loadPreferredMovieList: ");
    }


    public class TMDBQueryTask extends AsyncTask<URL, Void, MovieData[]> {

        @Override
        protected MovieData[] doInBackground(URL... urls) {
            Log.d(TAG, "BEGIN::doInBackground: " + urls[0]);
            // If got nothing, return
            if (0 != urls.length) {


                URL url = urls[0];
                String tmdbJsonString = "";
                try {
                    tmdbJsonString = NetworkUtils.getResponseFromHttpUrl(url);  // Popular | Top-Rated

                    Log.i(TAG, "doInBackground: >>>" + tmdbJsonString + "<<<");

                    tempMovies = OpenTMDJsonUtils
                            .getPopularOrTopJSON(MainActivity.this, tmdbJsonString);

                    getRuntimesForMoviesInList();

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            Log.d(TAG, "END::doInBackground: ");
            return tempMovies;
        }

        /**
         * ONPOSTEXECUTE - Picasso handles its own threading, so completing Movie object on main thread
         * @param movieDatas
         */
        @Override
        protected void onPostExecute(MovieData[] movieDatas) {
            // Used movieDatas because cannot return Void
            Log.d(TAG, "BEGIN::onPostExecute: ");

//            if (tempMovies != null) {
//                // Using imageView to get Drawable
//                ImageView imageView = new ImageView(MainActivity.this);
//
//                Drawable[] retPosters = new Drawable[tempMovies.length];
//                for (int i = 0; i < tempMovies.length; i++) {
//
//                    if (!(tempMovies[i].getPosterPath().equals(""))) {
//                        URL urlForPoster = NetworkUtils.buildURLForImage(tempMovies[i].getPosterPath());
//
//                        Log.d(TAG, "onPostExecute: Completed poster URL: " + urlForPoster);
//                        Picasso.with(MainActivity.this)
//                                .load(urlForPoster.toString())
//                                .placeholder(R.drawable.tmd_placeholder_poster)
//                                .error(R.drawable.tmd_error_poster)
//                                .into(imageView);
//
//                        // Save Drawable
//                        Log.i(TAG, "onPostExecute: Picasso ImageView: " + imageView.getDrawable() );
//                        retPosters[i] = imageView.getDrawable();
//                        tempMovies[i].setPoster(retPosters[i]);
//                    }
//                }
//
//                mMovieAdapter.setMoviesData(retPosters);
//            }

            mMovieAdapter.setMoviesData(movieDatas);
            Log.d(TAG, "END::onPostExecute: ");
        }
    }

}
