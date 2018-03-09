package app.com.vladimirjeune.popmovies;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
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
import app.com.vladimirjeune.popmovies.utilities.MainLoadingUtils;
import app.com.vladimirjeune.popmovies.utilities.NetworkUtils;
import app.com.vladimirjeune.popmovies.utilities.OpenTMDJsonUtils;

import static app.com.vladimirjeune.popmovies.data.MovieContract.MovieEntry.FAVORITE_FLAG;
import static app.com.vladimirjeune.popmovies.utilities.MainLoadingUtils.getTypeOrderIn;

public class MainActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        LoaderManager.LoaderCallbacks<ArrayList<ContentValues>>,
        MovieAdapter.MovieOnClickHandler, RemoveFavoriteDialogFragment.RemoveFavoriteListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int TMDBQUERY_LOADER = 41;
    private static final String NETWORK_URL_POP_OR_TOP_KEY = "pop_or_top";
    public static final String EXTRA_TYPE = "app.com.vladimirjeune.popmovies.VIEW_TYPE";  // Value is a String

    private static final boolean DEVELOPER_MODE = false;    /** EN/DIS-ABLE String Mode**/
    public static final int DETAIL_CODE = 69;

    private RecyclerView mRecyclerView;
    private MovieAdapter mMovieAdapter;
    private int mPosition = RecyclerView.NO_POSITION;

    private final int mNumberOfFakeMovies = 20;

    private ContentValues[] movieContentValues;
    private String mCurrentViewType;
//    public static final String VIEW_TYPE_POPULAR;
//    public static String VIEW_TYPE_TOP_RATED;
//    public static String VIEW_TYPE_FAVORITE;

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
            MovieEntry.TOP_RATED_ORDER_IN,
            FAVORITE_FLAG   // TODO: LOOKED AT USAGE
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
    private static final int INDEX_FAVORITE_ORDER_IN = 14;

    private boolean snackBarTriggered = false;
    private View mNoFavoritesLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        safeMode();  // SAFEMODE must be engaged as soon as possible.  Not a mistake.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "BEGIN::onCreate: ");

        mCurrentViewType = getString(R.string.pref_sort_popular);

        mProgressBar = findViewById(R.id.pb_grid_movies);
        mNoFavoritesLayout = findViewById(R.id.c_lyo_no_favorites);

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
     * SAFEMODE - StrictMode is a developer tool which detects things you might be doing by
     * accident and brings them to your attention so you can fix them.
     */
    private void safeMode() {
        if (DEVELOPER_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    //.penaltyLog()
                    .penaltyFlashScreen()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()  // .penaltyDeath()
                    .build());
        }
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
     * ONPAUSE - Lifecycle callback triggered when activity becomes partially visible.
     * Changes to underlying data made during display are sent to the database now.
     */
    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "onPause() called");
