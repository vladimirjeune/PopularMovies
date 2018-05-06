package app.com.vladimirjeune.popmovies;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
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
import app.com.vladimirjeune.popmovies.data.MovieContract.JoinYoutubeEntry;
import app.com.vladimirjeune.popmovies.data.MovieContract.MovieEntry;
import app.com.vladimirjeune.popmovies.data.MovieContract.ReviewEntry;
import app.com.vladimirjeune.popmovies.data.MovieContract.YoutubeEntry;
import app.com.vladimirjeune.popmovies.databinding.ActivityDetailBinding;
import app.com.vladimirjeune.popmovies.utilities.NetworkUtils;

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        RemoveFavoriteDialogFragment.RemoveFavoriteListener {

    ActivityDetailBinding mActivityDetailBinding;

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
    private static final int MEDIA_AQT_CALLBACK = -17;
    private Integer mHeartState0or1;
    private Integer mInitialHeartState;
    private long mIDForMovie;

    private String mViewType;

    private Uri mUri;
    private TextView mTitle;
    private View mTitleBackgroundView;
    private TextView mSynopsisTextView;
    private TextView mReleaseTextView;
//    private TextView mRatingTextView;
    private TextView mRuntimeTextView;
    private ImageView mOneSheetImageView;
    private ImageView mBackdropImageView;
    private CheckBox mHeartCheckboxView;

    private TextView mSynopsisTitleTextView;
    private TextView mMediaTitleTextView;
    private TextView mReviewsTitleTextView;
    private TextView mReleaseTitleTextView;
    private TextView mRatingTitleTextView;
    private TextView mRuntimeTitleTextView;

    private RecyclerView mReviewRecyclerView;
    private RecyclerView.LayoutManager mReviewLayoutManager;
    private ReviewAdapter mReviewAdapter;

    private RecyclerView mMediaRecyclerView;
    private RecyclerView.LayoutManager mMediaLayoutManager;
    private MediaAdapter mMediaAdapter;

    private boolean HEART_DISABLED;
    private String mPosterPath;

    private int mReviewPosition = RecyclerView.NO_POSITION;
    private TextView mNoReviewTextView;

    private int mMediaPosition = RecyclerView.NO_POSITION;
    private TextView mNoMediaTextView;


    private final Target mTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
//            mOneSheetImageView.setImageBitmap(bitmap);
            mActivityDetailBinding.imageViewOnesheet.setImageBitmap(bitmap);

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
                        mActivityDetailBinding.textViewTitleBackground.setBackgroundColor(vibrantColor);
                    } else if (darkVibrantColor != blackColor) {
                        mActivityDetailBinding.textViewTitleBackground.setBackgroundColor(darkVibrantColor);
                    } else if (lightVibrantColor != blackColor) {
                        mActivityDetailBinding.textViewTitleBackground.setBackgroundColor(lightVibrantColor);
                    } else if (darkMutedColor != blackColor) {
                        mActivityDetailBinding.textViewTitleBackground.setBackgroundColor(darkMutedColor);
                    } else if (lightMutedColor != blackColor) {
                        mActivityDetailBinding.textViewTitleBackground.setBackgroundColor(lightMutedColor);
                    } else if (mutedColor != blackColor) {
                        mActivityDetailBinding.textViewTitleBackground.setBackgroundColor(mutedColor);
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
//            mOneSheetImageView.setImageDrawable(errorDrawable);
            mActivityDetailBinding.imageViewOnesheet.setImageDrawable(errorDrawable);
        }

        /**
         * ONPREPARELOAD - Shown if placeholder called and we are waiting for
         * the image to arrive.  May be shown until image arrives or we error out.
         * @param placeHolderDrawable - Image to show in this case
         */
        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
//            mOneSheetImageView.setImageDrawable(placeHolderDrawable);
            mActivityDetailBinding.imageViewOnesheet.setImageDrawable(placeHolderDrawable);
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

        mActivityDetailBinding = DataBindingUtil.setContentView(this, R.layout.activity_detail);
