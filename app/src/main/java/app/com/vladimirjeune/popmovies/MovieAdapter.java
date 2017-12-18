package app.com.vladimirjeune.popmovies;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import app.com.vladimirjeune.popmovies.utilities.NetworkUtils;

/**
 * Adapter for the Grid View of poster for the user to select from.
 * Created by vladimirjeune on 11/7/17.
 */

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.PosterViewHolder> {

    private static final String TAG = MovieAdapter.class.getSimpleName();

    private Context mContext;

    private int mNumberOfItems;

    private boolean mIsPopular;

    private ArrayList<Pair<Long, String>> mPosterAndIds;

    private SQLiteDatabase mDb;

    /**
     * MOVIEADAPTER - CONSTRUCTOR
     * @param numberOfItems - Number of posters that should ultimately be displayed to the user
     */
    public MovieAdapter(Context context, int numberOfItems) {
        mContext = context;
        mNumberOfItems = numberOfItems;
        mIsPopular = true;  // Defaulting to true, will change when data is set

        mPosterAndIds = new ArrayList<>(mNumberOfItems);
        populateMovieArrayWithDummyData();
    }

    /**
     * POPULATEMOVIEARRAYWITHDUMMYDATA - Populates the Movie Array field. Done so placeholder images
     * appear faster while images load from the internet.
     * Dummy Movies have ids of -1 and paths of "".
     */
    private void populateMovieArrayWithDummyData() {
        final Long fakeId = -1L;
        String fakePath = "";

        for (int i = 0; i < mNumberOfItems; i++) {
            mPosterAndIds.add(new Pair<>(fakeId, fakePath));
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

        if (mPosterAndIds != null) {
            holder.bindTo(mPosterAndIds.get(position));
        }

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
         * @param aPosterMovieData - Single Movie Data so we can access poster path and ID
         */
        private void bindTo(Pair<Long, String> aPosterMovieData) {  // TODO: Change to Pairs

            // If there is no actual movie, use placeholder and leave
            if (aPosterMovieData.first < 0){
                listItemPosterView.setImageDrawable(ContextCompat
                        .getDrawable(mContext, R.drawable.tmd_placeholder_poster));
                return;
            }

            String posterPath = aPosterMovieData.second;
            if (posterPath != null) {
                String urlForPosterPath = NetworkUtils.buildURLForImage(posterPath)
                        .toString();

                Picasso.with(mContext)
                        .load(urlForPosterPath)
                        .placeholder(R.drawable.tmd_placeholder_poster)
                        .placeholder(R.drawable.tmd_error_poster)
                        .into(listItemPosterView);

                listItemPosterView.setTag(aPosterMovieData.first);  // This is the ID of the movie
            }
        }

    }


    /**
     * SETDATA - Takes list of posters in order and whether this batch is popular
     * @param dataList - List of Movie Posters with IDs
     * @param isPopular - Whether want Popular or Top-rated movies
     */
    public void setData(ArrayList<Pair<Long, String>> dataList, boolean isPopular) {

        if (dataList != null) {
            mPosterAndIds = dataList;
            mIsPopular = isPopular;
            mNumberOfItems = mPosterAndIds.size();  // Should always be 20
            notifyDataSetChanged();  // This function was called because there was a change, so update things.
        }

    }

}
