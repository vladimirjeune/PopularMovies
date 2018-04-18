package app.com.vladimirjeune.popmovies;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeThumbnailLoader;
import com.google.android.youtube.player.YouTubeThumbnailView;

import app.com.vladimirjeune.popmovies.data.MovieContract.YoutubeEntry;
import app.com.vladimirjeune.popmovies.utilities.NetworkUtils;

/**
 * Created by vladimirjeune on 4/17/18.
 */

public class MediaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final String TAG = MediaViewHolder.class.getSimpleName();

    private final Context mContext;
    private Cursor mCursor;

    public MediaAdapter(Context context) {
        mContext = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        int layoutForListItem = R.layout.media_grid_item;
        boolean attachRoot = false;
        View vh_view = layoutInflater.inflate(layoutForListItem, parent, attachRoot);

        return new MediaViewHolder(vh_view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (mCursor != null) {
            ((MediaViewHolder) holder).bindTo(position);
        }
    }

    @Override
    public int getItemCount() {

        if (mCursor == null) {

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


    class MediaViewHolder extends RecyclerView.ViewHolder {

        String mYoutubeId;
        String mSiteKey;
        YouTubeThumbnailView mThumbnailImageView;
        TextView mNameTextView;
        TextView mTypeTextView;

        public MediaViewHolder(View itemView) {
            super(itemView);

            mThumbnailImageView = itemView.findViewById(R.id.ytv_media_thumbnail);
            mNameTextView = itemView.findViewById(R.id.tv_media_name);
            mTypeTextView = itemView.findViewById(R.id.tv_media_type);

        }


        /**
         * BINDTO - A convenience method that will attach inputted data to child views.
         * @param position - Position in cursor of Youtube Data we are looking for
         * Help from: http://www.androhub.com/how-to-show-youtube-video-thumbnail-play-youtube-video-in-android-app/
         */
        public void bindTo(int position) {

            final String youtubeAccessKey = NetworkUtils.obtainKeyOfType(mContext, NetworkUtils.YOUTUBE_DATA_V3_KEY);

            if (mCursor.moveToPosition(position)) {

                int youtubeIdIndex = mCursor.getColumnIndex(YoutubeEntry.YOUTUBE_ID);
                int keyIndex = mCursor.getColumnIndex(YoutubeEntry.KEY);
                int nameIndex = mCursor.getColumnIndex(YoutubeEntry.NAME);
                int typeIndex = mCursor.getColumnIndex(YoutubeEntry.TYPE);

                mYoutubeId = mCursor.getString(youtubeIdIndex);
                mSiteKey = mCursor.getString(keyIndex);

                mNameTextView.setText(mCursor.getString(nameIndex));
                mTypeTextView.setText(mCursor.getString(typeIndex));

                // You have to initialize a Thumbnail, not set it
                mThumbnailImageView.initialize(youtubeAccessKey, new YouTubeThumbnailView.OnInitializedListener() {
                    @Override
                    public void onInitializationSuccess(YouTubeThumbnailView youTubeThumbnailView
                            , final YouTubeThumbnailLoader youTubeThumbnailLoader) {

                        // SUCCESS: Will get video using the odd set of letters at end of youtube urls
                        youTubeThumbnailLoader.setVideo(mSiteKey);

                        youTubeThumbnailLoader.setOnThumbnailLoadedListener(new YouTubeThumbnailLoader.OnThumbnailLoadedListener() {
                            @Override
                            public void onThumbnailLoaded(YouTubeThumbnailView youTubeThumbnailView, String s) {

                                // Loader is done; release it.
                                youTubeThumbnailLoader.release();
                            }

                            @Override
                            public void onThumbnailError(YouTubeThumbnailView youTubeThumbnailView, YouTubeThumbnailLoader.ErrorReason errorReason) {
                                Log.e(TAG, "onThumbnailError: There was an error loading the Thumbnail for " + mSiteKey);
                            }
                        });


                    }

                    @Override
                    public void onInitializationFailure(YouTubeThumbnailView youTubeThumbnailView
                            , YouTubeInitializationResult youTubeInitializationResult) {

                        Log.e(TAG, "onInitializationFailure: This key failed to initialize " + mSiteKey);

                    }
                });

            }

        }


    }


}