//        setContentView(R.layout.activity_detail);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);  // May be needed for Result

        Intent movieIntent = getIntent();

        mUri = movieIntent.getData();

        if (mUri == null) {
            throw new NullPointerException("Uri passed to DetailActivity cannot be null");
        }

        mViewType =  movieIntent.getStringExtra(MainActivity.EXTRA_TYPE);

//        mTitle = findViewById(R.id.textViewTitle);
        mHeartState0or1 = HEART_FALSE;
//        mTitleBackgroundView = findViewById(R.id.textViewTitleBackground);

        // Data
//        mSynopsisTextView = findViewById(R.id.textViewSynopsis);
//        mReleaseTextView = findViewById(R.id.textViewRelease);
//        mRatingTextView = findViewById(R.id.textViewRating);
//        mRuntimeTextView = findViewById(R.id.textViewRuntime);
//        mOneSheetImageView = findViewById(R.id.imageViewOnesheet);
//        mHeartCheckboxView = findViewById(R.id.checkbox_favorite);

        // Titles
//        mSynopsisTitleTextView = findViewById(R.id.textViewSynopsisTitle);
//        mReleaseTitleTextView = findViewById(R.id.textViewReleaseTitle);
//        mRatingTitleTextView = findViewById(R.id.textViewRatingTitle);
//        mRuntimeTitleTextView = findViewById(R.id.textViewRuntimeTitle);
//        mReviewsTitleTextView = findViewById(R.id.textViewReviewsTitle);
//        mMediaTitleTextView = findViewById(R.id.textViewMediaTitle);

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
//            mHeartCheckboxView.setEnabled(HEART_DISABLED);  // May be too late, here
            mActivityDetailBinding.checkboxFavorite.setEnabled(HEART_DISABLED);  // May be too late, here
            Log.i(TAG, "onCreate: CHECKBOX IS: [HD=T] " + ((HEART_DISABLED) ? "DISABLED" : "ENABLED" ));
        }


        long tmpID = Long.parseLong(mUri.getLastPathSegment());  // General

        // Text for when there are no reviews
//        mNoReviewTextView = findViewById(R.id.tv_review_empty);

//        mReviewRecyclerView = findViewById(R.id.rv_horizontal_linear_reviews);
        mActivityDetailBinding.rvHorizontalLinearReviews.addItemDecoration(new SpaceDecoration(this));  // Use this instead of padding.
        mActivityDetailBinding.rvHorizontalLinearReviews.setHasFixedSize(true);

        boolean reverseLayout = false;
        mReviewLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, reverseLayout);
        mActivityDetailBinding.rvHorizontalLinearReviews.setLayoutManager(mReviewLayoutManager);

        mReviewAdapter = new ReviewAdapter(this, mViewType);
        mActivityDetailBinding.rvHorizontalLinearReviews.setAdapter(mReviewAdapter);

        setReviewRecyclerViewForID(tmpID);

        // Media RecyclerView
//        mNoMediaTextView = findViewById(R.id.tv_media_empty);

