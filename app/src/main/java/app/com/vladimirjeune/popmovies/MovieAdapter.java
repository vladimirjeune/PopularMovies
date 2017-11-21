package app.com.vladimirjeune.popmovies;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import app.com.vladimirjeune.popmovies.utilities.NetworkUtils;

/**
 * Adapter for the Grid View of poster for the user to select from.
 * Created by vladimirjeune on 11/7/17.
 */

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.PosterViewHolder> {

    private static final String TAG = MovieAdapter.class.getSimpleName();

    private Context mContext;

    private int mNumberOfItems;

    // Will change to something else later, now just for testing
    private MovieData[] mMovieData;

    /**
     * MOVIEADAPTER - CONSTRUCTOR
     * @param numberOfItems - Number of posters that will ultimately be displayed to the user
     */
    public MovieAdapter(Context context, int numberOfItems) {
        mContext = context;
        mNumberOfItems = numberOfItems;

        populateMovieArrayWithDummyData();
    }

    /**
     * POPULATEMOVIEARRAYWITHDUMMYDATA - Populates the Movie Array field. Done so placeholder images
     * appear faster while images load from the internet.
     */
    private void populateMovieArrayWithDummyData() {
        mMovieData = new MovieData[mNumberOfItems];
        for (int i = 0; i < mNumberOfItems; i++) {
            mMovieData[i] = new MovieData();
        }
    }

    @Override
    public MovieAdapter.PosterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutForListItem = R.layout.movies_grid_item;
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        boolean attachToParentNow = false;

        View vhView = layoutInflater.inflate(layoutForListItem, parent, attachToParentNow);

        return new PosterViewHolder(vhView);
    }

    @Override
    public void onBindViewHolder(MovieAdapter.PosterViewHolder holder, int position) {
        Log.d(TAG, "BEGIN::onBindViewHolder: PosterViewHolder:" + holder + " Position:" + position + " mMovieData:" + ((mMovieData != null) ? mMovieData[position].getPosterPath() : null) + "\n");

        if (mMovieData != null) {
            holder.bindTo(mMovieData[position]);
        }

        Log.d(TAG, "END::onBindViewHolder: PosterViewHolder:" + holder + " Position:" + position + " mPosterData:" + ((mMovieData != null) ? mMovieData[position].getPosterPath() : null) + "\n");
    }

    /**
     * GETITEMCOUNT - Return the number of elements to ultimately show in the RecyclerView
     * @return - The number of items
     */
    @Override
    public int getItemCount() {
        return mNumberOfItems;
    }

    /**
     * Cache of the child views of a list item
     */
    class PosterViewHolder extends RecyclerView.ViewHolder {

        // Will hold the poster for this item.
        ImageView listItemPosterView;

        /**
         * POSTERVIEWHOLDER - Constructor gets a reference to our textViews holding children.
         * @param itemView - View inflated in onCreateViewHolder
         */
        public PosterViewHolder(View itemView) {
            super(itemView);
            listItemPosterView = itemView.findViewById(R.id.iv_movie_poster_item);
        }

        /**
         * BINDTO - A convenience method that will attach inputted data to child views.
         * @param aMovieData - Single Movie Data so we can access poster path if available
         */
        public void bindTo(MovieData aMovieData) {

            boolean isMovieEmptyOrNull = ((aMovieData.getMovieId() == 0)
                    && (null == aMovieData.getOriginalTitle()));

            // If there is no movie, use placeholder and leave
            if ((null == aMovieData) || (isMovieEmptyOrNull)){
                listItemPosterView.setImageDrawable(ContextCompat
                        .getDrawable(mContext, R.drawable.tmd_placeholder_poster));
                return;
            }

            // Posterpath will not be null, because handled earlier
            String posterPath = aMovieData.getPosterPath();
            if (!(posterPath.equals(""))) {
                String urlForPosterPath = NetworkUtils.buildURLForImage(posterPath)
                        .toString();

                Picasso.with(mContext)
                        .load(urlForPosterPath)
                        .placeholder(R.drawable.tmd_placeholder_poster)
                        .placeholder(R.drawable.tmd_error_poster)
                        .into(listItemPosterView);
            }
        }

    }

    /**
     * SETPOSTERDATA - Takes in a list of MovieData whose posters will be shown in the
     * GridView.
     * @param movieDatas - Array of Movies
     */
    public void setMoviesData(MovieData[] movieDatas) {
            Log.d(TAG, "BEGIN::setMoviesData: " + movieDatas);

        if (movieDatas != null){
            mMovieData = movieDatas;
            notifyDataSetChanged();
        }
            Log.d(TAG, "END::setMoviesData: " + mMovieData);
    }
}
