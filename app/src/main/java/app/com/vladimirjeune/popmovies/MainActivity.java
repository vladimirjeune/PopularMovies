package app.com.vladimirjeune.popmovies;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.net.URL;

import app.com.vladimirjeune.popmovies.data.MovieContract.MovieEntry;
import app.com.vladimirjeune.popmovies.data.MovieDBHelper;
import app.com.vladimirjeune.popmovies.utilities.NetworkUtils;
import app.com.vladimirjeune.popmovies.utilities.OpenTMDJsonUtils;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private String mTMDKey;

    private RecyclerView mRecyclerView;
    private MovieAdapter mMovieAdapter;

    private final int mNumberOfFakeMovies = 20;

    // Test to read JSON list object for most of Movie data
    // Maybe doing too much at once.  Just see if you can get the JSON
//    private MovieData[] tempMovies;
    private ContentValues[] movieContentValues;
    private SQLiteDatabase mDb;

    private Toast mToast;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "BEGIN::onCreate: ");

        MovieDBHelper movieDBHelper = new MovieDBHelper(this);

        mDb = movieDBHelper.getWritableDatabase();

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

        setupSharedPreferences();

        loadPreferredMovieList();  // Calls AsyncTask and gets posters for MainPage
        Log.d(TAG, "END::onCreate: ");
    }

    /**
     * SETUPSHAREDPREFERENCES - Anything having to do with SharedPreferences will be handled here.
     *
     */
    private void setupSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // TODO: Do stuff here, you want to be able to pick from Pop or Top.  Get curr from SharedPrefs

        sharedPreferences.registerOnSharedPreferenceChangeListener(this);  // Should be done early like in OnCreate
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = new MenuInflater(this);
        menuInflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();

        switch (itemId) {
            case R.id.action_settings:
                Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
                startActivity(startSettingsActivity);
                return true;
            case R.id.action_about:
                Toast.makeText(this, "About Dialog goes here!!!", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }

    }

    /**
     * GETRUNTIMESFORMOVIESINLIST - Add the runtimes to the Movies that were just created from
     * JSON call that should precede this one.  Update database with runtimes
     * Note: Makes network call.
     */
    private void getRuntimesForMoviesInList(Cursor cursor) {
        Log.d(TAG, "BEGIN::getRuntimesForMoviesInList: ");
        // Get runtime for found movies and place in correct Movies.
        if (cursor != null) {  // Cursor exists
            if (cursor.moveToFirst()) {  // Cursor is valid
                ContentValues idContentValues;
                int movieIdCursorIndex = cursor.getColumnIndex(MovieEntry._ID);  // Ge index of id
                for (int i = 0; cursor.moveToPosition(i); i++) {
                    idContentValues = new ContentValues();
                    long movieID = cursor.getLong(movieIdCursorIndex);  // Get movieId for later in loop

                    int runtime = getSingleMovieRuntimeFromTMDB("" + movieID);  // Get runtime for this id
                    String[] idArgsArray = {"" + movieID};
                    idContentValues.put(MovieEntry.RUNTIME, runtime);  // It is the runtime we want to update

                    // Update runtime where ID = id
                    mDb.update(MovieEntry.TABLE_NAME
                            , idContentValues
                            , MovieEntry._ID + " = ? "
                            , idArgsArray
                    );
//                    Log.i(TAG, "getRuntimesForMoviesInList: " + (i + 1) + ":] " + tempMovies[i] + "\n");

                }
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
        Log.d(TAG, "BEGIN::loadPreferredMovieList: ");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String popularOrTopRated = sharedPreferences.getString(getString(R.string.pref_sort_key),
                getString(R.string.pref_sort_default));  // Get from SP or default

            new TMDBQueryTask().execute(NetworkUtils.buildUrlForPopularOrTopRated(this, popularOrTopRated));  // TODO: Add String so can be either/or
        Log.d(TAG, "END::loadPreferredMovieList: ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Log.d(TAG, "BEGIN::onSharedPreferenceChanged: ");
        if (s.equals(getString(R.string.pref_sort_key))) {
            String popularOrTopRated = sharedPreferences.getString(getString(R.string.pref_sort_key),
                    getString(R.string.pref_sort_default));
            new TMDBQueryTask().execute(NetworkUtils.buildUrlForPopularOrTopRated(this, popularOrTopRated));
        }
        Log.d(TAG, "END::onSharedPreferenceChanged: ");
    }


    public class TMDBQueryTask extends AsyncTask<URL, Void, Boolean> {

        /**
         * DOINBACKGROUND - Get data from server and parse JSON
         * @param urls - Only 1 url for a list of movies will be used, urls[0]
         * @return MovieData[] - Array of Movie objects that were created after request
         */
        @Override
        protected Boolean doInBackground(URL... urls) {
            Log.d(TAG, "BEGIN::doInBackground: " + urls[0]);
            boolean isPopular = true;
            // If got nothing, return
            if (0 != urls.length) {

                URL url = urls[0];
                String tmdbJsonString = "";
                try {
                    tmdbJsonString = NetworkUtils.getResponseFromHttpUrl(url);  // Popular | Top-Rated

                    Log.i(TAG, "doInBackground: >>>" + tmdbJsonString + "<<<");
                    isPopular = (url.toString()).contains(NetworkUtils.TMDB_POPULAR);
//                    tempMovies = OpenTMDJsonUtils
//                            .getPopularOrTopJSON(MainActivity.this, tmdbJsonString, isPopular);
                    movieContentValues = OpenTMDJsonUtils
                            .getPopularOrTopJSONContentValues(MainActivity.this, tmdbJsonString, isPopular);

                    // Not sure if DB calls need to be done here, but since we are here, why not.

                    try {
                        mDb.beginTransaction();

                        // TODO: Code to keep from having duplicates

                        for (int i = 0; i < movieContentValues.length; i++) {
                            mDb.insert(MovieEntry.TABLE_NAME, null, movieContentValues[i]);
                        }

                        mDb.setTransactionSuccessful();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } finally {
                        mDb.endTransaction();
                    }

                    Cursor cursorPosterPathsMovieIds = getCursorPosterPathsMovieIds(isPopular);

                    if (cursorPosterPathsMovieIds != null) {
                        getRuntimesForMoviesInList(cursorPosterPathsMovieIds);
                        cursorPosterPathsMovieIds.close();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            Log.d(TAG, "END::doInBackground: ");
            return isPopular;
        }

        /**
         * GETCURSORPOSTERPATHSMOVIEIDS - Will obtain cursor containing the movieId, and the posterPath for each movie
         * of the specified type.
         * @param isPopular - boolean - Whether this is for Popular or Top-Rated
         * @return Cursor - Result of query.  Can return NULL if database not yet set.
         */
        private Cursor getCursorPosterPathsMovieIds(boolean isPopular) {
            // Do SQL query to get Cursor.  SELECT movieId from TABLE where Type==Pop|Top depnding on isPopular
            // AND runtime IS NULL.  Pass in Cursor of runtime that need filling to getRuntimesForMoviesWithIds
            String orderByTypeIndex;
            String selection ;
            if (isPopular) {
                orderByTypeIndex = MovieEntry.POPULAR_ORDER_IN;
                selection = MovieEntry.POPULAR_ORDER_IN;
            } else {
                orderByTypeIndex = MovieEntry.TOP_RATED_ORDER_IN;
                selection = MovieEntry.TOP_RATED_ORDER_IN;
            }

            String[] posterPathMovieIdColumns = {MovieEntry._ID, MovieEntry.POSTER_PATH};
            // Trying to say SELECT movieId, posterPath FROM movies WHERE selection IS NOT NULL ORDER BY xxxORDERIN
            // Give me 2 cols of all the movies that are POPULAR|TOPRATED and have them in the order they were downloaded(by pop or top)
            return mDb.query(MovieEntry.TABLE_NAME,
                    posterPathMovieIdColumns,
                    selection + " IS NOT NULL ",  // When doing Populuar,
                    null,
                    null,
                    null,
                    orderByTypeIndex);
        }

        /**
         * ONPOSTEXECUTE - Will set the posters using the newly acquired MovieData
         */
        @Override
        protected void onPostExecute(Boolean isPopular) {

            Log.d(TAG, "BEGIN::onPostExecute: ");
            // TODO: Maybe put cursor into array.  Send array and close the cursor here.  Possibly, just send DB and hold?
            mMovieAdapter.setMoviesData(mDb, isPopular);
            Log.d(TAG, "END::onPostExecute: ");
        }
    }
}