//        mMediaRecyclerView = findViewById(R.id.rv_horizontal_linear_media);  // Get view from XML
        mActivityDetailBinding.rvHorizontalLinearMedia.addItemDecoration(new SpaceDecoration(this));  // Use instead of padding
        mActivityDetailBinding.rvHorizontalLinearMedia.setHasFixedSize(true);

        // Set LayoutManager
        boolean reverseMediaLayout = false;
        mMediaLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, reverseMediaLayout);
        mActivityDetailBinding.rvHorizontalLinearMedia.setLayoutManager(mMediaLayoutManager);

        // Set Adapter
        mMediaAdapter = new MediaAdapter(this);
        mActivityDetailBinding.rvHorizontalLinearMedia.setAdapter(mMediaAdapter);

        setMediaRecyclerViewForID(tmpID);


        setRecyclerViewBackgroundByType();  // General

    }


    /**
     * SETRECYCLERVIEWBACKGROUNDBYTYPE - Sets the backgrounds for the Review and Media RecyclerViews
     * so they have the correct background for this particular Movies current viewType
     */
    private void setRecyclerViewBackgroundByType() {

        if (mViewType.equals(getString(R.string.pref_sort_popular))) {
            mActivityDetailBinding.rvHorizontalLinearReviews
                    .setBackground(ContextCompat.getDrawable(this, R.drawable.rv_orange_background_gradient));
            mActivityDetailBinding.rvHorizontalLinearMedia
                    .setBackground(ContextCompat.getDrawable(this, R.drawable.rv_orange_background_gradient));
        } else if (mViewType.equals(getString(R.string.pref_sort_top_rated))) {
            mActivityDetailBinding.rvHorizontalLinearReviews
                    .setBackground(ContextCompat.getDrawable(this, R.drawable.rv_blue_background_gradient));
            mActivityDetailBinding.rvHorizontalLinearMedia
                    .setBackground(ContextCompat.getDrawable(this, R.drawable.rv_blue_background_gradient));
        } else if (mViewType.equals(getString(R.string.pref_sort_favorite))) {
            mActivityDetailBinding.rvHorizontalLinearReviews
                    .setBackground(ContextCompat.getDrawable(this, R.drawable.rv_purple_background_gradient));
            mActivityDetailBinding.rvHorizontalLinearMedia
                    .setBackground(ContextCompat.getDrawable(this, R.drawable.rv_purple_background_gradient));
        }
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
//                    mTitle.getText().toString(),
                    mActivityDetailBinding.textViewTitle.getText().toString(),
                    mPosterPath,
//                    mRatingTextView.getText().toString(),
                    mActivityDetailBinding.textViewRating.getText().toString(),
//                    mRuntimeTextView.getText().toString(),
                    mActivityDetailBinding.textViewRuntime.getText().toString(),
//                    mReleaseTextView.getText().toString(),
                    mActivityDetailBinding.textViewRelease.getText().toString(),
//                    mSynopsisTextView.getText().toString(),
                    mActivityDetailBinding.textViewSynopsis.getText().toString(),
                    ContextCompat.getColor(this, R.color.logo_purple),  // Favorite color
                    ContextCompat.getColor(this, R.color.header_favorite_background),  // Favorite color
                    R.drawable.rv_purple_background_gradient
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

//                mTitle.setText(ms.getTitle());
                mActivityDetailBinding.textViewTitle.setText(ms.getTitle());
                mPosterPath = ms.getImagePath();
//                mRatingTextView.setText(ms.getRating());
                mActivityDetailBinding.textViewRating.setText(ms.getRating());
//                mRuntimeTextView.setText(ms.getRuntime());
                mActivityDetailBinding.textViewRuntime.setText(ms.getRuntime());
//                mReleaseTextView.setText(ms.getRelease());
                mActivityDetailBinding.textViewRelease.setText(ms.getRelease());
//                mSynopsisTextView.setText(ms.getSynopsis());
                mActivityDetailBinding.textViewSynopsis.setText(ms.getSynopsis());

                // Text Colors for Favorites
                mActivityDetailBinding.textViewRatingTitle.setTextColor(ms.getTextColor());
                mActivityDetailBinding.textViewRuntimeTitle.setTextColor(ms.getTextColor());
                mActivityDetailBinding.textViewReleaseTitle.setTextColor(ms.getTextColor());
                mActivityDetailBinding.textViewSynopsisTitle.setTextColor(ms.getTextColor());
                mActivityDetailBinding.textViewReviewsTitle.setTextColor(ms.getTextColor());

                // Text Background Colors for Favorites
                mActivityDetailBinding.textViewRatingTitle.setBackgroundColor(ms.getTextBackgroundColor());
                mActivityDetailBinding.textViewRuntimeTitle.setBackgroundColor(ms.getTextBackgroundColor());
                mActivityDetailBinding.textViewReleaseTitle.setBackgroundColor(ms.getTextBackgroundColor());
                mActivityDetailBinding.textViewSynopsisTitle.setBackgroundColor(ms.getTextBackgroundColor());
                mActivityDetailBinding.textViewReviewsTitle.setBackgroundColor(ms.getTextBackgroundColor());

                // Background Drawables for Favorites
                mActivityDetailBinding.rvHorizontalLinearReviews.setBackground(ContextCompat
                        .getDrawable(this, ms.getDrawableID()));  // TODO: Check that rotation works in Favorites

            }

            loadMovieImage();

            HEART_DISABLED = savedInstanceState.getBoolean(HEART_DISABLED_KEY);
