package app.com.vladimirjeune.popmovies;

import android.content.Context;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.net.URL;

import app.com.vladimirjeune.popmovies.data.MovieContract;
import app.com.vladimirjeune.popmovies.data.MovieContract.ReviewEntry;
import app.com.vladimirjeune.popmovies.utilities.NetworkUtils;

/**
 * Created by vladimirjeune on 4/3/18.
 */

class ReviewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final String TAG = getClass().getSimpleName();

    private final Context mContext;
    private Cursor mCursor;
    private String mBackdropPath;


    public ReviewAdapter(Context context) {
        mContext = context;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        int layoutForListItem = R.layout.reviews_grid_item;
        boolean attachToRoot = false;
        View viewHolderView = layoutInflater.inflate(layoutForListItem, parent, attachToRoot);

        return new ReviewViewHolder(viewHolderView);

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (mCursor != null) {
            ((ReviewViewHolder) holder).bindTo(position, mBackdropPath);
        }

    }


    /**
     * GETITEMCOUNT - How many items are in this adapter
     * @return - Number of items we're dealing with currently.
     */
    @Override
    public int getItemCount() {
        if (null == mCursor) {
            return 0;
        }

        return mCursor.getCount();

    }


    /**
     * SWAPCURSOR - Swaps the Cursor currently being used by the Adapter for data.
     * Will be called by DetailActivity after a load has finished.  Or, when Loader
     * for this data is reset.  When this is called there most likely has been all new
     * data.  So we notify the recyclerView of the change so it can update.
     * @param newCursor - Cursor with new data or null.
     */
    void swapCursor(Cursor newCursor) {

        mCursor = newCursor;



        notifyDataSetChanged();

    }


    /**
     * SETBACKDROPPATH - Sets backdrop for the individual reviews.  Since it's the same for each one.
     * @param aBackdropPath - Path to image online
     */
    void setBackdropPath(String aBackdropPath) {
        if (aBackdropPath != null) {
            mBackdropPath = aBackdropPath;
        }
    }


    class ReviewViewHolder extends RecyclerView.ViewHolder {

        Long mReviewId;
        TextView mAuthorTextView;
        TextView mContentTextView;
        ImageView mBackDropImageView;
        Button mButton;

        public ReviewViewHolder(View itemView) {
            super(itemView);

            mAuthorTextView = itemView.findViewById(R.id.tv_review_author);
            mContentTextView = itemView.findViewById(R.id.tv_review_paragrah);
            mBackDropImageView = itemView.findViewById(R.id.iv_review_backdrop);
            mButton = itemView.findViewById(R.id.bt_review_more);

//            fillBackgroundImageView();

//            mBackDropImageView.setColorFilter(Integer.valueOf(0x682700), PorterDuff.Mode.MULTIPLY);
            mBackDropImageView.setColorFilter(ContextCompat.getColor(mContext, R.color.orange_dark_opaque), PorterDuff.Mode.MULTIPLY);


        }


        /**
         * FILLBACKGROUNDIMAGEVIEW - Fill the background of review
         * @param aBackdropPath - Path to image backdrop
         */
        private void fillBackgroundImageView(String aBackdropPath) {

            if (aBackdropPath != null) {
                URL imageURL = NetworkUtils.buildURLForImageOfSize(aBackdropPath, NetworkUtils.TMDB_IMAGE_W500);

                // TODO: Remove
                Picasso.with(mContext)
                        .load(String.valueOf(imageURL))
                        .placeholder(R.drawable.tmd_placeholder_poster)
                        .error(R.drawable.tmd_error_poster)
                        .into(mBackDropImageView, new Callback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "onSuccess() called");
                            }

                            @Override
                            public void onError() {
                                Log.d(TAG, "onError() called");
                            }
                        });


            }
        }


        /**
         * BINDTO - A convenience method that will attach inputted data to child views.
         * @param position - Position in cursor of Review Data we are looking for
         */
        public void bindTo(int position, String aBackdropPath) {

            if (mCursor.moveToPosition(position)) {
                int reviewIdIndex = mCursor.getColumnIndex(ReviewEntry.REVIEW_ID);
                int authorIndex = mCursor.getColumnIndex(ReviewEntry.AUTHOR);
                int contentIndex = mCursor.getColumnIndex(ReviewEntry.CONTENT);
                int backdropIndex = mCursor.getColumnIndex(MovieContract.MovieEntry.BACKDROP_PATH);

                mReviewId = mCursor.getLong(reviewIdIndex);
                mAuthorTextView.setText(mCursor.getString(authorIndex));
                mContentTextView.setText(mCursor.getString(contentIndex));
                String backdropPath = mCursor.getString(backdropIndex);
//                loadBackdrop();

                if (aBackdropPath != null) {
                    fillBackgroundImageView(aBackdropPath);
                }

                mButton.setTag(mReviewId);  // So, can find Review to expand, if necessary
            }

        }
    }

}
