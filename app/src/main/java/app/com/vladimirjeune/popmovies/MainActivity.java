package app.com.vladimirjeune.popmovies;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import app.com.vladimirjeune.popmovies.data.MovieContract.MovieEntry;
import app.com.vladimirjeune.popmovies.data.MovieDBHelper;
import app.com.vladimirjeune.popmovies.utilities.NetworkUtils;
import app.com.vladimirjeune.popmovies.utilities.OpenTMDJsonUtils;

public class MainActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        LoaderManager.LoaderCallbacks<ArrayList<Pair<Long, String>>> {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int TMDBQUERY_LOADER = 41;
    private static final String NETWORK_URL_POP_OR_TOP_KEY = "pop_or_top";
    private String mTMDKey;

    private RecyclerView mRecyclerView;
    private MovieAdapter mMovieAdapter;

    private final int mNumberOfFakeMovies = 20;

    private ContentValues[] movieContentValues;
    private SQLiteDatabase mDb;
    private String mIsPopular ;

    private Toast mToast;

    // *** IMPORTANT ***  This projection and the following ints MUST REMAIN CORRELATED in order
    private static String[] MAIN_MOVIE_PROJECTION = {
            MovieEntry._ID,
            MovieEntry.ORIGINAL_TITLE,
            MovieEntry.POSTER_PATH,
            MovieEntry.SYNOPSIS,
            MovieEntry.RELEASE_DATE,
            MovieEntry.VOTER_AVERAGE,
            MovieEntry.BACKDROP_PATH,
            MovieEntry.POPULARITY,
            MovieEntry.RUNTIME,
            MovieEntry.POSTER,
            MovieEntry.BACKDROP,
            MovieEntry.COLUMN_TIMESTAMP,
            MovieEntry.POPULAR_ORDER_IN,
            MovieEntry.TOP_RATED_ORDER_IN
    };

    // *** IMPORTANT ***  These ints and the previous projection MUST REMAIN CORRELATED
    private static final int INDEX_ID = 0;
    private static final int INDEX_ORIGINAL_TITLE = 1;
    private static final int INDEX_POSTER_PATH = 2;
    private static final int INDEX_SYNOPSIS = 3;
    private static final int INDEX_RELEASE_DATE = 4;
    private static final int INDEX_VOTER_AVERAGE = 5;
    private static final int INDEX_BACKDROP_PATH = 6;
    private static final int INDEX_POPULARITY = 7;
    private static final int INDEX_RUNTIME = 8;
    private static final int INDEX_POSTER = 9;
    private static final int INDEX_BACKDROP = 10;
    private static final int INDEX_COLUMN_TIMESTAMP = 11;
    private static final int INDEX_POPULAR_ORDER_IN = 12;
    private static final int INDEX_TOP_RATED_ORDER_IN = 13;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "BEGIN::onCreate: ");

        mIsPopular = getString(R.string.pref_sort_popular);

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

        // Get current type from SharedPrefs
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mIsPopular = sharedPreferences.getString(getString(R.string.pref_sort_key),
                getString(R.string.pref_sort_default));  // Get from SP or default

        Bundle urlBundle = getTMDQueryBundle();

        Loader<ArrayList<Pair<Long, String>>> tmdbQueryLoader = getSupportLoaderManager()
                .getLoader(TMDBQUERY_LOADER);

        if (null == tmdbQueryLoader) {
            /**
             * Make sure loader is initialized and active.  If loader doesn't already exists; create one
             * and start it.  Otherwise, use last created loader
             **/
            getSupportLoaderManager().initLoader(TMDBQUERY_LOADER, urlBundle, this);
        } else {
            getSupportLoaderManager().restartLoader(TMDBQUERY_LOADER, urlBundle, this);
        }