//            mHeartCheckboxView.setEnabled(! HEART_DISABLED);  //
            mActivityDetailBinding.checkboxFavorite.setEnabled(! HEART_DISABLED);

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

        // Foreground and Background color for Section Titles
        Pair<Integer, Integer> titleTextAndBackgroundColor = getTextAndBackgroundColorsBasedOnType();
        int textTypeColor = titleTextAndBackgroundColor.first;
        int backgroundColor = titleTextAndBackgroundColor.second;

        // Image
        mPosterPath = data.getString(DETAIL_INDEX_POSTER_PATH);
        loadMovieImage();

        // Title, ID & set ImageView ContentDescription
        mIDForMovie = data.getLong(DETAIL_INDEX_ID);
        String originalTitle = data.getString(DETAIL_INDEX_ORIGINAL_TITLE);
        String a11yPosterText = getString(R.string.a11y_poster, originalTitle);
//        mTitle.setText(originalTitle);
        mActivityDetailBinding.textViewTitle.setText(originalTitle);
//        mOneSheetImageView.setContentDescription(a11yPosterText);
        mActivityDetailBinding.imageViewOnesheet.setContentDescription(a11yPosterText);

        // Rating
//        mRatingTextView.setText(data.getString(DETAIL_INDEX_VOTER_AVERAGE));
        mActivityDetailBinding.textViewRating.setText(data.getString(DETAIL_INDEX_VOTER_AVERAGE));
        mActivityDetailBinding.textViewRatingTitle.setTextColor(textTypeColor);
        mActivityDetailBinding.textViewRatingTitle.setBackgroundColor(backgroundColor);

        // Preparing Runtime
        String fullRuntime = String.format("%s min", data.getString(DETAIL_INDEX_RUNTIME));  // 141 min
//        mRuntimeTextView.setText(fullRuntime);
        mActivityDetailBinding.textViewRuntime.setText(fullRuntime);
        mActivityDetailBinding.textViewRuntimeTitle.setTextColor(textTypeColor);
        mActivityDetailBinding.textViewRuntimeTitle.setBackgroundColor(backgroundColor);

        // Preparing Release Year
        String releaseDate = data.getString(DETAIL_INDEX_RELEASE_DATE);  // 2017-12-13
        String[] releaseDataParts = releaseDate.split("-");  // 0:[2017] 1:[12] 2:[13]
        String releaseYear = releaseDataParts[0];  // 2017
//        mReleaseTextView.setText(releaseYear);
        mActivityDetailBinding.textViewRelease.setText(releaseYear);

        mActivityDetailBinding.textViewReleaseTitle.setTextColor(textTypeColor);
        mActivityDetailBinding.textViewReleaseTitle.setBackgroundColor(backgroundColor);

