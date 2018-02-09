package app.com.vladimirjeune.popmovies;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.net.URL;

import app.com.vladimirjeune.popmovies.data.MovieContract.MovieEntry;
import app.com.vladimirjeune.popmovies.utilities.NetworkUtils;

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

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

    private static final String TAG = DetailActivity.class.getSimpleName();

    private String mViewType;

    private Uri mUri;
    private TextView mTitle;
    private View mTitleBackgroundView;
    private TextView mSynopsisTextView;
    private TextView mReleaseTextView;
    private TextView mRatingTextView;
    private TextView mRuntimeTextView;
    private ImageView mOneSheetImageView;

    private TextView mSynopsisTitleTextView;
    private TextView mReleaseTitleTextView;
    private TextView mRatingTitleTextView;
    private TextView mRuntimeTitleTextView;


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
         * the image to arrive.  May be shown until image arrives of we error out.
         * @param placeHolderDrawable - Image to show in this case
         */
        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            mOneSheetImageView.setImageDrawable(placeHolderDrawable);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent movieIntent = getIntent();

        mUri = movieIntent.getData();

        if (mUri == null) {
            throw new NullPointerException("Uri passed to DetailActivity cannot be null");
        }

        mViewType =  movieIntent.getStringExtra(MainActivity.EXTRA_TYPE);

        mTitle = findViewById(R.id.textViewTitle);
        mTitleBackgroundView = findViewById(R.id.textViewTitleBackground);

        // Data
        mSynopsisTextView = findViewById(R.id.textViewSynopsis);
        mReleaseTextView = findViewById(R.id.textViewRelease);
        mRatingTextView = findViewById(R.id.textViewRating);
        mRuntimeTextView = findViewById(R.id.textViewRuntime);
        mOneSheetImageView = findViewById(R.id.imageViewOnesheet);

        // Titles
        mSynopsisTitleTextView = findViewById(R.id.textViewSynopsisTitle);
        mReleaseTitleTextView = findViewById(R.id.textViewReleaseTitle);
        mRatingTitleTextView = findViewById(R.id.textViewRatingTitle);
        mRuntimeTitleTextView = findViewById(R.id.textViewRuntimeTitle);


        getSupportLoaderManager().initLoader(DETAIL_LOADER_ID, null, this);

    }

    /**
     * ONCREATELOADER - Makes and returns a CursoLoader that loads the data for our URI and stores it in a Cursor.
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
        }

        if ( ! cursorHasValidData) {  // Nothing to do here
            return;
        }


        // Image
        String posterPath = data.getString(DETAIL_INDEX_POSTER_PATH);
        URL imageURL = NetworkUtils.buildURLForImage(posterPath);

        Picasso.with(this)
                .load(String.valueOf(imageURL))
                .placeholder(R.drawable.tmd_placeholder_poster)
                .error(R.drawable.tmd_error_poster)
                .into(mTarget);

        // Foreground and Background color for Section Titles
        Pair<Integer, Integer> titleTextAndBackgroundColor = getTextAndBackgroundColorsBasedOnType();
        int textTypeColor = titleTextAndBackgroundColor.first;
        int backgroundColor = titleTextAndBackgroundColor.second;

        // Title & set ImageView ContentDescription
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
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

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
        }

        return super.onOptionsItemSelected(item);  // Keep looking
    }
}
