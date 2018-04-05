package app.com.vladimirjeune.popmovies;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import app.com.vladimirjeune.popmovies.data.MovieContract.ReviewEntry;

/**
 * Created by vladimirjeune on 4/3/18.
 */

class ReviewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context mContext;
    private Cursor mCursor;


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
            ((ReviewViewHolder) holder).bindTo(position);
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


    class ReviewViewHolder extends RecyclerView.ViewHolder {

        Long mReviewId;
        TextView mAuthorTextView;
        TextView mContentTextView;
        Button mButton;

        public ReviewViewHolder(View itemView) {
            super(itemView);

            mAuthorTextView = itemView.findViewById(R.id.tv_review_author);
            mContentTextView = itemView.findViewById(R.id.tv_review_paragrah);
            mButton = itemView.findViewById(R.id.bt_review_more);

        }


        /**
         * BINDTO - A convenience method that will attach inputted data to child views.
         * @param position - Position in cursor of Review Data we are looking for
         */
        public void bindTo(int position) {

            if (mCursor.moveToPosition(position)) {
                int reviewIdIndex = mCursor.getColumnIndex(ReviewEntry.REVIEW_ID);
                int authorIndex = mCursor.getColumnIndex(ReviewEntry.AUTHOR);
                int contentIndex = mCursor.getColumnIndex(ReviewEntry.CONTENT);

                mReviewId = mCursor.getLong(reviewIdIndex);
                mAuthorTextView.setText(mCursor.getString(authorIndex));
                mContentTextView.setText(mCursor.getString(contentIndex));

                mButton.setTag(mReviewId);  // So, can find Review to expand, if necessary
            }

        }
    }

}
