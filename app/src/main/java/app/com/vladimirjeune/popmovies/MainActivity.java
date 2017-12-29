package app.com.vladimirjeune.popmovies;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
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
        LoaderManager.LoaderCallbacks<ArrayList<Pair<Long, Pair<String, String>>>>,
        MovieAdapter.MovieOnClickHandler {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int TMDBQUERY_LOADER = 41;
    private static final String NETWORK_URL_POP_OR_TOP_KEY = "pop_or_top";
    private String mTMDKey;

    private RecyclerView mRecyclerView;
    private MovieAdapter mMovieAdapter;
    private int mPosition = RecyclerView.NO_POSITION;

    private final int mNumberOfFakeMovies = 20;

    private ContentValues[] movieContentValues;
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

        // Find RecyclerView from XML
        mRecyclerView = findViewById(R.id.rv_grid_movies);

        boolean reverseLayoutForGridView = false;
        int spanCount = 2;
        GridLayoutManager gridLayoutManager
                = new GridLayoutManager(this, spanCount, GridLayoutManager.VERTICAL, reverseLayoutForGridView);

        mRecyclerView.setLayoutManager(gridLayoutManager);

        mRecyclerView.addItemDecoration(new SpaceDecoration(this));  // Use this instead of padding.
        // Creates efficiency because no need to unnecessarily measure
        mRecyclerView.setHasFixedSize(true);

        // Adapter links our poster data with views that will show the posters.
        // 2 this are because of separation of concerns.  Not a mistake.
        mMovieAdapter = new MovieAdapter(this, this, mNumberOfFakeMovies);

        // Attaches Adapter to RecyclerView in our layout
        mRecyclerView.setAdapter(mMovieAdapter);

        setupSharedPreferences();

        loadPreferredMovieList();  // Calls AsyncTaskLoader and gets posters for MainPage
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
                Intent startAboutActivity = new Intent(this, AboutActivity.class);
                startActivity(startAboutActivity);
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }

    }

    /**
     * GETRUNTIMESFORMOVIESINLIST - Add the runtimes to the Movies that were just created from
     * JSON call that should precede this one.  Update database with runtimes
     * Note: Makes network call.  Calls DB
     * @param cursor - Holds data we need to look through
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
                    idContentValues.put(MovieEntry.RUNTIME, runtime);  // It is the runtime we want to update

                    // Update runtime where ID = id
                    getContentResolver().update(
                            MovieEntry.buildUriWithMovieId(movieID),
                            idContentValues,
                            null,
                            null);

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

        Loader<ArrayList<Pair<Long, Pair<String, String>>>> tmdbQueryLoader = getSupportLoaderManager()
                .getLoader(TMDBQUERY_LOADER);

        if (null == tmdbQueryLoader) {
            // Make sure loader is initialized and active.  If loader doesn't already exists; create one
            // and start it.  Otherwise, use last created loader
            getSupportLoaderManager().initLoader(TMDBQUERY_LOADER, urlBundle, this);
        } else {
            getSupportLoaderManager().restartLoader(TMDBQUERY_LOADER, urlBundle, this);
        }

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
            mMovieAdapter.notifyDataSetChanged();   // Made no difference
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
    public Loader<ArrayList<Pair<Long, Pair<String, String>>>> onCreateLoader(int id, final Bundle args) {

        if (TMDBQUERY_LOADER == id) {
            return new AsyncTaskLoader<ArrayList<Pair<Long, Pair<String, String>>>>(this) {

                // Holds and helps to cache our data
                ArrayList<Pair<Long, Pair<String, String>>> mIdsAndPosters = null;

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
                 * @return - ArrayList<Pair<Long, Pair<String, String>>> - Ordered Arraylist of Movie posters and ids.
                 */
                @Override
                public ArrayList<Pair<Long, Pair<String, String>>> loadInBackground() {

                    Log.d(TAG, "loadInBackground: ");
                    boolean isPopular = true;

                    String urlString = (String) args.getCharSequence(NETWORK_URL_POP_OR_TOP_KEY);
                    ArrayList<Pair<Long, Pair<String, String>>> titlesAndPosters = null;

                    if (urlString != null) {
                        String tmdbJsonString = "";
                        titlesAndPosters = new ArrayList<>();

                        try {
                            isPopular = mIsPopular.equals(NetworkUtils.TMDB_POPULAR);

                            // Normal if can connect.  Otherwise, see if have this type of data in DB, if so fill array with it
                            if (NetworkUtils.doWeHaveInternet()) {

                                tmdbJsonString = NetworkUtils.getResponseFromHttpUrl(new URL(urlString));

                                Log.d(TAG, "loadInBackground: >>>" + tmdbJsonString + "<<<");

                                movieContentValues = OpenTMDJsonUtils
                                        .getPopularOrTopJSONContentValues(MainActivity.this, tmdbJsonString, isPopular);

                                try {
                                    // Projection and following final ints need to always be in sync
                                    final String[] projection = new String[]{
                                            MovieEntry._ID,
                                            MovieEntry.ORIGINAL_TITLE,
                                            MovieEntry.POPULAR_ORDER_IN,
                                            MovieEntry.TOP_RATED_ORDER_IN
                                    };

                                    // Preceding Projection and these final ints always MUST be in sync
                                    final int idPos = 0;
                                    final int titlePos = 1;
                                    final int popOrderInPos = 2;
                                    final int topRatedOrderInPos = 3;

                                    String where = getTypeOrderIn(isPopular) + " IS NOT NULL ";

                                    // Query of what already in DB
                                    Cursor idAndTitleOldCursor = getContentResolver().query(
                                            MovieEntry.CONTENT_URI,
                                            projection,
                                            where,
                                            null,
                                            null
                                    );  // Do not need order for Set

                                    // If already stuff in db, will need to update, as well as, insert and remove
                                    if ((idAndTitleOldCursor != null)
                                            && (idAndTitleOldCursor.getCount() > 0)) {  // So if there are already things in db
                                        // Make set of IDs currently in DB
                                        Set<Long> idOldSet = new HashSet<>();
                                        makeSetOfOldIds(idPos, idAndTitleOldCursor, idOldSet);

                                        // Insert or Update int DB based on New Ids - Old Ids + Intersection of New Ids & Old Ids
                                        Set<Long> idNewSet = new HashSet<>();
                                        insertUpdateAndMakeNewIdSet(isPopular, idAndTitleOldCursor, idOldSet, idNewSet);

                                        // Delete movies that moved off list
                                        idOldSet.removeAll(idNewSet);  // Old - New => Set of IDs up for deletion
                                        deleteChartDroppedMovies(idOldSet);

                                        idAndTitleOldCursor.close();  // Closing the Cursor
                                    } else {  // Got null, or the Cursor has no rows.  So can bulkInsert, since no updates.
                                        getContentResolver().bulkInsert(MovieEntry.CONTENT_URI, movieContentValues);
                                    }

                                } catch (SQLException sqe) {
                                    sqe.printStackTrace();
                                }

                                Cursor cursorPosterPathsMovieIds = getCursorPosterPathsMovieIds(isPopular);

                                if (cursorPosterPathsMovieIds != null) {

                                    // Add Runtimes to DB for Movies
                                    getRuntimesForMoviesInList(cursorPosterPathsMovieIds);
                                    createArrayListOfPairsForPosters(titlesAndPosters, cursorPosterPathsMovieIds);

                                    cursorPosterPathsMovieIds.close();  // Closing Cursor
                                }
                            } else {  // No internet for call, get data from DB if any available for wanted type.

                                // You CANNOT make a Toast from a Thread.  Toasts must be done on UI Thread.
                                // This function puts call on the UI queue
                                // https://stackoverflow.com/questions/3875184/cant-create-handler-inside-thread-that-has-not-called-looper-prepare
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, R.string.warning_toast_internet, Toast.LENGTH_LONG).show();
                                    }
                                });

                                Cursor cursorForIdsAndPosters = getCursorPosterPathsMovieIds(isPopular);
                                if (cursorForIdsAndPosters != null) {
                                    createArrayListOfPairsForPosters(titlesAndPosters, cursorForIdsAndPosters);  // Fill ArrayList
                                    cursorForIdsAndPosters.close();
                                }
                            }
                        } catch (JSONException je) {
                            je.printStackTrace();
                            return null;
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                            return null;
                        }

                        Log.d(TAG, "loadInBackground: Poster Count: " + titlesAndPosters.size() );
                    }

                    return titlesAndPosters;
                }

                /**
                 * DELETECHARTDROPPEDMOVIES - Deletes movies that fell off the chart from the DB.
                 * @param idOldSet - Modified set of old Movie IDs consists of Old IDs - New Ids
                 *                 and the Intersection of Old & New
                 */
                private void deleteChartDroppedMovies(Set<Long> idOldSet) {
                    for (Long deleteOldId : idOldSet) {
                        getContentResolver().delete(
                                MovieEntry.buildUriWithMovieId(deleteOldId),
                                null,
                                null);
                    }
                }

                /**
                 * INSERTUPDATEANDMAKENEWIDSET - Inserts new movies into DB, Updates chart movers in DB, and makes a
                 * new ID set of incoming movies to compare against what is already in the database
                 * @param isPopular - Type the user is looking for
                 * @param idAndTitleOldCursor - Cursor of titles that are previously in DB
                 * @param idOldSet - Set of IDs of movies previously in our DB
                 * @param idNewSet - Set of IDs that are coming in from TMDb.
                 */
                private void insertUpdateAndMakeNewIdSet(boolean isPopular, Cursor idAndTitleOldCursor, Set<Long> idOldSet, Set<Long> idNewSet) {
                    for (int i = 0; i < movieContentValues.length; i++) {

                        Long newId = movieContentValues[i].getAsLong(MovieEntry._ID);
                        idNewSet.add(newId);    // Create new Set for difference op later

                        if (idOldSet.contains(newId)) {
                            // Update
                            String orderType = getTypeOrderIn(isPopular);
                            String[] whereArgs
                                    = new String[] {"" + newId
                                    , getOldPositionOfNewId(isPopular, idAndTitleOldCursor, newId)};  // ID, TypeOrderIn position

                            String where = MovieEntry._ID + " = ? AND " + orderType + " = ? ";
                            getContentResolver().update(
                                    MovieEntry.CONTENT_URI,
                                    movieContentValues[i],
                                    where,
                                    whereArgs);
                        } else {
                            getContentResolver().insert(
                                    MovieEntry.CONTENT_URI,
                                    movieContentValues[i]);
                        }
                    }
                }

                /**
                 * MAKESETOFOLDIDS - Create a Set of the IDs that are currently in the DB.
                 * Will be used later to ensure proper updating when new data comes in.
                 * No duplicates and proper updating.
                 * @param idPos - Position of the id column
                 * @param idAndTitleOldCursor - Cursor with old DB data
                 * @param idOldSet - Set holding the IDs of the old DB movies.
                 */
                private void makeSetOfOldIds(int idPos, Cursor idAndTitleOldCursor, Set<Long> idOldSet) {
                    // Make a Set of IDs you already have in DB.  Later, compare to incoming IDs
                    if ((idAndTitleOldCursor != null) && (idAndTitleOldCursor.getCount() > 0)) {
                        idAndTitleOldCursor.moveToFirst();
                        do {
                            idOldSet.add(idAndTitleOldCursor.getLong(idPos));
                        } while (idAndTitleOldCursor.moveToNext());
                    }
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

                    if ((idAndTitleOldCursor != null)
                            && (idAndTitleOldCursor.moveToFirst()) && (idAndTitleOldCursor.getCount() > 0)) {
                        int idIndex = idAndTitleOldCursor.getColumnIndex(MovieEntry._ID);
                        int orderTypeIndex = idAndTitleOldCursor.getColumnIndex(orderType);  // Will pick correct of Pop or Top index

                        do {
                            if (idAndTitleOldCursor.getLong(idIndex) == oldId) {
                                retIndex = idAndTitleOldCursor.getInt(orderTypeIndex);
                                return "" + retIndex;  // Found it, jump out.
                            }
                        } while (idAndTitleOldCursor.moveToNext());
                    }
                    Log.d(TAG, "getOldPositionOfNewId: For some reason we did not find it. Old ID: " + oldId);
                    return "" + retIndex;
                }

                /**
                 * CREATEARRAYLISTOFPAIRSFORPOSTERS - Creates the ArrayList needed for Posters
                 * @param idsTitlesAndPosters - ArrayList<Pairs<Long, Pair<String, String>>> - Posters for MainActivity in order
                 * @param cursorPosterPathsMovieIds - Cursor - Cursor from DB in order of Popularity or Rating
                 */
                private void createArrayListOfPairsForPosters(ArrayList<Pair<Long, Pair<String, String>>> idsTitlesAndPosters, Cursor cursorPosterPathsMovieIds) {
                    // Create ArrayList of Pairs for posters
                    if ((cursorPosterPathsMovieIds != null)
                            && (cursorPosterPathsMovieIds.moveToFirst())) {
                        int idIndex = cursorPosterPathsMovieIds.getColumnIndex(MovieEntry._ID);
                        int titleIndex = cursorPosterPathsMovieIds.getColumnIndex(MovieEntry.ORIGINAL_TITLE);
                        int posterPathIndex = cursorPosterPathsMovieIds.getColumnIndex(MovieEntry.POSTER_PATH);

                        if ((-1 != idIndex) && (-1 != posterPathIndex) && (-1 != titleIndex)) {
                            do {
                                long movieId = cursorPosterPathsMovieIds.getLong(idIndex);
                                String title = cursorPosterPathsMovieIds.getString(titleIndex);
                                String posterPath = cursorPosterPathsMovieIds.getString(posterPathIndex);

                                Pair<String, String> payload = new Pair<>(title, posterPath);
                                idsTitlesAndPosters.add(new Pair<>(movieId, payload));
                            } while (cursorPosterPathsMovieIds.moveToNext()) ;
                        }
                    }
                }

                /**
                 * DELIVERRESULTS - Store data from load in here for caching, and then deliver.
                 * @param data - ArrayList<Pair<Long, Pair<String, String>>>
                 */
                @Override
                public void deliverResult(ArrayList<Pair<Long, Pair<String, String>>> data) {
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

                    String[] posterPathMovieIdColumns = {MovieEntry._ID, MovieEntry.ORIGINAL_TITLE, MovieEntry.POSTER_PATH};
                    String selectionIsNotNull = selection + " IS NOT NULL ";
                    // Trying to say SELECT movieId, original title, posterPath FROM movies WHERE selection IS NOT NULL ORDER BY xxxORDERIN
                    // Give me 3 cols of all the movies that are POPULAR|TOPRATED and have them in the order they were downloaded(by pop or top)
                    return getContentResolver().query(
                            MovieEntry.CONTENT_URI,
                            posterPathMovieIdColumns,
                            selectionIsNotNull,
                            null,
                            orderByTypeIndex);
                }

            };
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<Pair<Long, Pair<String, String>>>> loader, ArrayList<Pair<Long, Pair<String, String>>> data) {
        Log.d(TAG, "onLoadFinished: ");

        if ((data != null) && (data.size() != 0)) {

            setRecyclerVIewToCorrectPosition();

            mMovieAdapter.setData(data, mIsPopular.equals(getString(R.string.pref_sort_popular)));
        }

        Log.d(TAG, "onLoadFinished: ");
    }

    /**
     * SETRECYCLERVIEWTOCORRECTPOSITION - Makes sure that the RecyclerView is at the start
     * when the user switches from one type of list to another.  Otherwise, we would be
     * in the same position as we were before the change, but in another list.
     */
    private void setRecyclerVIewToCorrectPosition() {
        if (RecyclerView.NO_POSITION == mPosition) {
            mPosition = 0;
        }
//        Log.d(TAG, "setRecyclerVIewToCorrectPosition() called" + mRecyclerView.getLayoutManager().onSaveInstanceState());
        mRecyclerView.scrollToPosition(mPosition);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<Pair<Long, Pair<String, String>>>> loader) {
        // Nothing here
    }

    /**
     * ONCLICK - Responds to clicks from our list
     * @param movieId - ID of movie represented by the one list item that was clicked
     */
    @Override
    public void onClick(long movieId) {
        Log.d(TAG, "onClick() called with: movieId = [" + movieId + "]");
            Intent movieDetailIntent = new Intent(this, DetailActivity.class);
            Uri movieDataUri = MovieEntry.buildUriWithMovieId(movieId);
            movieDetailIntent.setData(movieDataUri);
            startActivity(movieDetailIntent);
    }
}
