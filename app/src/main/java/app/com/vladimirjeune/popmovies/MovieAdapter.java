package app.com.vladimirjeune.popmovies;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Adapter for the Grid View of poster for the user to select from.
 * Created by vladimirjeune on 11/7/17.
 */

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.PosterViewHolder> {

    private static final String TAG = MovieAdapter.class.getSimpleName();

    private int mNumberOfItems;

    private Context mContext;

    // Will change to something else later, now just for testing
    private Drawable[] mPosterData;

    /**
     * MOVIEADAPTER - CONSTRUCTOR
     * @param numberOfItems - Number of posters that will ultimately be displayed to the user
     */
    public MovieAdapter(Context context, int numberOfItems) {
        mContext = context;
        mNumberOfItems = numberOfItems;
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
        holder.bindTo(mPosterData[position]);
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
         * @param poster -
         */
        public void bindTo(Drawable poster) {
            listItemPosterView.setImageDrawable(poster);
        }

    }

    /**
     * SETPOSTERDATA - Takes in a list of Drawables that will be shown in the
     * GridView.
     * @param posterData - Array of Drawables
     */
    public void setPosterData(Drawable[] posterData) {
        mPosterData = posterData;
        notifyDataSetChanged();
    }
}
