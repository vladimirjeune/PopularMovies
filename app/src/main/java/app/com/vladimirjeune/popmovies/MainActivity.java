package app.com.vladimirjeune.popmovies;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import org.json.JSONException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import app.com.vladimirjeune.popmovies.data.MovieContract.MovieEntry;
import app.com.vladimirjeune.popmovies.data.MovieDBHelper;
import app.com.vladimirjeune.popmovies.utilities.MainLoadingUtils;
import app.com.vladimirjeune.popmovies.utilities.NetworkUtils;
import app.com.vladimirjeune.popmovies.utilities.OpenTMDJsonUtils;

public class MainActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        LoaderManager.LoaderCallbacks<ArrayList<Pair<Long, Pair<String, String>>>>,
        MovieAdapter.MovieOnClickHandler {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int TMDBQUERY_LOADER = 41;
    private static final String NETWORK_URL_POP_OR_TOP_KEY = "pop_or_top";
    public static final String EXTRA_TYPE = "app.com.vladimirjeune.popmovies.VIEW_TYPE";  // Value is a boolean

    private String mTMDKey;

    private RecyclerView mRecyclerView;
    private MovieAdapter mMovieAdapter;
    private int mPosition = RecyclerView.NO_POSITION;

    private final int mNumberOfFakeMovies = 20;

    private ContentValues[] movieContentValues;
    private String mIsPopular ;

    private ProgressBar mProgressBar;

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

        mProgressBar = findViewById(R.id.pb_grid_movies);

        // Find RecyclerView from XML
        mRecyclerView = findViewById(R.id.rv_grid_movies);

        mRecyclerView.addItemDecoration(new SpaceDecoration(this));  // Use this instead of padding.
        // Creates efficiency because no need to unnecessarily measure
        mRecyclerView.setHasFixedSize(true);

        // Inflate the header we are using in the RecyclerView
        View header = LayoutInflater.from(this)
                .inflate(R.layout.main_page_header, mRecyclerView, false);

        // Adapter links our poster data with views that will show the posters.
        // 2 this are because of separation of concerns.  Not a mistake.
        mMovieAdapter = new MovieAdapter(this, this, header, mNumberOfFakeMovies);

        // Need reference so we can set span
        final GridLayoutManager gridLayoutManager
                = (GridLayoutManager) mRecyclerView.getLayoutManager();

        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // Will return at least 1
                return mMovieAdapter.isHeader(position)
                        ? gridLayoutManager.getSpanCount() : 1;
            }
        });

        // Attaches Adapter to RecyclerView in our layout
        mRecyclerView.setAdapter(mMovieAdapter);

        setupSharedPreferences();

        showLoading();  // Until data is ready.

        loadPreferredMovieList();  // Calls AsyncTaskLoader and gets posters for MainPage
        Log.d(TAG, "END::onCreate: ");
    }


    /**
     * SETUPSHAREDPREFERENCES - Anything having to do with SharedPreferences will be handled here.
     *
     */
    private void setupSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
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

            setRecyclerVIewToCorrectPosition();  // I think this works best here.
            showLoading();

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
                            isPopular = isCurrentTypePopular();

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

                                    String where = MainLoadingUtils.getTypeOrderIn(isPopular) + " IS NOT NULL ";

                                    // Query of what already in DB
                                    Cursor idAndTitleOldCursor = getContentResolver().query(
                                            MovieEntry.CONTENT_URI,
                                            projection,
                                            where,
                                            null,
                                            null
                                    );  // Do not need order for Set

                                    final String oppositeWhere = MovieEntry.POPULAR_ORDER_IN + " IS NOT NULL "
                                            + " AND " + MovieEntry.TOP_RATED_ORDER_IN + " IS NOT NULL ";
                                    Cursor idandTitleOppositeCursor = getContentResolver().query(
                                            MovieEntry.CONTENT_URI,
                                            projection,
                                            oppositeWhere,
                                            null,
                                            null
                                    );  // If not NULL, then this is the opposite type of idAndTtitleOldCursor




                                    // If already stuff in db, will need to update, as well as, insert and remove
                                    if ((idAndTitleOldCursor != null)
                                            && (idAndTitleOldCursor.getCount() > 0)) {  // So if there are already things in db
                                        // Make set of IDs currently in DB
                                        Set<Long> idOldSet = new HashSet<>();
                                        MainLoadingUtils.makeSetOfIdsFromCursor(idPos, idAndTitleOldCursor, idOldSet);

                                        // Insert or Update int DB based on New Ids - Old Ids + Intersection of New Ids & Old Ids
                                        Set<Long> idNewSet = new HashSet<>();
                                        insertUpdateAndMakeNewIdSet(isPopular, idAndTitleOldCursor, idandTitleOppositeCursor, idOldSet, idNewSet);

                                        // Delete movies that moved off list
                                        idOldSet.removeAll(idNewSet);  // Old - New => Set of IDs up for deletion
                                        deleteChartDroppedMovies(isPopular, idOldSet, idandTitleOppositeCursor);

                                        idAndTitleOldCursor.close();  // Closing the Cursor
                                    } else {  // Got null, or the Cursor has no rows.  So can bulkInsert, since no updates.
                                        getContentResolver().bulkInsert(MovieEntry.CONTENT_URI, movieContentValues);
                                    }

                                } catch (SQLException sqe) {
                                    sqe.printStackTrace();
                                }

                                Cursor cursorPosterPathsMovieIds = MainLoadingUtils
                                        .getCursorPosterPathsMovieIds(isPopular, MainActivity.this);

                                if (cursorPosterPathsMovieIds != null) {

                                    // Add Runtimes to DB for Movies
                                    MainLoadingUtils.getRuntimesForMoviesInList(cursorPosterPathsMovieIds, MainActivity.this);
                                    MainLoadingUtils.createArrayListOfPairsForPosters(
                                            titlesAndPosters, cursorPosterPathsMovieIds);

                                    cursorPosterPathsMovieIds.close();  // Closing Cursor
                                }
                            } else {  // No internet for call, get data from DB if any available for wanted type.

                                // You CANNOT make a Toast from a Thread.  Toasts must be done on UI Thread.
                                // This function puts call on the UI queue
                                // https://stackoverflow.com/questions/3875184/cant-create-handler-inside-thread-that-has-not-called-looper-prepare
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Snackbar.make(findViewById(R.id.coordinator_layout_main),
                                                R.string.warning_snackbar_internet, Snackbar.LENGTH_LONG)
                                                .setAction(R.string.snackbar_settings, new MySettingsListener())
                                                .show();
                                    }
                                });

                                Cursor cursorForIdsAndPosters = MainLoadingUtils
                                        .getCursorPosterPathsMovieIds(isPopular, MainActivity.this);
                                if (cursorForIdsAndPosters != null) {
                                    MainLoadingUtils.createArrayListOfPairsForPosters(
                                            titlesAndPosters, cursorForIdsAndPosters);  // Fill ArrayList
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
                 * @param isPopular - What type we are currently dealing with
                 * @param idDeleteSet - Modified set of old Movie IDs consists of Old IDs - New Ids
                 *                 and the Intersection of Old & New
                 * @param oppositeTitlesCursor - In case we have to delete something that is both types.  Just update
                 */
                private void deleteChartDroppedMovies(boolean isPopular, Set<Long> idDeleteSet, Cursor oppositeTitlesCursor) {

                    Set<Long> idOppositeSet = new HashSet<>();
                    MainLoadingUtils.makeSetOfIdsFromCursor(INDEX_ID, oppositeTitlesCursor, idOppositeSet);

                    for (Long deleteOldId : idDeleteSet) {  // TODO: Test this out

                        // Situation: ID is in bot types.  Want this one updated to exists only in the other
                        if (idOppositeSet.contains(deleteOldId)) {
                            ContentValues nullOutTypeCV = new ContentValues();
                            // Nulling out this usage, since ID is currently used for other type.
                            nullOutTypeCV.put(MainLoadingUtils.getTypeOrderIn(isPopular), (String) null);

                            String where = MovieEntry._ID + " = ? ";
                            String[] whereArgs = {"" + deleteOldId};

                            // Updating the ID that is in both types to no longer be in this one.
                            getContentResolver().update(
                                    MovieEntry.CONTENT_URI,
                                    nullOutTypeCV,
                                    where,
                                    whereArgs);
                        } else {  // Situation: Normal, delete ID
                            getContentResolver().delete(  // TODO: Test against OppositeSet, to see if need update instead
                                    MovieEntry.buildUriWithMovieId(deleteOldId),
                                    null,
                                    null);
                        }
                    }
                }


                /**
                 * INSERTUPDATEANDMAKENEWIDSET - Inserts new movies into DB, Updates chart movers in DB, and makes a
                 * new ID set of incoming movies to compare against what is already in the database
                 * @param isPopular - Type the user is looking for
                 * @param idAndTitleOldCursor - Cursor of titles that are previously in DB
                 * @param oppositeTitlesCursor - Cursor of the opposite type to this one, in case this ID is in both types.
                 * @param idOldSet - Set of IDs of movies previously in our DB
                 * @param idNewSet - Set of IDs that are coming in from TMDb.
                 */
                private void insertUpdateAndMakeNewIdSet(boolean isPopular, Cursor idAndTitleOldCursor,
                                                         Cursor oppositeTitlesCursor,Set<Long> idOldSet, Set<Long> idNewSet) {
                    // Makes Set from the movies of the opposite type, in case same movie is both types,
                    // but can only take 1 id.
                    Set<Long> idOppositeSet = new HashSet<>();
                    MainLoadingUtils.makeSetOfIdsFromCursor(INDEX_ID, oppositeTitlesCursor, idOppositeSet);

                    for (int i = 0; i < movieContentValues.length; i++) {

                        Long newId = movieContentValues[i].getAsLong(MovieEntry._ID);
                        idNewSet.add(newId);    // Create new Set for difference op later

                        // TODO: If NewId is in OppositeSet, Update
                        // If this ID exists in the opposite type, need special processing so always
                        // 1 entry.  Update instead so is Pop&Top.
                        if (idOppositeSet.contains(newId)) {
                            // Update Item position of other thing for this type.
                            // Update
                            String orderType = MainLoadingUtils.getTypeOrderIn(!isPopular);
                            String where = MovieEntry._ID + " = ? AND " + orderType + " = ? ";
                            String[] whereArgs
                                    = new String[] {"" + newId
                                    , MainLoadingUtils.getOldPositionOfNewId(
                                            !isPopular, oppositeTitlesCursor, newId)};  // ID, TypeOrderIn position

                            getContentResolver().update(
                                    MovieEntry.CONTENT_URI,
                                    movieContentValues[i],
                                    where,
                                    whereArgs);
                        } else if (idOldSet.contains(newId)) {
                            // Update
                            String orderType = MainLoadingUtils.getTypeOrderIn(isPopular);
                            String[] whereArgs
                                    = new String[] {"" + newId
                                    , MainLoadingUtils.getOldPositionOfNewId(
                                            isPopular, idAndTitleOldCursor, newId)};  // ID, TypeOrderIn position

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
                 * DELIVERRESULTS - Store data from load in here for caching, and then deliver.
                 * @param data - ArrayList<Pair<Long, Pair<String, String>>>
                 */
                @Override
                public void deliverResult(ArrayList<Pair<Long, Pair<String, String>>> data) {
                    mIdsAndPosters = data;  // Assignment for caching
                    super.deliverResult(data);  // Then deliver results
                }


            };
        }

        return null;
    }

    private boolean isCurrentTypePopular() {
        return mIsPopular.equals(getString(R.string.pref_sort_popular));
    }


    @Override
    public void onLoadFinished(Loader<ArrayList<Pair<Long, Pair<String, String>>>> loader, ArrayList<Pair<Long, Pair<String, String>>> data) {
        Log.d(TAG, "onLoadFinished: ");

        if ((data != null) && (data.size() != 0)) {

            showPosters();  // The data is here, we should show it.
            mMovieAdapter.setData(data, isCurrentTypePopular());
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
        movieDetailIntent.putExtra(EXTRA_TYPE, isCurrentTypePopular());
        startActivity(movieDetailIntent);
    }


    /**
     * SHOWLOADING - Shows the loading indicator and hides the posters.  This shoule be
     * used to hide the posters until the data has come in.
     */
    public void showLoading() {
        mRecyclerView.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
    }


    /**
     * SHOWPOSTERS - Shows the posters and hides the loadingIndicator.  This should be
     * called when the poster data has arrived and it is safe for the user to interact
     * with the posters.
     */
    public void showPosters() {
        mProgressBar.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }


}
