package app.com.vladimirjeune.popmovies;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import app.com.vladimirjeune.popmovies.data.MovieContract;
import app.com.vladimirjeune.popmovies.utilities.NetworkUtils;

import static android.os.Build.VERSION_CODES.M;

/**
 * Adapter for the Grid View of poster for the user to select from.
 * Created by vladimirjeune on 11/7/17.
 */

class MovieAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = MovieAdapter.class.getSimpleName();

    private Context mContext;

    private int mNumberOfItems;

    private static final int ITEM_VIEW_TYPE_HEADER = 0;

    private static final int ITEM_VIEW_TYPE_POSTER = 1;

    private static final int HEADER_TAG = -100;

    private final View mHeader;

    private String mViewType;

    private ArrayList<ContentValues> mPosterAndIds;

    private final Integer FAVORITE_IN_TRUE = 1;
    private final Integer FAVORITE_IN_FALSE = 0;

    // Interface to accept clicks is below.  Will take a handler in Ctor
    // for when an item is clicked on the list
    final private MovieOnClickHandler mClickHandler;

    public interface MovieOnClickHandler {
        void onClick(long movieId);
    }

    /**
     * MOVIEADAPTER - CONSTRUCTOR
     * @param context - Needed for some function calls
     * @param aMovieOnClickHandler - For when an item is clicked on the list
     * @param numberOfItems - Number of posters that should ultimately be displayed to the user
     */
    public MovieAdapter(Context context, MovieOnClickHandler aMovieOnClickHandler, View aHeader, int numberOfItems) {

        if (null == aHeader) {
            throw new IllegalArgumentException("Header must not be null");
        }

        mContext = context;
        mHeader = aHeader;
        mClickHandler = aMovieOnClickHandler;
        mNumberOfItems = numberOfItems;
        mViewType = context.getString(R.string.pref_sort_popular);  // Changes when data is set

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
        String fakeTitle = "";
        Integer fakeFavoriteOff = FAVORITE_IN_FALSE;  // 0 means False

        ContentValues fakeContentValues = new ContentValues();
        fakeContentValues.put(MovieContract.MovieEntry._ID, fakeId);
        fakeContentValues.put(MovieContract.MovieEntry.ORIGINAL_TITLE, fakeTitle);
        fakeContentValues.put(MovieContract.MovieEntry.POSTER_PATH, fakePath);
        fakeContentValues.put(MovieContract.MovieEntry.BACKDROP_PATH, fakePath);
        fakeContentValues.put(MovieContract.MovieEntry.FAVORITE_FLAG, fakeFavoriteOff);  // Initially Hearts are OFF  // TODO: LOOKED AT USAGE

        for (int i = 0; i < mNumberOfItems; i++) {
            mPosterAndIds.add(fakeContentValues);
        }
    }

    /**
     * ISHEADER - Tells whether this position is the position of the header for the MainActivity.
     * @param position - index passed in
     * @return - boolean - Whether the index passed in is a header or not
     */
    public boolean isHeader(int position) {
        return position == 0;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // If this is a header there is nothing else to do wrap that header
        if (ITEM_VIEW_TYPE_HEADER == viewType) {
            return new HeaderViewHolder(mHeader);
        }

        int layoutForListItem = R.layout.movies_grid_item;
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        boolean attachToParentNow = false;

        View vhView = layoutInflater.inflate(layoutForListItem, parent, attachToParentNow);

        vhView.setFocusable(true);

        return new PosterViewHolder(vhView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

        if (isHeader(position)) {
            if (mViewType.equals(mContext.getString(R.string.pref_sort_popular))) {
                ((HeaderViewHolder)holder).textViewHeader.setText(R.string.pref_sort_popular_label);
                ((HeaderViewHolder)holder).textViewHeader.setTextColor(ContextCompat.getColor(mContext, R.color.text_header_orange));
                ((HeaderViewHolder)holder).textViewHeader.setBackgroundColor(ContextCompat.getColor(mContext, R.color.header_popular_background));
            } else if (mViewType.equals(mContext.getString(R.string.pref_sort_top_rated))) {
                ((HeaderViewHolder)holder).textViewHeader.setText(R.string.pref_sort_top_rated_label);
                ((HeaderViewHolder)holder).textViewHeader.setTextColor(ContextCompat.getColor(mContext, R.color.text_header_blue));
                ((HeaderViewHolder)holder).textViewHeader.setBackgroundColor(ContextCompat.getColor(mContext, R.color.header_top_rated_background));
            } else if (mViewType.equals(mContext.getString(R.string.pref_sort_favorite))) {
                ((HeaderViewHolder)holder).textViewHeader.setText(R.string.pref_sort_favorite_label);
                ((HeaderViewHolder)holder).textViewHeader.setTextColor(ContextCompat.getColor(mContext, R.color.text_header_purple));
                ((HeaderViewHolder)holder).textViewHeader.setBackgroundColor(ContextCompat.getColor(mContext, R.color.header_favorite_background));
            }
            return;
        }

        // Remember, the header took the 0th position.  Must make up for it so 0th position can get seen
        int correctedPosition = position - 1;
        if (mPosterAndIds != null) {
            ((PosterViewHolder) holder).bindTo(mPosterAndIds.get(correctedPosition));
        }

    }

    /**
     * GETITEMCOUNT - Return the number of elements to ultimately show in the RecyclerView
     * @return - The number of items
     */
    @Override
    public int getItemCount() {
        return mNumberOfItems + 1;  // Making up for header taking up the 0
    }

    /**
     * GETITEMVIEWTYPE - Returns appropriate itemViewType
     * @param position - index we are currently at
     * @return - Proper viewType integer
     */
    public int getItemViewType(int position) {
        return isHeader(position) ? ITEM_VIEW_TYPE_HEADER : ITEM_VIEW_TYPE_POSTER;
    }

    /**
     * SETDATA - Takes list of posters in order and whether this batch is popular
     * @param dataList - List of Movie data.  Posters and Titles
     * @param viewType - Whether want Popular or Top-rated movies or Favorite
     */
    public void setData(ArrayList<ContentValues> dataList, String viewType) {

        if (dataList != null) {
            mPosterAndIds = dataList;
            mViewType = viewType;
            mNumberOfItems = mPosterAndIds.size();  // Should usually be 20
            notifyDataSetChanged();  // This function was called because there was a change, so update things.
        }
    }

    /**
     * GETDATA - Returns the data list used to populate the Main Page.
     * @return - ArrayList<ContentValues> List of data for page population
     */
    ArrayList<ContentValues> getData() {
        return mPosterAndIds;
    }

    /**
     * HEADERVIEWHOLDER - VH for the Header View Type
     */
    class HeaderViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewHeader;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            textViewHeader = itemView.findViewById(R.id.textView_main_header);
        }
    }

    /**
     * Cache of the child views of a list item
     */
    class PosterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // Will hold the poster for this item.
        ImageView mListItemPosterView;
        TextView mListItemTextView;
        CheckBox mListItemButtonView;

        View mToastLayout;
        TextView mToastTextView;
        Toast mFavoriteToast;

        /**
         * POSTERVIEWHOLDER - Constructor gets a reference to our textViews holding children.
         * @param itemView - View inflated in onCreateViewHolder
         */
        public PosterViewHolder(View itemView) {
            super(itemView);
            mListItemPosterView = itemView.findViewById(R.id.iv_movie_poster_item);
            mListItemTextView = itemView.findViewById(R.id.tv_movie_title_item);
            mListItemButtonView = itemView.findViewById(R.id.checkbox_favorite);

            LayoutInflater inflater = LayoutInflater.from(mContext);
            mToastLayout = inflater.inflate(
                    R.layout.toast_colored,
                    (ViewGroup) ((Activity)mContext).findViewById(R.id.ll_custom_toast));

            // Make function to pick correct color
//            MainLoadingUtils.toastColorForType(mContext, mViewType, mToastLayout);
            toastColorForType();  //

            mToastTextView = mToastLayout.findViewById(R.id.tv_toast);  //

            mFavoriteToast = new Toast(mContext);
            mFavoriteToast.setDuration(Toast.LENGTH_SHORT);
            mFavoriteToast.setView(mToastLayout);


            // Heart: Will modify list to show whether heart on/off
            ((CheckBox)mListItemButtonView).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CheckBox checkBox = (CheckBox)view;
                    Long thisId = (Long) checkBox.getTag();  // The Movie id that was set

                    if (thisId != null) {

                        // Find CV of ID
                        ContentValues foundCVs;

                        for (int i = 0; i < mPosterAndIds.size(); i++) {
                            if (mPosterAndIds.get(i).getAsLong(MovieContract.MovieEntry._ID)
                                    .equals(thisId)) {
                                foundCVs = mPosterAndIds.get(i);
                                if (checkBox.isChecked()) {  // CVs are like HashMaps, same key for value will be replaced
                                    // TODO: Move; Get title for this position, set in string.
                                    mToastTextView.setText(mContext.getString(R.string.toast_detail_favorite_on, "Avengers: Age of Ultron"));
                                    mFavoriteToast.show();
                                    foundCVs.put(MovieContract.MovieEntry.FAVORITE_FLAG, FAVORITE_IN_TRUE);  // True == 1   // TODO: LOOKED AT USAGE
                                } else {
                                    mToastTextView.setText("Item Unfavorited");
                                    mFavoriteToast.show();
                                    foundCVs.put(MovieContract.MovieEntry.FAVORITE_FLAG, FAVORITE_IN_FALSE);  // False == 0   // TODO: LOOKED AT USAGE
                                }
                                break;  // FOUND IT
                            }
                        }

                    }
                }
            });

            itemView.setOnClickListener(this);  // For non-Heart part
        }

        /**
         * ONCLICK - This gets called by child views during a click.  We call the onClickHandler
         * registered with the adapter, passing the Movie Id associated with the view passed in.
         * @param view - View that was clicked
         */
        public void onClick(View view) {

            ImageView imageView = view.findViewById(R.id.iv_movie_poster_item);  // Find the view with the Tag U set
            if (imageView.getTag() != null) {
                long movieId = (Long) imageView.getTag();  // Get the tag you set on the imageView
                mClickHandler.onClick(movieId);
            }

        }

        /**
         * BINDTO - A convenience method that will attach inputted data to child views.
         * @param aPosterMovieData - Single Movie Data so we can access ID, title and posterPath
         */
        private void bindTo(ContentValues aPosterMovieData) {

            Long movieID = aPosterMovieData.getAsLong(MovieContract.MovieEntry._ID);
            String movieTitle = aPosterMovieData.getAsString(MovieContract.MovieEntry.ORIGINAL_TITLE);
            String moviePosterPath = aPosterMovieData.getAsString(MovieContract.MovieEntry.POSTER_PATH);
            String movieBackdropPath = aPosterMovieData.getAsString(MovieContract.MovieEntry.BACKDROP_PATH);
            Integer movieFavoriteFlag = aPosterMovieData.getAsInteger(MovieContract.MovieEntry.FAVORITE_FLAG);   // TODO: LOOKED AT USAGE

            // If there is no actual movie, use placeholder and leave
//            if (aPosterMovieData.first < 0){
            if (movieID < 0){
                mListItemPosterView.setImageDrawable(ContextCompat
                        .getDrawable(mContext, R.drawable.tmd_placeholder_poster));
                return;
            }

            createRippleDrawableEffectMarshmallowUp();

//            Pair<String, String> payload = aPosterMovieData.second;  // This gets you the interior Pair

//            String title = payload.first;  // Set Title
            mListItemTextView.setText(movieTitle);

//            String posterPath = payload.second;
            // Set Poster and Content Description
            if (moviePosterPath != null) {
                String urlForPosterPath = NetworkUtils.buildURLForImage(moviePosterPath)
                        .toString();

                Picasso.with(mContext)
                        .load(urlForPosterPath)
                        .placeholder(R.drawable.tmd_placeholder_poster)
                        .error(R.drawable.tmd_error_poster)
                        .into(mListItemPosterView);


                String a11yPoster = mContext.getString(R.string.a11y_poster, movieTitle);
                mListItemPosterView.setContentDescription(a11yPoster);
                mListItemPosterView.setTag(movieID);  // This is the ID of the movie

                // Toast
//                toastColorForType();  //

                // Set tag for button
                mListItemButtonView.setTag(movieID);
                mListItemButtonView.setChecked(movieFavoriteFlag.equals(FAVORITE_IN_TRUE));  // If MFI is MAX, Heart ON


            }
        }

        /**
         * CREATERIPPLEDRAWABLEEFFECTMARSHMALLOWUP - Makes ripple drawable effect on the posters
         * when touched.
         */
        private void createRippleDrawableEffectMarshmallowUp() {
            // Add Ripple to Poster in post Lollipop phones. Otherwise, nothing
            int[] attrs = new int[]{R.attr.selectableItemBackground};
            TypedArray typedArray = mContext.obtainStyledAttributes(attrs);
            int indexValue = 0;
            int defaultValue = 0;
            int backgroundResource = typedArray.getResourceId(indexValue, defaultValue);

            if (Build.VERSION.SDK_INT >= M) {
                Drawable ripple = ContextCompat.getDrawable(mContext, backgroundResource);
                mListItemPosterView.setForeground(ripple);
            }

            typedArray.recycle();  // MUST recycle
        }


        /**
         * TOASTCOLORFORTYPE - Sets the Toast color for the type of View we are displaying
         */
        private void toastColorForType() {
            if (mViewType.equals(mContext.getString(R.string.pref_sort_popular))) {
                mToastLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.toast_background_orange));
            } else if (mViewType.equals(mContext.getString(R.string.pref_sort_top_rated))) {
                mToastLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.toast_background_blue));
            } else if (mViewType.equals(mContext.getString(R.string.pref_sort_favorite))) {
                mToastLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.toast_background_purple));

            }
        }

    }

}