//        new TMDBQueryTask().execute(NetworkUtils.buildUrlForPopularOrTopRated(this, mIsPopular));
        Log.d(TAG, "END::loadPreferredMovieList: ");
    }

    /**
     * GETTMDQUERYBUNDLE - Bundles the needed URL as a string so that it can be
     * used later by the loader.
     * @return - Bundle - The Bundle that should be used for the loading of the TMDBQuery
     */
    @NonNull
    private Bundle getTMDQueryBundle() {
        // Prepare to call loader
        Bundle urlBundle = new Bundle();
        URL urlForPopularOrTopRated = NetworkUtils.buildUrlForPopularOrTopRated(this, mIsPopular);
        String stringOfUrl = ((null == urlForPopularOrTopRated) ? null : urlForPopularOrTopRated.toString());
        Log.d(TAG, "loadPreferredMovieList: URL: [" + stringOfUrl + "]");

        urlBundle.putCharSequence(NETWORK_URL_POP_OR_TOP_KEY, stringOfUrl);
        return urlBundle;
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
            mIsPopular = sharedPreferences.getString(getString(R.string.pref_sort_key),
                    getString(R.string.pref_sort_default));

            getSupportLoaderManager().restartLoader(TMDBQUERY_LOADER, getTMDQueryBundle(), this);
//            new TMDBQueryTask().execute(NetworkUtils.buildUrlForPopularOrTopRated(this, mIsPopular));
        }
        Log.d(TAG, "END::onSharedPreferenceChanged: ");
    }

    /**
     * ONCREATELOADER - Create and return a new loader for the the given id
     * @param id - int - For differentiating loaders
     * @param args - Bundle - Important data for loader
     * @return - New Loader instance that is ready to start loading
     */
    @Override
    public Loader<ArrayList<Pair<Long, String>>> onCreateLoader(int id, final Bundle args) {

        if (TMDBQUERY_LOADER == id) {
            return new AsyncTaskLoader<ArrayList<Pair<Long, String>>>(this) {

                // Holds and helps to cache our data
                ArrayList<Pair<Long, String>> mIdsAndPosters = null;

                @Override
                protected void onStartLoading() {

                    if (args == null) {  // Nothing to do, with no arguments
                        return;
                    }

                    // If we have cached results, deliver immediately, otherwise, forceLoad
                    if (mIdsAndPosters != null) {
                        deliverResult(mIdsAndPosters);
                    } else {
                        forceLoad();
                    }
                }

                /**
                 * LOADINBACKGROUND - Done on a background thread
                 * @return - ArrayList<Pair<Long, String>> - Ordered Arraylist of Movie posters and ids.
                 */
                @Override
                public ArrayList<Pair<Long, String>> loadInBackground() {

                    Log.d(TAG, "loadInBackground: ");
                    boolean isPopular = true;

                    String urlString = (String) args.getCharSequence(NETWORK_URL_POP_OR_TOP_KEY);
                    ArrayList<Pair<Long, String>> idsAndPosters = null;

                    if (urlString != null) {
                        String tmdbJsonString = "";
                        idsAndPosters = new ArrayList<>();

                        try {

                            tmdbJsonString = NetworkUtils.getResponseFromHttpUrl(new URL(urlString));

                            Log.d(TAG, "loadInBackground: >>>" + tmdbJsonString + "<<<");

                            isPopular = mIsPopular.equals(NetworkUtils.TMDB_POPULAR);
                            movieContentValues = OpenTMDJsonUtils
                                    .getPopularOrTopJSONContentValues(MainActivity.this, tmdbJsonString, isPopular);

                            try {
                                mDb.beginTransaction();

                                Set<Long> idOldSet = new HashSet<>();

                                // TODO: Need to add both OrderIns to projection
                                final String[] projection = new String[] {
                                        MovieEntry._ID,
                                        MovieEntry.ORIGINAL_TITLE,
                                        MovieEntry.POPULAR_ORDER_IN,
                                        MovieEntry.TOP_RATED_ORDER_IN
                                };

                                final int idPos = 0;
                                final int titlePos = 1;
                                final int popOrderInPos = 2;
                                final int topRatedOrderInPos = 3;

                                String where = getTypeOrderIn(isPopular) + " IS NOT NULL ";

                                Cursor idAndTitleOldCursor = mDb.query(MovieEntry.TABLE_NAME,
                                       projection,
                                        where,
                                        null,
                                        null,
                                        null,
                                        null);  // Do not need order for Set

                                // Make a Set of IDs you already have in DB.  Later, compare to incoming
                                idAndTitleOldCursor.moveToFirst();
                                do {
                                    idOldSet.add(idAndTitleOldCursor.getLong(idPos));
                                } while(idAndTitleOldCursor.moveToNext()) ;


                                // TODO:  Code to stop duplicates
                                // Insert or Update based on Intersection of Old and New IDs.
                                Set<Long> idNewSet = new HashSet<>();
                                for (int i = 0; i < movieContentValues.length; i++) {

                                    Long newId = movieContentValues[i].getAsLong(MovieEntry._ID);
                                    idNewSet.add(newId);    // Create new Set for difference op later

                                    if (idOldSet.contains(newId)) {
                                        // Update
                                        String orderType = getTypeOrderIn(isPopular);
                                        String[] whereArgs
                                                = new String[] {"" + newId
                                                , getOldPositionOfNewId(isPopular, idAndTitleOldCursor, newId)};

                                        mDb.update(MovieEntry.TABLE_NAME,
                                                movieContentValues[i],
                                                MovieEntry._ID + " = ? AND " + orderType + " = ? ",
                                                whereArgs);
                                    } else {
                                        mDb.insert(MovieEntry.TABLE_NAME, null, movieContentValues[i]);
                                    }
                                }

                                // Delete movies that moved off list
                                idOldSet.removeAll(idNewSet);  // Old - New => Set of IDs up for deletion

                                for (Long deleteOldId : idOldSet) {
                                    mDb.delete(MovieEntry.TABLE_NAME,
                                            MovieEntry._ID + " = ? ",
                                            new String[] {"" + deleteOldId});
                                }

                                idAndTitleOldCursor.close();  // Closing the Cursor
                                mDb.setTransactionSuccessful();
                            } catch (SQLException sqe) {
                                sqe.printStackTrace();
                            } finally {
                                mDb.endTransaction();
                            }

                            Cursor cursorPosterPathsMovieIds = getCursorPosterPathsMovieIds(isPopular);

                            if (cursorPosterPathsMovieIds != null) {

                                // Add Runtimes for Movies
                                getRuntimesForMoviesInList(cursorPosterPathsMovieIds);
                                createArrayListOfPairsForPosters(idsAndPosters, cursorPosterPathsMovieIds);

                                cursorPosterPathsMovieIds.close();
                            }

                        } catch (JSONException je) {
                            je.printStackTrace();
                            return null;
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                            return null;
                        }
                        Log.d(TAG, "loadInBackground: Poster Count: " + idsAndPosters.size() );
                    }

                    return idsAndPosters;
                }

                /**
                 * GETTYPEORDERIN - Used to get the proper [TYPE]OrderIn depending on the boolean
                 * @param isPopular - boolean - Does the User want Popular order, or Top-Rated order
                 * @return - String - Proper key based on the boolean passed in
                 */
                @Nullable
                private String getTypeOrderIn(boolean isPopular) {
                    return (isPopular) ? MovieEntry.POPULAR_ORDER_IN : MovieEntry.TOP_RATED_ORDER_IN;
                }

                /**
                 * GETOLDPOSITIONOFNEWID - Returns the old Position of the movie with this ID
                 * @param isPopular - Popular, or Top-Rated
                 * @param idAndTitleOldCursor - Cursor - Place to find old ID
                 * @param oldId - Long - Old Id we are looking for
                 * @return - String - Order Index of the Type that the user is asking for.  Or "-1", if no match
                 */
                @Nullable
                private String getOldPositionOfNewId(boolean isPopular, final Cursor idAndTitleOldCursor, final Long oldId) {

                    int retIndex = -1;
                    String orderType = getTypeOrderIn(isPopular);

                    idAndTitleOldCursor.moveToFirst();
                    int idIndex = idAndTitleOldCursor.getColumnIndex(MovieEntry._ID);
                    int orderTypeIndex = idAndTitleOldCursor.getColumnIndex(orderType);  // Will pick correct of Pop or Top index

                    do {
                        if (idAndTitleOldCursor.getLong(idIndex) == oldId) {
                            retIndex = idAndTitleOldCursor.getInt(orderTypeIndex);
                            return "" + retIndex;  // Found it, jump out.
                        }
                    } while (idAndTitleOldCursor.moveToNext());

                    Log.d(TAG, "getOldPositionOfNewId: For some reason we did not find it. Old ID: " + oldId);
                    return "" + retIndex;
                }

                /**
                 * CREATEARRAYLISTOFPAIRSFORPOSTERS - Creates the ArrayList needed for Posters
                 * @param idsAndPosters - ArrayList<Pairs<Long, String>> - Posters for MainActivity in order
                 * @param cursorPosterPathsMovieIds - Cursor - Cursor from DB in order of Popularity or Rating
                 */
                private void createArrayListOfPairsForPosters(ArrayList<Pair<Long, String>> idsAndPosters, Cursor cursorPosterPathsMovieIds) {
                    // Create ArrayList of Pairs for posters
                    if (cursorPosterPathsMovieIds.moveToFirst()) {
                        int idIndex = cursorPosterPathsMovieIds.getColumnIndex(MovieEntry._ID);
                        int posterPathIndex = cursorPosterPathsMovieIds.getColumnIndex(MovieEntry.POSTER_PATH);

                        if ((-1 != idIndex) && (-1 != posterPathIndex)) {
                            do {
                                long movieId = cursorPosterPathsMovieIds.getLong(idIndex);
                                String posterPath = cursorPosterPathsMovieIds.getString(posterPathIndex);

                                idsAndPosters.add(new Pair<>(movieId, posterPath));
                            } while (cursorPosterPathsMovieIds.moveToNext()) ;
                        }
                    }
                }

                /**
                 * DELIVERRESULTS - Store data from load in here for caching, and then deliver.
                 * @param data - ArrayList<Pair<Long, String>>
                 */
                @Override
                public void deliverResult(ArrayList<Pair<Long, String>> data) {
                    mIdsAndPosters = data;  // Assignment for caching
                    super.deliverResult(data);  // Then deliver results
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

            };
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<Pair<Long, String>>> loader, ArrayList<Pair<Long, String>> data) {
        Log.d(TAG, "onLoadFinished: ");

        if (data != null) {
            mMovieAdapter.setData(data, mIsPopular.equals(getString(R.string.pref_sort_popular)));
        }

        Log.d(TAG, "onLoadFinished: ");
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<Pair<Long, String>>> loader) {
        // Nothing here
    }


//    public class TMDBQueryTask extends AsyncTask<URL, Void, Boolean> {
//
//        /**
//         * DOINBACKGROUND - Get data from server and parse JSON
//         * @param urls - Only 1 url for a list of movies will be used, urls[0]
//         * @return MovieData[] - Array of Movie objects that were created after request
//         */
//        @Override
//        protected Boolean doInBackground(URL... urls) {
//            Log.d(TAG, "BEGIN::doInBackground: " + urls[0]);
//            boolean isPopular = true;
//            // If got nothing, return
//            if (0 != urls.length) {
//                ArrayList<Pair<Long, String>> posterAndIds = new ArrayList<>();
//
//                URL url = urls[0];
//                String tmdbJsonString = "";
//                try {
//                    tmdbJsonString = NetworkUtils.getResponseFromHttpUrl(url);  // Popular | Top-Rated
//
//                    Log.i(TAG, "doInBackground: >>>" + tmdbJsonString + "<<<");
////                    isPopular = (url.toString()).contains(NetworkUtils.TMDB_POPULAR);
//                    isPopular = mIsPopular.equals(NetworkUtils.TMDB_POPULAR);
//                    movieContentValues = OpenTMDJsonUtils
//                            .getPopularOrTopJSONContentValues(MainActivity.this, tmdbJsonString, isPopular);
//
//                    // Not sure if DB calls need to be done here, but since we are here, why not.
//
//                    try {
//                        mDb.beginTransaction();
//
//                        // TODO: Code to keep from having duplicates
//
//                        for (int i = 0; i < movieContentValues.length; i++) {
//                            mDb.insert(MovieEntry.TABLE_NAME, null, movieContentValues[i]);
//                        }
//
//                        mDb.setTransactionSuccessful();
//                    } catch (SQLException e) {
//                        e.printStackTrace();
//                    } finally {
//                        mDb.endTransaction();
//                    }
//
//                    Cursor cursorPosterPathsMovieIds = getCursorPosterPathsMovieIds(isPopular);
//
//                    if (cursorPosterPathsMovieIds != null) {
//                        getRuntimesForMoviesInList(cursorPosterPathsMovieIds);
//
//                        if (cursorPosterPathsMovieIds.moveToFirst()) {
//                            int idIndex = cursorPosterPathsMovieIds.getColumnIndex(MovieEntry._ID);
//                            int posterPathIndex = cursorPosterPathsMovieIds.getColumnIndex(MovieEntry.POSTER_PATH);
//
//                            if ((-1 != idIndex) && (-1 != posterPathIndex)) {
//                                do {
//                                    long movieId = cursorPosterPathsMovieIds.getLong(idIndex);
//                                    String posterPath = cursorPosterPathsMovieIds.getString(posterPathIndex);
//
//                                    posterAndIds.add(new Pair<>(movieId, posterPath));
//                                } while (cursorPosterPathsMovieIds.moveToNext());
//                            }
//
//                        }
//
//                        cursorPosterPathsMovieIds.close();
//                    }
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//            }
//
//            Log.d(TAG, "END::doInBackground: ");
//            return isPopular;
//        }
//
//        /**
//         * GETCURSORPOSTERPATHSMOVIEIDS - Will obtain cursor containing the movieId, and the posterPath for each movie
//         * of the specified type.
//         * @param isPopular - boolean - Whether this is for Popular or Top-Rated
//         * @return Cursor - Result of query.  Can return NULL if database not yet set.
//         */
//        private Cursor getCursorPosterPathsMovieIds(boolean isPopular) {
//            // Do SQL query to get Cursor.  SELECT movieId from TABLE where Type==Pop|Top depnding on isPopular
//            // AND runtime IS NULL.  Pass in Cursor of runtime that need filling to getRuntimesForMoviesWithIds
//            String orderByTypeIndex;
//            String selection ;
//            if (isPopular) {
//                orderByTypeIndex = MovieEntry.POPULAR_ORDER_IN;
//                selection = MovieEntry.POPULAR_ORDER_IN;
//            } else {
//                orderByTypeIndex = MovieEntry.TOP_RATED_ORDER_IN;
//                selection = MovieEntry.TOP_RATED_ORDER_IN;
//            }
//
//            String[] posterPathMovieIdColumns = {MovieEntry._ID, MovieEntry.POSTER_PATH};
//            // Trying to say SELECT movieId, posterPath FROM movies WHERE selection IS NOT NULL ORDER BY xxxORDERIN
//            // Give me 2 cols of all the movies that are POPULAR|TOPRATED and have them in the order they were downloaded(by pop or top)
//            return mDb.query(MovieEntry.TABLE_NAME,
//                    posterPathMovieIdColumns,
//                    selection + " IS NOT NULL ",  // When doing Populuar,
//                    null,
//                    null,
//                    null,
//                    orderByTypeIndex);
//        }
//
//        /**
//         * ONPOSTEXECUTE - Will set the posters using the newly acquired MovieData
//         */
//        @Override
//        protected void onPostExecute(Boolean isPopular) {
//
//            Log.d(TAG, "BEGIN::onPostExecute: ");
//            // TODO: Maybe put cursor into array.  Send array and close the cursor here.  Possibly, just send DB and hold?
//            mMovieAdapter.setMoviesData(mDb, isPopular);
//            Log.d(TAG, "END::onPostExecute: ");
//        }
//    }


}