//        mSynopsisTextView.setText(data.getString(DETAIL_INDEX_SYNOPSIS));
        mActivityDetailBinding.textViewSynopsis.setText(data.getString(DETAIL_INDEX_SYNOPSIS));
        mActivityDetailBinding.textViewSynopsisTitle.setTextColor(textTypeColor);
        mActivityDetailBinding.textViewSynopsisTitle.setBackgroundColor(backgroundColor);

        // Reviews
        mActivityDetailBinding.textViewReviewsTitle.setTextColor(textTypeColor);
        mActivityDetailBinding.textViewReviewsTitle.setBackgroundColor(backgroundColor);

        // Media
        mActivityDetailBinding.textViewMediaTitle.setTextColor(textTypeColor);
        mActivityDetailBinding.textViewMediaTitle.setBackgroundColor(backgroundColor);

        // Stuff with Checkbox
        mHeartState0or1 = data.getInt(DETAIL_INDEX_FAVORITE_FLAG);
        if (mInitialHeartState == null) {  // Only 1st time thru
            mInitialHeartState = mHeartState0or1.intValue();  // State when initially loaded.  Needed nonreference
        }
//        mHeartCheckboxView.setChecked(mHeartState0or1.equals(HEART_TRUE));
        mActivityDetailBinding.checkboxFavorite.setChecked(mHeartState0or1.equals(HEART_TRUE));
        ((CheckBox) mActivityDetailBinding.checkboxFavorite).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mActivityDetailBinding.checkboxFavorite.isChecked()) {
                    mHeartState0or1 = HEART_TRUE;
                    sqlUpdateOrDelete();
                } else {
                    if (!(mViewType.equals(getString(R.string.pref_sort_favorite)))) {
                        mHeartState0or1 = HEART_FALSE;
                        sqlUpdateOrDelete();
                    } else {
                        mActivityDetailBinding.checkboxFavorite.setChecked(true);  // Will have to set to false later, if necessary
                        showRemoveFavoriteDialog();
                    }
                }
            }
        });

        // Cannot set This text in onCreate since no Title has been created at that point
        setNoReviewText();
        setNoMediaText();

