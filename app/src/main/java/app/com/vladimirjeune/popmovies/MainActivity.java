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

public class MainActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        LoaderManager.LoaderCallbacks<ArrayList<ContentValues>>,
        MovieAdapter.MovieOnClickHandler,
        RemoveFavoriteDialogFragment.RemoveFavoriteListener {

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

                                processIncomingIntoDB();

                                } catch (SQLException sqe) {
                                    sqe.printStackTrace();
                                }

                                Cursor cursorPosterPathsMovieIds = MainLoadingUtils
                                        .getCursorPosterPathsMovieIds(mCurrentViewType, MainActivity.this);

                                if (cursorPosterPathsMovieIds != null) {

                                    // Add Runtimes to DB for Movies
                                    MainLoadingUtils.getRuntimesForMoviesInList(cursorPosterPathsMovieIds, MainActivity.this);

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
                 * PROCESSINCOMINGINTODB - Processes incoming movie data so that old movies are properly
                 * updated or removed, and new movies are properly updated or inserted.
                 * Database = DB, Database of Type = DBOT, Incoming of Type = ICOT
                 * DBOT - ICOT => Delete or update
                 * DB intersect ICOT = > Update
                 * ICOT - DBOT => Insert
                 */
                private void processIncomingIntoDB() {
                    Set<Long> currentDBSet = new HashSet<>();
                    Set<Long> currentDBSetOfType = new HashSet<>();
                    Set<Long> incomingTypeSet = new HashSet<>();

                    // Whole DB
                    currentDBSet = MainLoadingUtils.currentDBIDs(getContext());

                    if (currentDBSet.size() != 0) {  // There are things in the DB to consider

                        currentDBSetOfType = MainLoadingUtils.currentDBIDsOfType(getContext(), mCurrentViewType);
                        incomingTypeSet = MainLoadingUtils.incomingIDs(movieContentValues);

                        // Deletion AND Insertion are type specific
                        Set<Long> needDeleteSetOfType = new HashSet<>(currentDBSetOfType);  // Using Copy Constructor
                        needDeleteSetOfType.removeAll(incomingTypeSet);
                        MainLoadingUtils.deleteUpdateIDsOfType(getContext(), mCurrentViewType, needDeleteSetOfType,
                                movieContentValues);

                        // IDs to update, comparing against entire DB, because incoming ID can already belong to multiple types
                        Set<Long> needUpdateSet = new HashSet<>(currentDBSet);
                        needUpdateSet.retainAll(incomingTypeSet);
                        MainLoadingUtils.updateIDsAgainstWholeDB(getContext(), needUpdateSet, movieContentValues);


                        // IDs to insert, since all updates of old and new data have already occurred.
                        Set<Long> needInsertSetOfType = new HashSet<>(incomingTypeSet);
                        needInsertSetOfType.removeAll(currentDBSetOfType);

                        // You need to remove stuff that was just updated, if it was not of this type initially
                        needInsertSetOfType.removeAll(needUpdateSet);

                        MainLoadingUtils.insertIDsOfType(getContext(), needInsertSetOfType, movieContentValues);

                    } else {  // Got null, or the Cursor has no rows.  So can bulkInsert, since no updates are necessary.
                        getContentResolver().bulkInsert(MovieEntry.CONTENT_URI, movieContentValues);
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
        startActivityForResult(movieDetailIntent, DETAIL_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DETAIL_CODE) {

            if (resultCode == Activity.RESULT_OK) {
                long[] detailResults = data.getLongArrayExtra(DetailActivity.DETAIL_ACTIVITY_RETURN);
                long heartStateChanged = detailResults[1];

                // If changed then need to update everything
                final long heartChangedTrue = 1L;
                if (heartStateChanged == heartChangedTrue) {
                    showLoading();
                    loadPreferredMovieList();
                }
                // Else visually all is the same

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
     * SHOWNOFAVORITES - Shows the empty Favorites page.  This should be triggered when Favorites
     * is the type currently selected and there are no Favorites to show.  This page will be shown
     * instead.
     */
    private void showNoFavorites() {
        mProgressBar.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.INVISIBLE);
        mNoFavoritesLayout.setVisibility(View.VISIBLE);
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
