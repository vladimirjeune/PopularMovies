package app.com.vladimirjeune.popmovies;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.URL;

import app.com.vladimirjeune.popmovies.data.MovieContract;
import app.com.vladimirjeune.popmovies.data.MovieContract.MovieEntry;
import app.com.vladimirjeune.popmovies.data.MovieContract.ReviewEntry;
import app.com.vladimirjeune.popmovies.utilities.NetworkUtils;

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        RemoveFavoriteDialogFragment.RemoveFavoriteListener {

    public static final String DETAIL_ACTIVITY_RETURN =  "app.com.vladimirjeune.popmovies.DETAILACTIVITYRETURN" ;

    private static final int DETAIL_LOADER_ID = 117;

    private static final String[] DETAIL_MOVIE_PROJECTION = {
            MovieEntry._ID,
            MovieEntry.ORIGINAL_TITLE,
            MovieEntry.POSTER_PATH,
            MovieEntry.SYNOPSIS,
            MovieEntry.RELEASE_DATE,
            MovieEntry.VOTER_AVERAGE,
            MovieEntry.BACKDROP_PATH,
            MovieEntry.RUNTIME,
            MovieEntry.POSTER,
            MovieEntry.BACKDROP,
            MovieEntry.FAVORITE_FLAG,
    };

    // *** IMPORTANT ***  These ints and the previous projection MUST REMAIN CORRELATED
    private static final int DETAIL_INDEX_ID = 0;
    private static final int DETAIL_INDEX_ORIGINAL_TITLE = 1;
    private static final int DETAIL_INDEX_POSTER_PATH = 2;
    private static final int DETAIL_INDEX_SYNOPSIS = 3;
    private static final int DETAIL_INDEX_RELEASE_DATE = 4;
    private static final int DETAIL_INDEX_VOTER_AVERAGE = 5;
    private static final int DETAIL_INDEX_BACKDROP_PATH = 6;
    private static final int DETAIL_INDEX_RUNTIME = 7;
    private static final int DETAIL_INDEX_POSTER = 8;
    private static final int DETAIL_INDEX_BACKDROP = 9;
    private static final int DETAIL_INDEX_FAVORITE_FLAG = 10;


    private static final String TAG = DetailActivity.class.getSimpleName();
    private static final Integer HEART_FALSE = 0;
    private static final Integer HEART_TRUE = 1;
    private static final java.lang.String HEART_DISABLED_KEY = "HEART_DISABLED_KEY";

    private static final int REVIEW_AQT_CALLBACK = -1;
    private Integer mHeartState0or1;
    private long mIDForMovie;

    private String mViewType;

    private Uri mUri;
    private TextView mTitle;
    private View mTitleBackgroundView;
    private TextView mSynopsisTextView;
    private TextView mReleaseTextView;
    private TextView mRatingTextView;
    private TextView mRuntimeTextView;
    private ImageView mOneSheetImageView;
    private CheckBox mHeartCheckboxView;

    private TextView mSynopsisTitleTextView;
    private TextView mReleaseTitleTextView;
    private TextView mRatingTitleTextView;
    private TextView mRuntimeTitleTextView;

    private RecyclerView mReviewRecyclerView;
    private RecyclerView.LayoutManager mReviewLayoutManager;
    private ReviewAdapter mReviewAdapter;

    private boolean HEART_DISABLED;
    private String mPosterPath;

    private int mPosition = RecyclerView.NO_POSITION;
    private TextView mNoReviewTextView;


    private final Target mTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            mOneSheetImageView.setImageBitmap(bitmap);

            Palette.Builder paletteBuilder = getBuilderWithWhiteTextBGFilter(bitmap);
            setTitleTextBackgroundColor(paletteBuilder);

        }

        private void setTitleTextBackgroundColor(Palette.Builder paletteBuilder) {
            // Use the Palette Builder to generate an appropriate background for Title text
            paletteBuilder.generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {

                    final int blackColor = 0;
                    final int vibrantColor = palette.getVibrantColor(blackColor);
                    final int darkVibrantColor = palette.getDarkVibrantColor(blackColor);
                    final int darkMutedColor = palette.getDarkMutedColor(blackColor);
                    final int mutedColor = palette.getMutedColor(blackColor);
                    final int lightVibrantColor = palette.getLightVibrantColor(blackColor);
                    final int lightMutedColor = palette.getLightMutedColor(blackColor);

                    if (vibrantColor != blackColor) {
                        mTitleBackgroundView.setBackgroundColor(vibrantColor);
                    } else if (darkVibrantColor != blackColor) {
                        mTitleBackgroundView.setBackgroundColor(darkVibrantColor);
                    } else if (lightVibrantColor != blackColor) {
                        mTitleBackgroundView.setBackgroundColor(lightVibrantColor);
                    } else if (darkMutedColor != blackColor) {
                        mTitleBackgroundView.setBackgroundColor(darkMutedColor);
                    } else if (lightMutedColor != blackColor) {
                        mTitleBackgroundView.setBackgroundColor(lightMutedColor);
                    } else if (mutedColor != blackColor) {
                        mTitleBackgroundView.setBackgroundColor(mutedColor);
                    }
                }
            });
        }

        /**
         * GETBUILDERWITHWHITETEXTBGFILTER - Returns a Builder that has filtered out the Palettes
         * that would make white text placed on top of it look bad.
         * @param bitmap - The Bitmap we are looking at to get the appropriate background color
         * @return - Palette.Builder - Palettes that would make good white text backgrounds
         * based on Bitmap.
         */
        @NonNull
        private Palette.Builder getBuilderWithWhiteTextBGFilter(Bitmap bitmap) {
            return new Palette.Builder(bitmap)
                            .addFilter(new Palette.Filter() {
                                @Override
                                public boolean isAllowed(int rgb, float[] hsl) {
                                    // From: https://stackoverflow.com/questions/3942878/
                                    // how-to-decide-font-color-in-white-or-black-depending-on-background-color
                                    float contrastFormulaBlackWhiteText = 0.179f;
                                    int luminanceIndex = 2;
                                    float luminance = hsl[luminanceIndex];

                                    return luminance <= contrastFormulaBlackWhiteText;  // Good BG for White Text
                                }
                            });
        }

        /**
         * ONBITMAPFAILED - Called when you call error
         * @param errorDrawable - What to show in this case
         */
        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            mOneSheetImageView.setImageDrawable(errorDrawable);
        }

        /**
         * ONPREPARELOAD - Shown if placeholder called and we are waiting for
         * the image to arrive.  May be shown until image arrives or we error out.
         * @param placeHolderDrawable - Image to show in this case
         */
        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            mOneSheetImageView.setImageDrawable(placeHolderDrawable);
        }
    };


    /**
     * ONPAUSE - Lifecycle callback triggered when activity becomes partially visible.
     * Changes to underlying data made during display are sent to the database now.
     */
    @Override
    protected void onPause() {
        super.onPause();


    }

    /**
     * SQLUPDATEORDELETE - Updates this movie in the database or Deletes it under the special
     * circumstance that it is a Favorite without any other type affiliations and is unHearted.
     */
    private void sqlUpdateOrDelete() {
        ContentValues heartContentValues = new ContentValues();
        heartContentValues.put(MovieEntry.FAVORITE_FLAG, mHeartState0or1);
        String where = MovieEntry._ID + " = ? ";
        String[] whereArgs = {"" + mIDForMovie};

        Log.i(TAG, "sqlUpdateOrDelete: DetailActivity called. Heart state is: " + ((mHeartState0or1 == 1) ? "[TRUE]" : "[FALSE]"));

        // If ViewType is NOT Favorite or we are turning Heart On
        if ((! mViewType.equals(getString(R.string.pref_sort_favorite)))
                || (mHeartState0or1.equals(HEART_TRUE))) {
            simpleUpdate(heartContentValues, where, whereArgs);
        } else {
            String[] favElseProjections =
                    new String[] {MovieContract.MovieEntry._ID, MovieContract.MovieEntry.ORIGINAL_TITLE};
            String favElseWhere = MovieContract.MovieEntry._ID + " = ? "
                    + " AND ( " + MovieContract.MovieEntry.POPULAR_ORDER_IN + " IS NOT NULL OR "
                    + MovieContract.MovieEntry.TOP_RATED_ORDER_IN + " IS NOT NULL ) ";

            // Search for other links that would delay deletion
            Cursor hasOtherTypesCursor = getContentResolver().query(
                    MovieContract.MovieEntry.CONTENT_URI,
                    favElseProjections,
                    favElseWhere,
                    new String[] { "" + mIDForMovie},
                    null
            );

            // If has other types, then update
            if ((hasOtherTypesCursor != null)
                    && (hasOtherTypesCursor.moveToFirst())) {
                simpleUpdate(heartContentValues, where, whereArgs);
            } else { // Else delete since no other links.  Must delete to restrict zombie ids in DB
                getContentResolver().delete(
                        MovieContract.MovieEntry.CONTENT_URI,
                        where,
                        whereArgs);  // TODO: Maybe set a boolean that will be returned to Main saying delete this ID and restartLoader
            }

            if (hasOtherTypesCursor != null) {
                hasOtherTypesCursor.close();
            }


        }
    }

    /**
     * SIMPLEUPDATE - Does a simple update to the database.  Is used by another function
     * @param heartContentValues - ContentValues for update
     * @param where - Selection Clause
     * @param whereArgs - Arguments for Selection clause
     */
    private void simpleUpdate(ContentValues heartContentValues, String where, String[] whereArgs) {
        getContentResolver().update(
                MovieEntry.CONTENT_URI,
                heartContentValues,
                where,
                whereArgs
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);  // May be needed for Result

        Intent movieIntent = getIntent();

        mUri = movieIntent.getData();

        if (mUri == null) {
            throw new NullPointerException("Uri passed to DetailActivity cannot be null");
        }

        mViewType =  movieIntent.getStringExtra(MainActivity.EXTRA_TYPE);

        mTitle = findViewById(R.id.textViewTitle);
        mHeartState0or1 = HEART_FALSE;
        mTitleBackgroundView = findViewById(R.id.textViewTitleBackground);

        // Data
        mSynopsisTextView = findViewById(R.id.textViewSynopsis);
        mReleaseTextView = findViewById(R.id.textViewRelease);
        mRatingTextView = findViewById(R.id.textViewRating);
        mRuntimeTextView = findViewById(R.id.textViewRuntime);
        mOneSheetImageView = findViewById(R.id.imageViewOnesheet);
        mHeartCheckboxView = findViewById(R.id.checkbox_favorite);

        // Titles
        mSynopsisTitleTextView = findViewById(R.id.textViewSynopsisTitle);
        mReleaseTitleTextView = findViewById(R.id.textViewReleaseTitle);
        mRatingTitleTextView = findViewById(R.id.textViewRatingTitle);
        mRuntimeTitleTextView = findViewById(R.id.textViewRuntimeTitle);

        // CheckBox state from SavedBundle
        if ((savedInstanceState != null)
                && (mViewType.equals(getString(R.string.pref_sort_favorite)))) {
            HEART_DISABLED = savedInstanceState.getBoolean(HEART_DISABLED_KEY);
            Log.i(TAG, "onCreate: HEART IS: " + ((HEART_DISABLED) ? "DISABLED" : "ENABLED" ));
        }

        if ( ! HEART_DISABLED) {
            getSupportLoaderManager().initLoader(DETAIL_LOADER_ID, null, this);
            Log.i(TAG, "onCreate: CHECKBOX IS: [HD=F] " + ((HEART_DISABLED) ? "DISABLED" : "ENABLED" ));
        } else {
            mHeartCheckboxView.setEnabled(HEART_DISABLED);  // May be too late, here
            Log.i(TAG, "onCreate: CHECKBOX IS: [HD=T] " + ((HEART_DISABLED) ? "DISABLED" : "ENABLED" ));
        }

        // Text for when there are no reviews
        mNoReviewTextView = findViewById(R.id.tv_review_empty);


        mReviewRecyclerView = findViewById(R.id.rv_horizontal_linear_reviews);
        mReviewRecyclerView.setHasFixedSize(true);
        boolean reverseLayout = false;
        mReviewLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, reverseLayout);
        mReviewRecyclerView.setLayoutManager(mReviewLayoutManager);

        mReviewAdapter = new ReviewAdapter(this);
        mReviewRecyclerView.setAdapter(mReviewAdapter);

    }


    /**
     * ONSAVEINSTANCESTATE - MUST override the one with only one parameter and with a, 'protected',
     * modifier.  The other ones are not the ones you are looking for and will cause hard to figure
     * out errors.
     * @param outState - Bundle
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mViewType.equals(getString(R.string.pref_sort_favorite))) {
            outState.putBoolean(HEART_DISABLED_KEY, HEART_DISABLED);

            MovieState movieState = new MovieState(
                    mTitle.getText().toString(),
                    mPosterPath,
                    mRatingTextView.getText().toString(),
                    mRuntimeTextView.getText().toString(),
                    mReleaseTextView.getText().toString(),
                    mSynopsisTextView.getText().toString(),
                    ContextCompat.getColor(this, R.color.logo_purple),  // Favorite color
                    ContextCompat.getColor(this, R.color.header_favorite_background)  // Favorite color
            );

            outState.putSerializable(MovieState.MOVIESTATE, movieState);
        }

        Log.d(TAG, "onSaveInstanceState() called with: HEART_DISABLED = [" + HEART_DISABLED + "]");
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (mViewType.equals(getString(R.string.pref_sort_favorite))) {
            MovieState ms = (MovieState) savedInstanceState.getSerializable(MovieState.MOVIESTATE);

            if (ms != null) {

                mTitle.setText(ms.getTitle());
                mPosterPath = ms.getImagePath();
                mRatingTextView.setText(ms.getRating());
                mRuntimeTextView.setText(ms.getRuntime());
                mReleaseTextView.setText(ms.getRelease());
                mSynopsisTextView.setText(ms.getSynopsis());

                // Text Colors for Favorites
                mRatingTitleTextView.setTextColor(ms.getTextColor());
                mRuntimeTitleTextView.setTextColor(ms.getTextColor());
                mReleaseTitleTextView.setTextColor(ms.getTextColor());
                mSynopsisTitleTextView.setTextColor(ms.getTextColor());

                // Text Background Colors for Favorites
                mRatingTitleTextView.setBackgroundColor(ms.getTextBackgroundColor());
                mRuntimeTitleTextView.setBackgroundColor(ms.getTextBackgroundColor());
                mReleaseTitleTextView.setBackgroundColor(ms.getTextBackgroundColor());
                mSynopsisTitleTextView.setBackgroundColor(ms.getTextBackgroundColor());

            }

            loadMovieImage();

            HEART_DISABLED = savedInstanceState.getBoolean(HEART_DISABLED_KEY);
            mHeartCheckboxView.setEnabled(! HEART_DISABLED);

        }

        Log.d(TAG, "onRestoreInstanceState() called with: HEART_DISABLED = [" + HEART_DISABLED + "]");

    }


    /**
     * LOADMOVIEIMAGE - Loads the movie image that is stored at the Poster Path
     * from the internet.
     */
    private void loadMovieImage() {
        URL imageURL = NetworkUtils.buildURLForImage(mPosterPath);

        Picasso.with(this)
                .load(String.valueOf(imageURL))
                .placeholder(R.drawable.tmd_placeholder_poster)
                .error(R.drawable.tmd_error_poster)
                .into(mTarget);
    }

    /**
     * ONCREATELOADER - Makes and returns a CursorLoader that loads the data for our URI and stores it in a Cursor.
     * @param loaderId - Loader ID should be the ID for the loader we need to create
     * @param loaderArgs - Arguments supplied by the caller
     * @return  New Loader instance that is ready to start loading.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle loaderArgs) {

        switch (loaderId) {
            case DETAIL_LOADER_ID:

                // Will call the ContentResolver with these exact parameters
                return new CursorLoader(this,
                        mUri,
                        DETAIL_MOVIE_PROJECTION,
                        null,
                        null,
                        null);
            default:
                throw new UnsupportedOperationException("Unknown ID: " + loaderId);
        }
    }

    /**
     * NOTE FOR RECYCLERVIEW CURSOR: There is one small bug in this code. If no data is present in the cursor do to an
     * initial load being performed with no access to internet, the loading indicator will show
     * indefinitely, until data is present from the ContentProvider.
     * @param loader
     * @param data
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        /*
         * Before we bind the data to the UI that will display that data, we need to check the
         * cursor to make sure we have the results that we are expecting. In order to do that, we
         * check to make sure the cursor is not null and then we call moveToFirst on the cursor.
         * Although it may not seem obvious at first, moveToFirst will return true if it contains
         * a valid first row of data.
         *
         * If we have valid data, we want to continue on to bind that data to the UI. If we don't
         * have any data to bind, we just return from this method.
         */
        boolean cursorHasValidData = false;

        if ((data != null) && data.moveToFirst()) {
            cursorHasValidData = true;  // Ready to go
            Log.d(TAG, "onLoadFinished: Cursor HAS valid data!!!");
        }

        if ( ! cursorHasValidData) {  // Nothing to do here
            Log.d(TAG, "onLoadFinished: Cursor DOES NOT have valid data!!!");
            return;
        }


        // Image
        mPosterPath = data.getString(DETAIL_INDEX_POSTER_PATH);

        loadMovieImage();

        // Foreground and Background color for Section Titles
        Pair<Integer, Integer> titleTextAndBackgroundColor = getTextAndBackgroundColorsBasedOnType();
        int textTypeColor = titleTextAndBackgroundColor.first;
        int backgroundColor = titleTextAndBackgroundColor.second;

        // Title, ID & set ImageView ContentDescription
        mIDForMovie = data.getLong(DETAIL_INDEX_ID);
        String originalTitle = data.getString(DETAIL_INDEX_ORIGINAL_TITLE);
        String a11yPosterText = getString(R.string.a11y_poster, originalTitle);
        mTitle.setText(originalTitle);
        mOneSheetImageView.setContentDescription(a11yPosterText);

        // Rating
        mRatingTextView.setText(data.getString(DETAIL_INDEX_VOTER_AVERAGE));
        mRatingTitleTextView.setTextColor(textTypeColor);
        mRatingTitleTextView.setBackgroundColor(backgroundColor);

        // Preparing Runtime
        String fullRuntime = String.format("%s min", data.getString(DETAIL_INDEX_RUNTIME));  // 141 min
        mRuntimeTextView.setText(fullRuntime);
        mRuntimeTitleTextView.setTextColor(textTypeColor);
        mRuntimeTitleTextView.setBackgroundColor(backgroundColor);

        // Preparing Release Year
        String releaseDate = data.getString(DETAIL_INDEX_RELEASE_DATE);  // 2017-12-13
        String[] releaseDataParts = releaseDate.split("-");  // 0:[2017] 1:[12] 2:[13]
        String releaseYear = releaseDataParts[0];  // 2017
        mReleaseTextView.setText(releaseYear);
        mReleaseTitleTextView.setTextColor(textTypeColor);
        mReleaseTitleTextView.setBackgroundColor(backgroundColor);

        mSynopsisTextView.setText(data.getString(DETAIL_INDEX_SYNOPSIS));
        mSynopsisTitleTextView.setTextColor(textTypeColor);
        mSynopsisTitleTextView.setBackgroundColor(backgroundColor);

        // Stuff with Checkbox
        mHeartState0or1 = data.getInt(DETAIL_INDEX_FAVORITE_FLAG);
        mHeartCheckboxView.setChecked(mHeartState0or1.equals(HEART_TRUE));
        ((CheckBox) mHeartCheckboxView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mHeartCheckboxView.isChecked()) {
                    mHeartState0or1 = HEART_TRUE;
                    sqlUpdateOrDelete();
                } else {
                    if (!(mViewType.equals(getString(R.string.pref_sort_favorite)))) {
                        mHeartState0or1 = HEART_FALSE;
                        sqlUpdateOrDelete();
                    } else {
                        mHeartCheckboxView.setChecked(true);  // Will have to set to false later, if necessary
                        showRemoveFavoriteDialog();
                    }
                }
            }
        });

        // TODO: Query DB about Reviews for this MovieID.
        ReviewQueryHandler reviewQueryHandler = new ReviewQueryHandler(getContentResolver(), this);
        reviewsForMovie(reviewQueryHandler);

        data.close();  // TODO: Rememeber to close the Cursor

    }


    /**
     * REVIEWQUERYHANDLER - Static class needed to not leak memory while accessing the
     * the database using AsyncQueryHandler task.
     */
    private static class ReviewQueryHandler extends AsyncQueryHandler {
    // https://stackoverflow.com/questions/37188519/this-handler-class-should
    // -be-static-or-leaks-might-occurasyncqueryhandler
        private final WeakReference<DetailActivity> mActivity;

        // DO NOT USE
        public ReviewQueryHandler(ContentResolver cr) {
            super(cr);
            mActivity = null;
        }

        public ReviewQueryHandler(ContentResolver cr, DetailActivity da) {
            super(cr);
            mActivity = new WeakReference<DetailActivity>(da);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {

            if (mActivity != null) {
                DetailActivity detailActivity = mActivity.get();  // Getting the Activity from the WeakReference
                ReviewAdapter reviewAdapter = detailActivity.getReviewAdapter();

                if (cursor == null) {
                    detailActivity.showRecyclerView(false);
                    return;
                }

                if ((cursor.moveToFirst())
                        && (reviewAdapter != null)) {
                    reviewAdapter.swapCursor(cursor);  // May have 2 send ViewType in later if send BG for ImageView
                    detailActivity.showRecyclerView(true);

                } else {
                    detailActivity.showRecyclerView(false);
                }

            }
        }
    }


    /**
     * GETREVIEWADAPTER - Returns the ReviewAdapter
     * @return - ReviewAdapter - Can be null
     */
    ReviewAdapter getReviewAdapter() {
        return mReviewAdapter;
    }


    /**
     * REVIEWSFORMOVIE - Getting reviews, if any, for this movie id
     * Result sent to Callback for AsyncQueryTask
     */
    private void reviewsForMovie(ReviewQueryHandler queryHandler) {
        Uri reviewUri = ReviewEntry.CONTENT_URI;
        String[] projection = new String[] {
                ReviewEntry.REVIEW_ID,
                ReviewEntry.AUTHOR,
                ReviewEntry.CONTENT
        };
        String selection = ReviewEntry.MOVIE_ID + " = ? ";
        String[] selectionArgs = new String[] {""+mIDForMovie};
        String orderBy = ReviewEntry.REVIEW_ID;

        queryHandler.startQuery(
                REVIEW_AQT_CALLBACK,
                null,
                reviewUri,
                projection,
                selection,
                selectionArgs,
                orderBy
        );

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
        mReviewRecyclerView.scrollToPosition(mPosition);
    }


    /**
     * SHOWRECYCLERVIEW - RecyclerView starts out as VIEW.GONE because most movies won't have reviews.
     * So a TextView is shown stating that this movie has no reviews.  If there is data for Reviews
     * the TextView is made VIEW.GONE and the RecyclerView is shown.
     * @param show - Show the RecyclerView or not
     */
    void showRecyclerView(boolean show) {

        if (show) {
            mReviewRecyclerView.setVisibility(View.VISIBLE);
            mNoReviewTextView.setVisibility(View.GONE);
        } else {
            mNoReviewTextView.setText(getString(R.string.detail_review_no_review, mTitle.getText()));
            mNoReviewTextView.setVisibility(View.VISIBLE);
            mReviewRecyclerView.setVisibility(View.GONE);
        }

    }


    /**
     * GETTEXTANDBACKGROUNDCOLORSBASEDONTYPE - Returns the colors the text and background should be,
     * based on the type the movie is from.
     * @return - Pair<Integer, Integer> - Title and Background color for this type
     */
    private Pair<Integer, Integer> getTextAndBackgroundColorsBasedOnType() {

        int titleColor = ContextCompat.getColor(this, R.color.logo_orange);

        int backgroundColor = ContextCompat.getColor(this, R.color.header_popular_background) ;

        if (mViewType.equals(getString(R.string.pref_sort_popular))) {
            titleColor = ContextCompat.getColor(this, R.color.logo_orange);
            backgroundColor = ContextCompat.getColor(this, R.color.header_popular_background);
        } else if (mViewType.equals(getString(R.string.pref_sort_top_rated))) {
            titleColor = ContextCompat.getColor(this, R.color.logo_blue);
            backgroundColor = ContextCompat.getColor(this, R.color.header_top_rated_background);
        } else if (mViewType.equals(getString(R.string.pref_sort_favorite))) {
            titleColor = ContextCompat.getColor(this, R.color.logo_purple);
            backgroundColor = ContextCompat.getColor(this, R.color.header_favorite_background);
        }

        return new Pair<>(titleColor, backgroundColor);

    }


    /**
     * Called when a previously created loader is being reset, thus making its data unavailable.
     * The application should at this point remove any references it has to the Loader's data.
     * Since we don't store any of this cursor's data, there are no references we need to remove.
     * The swap is for the RView Cursor
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mReviewAdapter.swapCursor(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = new MenuInflater(this);
        menuInflater.inflate(R.menu.detail_menu, menu);

        return true;  // Successful
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();

        if (itemId == R.id.action_about) {
            Intent startAboutIntent = new Intent(this, AboutActivity.class);
            startActivity(startAboutIntent);
            return true;                     // We found it, stop looking
        } else if (itemId == android.R.id.home) {

            onBackPressed();

            return true;
        }

        return super.onOptionsItemSelected(item);  // Keep looking
    }


    @Override
    public void onBackPressed() {
        Intent detailResultIntent = new Intent();
        detailResultIntent.putExtra(DETAIL_ACTIVITY_RETURN, new long[] {mIDForMovie, mHeartState0or1});
        setResult(Activity.RESULT_OK, detailResultIntent);

        super.onBackPressed();
    }


    /**
     * SHOWREMOVEFAVORITEDIALOG - Will create an instance of the dialog fragment and then
     * show it.
     */
    public void showRemoveFavoriteDialog() {
        Log.v(TAG, "showRemoveFavoriteDialog: ");

        // Do not use Constructor, it is empty
        DialogFragment dialogFragment =  RemoveFavoriteDialogFragment.newInstance(mIDForMovie);
        dialogFragment.show(getSupportFragmentManager()
                , RemoveFavoriteDialogFragment.REMOVEFAVORITEDIALOG_TAG);
    }


    @Override
    public void onDialogAffirmativeClick(RemoveFavoriteDialogFragment dialogFragment) {
        Log.d(TAG, "onDialogAffirmativeClick() called with: dialogFragment = [" + dialogFragment + "]");
        mHeartState0or1 = HEART_FALSE;           // Set the Heart state to OFF
        mHeartCheckboxView.setChecked(false);    // Set Checkbox to visibly OFF
        mHeartCheckboxView.setEnabled(false);    // Disable Heart 'cause in some cases film won't be available to be refavorited
        HEART_DISABLED = true;
        sqlUpdateOrDelete();                     // Set DB state to reflect choice.  Sometimes movie will be removed from DB entirely
        dialogFragment.dismiss();
    }


    @Override
    public void onDialogNegativeClick(RemoveFavoriteDialogFragment dialogFragment) {
        Log.d(TAG, "onDialogNegativeClick() called with: dialogFragment = [" + dialogFragment + "]");

        mHeartState0or1 = HEART_TRUE;  // Reset Heart state, since we are not removing this film
        dialogFragment.dismiss();
    }

    /**
     * Holds state of the movie on this page in case Favorite is deleted and user still wants to
     * rotate screen.
     * https://stackoverflow.com/questions/10259776/how-to-save-state-with-onsaveinstancestate-and-onrestoreinstancestate-while-orie
     */
    private static  class MovieState implements Serializable {

        private static final String  MOVIESTATE = "app.com.vladimirjeune.popmovies.MovieState";

        private String title;
        private String imagePath;
        private String rating;
        private String runtime;
        private String release;
        private String synopsis;
        private int textColor;
        private int textBackgroundColor;


        public MovieState(
                String aTitle,
                String anImagePath,
                String aRating,
                String aRuntime,
                String aRelease,
                String aSynopsis,
                int aTextColor,
                int aTextBackgroundColor
        ) {
            title = aTitle;
            imagePath = anImagePath;
            rating = aRating;
            runtime = aRuntime;
            release = aRelease;
            synopsis = aSynopsis;
            textColor = aTextColor;
            textBackgroundColor = aTextBackgroundColor;
        }


        public String getTitle() {
            return title;
        }


        public void setTitle(String title) {
            this.title = title;
        }


        public String getRating() {
            return rating;
        }


        public void setRating(String rating) {
            this.rating = rating;
        }


        public String getRuntime() {
            return runtime;
        }


        public void setRuntime(String runtime) {
            this.runtime = runtime;
        }


        public String getRelease() {
            return release;
        }


        public void setRelease(String release) {
            this.release = release;
        }


        public String getSynopsis() {
            return synopsis;
        }


        public void setSynopsis(String synopsis) {
            this.synopsis = synopsis;
        }


        public String getImagePath() {
            return imagePath;
        }


        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        public int getTextColor() {
            return textColor;
        }

        public void setTextColor(int textColor) {
            this.textColor = textColor;
        }

        public int getTextBackgroundColor() {
            return textBackgroundColor;
        }

        public void setTextBackgroundColor(int textBackgroundColor) {
            this.textBackgroundColor = textBackgroundColor;
        }
    }


}