//
//        ArrayList<ContentValues> adaptersData = mMovieAdapter.getData();
//
//        // Only do anything if data is populated and is not null
//        if (!postersAreShowing() || (adaptersData == null)) {
//            return;
//        }
//
//        ContentValues heartsValues = new ContentValues();
//
//        // Loop to update DB for heart state
//        for (ContentValues currentContentValues : adaptersData) {
//
//            Integer currentHeartState = currentContentValues.
//                    getAsInteger(FAVORITE_FLAG);  // 0 || 1 // TODO: LOOKED AT USAGE
//
//            String where = MovieEntry._ID + " = ? ";
//            Long currentId = currentContentValues.getAsLong(MovieEntry._ID);
//            String[] whereArgs = {"" + currentId};
//            heartsValues.put(FAVORITE_FLAG, currentHeartState);  // TODO: LOOKED AT USAGE
//
//            getContentResolver().update(
//                    MovieEntry.CONTENT_URI,
//                    heartsValues,
//                    where,
//                    whereArgs
//            );
//        }
    }

    /**
     * LOADPREFERREDMOVIELIST - Loads the movie list that the user has set in SharedPreferences
     */
    private void loadPreferredMovieList() {
        Log.d(TAG, "BEGIN::loadPreferredMovieList: ");

        // Get current type from SharedPrefs
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mCurrentViewType = sharedPreferences.getString(getString(R.string.pref_sort_key),
                getString(R.string.pref_sort_default));  // Get from SP or default

        Bundle urlBundle = getTMDQueryBundle();

        Loader<ArrayList<ContentValues>> tmdbQueryLoader = getSupportLoaderManager()
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
        URL urlForPopularOrTopRated = NetworkUtils.buildUrlForPopularOrTopRated(this, mCurrentViewType);
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
            mCurrentViewType = sharedPreferences.getString(getString(R.string.pref_sort_key),
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
    public Loader<ArrayList<ContentValues>> onCreateLoader(int id, final Bundle args) {

        if (TMDBQUERY_LOADER == id) {
            return new AsyncTaskLoader<ArrayList<ContentValues>>(this) {

                // Holds and helps to cache our data
                ArrayList<ContentValues> mIdsAndPosters = null;

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
                 * @return - ArrayList<ContentValues> - Ordered Arraylist of Movie posters and ids.
                 */
                @Override
                public ArrayList<ContentValues> loadInBackground() {

                    Log.d(TAG, "loadInBackground: ");

                    String urlString = (String) args.getCharSequence(NETWORK_URL_POP_OR_TOP_KEY);
                    ArrayList<Pair<Long, Pair<String, String>>> titlesAndPosters = null;
                    ArrayList<ContentValues> dataOutput = new ArrayList<>();

                    if (urlString != null) {  // TODO: If == null, then either nothing or, 'Favorite'
                        String tmdbJsonString = "";

                        try {
//                            isPopular = isCurrentTypePopular();

                            // Normal if can connect.  Otherwise, see if have this type of data in DB, if so fill array with it
                            if (NetworkUtils.doWeHaveInternet()) {

                                // TODO: Have an if/else so can bring up favorites just by going to db
                                // TODO: If not favorites, show EMPTY PAGE, else regular populate by Adapter
                                // TODO: Should be function so can have same thing if NO INTERNET.  'cause works either way


                                tmdbJsonString = NetworkUtils.getResponseFromHttpUrl(new URL(urlString));

                                Log.d(TAG, "loadInBackground: >>>" + tmdbJsonString + "<<<");

                                // Favorites will not have JSON
                                movieContentValues = OpenTMDJsonUtils
                                        .getPopularOrTopJSONContentValues(MainActivity.this, tmdbJsonString, isCurrentTypePopular());

                                try {
                                    // Projection and following final ints need to always be in sync
                                    final String[] projection = new String[]{
                                            MovieEntry._ID,
                                            MovieEntry.ORIGINAL_TITLE,
                                            MovieEntry.POPULAR_ORDER_IN,
                                            MovieEntry.TOP_RATED_ORDER_IN,
                                            FAVORITE_FLAG  // TODO: LOOKED AT USAGE
                                    };

                                    // Preceding Projection and these final ints always MUST be in sync
                                    final int idPos = 0;
                                    final int titlePos = 1;
                                    final int popOrderInPos = 2;
                                    final int topRatedOrderInPos = 3;
                                    final int favoriteInPos = 4;

                                    String where = MainLoadingUtils.getTypeOrderIn(getContext(), mCurrentViewType) + " IS NOT NULL ";

                                    // Query of what already in DB
                                    Cursor idAndTitleOldCursor = getContentResolver().query(
                                            MovieEntry.CONTENT_URI,
                                            projection,
                                            where,
                                            null,
                                            null
                                    );  // Do not need order for Set


                                    String onlyExistsInFavorites
                                            =  " OR  + ( "
                                            + MovieEntry.POPULAR_ORDER_IN + " IS NULL AND "
                                            + MovieEntry.TOP_RATED_ORDER_IN + " IS NULL AND "
                                            + MovieEntry.FAVORITE_FLAG + " == 1 ) ";

                                    final String oppositeWhere = MainLoadingUtils
                                            .findOppositeTypeOrderIns(getContext(), mCurrentViewType)
                                            + onlyExistsInFavorites ;  // TODO: Check to see if OK

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
                                        insertUpdateAndMakeNewIdSet(idAndTitleOldCursor, idandTitleOppositeCursor, idOldSet, idNewSet);

                                        // Delete movies that moved off list
                                        idOldSet.removeAll(idNewSet);  // Old - New => Set of IDs up for deletion
                                        deleteChartDroppedMovies(idOldSet, idandTitleOppositeCursor);

                                        idAndTitleOldCursor.close();  // Closing the Cursor
                                        if (idandTitleOppositeCursor != null) {
                                            idandTitleOppositeCursor.close();
                                        }
                                    } else {  // Got null, or the Cursor has no rows.  So can bulkInsert, since no updates.
                                        getContentResolver().bulkInsert(MovieEntry.CONTENT_URI, movieContentValues);
                                    }

                                } catch (SQLException sqe) {
                                    sqe.printStackTrace();
                                }

                                Cursor cursorPosterPathsMovieIds = MainLoadingUtils
                                        .getCursorPosterPathsMovieIds(mCurrentViewType, MainActivity.this);

                                if (cursorPosterPathsMovieIds != null) {

                                    // Add Runtimes to DB for Movies
                                    MainLoadingUtils.getRuntimesForMoviesInList(cursorPosterPathsMovieIds, MainActivity.this);

                                    // TODO: Engage
                                    MainLoadingUtils.createArrayListOfContentValuesForPosters(
                                            dataOutput,
                                            cursorPosterPathsMovieIds
                                    );

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
                                                R.string.warning_snackbar_internet, Snackbar.LENGTH_INDEFINITE)
                                                .setAction(R.string.snackbar_settings, new MySettingsListener())
                                                .show();
                                    }
                                });

                                // If no internet but we have some data, deal with that

                                // TODO: Engage
                                useStoredDataToPopulateArraylistContentValues(dataOutput);

                            }
                        } catch (JSONException je) {
                            je.printStackTrace();
                            return null;
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                            return null;
                        }

                        Log.d(TAG, "loadInBackground: Poster Count: " + dataOutput.size() );        // TODO: Engage
                    }

                    // If we are Favorites type, use stored Favorites to display
                    if (mCurrentViewType.equals(getString(R.string.pref_sort_favorite))) {
                        dataOutput = new ArrayList<>();     // TODO: Engage

                        useStoredDataToPopulateArraylistContentValues(dataOutput);                         // TODO: Engage

                        if (dataOutput.size() == 0) {  // TODO: Engage, otherwise alphabetize FavoriteIn to show correctly

                            // Cannot call showNoFavorites off of UIThread
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    showNoFavorites();  // Transition from current progressBar

                                }
                            });

                        }
                    }

                    return dataOutput; // TODO: Engage
                }


                /**
                 * DELETECHARTDROPPEDMOVIES - Deletes movies that fell off the chart from the DB.
                 * @param idDeleteSet - Modified set of old Movie IDs consists of Old IDs - New Ids
                 *                 and the Intersection of Old & New
                 * @param oppositeTitlesCursor - In case we have to delete something that is many types.  Just update
                 */
                private void deleteChartDroppedMovies(Set<Long> idDeleteSet, Cursor oppositeTitlesCursor) {

                    Set<Long> idOppositeSet = new HashSet<>();
                    MainLoadingUtils.makeSetOfIdsFromCursor(INDEX_ID, oppositeTitlesCursor, idOppositeSet);

                    for (Long deleteOldId : idDeleteSet) {  // TODO: Make work with multiple types

                        // Situation: ID is in many types.  Want this one updated to exists only in the others
                        if (idOppositeSet.contains(deleteOldId)) {
                            ContentValues nullOutTypeCV = new ContentValues();
                            // Nulling out this usage, since ID is currently used for other type.
                            nullOutTypeCV.put(MainLoadingUtils.getTypeOrderIn(getContext(), mCurrentViewType), (String) null);

                            String where = MovieEntry._ID + " = ? ";
                            String[] whereArgs = {"" + deleteOldId};

                            // Updating the ID that is in many, types to no longer be in this one.
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
                 * @param idAndTitleOldCursor - Cursor of titles that are previously in DB
                 * @param oppositeTitlesCursor - Cursor of the opposite type to this one, in case this ID multiple types.
                 * @param idOldSet - Set of IDs of movies previously in our DB
                 * @param idNewSet - Set of IDs that are coming in from TMDb.
                 */
                private void insertUpdateAndMakeNewIdSet(Cursor idAndTitleOldCursor,
                                                         Cursor oppositeTitlesCursor,
                                                         Set<Long> idOldSet,
                                                         Set<Long> idNewSet) {
                    // Makes Set from the movies of the opposite type, in case same movie is many types,
                    // but can only take 1 id.
                    Set<Long> idOppositeSet = new HashSet<>();
                    MainLoadingUtils.makeSetOfIdsFromCursor(INDEX_ID, oppositeTitlesCursor, idOppositeSet);

                    for (int i = 0; i < movieContentValues.length; i++) {

                        Long newId = movieContentValues[i].getAsLong(MovieEntry._ID);
                        idNewSet.add(newId);    // Create new Set for difference op later

                        // TODO: If NewId is in OppositeSet, Update
                        // If this ID exists in the opposite type, need special processing so always
                        // 1 entry.  Update instead so is Pop&Top.
                        // So finding in db the movie with the specific ID and a known position in
                        // another type than current.  Once found, do an update to it so there is a
                        // OrderIn for this type as well.  Make sure only THIS OrderIn is updated; not
                        // the one that you found.  You only need to find one other OrderIn, even if
                        // there exists more than one.  We are just trying to find movies that are in
                        // multiple locations so they are only updated instead of getting duplicates.
                        // TODO: Look for ID with other types using query (get 1st match) and do this
                        // TODO: Make sure that by this point one should match.  There is a function that
                        // TODO: returns the other types.  Use that.
                        if (idOppositeSet.contains(newId)) {


                            /////////////
                            Pair<String, String> otherTypes = MainLoadingUtils.findOtherTypeIns(getContext(), mCurrentViewType);
                            String otherTypeIn1 = otherTypes.first;
                            String otherTypeIn2 = otherTypes.second;

                            // Now query one, and if nothing, then the other.  There should be one at this point.
                            // Use that other in the function calls below

                            String[] projection = new String[] {
                                    MovieEntry._ID,
                                    MovieEntry.ORIGINAL_TITLE,
                                    MovieEntry.POPULAR_ORDER_IN,
                                    MovieEntry.TOP_RATED_ORDER_IN,
                                    FAVORITE_FLAG  // TODO: LOOKED AT USAGE
                            };

                            String whereIDTypeAndOtherType_1 =
                                    MovieEntry._ID + " = ? AND "
                                            + getTypeOrderIn(getContext(), mCurrentViewType)
                                            + " IS NOT NULL AND "
                                            + MainLoadingUtils.getOtherOrderInWhereString(otherTypeIn1);

                            String[] whereArgs = new String[] {"" + newId};

                            Cursor typeAndFirstCursor = getContentResolver().query(
                                    MovieEntry.CONTENT_URI,
                                    projection,
                                    whereIDTypeAndOtherType_1,
                                    whereArgs,
                                    null
                            );

                            // If we already have a movie with the current viewType and others
                            if (typeAndFirstCursor != null) {
                                if (typeAndFirstCursor.moveToFirst()) {

                                    getContentResolver().update(
                                            MovieEntry.CONTENT_URI,
                                            movieContentValues[i],
                                            whereIDTypeAndOtherType_1,
                                            whereArgs
                                    );

                                } else {  // So must have been the other type

                                    String whereIDTypeAndOtherType_2 =
                                            MovieEntry._ID + " = ? AND "
                                                    + getTypeOrderIn(getContext(), mCurrentViewType)
                                                    + " IS NOT NULL AND "
                                                    + MainLoadingUtils.getOtherOrderInWhereString(otherTypeIn2);

                                    getContentResolver().update(
                                            MovieEntry.CONTENT_URI,
                                            movieContentValues[i],
                                            whereIDTypeAndOtherType_2,
                                            whereArgs
                                    );
                                }
                                typeAndFirstCursor.close();
                            }
                            /////////////

                        } else if (idOldSet.contains(newId)) {
                            // Update
                            String orderInForType = MainLoadingUtils.getTypeOrderIn(getContext(), mCurrentViewType);
                            String[] whereArgs
                                    = new String[] {"" + newId
                                    , MainLoadingUtils.getOldPositionOfNewId(getContext(),
                                            mCurrentViewType, idAndTitleOldCursor, newId)};  // ID, TypeOrderIn position

                            String where = MovieEntry._ID + " = ? AND " + orderInForType + " = ? ";
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
                public void deliverResult(ArrayList<ContentValues> data) {
                    mIdsAndPosters = data;  // Assignment for caching
                    super.deliverResult(data);  // Then deliver results
                }


            };
        }

        return null;
    }


    /**
     *  USESTOREDDATATOPOPULATEARRAYLISTCONTENTVALUES - Data we already have will be used to populate list for adapter.
     * @param titlesAndData - The list to populate with data
     */
    private void useStoredDataToPopulateArraylistContentValues(ArrayList<ContentValues> titlesAndData) {

        Cursor cursorForIdsAndPosters = MainLoadingUtils
                .getCursorPosterPathsMovieIds(mCurrentViewType, MainActivity.this);

        if (cursorForIdsAndPosters != null) {
            MainLoadingUtils.createArrayListOfContentValuesForPosters(
                    titlesAndData, cursorForIdsAndPosters);  // Fill ArrayList

            cursorForIdsAndPosters.close();  // Close your cursors
        }

    }

    /**
     * ISCURRENTTYPEPOPULAR - Whether the current View Type is Popular.  Does not differentiate between
     * the other 2 types.
     * @return - boolean - Whether Popular or not
     */
    private boolean isCurrentTypePopular() {
        return mCurrentViewType.equals(getString(R.string.pref_sort_popular));
    }


    @Override
    public void onLoadFinished(Loader<ArrayList<ContentValues>> loader, ArrayList<ContentValues> data) {
        Log.d(TAG, "onLoadFinished: ");

        if ((data != null) && (data.size() != 0)) {

            showPosters();  // The data is here, we should show it.
            mMovieAdapter.setData(data, mCurrentViewType);
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
    public void onLoaderReset(Loader<ArrayList<ContentValues>> loader) {
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
        movieDetailIntent.putExtra(EXTRA_TYPE, mCurrentViewType);
//        startActivity(movieDetailIntent);
        startActivityForResult(movieDetailIntent, DETAIL_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DETAIL_CODE) {

            if (resultCode == Activity.RESULT_OK) {
                long[] detailResults = data.getLongArrayExtra(DetailActivity.DETAIL_ACTIVITY_RETURN);
                showLoading();
                loadPreferredMovieList();
                Log.d(TAG, "onActivityResult() called with: requestCode = [" + requestCode + "], resultCode = [" + resultCode + "], data = [" + data + "], ["
                + detailResults[0] +" ], [" + detailResults[1] + "]");
            }

        }
    }

    /**
     * SHOWLOADING - Shows the loading indicator and hides the posters.  This shoule be
     * used to hide the posters until the data has come in.
     */
    private void showLoading() {
        mRecyclerView.setVisibility(View.INVISIBLE);
        mNoFavoritesLayout.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
    }


    /**
     * SHOWPOSTERS - Shows the posters and hides the loadingIndicator.  This should be
     * called when the poster data has arrived and it is safe for the user to interact
     * with the posters.
     */
    private void showPosters() {
        mProgressBar.setVisibility(View.INVISIBLE);
        mNoFavoritesLayout.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    /**
     * POSTERSARESHOWING - Tells whether we are showing posters and the load has finished.
     * Important, so we know that the data for hearts is valid.
     * @return - boolean - Tells whether posters are showing.
     */
    private boolean postersAreShowing() {
        return (
                (mProgressBar.getVisibility() == View.INVISIBLE)
                && (mNoFavoritesLayout.getVisibility() == View.INVISIBLE)
                && (mRecyclerView.getVisibility() == View.VISIBLE)
                );
    }

    /**
     * SHOWNOFAVORITES - Shows the empty Favorites page.  This should be triggered when Favorites
     * is the type currently selected and there are no Favorites to show.  This page will be shown
     * instead.
     */
    private void showNoFavorites() {
        mProgressBar.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.INVISIBLE);
        mNoFavoritesLayout.setVisibility(View.VISIBLE);
    }


    /**
     * SHOWREMOVEFAVORITEDIALOG - Will create an instance of the dialog fragment and then
     * show it.
     */
    public void showRemoveFavoriteDialog() {
        DialogFragment dialogFragment = new RemoveFavoriteDialogFragment();
        dialogFragment.show(getSupportFragmentManager()
                , RemoveFavoriteDialogFragment.REMOVEFAVORITEDIALOG_TAG);
    }

    @Override
    public void onDialogAffirmativeClick(RemoveFavoriteDialogFragment dialogFragment) {
        // TODO: If you want to continue with adding this to Main
        // You need to pull the appropriate functions out of PosterViewHolder and
        // into the Adapter so you can call them from here.  Or recreate the functionality
        // in Main since you have access to adapter and database here.
        // Remember; there is only one specific situation when this would be called
        // So there would not need to be as many conditionals as there are in the RecyclerView
    }

    @Override
    public void onDialogNegativeClick(RemoveFavoriteDialogFragment dialogFragment) {

    }
}