//        data.close();  // Closing cursor here caused crash on rotation

    }

    /**
     * SETRECYCLERVIEWFORID - Uses passed in ID to obtain Reviews for this title, if any.
     * If there are reviews, they are shown in a RecyclerView.  Otherwise, a note saying
     * that there are no reviews is shown instead.
     * @param movieId - ID for Movie whose reviews we want to see
     */
    private void setReviewRecyclerViewForID(long movieId) {
        ReviewQueryHandler reviewQueryHandler = new ReviewQueryHandler(getContentResolver(), this);
        reviewsForMovie(reviewQueryHandler, movieId);
    }

    /**
     * SETMEDIARECYCLERVIEWFORID - Uses passed in ID to obtain Media for this title, if any.
     * If there are media, they are shown in a RecyclerView.  Otherwise, a note saying
     * that there are no media is shown instead.
     * @param movieId - ID for Movie whose media we want to see
     */
    private void setMediaRecyclerViewForID(long movieId) {
        MediaQueryHandler mediaQueryHandler = new MediaQueryHandler(getContentResolver(), this);
        mediaForMovie(mediaQueryHandler, movieId);
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
                    if (! cursor.isClosed()) {
                        cursor.close();
                    }
                    detailActivity.showRecyclerView(false);
                }

            }
        }
    }

    /**
     * MEDIAQUERYHANDLER - Static class needed to not leak memory while accessing the
     * the database using AsyncQueryHandler task.
     */
    private static class MediaQueryHandler extends AsyncQueryHandler {
    // https://stackoverflow.com/questions/37188519/this-handler-class-should
    // -be-static-or-leaks-might-occurasyncqueryhandler
        private final WeakReference<DetailActivity> mActivity;

        // DO NOT USE
        public MediaQueryHandler(ContentResolver cr) {
            super(cr);
            mActivity = null;
        }

        public MediaQueryHandler(ContentResolver cr, DetailActivity da) {
            super(cr);
            mActivity = new WeakReference<DetailActivity>(da);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {

            if (mActivity != null) {
                DetailActivity detailActivity = mActivity.get();  // Getting the Activity from the WeakReference
                MediaAdapter mediaAdapter = detailActivity.getMediaAdapter();

                if (cursor == null) {
                    detailActivity.showMediaRecyclerView(false);
                    return;
                }

                if ((cursor.moveToFirst())
                        && (mediaAdapter != null)) {
                    mediaAdapter.swapCursor(cursor);  // May have 2 send ViewType in later if send BG for ImageView
                    detailActivity.showMediaRecyclerView(true);

                } else {
                    if (! cursor.isClosed()) {
                        cursor.close();  // We know it exists
                    }
                    detailActivity.showMediaRecyclerView(false);
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
     * GETMEDIAADAPTER - Returns the MediaAdapter
     * @return - MediaAdapter - Can be null
     */
    MediaAdapter getMediaAdapter() {
        return mMediaAdapter;
    }


    /**
     * REVIEWSFORMOVIE - Getting reviews, if any, for this movie id
     * Result sent to Callback for AsyncQueryTask
     */
    private void reviewsForMovie(ReviewQueryHandler queryHandler, long movieID) {
        Uri joinEntryUri = MovieContract.JoinEntry.CONTENT_URI;
        String[] projection = new String[] {
                ReviewEntry.REVIEW_ID,
                ReviewEntry.AUTHOR,
                ReviewEntry.CONTENT,
                MovieEntry.BACKDROP_PATH
        };
        String selection = ReviewEntry.MOVIE_ID + " = ? ";
        String[] selectionArgs = new String[] {""+movieID};
        String orderBy = ReviewEntry.REVIEW_ID;

        queryHandler.startQuery(
                REVIEW_AQT_CALLBACK,
                null,
                joinEntryUri,
                projection,
                selection,
                selectionArgs,
                orderBy
        );

    }

    /**
     * MEDIAFORMOVIE - Getting media, if any, for this movie id
     * Result sent to Callback for AsyncQueryTask
     */
    private void mediaForMovie(MediaQueryHandler queryHandler, long movieID) {
        Uri joinMediaEntryUri = JoinYoutubeEntry.CONTENT_URI;  // Not typo
        String[] projection = new String[] {
                YoutubeEntry.YOUTUBE_ID,
                YoutubeEntry.KEY,
                YoutubeEntry.NAME,
                YoutubeEntry.TYPE,
                MovieEntry.ORIGINAL_TITLE  // For debugging
        };
        String selection = YoutubeEntry.MOVIE_ID + " = ? ";
        String[] selectionArgs = new String[] {""+movieID};
        String orderBy = YoutubeEntry.YOUTUBE_ID;

        queryHandler.startQuery(
                MEDIA_AQT_CALLBACK,
                null,
                joinMediaEntryUri,
                projection,
                selection,
                selectionArgs,
                orderBy
        );

    }


    /**
     * SETREVIEWRECYCLERVIEWTOCORRECTPOSITION - Makes sure that the RecyclerView is at the start
     * when the user switches from one type of list to another.  Otherwise, we would be
     * in the same position as we were before the change, but in another list.
     */
    private void setReviewRecyclerViewToCorrectPosition() {
        if (RecyclerView.NO_POSITION == mReviewPosition) {
            mReviewPosition = 0;
        }
        mActivityDetailBinding.rvHorizontalLinearReviews.scrollToPosition(mReviewPosition);
    }


    /**
     * SETMEDIARECYCLERVIEWTOCORRECTPOSITION - Makes sure that the RecyclerView is at the start
     * when the user switches from one type of list to another.  Otherwise, we would be
     * in the same position as we were before the change, but in another list.
     */
    private void setMediaRecyclerViewToCorrectPosition() {
        if (RecyclerView.NO_POSITION == mMediaPosition) {
            mMediaPosition = 0;
        }
        mActivityDetailBinding.rvHorizontalLinearMedia.scrollToPosition(mMediaPosition);
    }


    /**
     * SHOWRECYCLERVIEW - RecyclerView starts out as VIEW.GONE because most movies won't have reviews.
     * So a TextView is shown stating that this movie has no reviews.  If there is data for Reviews
     * the TextView is made VIEW.GONE and the RecyclerView is shown.
     * @param show - Show the RecyclerView or not
     */
    void showRecyclerView(boolean show) {

        if (show) {
            mActivityDetailBinding.rvHorizontalLinearReviews.setVisibility(View.VISIBLE);
            mActivityDetailBinding.tvReviewEmpty.setVisibility(View.GONE);
        } else {
//            setNoReviewText();
            mActivityDetailBinding.tvReviewEmpty.setVisibility(View.VISIBLE);
            mActivityDetailBinding.rvHorizontalLinearReviews.setVisibility(View.GONE);
        }
    }

    /**
     * SHOWMEDIARECYCLERVIEW - RecyclerView starts out as VIEW.GONE because most movies won't have media.
     * So a TextView is shown stating that this movie has no media.  If there is data for Media
     * the TextView is made VIEW.GONE and the RecyclerView is shown.
     * @param show - Show the Media RecyclerView or not
     */
    void showMediaRecyclerView(boolean show) {

        if (show) {
            mActivityDetailBinding.rvHorizontalLinearMedia.setVisibility(View.VISIBLE);
            mActivityDetailBinding.tvMediaEmpty.setVisibility(View.GONE);
        } else {
            mActivityDetailBinding.tvMediaEmpty.setVisibility(View.VISIBLE);
            mActivityDetailBinding.rvHorizontalLinearMedia.setVisibility(View.GONE);
        }
    }


    /**
     * SETNOREVIEWTEXT - SETS TITLE FOR THE TEXTVIEW SHOWN WHEN THERE ARE NO REVIEWS FOR A TITLE.
     */
    private void setNoReviewText() {
//        mNoReviewTextView.setText(getString(R.string.detail_review_no_review, mTitle.getText()));  //
        mActivityDetailBinding.tvReviewEmpty.setText(getString(R.string.detail_review_no_review, mActivityDetailBinding.textViewTitle.getText()));

    }

    /**
     * SETNOMEDIATEXT - SETS TITLE FOR THE TEXTVIEW SHOWN WHEN THERE ARE NO MEDIA FOR A TITLE.
     */
    private void setNoMediaText() {
//        mNoMediaTextView.setText(getString(R.string.detail_media_no_media, mTitle.getText()));
        mActivityDetailBinding.tvMediaEmpty.setText(getString(R.string.detail_media_no_media, mActivityDetailBinding.textViewTitle.getText()));
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

        detailResultIntent.putExtra(DETAIL_ACTIVITY_RETURN, new long[] {mIDForMovie, didHeartChange()});
        setResult(Activity.RESULT_OK, detailResultIntent);

        super.onBackPressed();
    }


    /**
     * DIDHEARTCHANGE - Whether the State of the Heart Icon was visibly changed since Activity started.
     * @return - Long - 1 True / 0 False
     */
    private long didHeartChange() {
        final long changedTrue = 1L;
        final long changedFalse = 0L;
        return (mHeartState0or1.equals(mInitialHeartState)) ? changedFalse : changedTrue;
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
        mActivityDetailBinding.checkboxFavorite.setChecked(false);    // Set Checkbox to visibly OFF
        mActivityDetailBinding.checkboxFavorite.setEnabled(false);    // Disable Heart 'cause in some cases film won't be available to be refavorited
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
        private int drawableID;


        public MovieState(
                String aTitle,
                String anImagePath,
                String aRating,
                String aRuntime,
                String aRelease,
                String aSynopsis,
                int aTextColor,
                int aTextBackgroundColor,
                int aDrawableID
        ) {
            title = aTitle;
            imagePath = anImagePath;
            rating = aRating;
            runtime = aRuntime;
            release = aRelease;
            synopsis = aSynopsis;
            textColor = aTextColor;
            textBackgroundColor = aTextBackgroundColor;
            drawableID = aDrawableID;
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

        public int getDrawableID() { return drawableID; }

        public void setDrawableID(int drawableID) { this.drawableID = drawableID; }
    }


}
