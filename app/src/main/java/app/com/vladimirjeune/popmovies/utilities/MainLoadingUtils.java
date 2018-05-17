package app.com.vladimirjeune.popmovies.utilities;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import app.com.vladimirjeune.popmovies.R;
import app.com.vladimirjeune.popmovies.data.MovieContract;
import app.com.vladimirjeune.popmovies.data.MovieContract.MovieEntry;
import app.com.vladimirjeune.popmovies.data.MovieContract.ReviewEntry;
import app.com.vladimirjeune.popmovies.data.MovieContract.YoutubeEntry;

/**
 * Class to hold some of the functions used to reorder movies when new data comes in.
 * Created by vladimirjeune on 1/5/18.
 */

public final class MainLoadingUtils {

    private static final String TAG = MainLoadingUtils.class.getSimpleName();
    private static final int FAVORITE_FALSE = 0;
    private static final int FAVORITE_TRUE = 1;


    /**
     * GETTYPEORDERIN - Used to get the proper [TYPE]OrderIn depending on the boolean
     * @param viewType - String - Does the User want Popular order, Top-Rated order, or Favorite order
     * @return - String - Proper key based on the boolean passed in
     */
    @Nullable
    public static String getTypeOrderIn(Context context, String viewType) {
        String typeOrder = null;

        if (viewType.equals(context.getString(R.string.pref_sort_popular))) {
            typeOrder = MovieEntry.POPULAR_ORDER_IN;
        } else if (viewType.equals(context.getString(R.string.pref_sort_top_rated))) {
            typeOrder = MovieEntry.TOP_RATED_ORDER_IN;
        } else if (viewType.equals(context.getString(R.string.pref_sort_favorite))) {
            typeOrder = MovieEntry.FAVORITE_FLAG;  // TODO: LOOKED AT USAGE
        }

        return typeOrder;
    }


    /**
     * CREATEARRAYLISTOFPAIRSFORPOSTERS - Creates the ArrayList needed for Posters
     * @param idsAndData - ArrayList<ContentValues> In order
     * @param cursorPosterPathsMovieIds - Cursor - Cursor from DB in order of Popularity or Rating
     */
    public static void createArrayListOfContentValuesForPosters(ArrayList<ContentValues> idsAndData, Cursor cursorPosterPathsMovieIds) {
        // Create ArrayList of Pairs for posters
        if ((cursorPosterPathsMovieIds != null)
                && (cursorPosterPathsMovieIds.moveToFirst())) {

            int idIndex = cursorPosterPathsMovieIds.getColumnIndex(MovieEntry._ID);
            int titleIndex = cursorPosterPathsMovieIds.getColumnIndex(MovieEntry.ORIGINAL_TITLE);
            int posterPathIndex = cursorPosterPathsMovieIds.getColumnIndex(MovieEntry.POSTER_PATH);
            int backdropPathIndex = cursorPosterPathsMovieIds.getColumnIndex(MovieEntry.BACKDROP_PATH);
            int favoriteOrderInIndex = cursorPosterPathsMovieIds.getColumnIndex(MovieEntry.FAVORITE_FLAG);  // TODO: LOOKED AT USAGE

            if ((-1 != idIndex) && (-1 != posterPathIndex) && (-1 != titleIndex)) {
                do {
                    ContentValues singleData = new ContentValues();

                    long movieId = cursorPosterPathsMovieIds.getLong(idIndex);
                    String title = cursorPosterPathsMovieIds.getString(titleIndex);
                    String posterPath = cursorPosterPathsMovieIds.getString(posterPathIndex);
                    String backdropPath = cursorPosterPathsMovieIds.getString(backdropPathIndex);
                    Integer favoriteOrderIn = cursorPosterPathsMovieIds.getInt(favoriteOrderInIndex);

                    singleData.put(MovieEntry._ID, movieId);
                    singleData.put(MovieEntry.ORIGINAL_TITLE, title);
                    singleData.put(MovieEntry.POSTER_PATH, posterPath);
                    singleData.put(MovieEntry.BACKDROP_PATH, backdropPath);
                    singleData.put(MovieEntry.FAVORITE_FLAG, favoriteOrderIn);  // TODO: LOOKED AT USAGE

                    idsAndData.add(singleData);

                } while (cursorPosterPathsMovieIds.moveToNext()) ;
            }
        }
    }


    /**
     * GETCURSORPOSTERPATHSMOVIEIDS - Will obtain cursor containing the movieId, and the posterPath for each movie
     * of the specified type.
     * @param viewType - String - Whether this is for Popular or Top-Rated or Favorite
     * @param context - Needed for function call
     * @return Cursor - Result of query.  Can return NULL if database not yet set.
     */
    public static Cursor getCursorPosterPathsMovieIds(String viewType, Context context) {
        // Do SQL query to get Cursor.  SELECT movieId from TABLE where Type==Pop|Top depending on isPopular
        // AND runtime IS NULL.  Pass in Cursor of runtime that need filling to getRuntimesForMoviesWithIds
        String orderByTypeIndex;
        String selection ;

        if (viewType.equals(context.getString(R.string.pref_sort_popular))) {
            orderByTypeIndex = MovieEntry.POPULAR_ORDER_IN;
            selection = MovieEntry.POPULAR_ORDER_IN + " IS NOT NULL ";
        } else if (viewType.equals(context.getString(R.string.pref_sort_top_rated))) {
            orderByTypeIndex = MovieEntry.TOP_RATED_ORDER_IN;
            selection = MovieEntry.TOP_RATED_ORDER_IN + " IS NOT NULL ";
        } else {
            orderByTypeIndex = MovieEntry.ORIGINAL_TITLE;  // TODO: Go by alphabetical order, orderBy OriginalTitle
            selection = MovieEntry.FAVORITE_FLAG + " == 1 ";  // TODO: LOOKED AT USAGE
        }

        String[] posterPathMovieIdColumns = {
                MovieEntry._ID,
                MovieEntry.ORIGINAL_TITLE,
                MovieEntry.POSTER_PATH,
                MovieEntry.BACKDROP_PATH,
                MovieEntry.FAVORITE_FLAG     // TODO: LOOKED AT USAGE
        };

        // Trying to say SELECT movieId, original title, posterPath FROM movies WHERE selection IS NOT NULL ORDER BY xxxORDERIN
        // Give me cols of all the movies that are POPULAR|TOPRATED and have them in the order they were downloaded(by pop or top)
        return context.getContentResolver().query(
                MovieEntry.CONTENT_URI,
                posterPathMovieIdColumns,
                selection,
                null,
                orderByTypeIndex);
    }


    /**
     * FINDOTHERTYPEINS - Finds the other 2 types of OrderIn that are not the one that belongs to what was inputted.
     * EX: Inputted == POPULAR => TOP_RATED_ORDER_IN, FAVORITE_FLAG
     * @param context - Needed for function calls
     * @param viewType - The current ViewType we are dealing with from calling function
     * @return Pair<String, String> - Holding the OrderIns of the other 2 types
     */
    public static Pair<String,String> findOtherTypeIns(Context context, String viewType) {
        Pair<String, String> retVal = null;

        if (viewType.equals(context.getString(R.string.pref_sort_popular))) {
            retVal =  new Pair<>(MovieEntry.TOP_RATED_ORDER_IN, MovieEntry.FAVORITE_FLAG);     // TODO: LOOKED AT USAGE, Order Alphabetically
        } else if (viewType.equals(context.getString(R.string.pref_sort_top_rated))) {
            retVal = new Pair<>(MovieEntry.POPULAR_ORDER_IN, MovieEntry.FAVORITE_FLAG);     // TODO: LOOKED AT USAGE, Order Alphabetically
        } else if (viewType.equals(context.getString(R.string.pref_sort_favorite))) {
            retVal = new Pair<>(MovieEntry.POPULAR_ORDER_IN, MovieEntry.TOP_RATED_ORDER_IN);
        }

        return retVal;
    }

    /**
     * FINDOPPOSITETYPEORDERINS - Creates String for a WHERE statement that consists of an OR for
     * OrderIns of types that are not the one that is passed in.
     * @param context - Needed for function calls
     * @param viewType - The current ViewType we are dealing with from calling function.  Pop|Top ONLY
     * @return String - For WHERE clause ORing the 2 OrderIns from the opposing types
     */
    public static String findOppositeTypeOrderIns(Context context, String viewType) {
        Pair<String, String> otherTypeIns = findOtherTypeIns(context,  viewType);

        // If Movie is in either of the other types
        return getTypeOrderIn(context, viewType) + " IS NOT NULL "
                + " AND ( " + getOtherOrderInWhereString(otherTypeIns.first )
                + " OR " + getOtherOrderInWhereString(otherTypeIns.second) + " ) ";  // TODO: Check SQL book for validity
    }

    /**
     * GETOTHERORDERINWHERESTRING - Returns the String necessary to complete the where String to
     * check for the presence of certain View Types for a Movie.  Handles Strings that need to be
     * compared to ints and nulls.
     *
     * @param otherTypeIn - A View Type Order, or Flag whose presence indicates Movie Type
     * @return String - String for a Where clause to check for presence of this type in Movie
     */
    @NonNull
    public static String getOtherOrderInWhereString(String otherTypeIn) {
        return otherTypeIn + (
                (otherTypeIn.equals(MovieEntry.POPULAR_ORDER_IN)
                        || (otherTypeIn.equals(MovieEntry.TOP_RATED_ORDER_IN)))
                        ? " IS NOT NULL " : " == 1 ");
    }

    /**
     * GETSINGLEMOVIERUNTIMEFROMTMDB - Get the runtime for the movie with the given movieId.
     * Note: Makes network call.
     * @param aMovieId - Movie Id for movie we are getting the runtime for
     * @return int - Runtime of movie
     */
    private static int getSingleMovieRuntimeFromTMDB(String aMovieId, Context context) {
        int movieRuntime = 0;

        try {
            String receivedSingleMovieJSON = NetworkUtils
                    .getResponseFromHttpUrl(NetworkUtils
                            .buildUrlForSingleMovie(context, aMovieId));

            movieRuntime = OpenTMDJsonUtils
                    .getRuntimeOfSingleMovie(receivedSingleMovieJSON);

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

        return movieRuntime;
    }


    /**
     * GETSINGLEMOVIESREVIEWFROMTMDB - Get the Review Stats for the movie with the given movieId.
     * Note: Makes network call.
     * @param aMovieId - Movie Id for movie we are getting the Review Data for
     * @return ContentValues[] - Review Data of movie.  Can be 0 - n, or possibly null
     */
    public static ContentValues[] getSingleMoviesReviewsFromTMDB(Context context, String aMovieId) {
        ContentValues[] reviewContentValues = null;

        try {

            // Get JSON from Network call
            String receivedSingleMoviesReviewJSON = NetworkUtils.getResponseFromHttpUrl(
                    NetworkUtils.buildURLForReviews(context, aMovieId)
            );

            // Turn JSON into ContentValues[] for this movieID
            reviewContentValues = OpenTMDJsonUtils.getReviewContentValues(context,
                    Long.valueOf(aMovieId), receivedSingleMoviesReviewJSON);

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

        return reviewContentValues;
    }


    /**
     * GETSINGLEMOVIESYOUTUBESFROMTMDB - Get the Youtube Stats for the movie with the given movieId.
     * Note: Makes network call.
     * @param aMovieId - Movie Id for movie we are getting the Review Data for
     * @return ContentValues[] - Youtube Data of movie.  Can be 0 - n, or possibly null
     */
    public static ContentValues[] getSingleMoviesYoutubesFromTMDB(Context context, String aMovieId) {
        ContentValues[] youtubeContentValues = null;

        try {

            // Get JSON from Network call
            String receivedSingleMoviesYoutubeJSON = NetworkUtils.getResponseFromHttpUrl(
                    NetworkUtils.buildURLforVideos(context, aMovieId)
            );

            // Turn JSON into ContentValues[] for this movieID
            youtubeContentValues = OpenTMDJsonUtils.getYoutubeContentValues(context,
                    Long.valueOf(aMovieId), receivedSingleMoviesYoutubeJSON);

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

        return youtubeContentValues;
    }



    /**
     * GETRUNTIMESFORMOVIESINLIST - Add the runtimes to the Movies that were just created from
     * JSON call that should precede this one.  Update database with runtimes
     * Note: Makes network call.  Calls DB
     * @param cursor - Holds data we need to look through
     */
    public static void getRuntimesForMoviesInList(Cursor cursor, Context context) {
        // Get runtime for found movies and place in correct Movies.
        if (cursor != null) {  // Cursor exists
            if (cursor.moveToFirst()) {  // Cursor is valid
                ContentValues idContentValues;
                int movieIdCursorIndex = cursor.getColumnIndex(MovieEntry._ID);  // Ge index of id
                for (int i = 0; cursor.moveToPosition(i); i++) {
                    idContentValues = new ContentValues();
                    long movieID = cursor.getLong(movieIdCursorIndex);  // Get movieId for later in loop

                    int runtime = MainLoadingUtils.getSingleMovieRuntimeFromTMDB("" + movieID, context);  // Get runtime for this id
                    idContentValues.put(MovieEntry.RUNTIME, runtime);  // It is the runtime we want to update

                    // Update runtime where ID = id
                    context.getContentResolver().update(
                            MovieEntry.buildUriWithMovieId(movieID),
                            idContentValues,
                            null,
                            null);

                }
            }
        }
    }


    /**
     * SETYOUTUBESFORMOVIEOFID - Add the Youtubes to the Youtube Tables for movie with passed in ID
     * Update database with Youtubes for Movie; if available
     * Note: Makes network call.  Calls DB
     * @param youtubesForSingleMovie - Array of Reviews for this move
     * @param movieId - Movie ID for the movie we want Youtubes for
     * @param context - Needed for some function calls
     */
    public static void setYoutubesForMovieOfId(ContentValues[] youtubesForSingleMovie, long movieId, Context context) {

        Cursor youtubeIdsForThisMovieCursor;

        // Query for Youtube IDs we already have, if any.
        String[] projection = new String[] {YoutubeEntry.YOUTUBE_ID};
        String selection = YoutubeEntry.MOVIE_ID + " = ? ";
        String[] selectionArgs = new String[] {""+ movieId};
        youtubeIdsForThisMovieCursor = context.getContentResolver()
                .query(YoutubeEntry.CONTENT_URI, projection, selection, selectionArgs, null);

        HashSet<String> alreadyInSet = cursorYoutubeIdsToSet(youtubeIdsForThisMovieCursor);

        insertYoutubesForMovie(context, alreadyInSet, youtubesForSingleMovie);

        if ((youtubeIdsForThisMovieCursor != null)
                && (! youtubeIdsForThisMovieCursor.isClosed())) {
            youtubeIdsForThisMovieCursor.close();
        }

    }


    /**
     * SETREVIEWSFORMOVIEOFID - Add the Reviews from net to the Review Tables in DB for movie
     * Update database with Reviews for Movie; if available
     * Note: Makes network call.  Calls DB
     * @param reviewsForSingleMovie - Array of Reviews for this move
     * @param movieId - Movie ID for the movie we want Youtubes for
     * @param context - Needed for some function calls
     */
    public static void setReviewsForMovieOfId(ContentValues[] reviewsForSingleMovie, long movieId, Context context) {

        Cursor reviewIDsForThisMovieCursor;

        // Query for Review IDs we already have, if any.  Do here to give time for return b4 next function
        String[] projection = new String[] {ReviewEntry.REVIEW_ID};
        String selection = ReviewEntry.MOVIE_ID + " = ? ";
        String[] selectionArgs = new String[] {""+ movieId};
        reviewIDsForThisMovieCursor = context.getContentResolver()
                .query(ReviewEntry.CONTENT_URI, projection, selection, selectionArgs, null);

        HashSet<String> alreadyInSet = cursorIdsToSet(reviewIDsForThisMovieCursor);

        insertReviewsForMovie(context, alreadyInSet, reviewsForSingleMovie);

        if ((reviewIDsForThisMovieCursor != null)
                && (! reviewIDsForThisMovieCursor.isClosed())) {
            reviewIDsForThisMovieCursor.close();
        }

    }


    /**
     * INSERTREVIEWSFORMOVIE - Inserts reviews passed in, if any, into the Reviews Database
     * @param context - Needed for function calls
     * @param alreadyInSet - Set of Reviews in DB for this Movie ID.  Can be null, or empty
     * @param reviewsForSingleMovie - ContentValues holding the reviews for a movie, if any
     */
    private static void insertReviewsForMovie(Context context, HashSet<String> alreadyInSet,
                                              ContentValues[] reviewsForSingleMovie) {

        if ( (reviewsForSingleMovie != null) && (reviewsForSingleMovie.length > 0) ) {

            if ((alreadyInSet != null) && (alreadyInSet.size() > 0)) {
                // Create Set of incoming Reviews for Set Operations with AlreadyInDBSet
                HashSet<String> incomingSet = makeIncomingSet(reviewsForSingleMovie);

                // Already in DB - Incoming => To be dropped from DB
                HashSet<String> deleteReviewSet = new HashSet<>(alreadyInSet);
                deleteReviewSet.removeAll(incomingSet);

                // Already in DB intersect Incoming => In need of update in DB
                HashSet<String> updateReviewSet = new HashSet<>(alreadyInSet);
                updateReviewSet.retainAll(incomingSet);

                // Incoming from net - Already in DB => Just insert, since new
                HashSet<String> insertReviewSet = new HashSet<>(incomingSet);
                insertReviewSet.removeAll(alreadyInSet);

                for (int i = 0; i < reviewsForSingleMovie.length; i++) {
                    String currentId = reviewsForSingleMovie[i].getAsString(ReviewEntry.REVIEW_ID);

                    String selection = ReviewEntry.REVIEW_ID + " = ? ";
                    String reviewId = reviewsForSingleMovie[i].getAsString(ReviewEntry.REVIEW_ID);
                    String[] selectionArgs = new String[]{reviewId};

                    // Remember; we are actually using the ReviewID:String for identity.  Which is NOT PK, or a Long
                    if (deleteReviewSet.contains(currentId)) {

                        context.getContentResolver().delete(ReviewEntry.CONTENT_URI, selection, selectionArgs);

                    } else if (updateReviewSet.contains(currentId)) {

                        context.getContentResolver().update(ReviewEntry.CONTENT_URI, reviewsForSingleMovie[i], selection, selectionArgs);

                    } else if (insertReviewSet.contains(currentId)) {

                        context.getContentResolver().insert(ReviewEntry.CONTENT_URI, reviewsForSingleMovie[i]);

                    } else {
                        throw new UnsupportedOperationException("Set operations failed!");
                    }

                }
            } else {
                // If DB is empty, just insert all from Net
                context.getContentResolver().bulkInsert(ReviewEntry.CONTENT_URI, reviewsForSingleMovie);
            }
        }
    }


    /**
     * INSERTYOUTUBESFORMOVIE - Inserts youtubes passed in, if any, into the Youtubes Database
     * @param context - Needed for function calls
     * @param alreadyInSet - Set of Reviews in DB for this Movie ID.  Can be null, or empty
     * @param youtubesForSingleMovie - ContentValues holding the youtubes for a movie, if any
     */
    private static void insertYoutubesForMovie(Context context, HashSet<String> alreadyInSet,
                                              ContentValues[] youtubesForSingleMovie) {

        if ( (youtubesForSingleMovie != null) && (youtubesForSingleMovie.length > 0) ) {
                String Y_ID = YoutubeEntry.YOUTUBE_ID;
                Uri Y_URI = YoutubeEntry.CONTENT_URI;

            if ((alreadyInSet != null) && (alreadyInSet.size() > 0)) {
                // Create Set of incoming Reviews for Set Operations with AlreadyInDBSet
                HashSet<String> incomingSet = makeYoutubeIncomingSet(youtubesForSingleMovie);

                // Already in DB - Incoming => To be dropped from DB
                HashSet<String> deleteYoutubeSet = new HashSet<>(alreadyInSet);
                deleteYoutubeSet.removeAll(incomingSet);

                // Already in DB intersect Incoming => In need of update in DB
                HashSet<String> updateYoutubeSet = new HashSet<>(alreadyInSet);
                updateYoutubeSet.retainAll(incomingSet);

                // Incoming from net - Already in DB => Just insert, since new
                HashSet<String> insertYoutubeSet = new HashSet<>(incomingSet);
                insertYoutubeSet.removeAll(alreadyInSet);

                for (int i = 0; i < youtubesForSingleMovie.length; i++) {
                    String currentId = youtubesForSingleMovie[i].getAsString(Y_ID);

                    String selection = Y_ID + " = ? ";
                    String youtubeId = youtubesForSingleMovie[i].getAsString(Y_ID);
                    String[] selectionArgs = new String[]{youtubeId};

                    // Remember; we are actually using the YoutubeID:String for identity.  Which is NOT PK, or a Long
                    if (deleteYoutubeSet.contains(currentId)) {

                        context.getContentResolver().delete(Y_URI, selection, selectionArgs);

                    } else if (updateYoutubeSet.contains(currentId)) {

                        context.getContentResolver().update(Y_URI, youtubesForSingleMovie[i], selection, selectionArgs);

                    } else if (insertYoutubeSet.contains(currentId)) {

                        context.getContentResolver().insert(Y_URI, youtubesForSingleMovie[i]);

                    } else {
                        throw new UnsupportedOperationException("Set operations failed!");
                    }

                }
            } else {
                // If DB is empty, just insert all from Net
                context.getContentResolver().bulkInsert(Y_URI, youtubesForSingleMovie);
            }
        }
    }


    /**
     * CURSORIDSTOSET - Will take a Cursor with a ReviewEntry ReviewId column and return the IDs in a
     * Set.
     * @param cursorOfIds - Cursor with ReviewIds as one of the columns
     * @return - Set<Long>, can be null
     */
    private static HashSet<String> cursorIdsToSet(Cursor cursorOfIds) {
        HashSet<String> databaseSet = null;

        if ((cursorOfIds != null) && (cursorOfIds.moveToFirst())) {
            databaseSet = new HashSet<>();

            do {
                int idColumnIndex = cursorOfIds.getColumnIndex(ReviewEntry.REVIEW_ID);
                databaseSet.add(cursorOfIds.getString(idColumnIndex));
            } while (cursorOfIds.moveToNext());

        }
        return databaseSet;
    }


    /**
     * CURSORYOUTUBEIDSTOSET - Will take a Cursor with a ReviewEntry ReviewId column and return the IDs in a
     * Set.
     * @param cursorOfIds - Cursor with ReviewIds as one of the columns
     * @return - Set<Long>, can be null
     */
    private static HashSet<String> cursorYoutubeIdsToSet(Cursor cursorOfIds) {
        HashSet<String> databaseSet = null;

        if ((cursorOfIds != null) && (cursorOfIds.moveToFirst())) {
            databaseSet = new HashSet<>();

            do {
                int idColumnIndex = cursorOfIds.getColumnIndex(YoutubeEntry.YOUTUBE_ID);
                databaseSet.add(cursorOfIds.getString(idColumnIndex));
            } while (cursorOfIds.moveToNext());

        }
        return databaseSet;
    }


    /**
     * MAKEINCOMINGSET - Turns paramter of ContentValues[] with ReviewID into a Set of the same
     * @param contentValues - Array of ContentValues with ID for Reviews, if not null, and full.
     * @return - Set of IDs for the Reviews that were represented in the parameter
     */
    private static HashSet<String> makeIncomingSet(ContentValues[] contentValues) {
        HashSet<String> incomingSet = null;

        if ((contentValues != null) && (contentValues.length > 0)) {
            incomingSet = new HashSet<>();

            for (int i = 0; i < contentValues.length; i++) {
                incomingSet.add(contentValues[i].getAsString(ReviewEntry.REVIEW_ID));
            }

        }

        return incomingSet;
    }


    /**
     * MAKEYOUTUBEINCOMINGSET - Turns paramater of ContentValues[] with YoutubeID into a Set of the same
     * @param contentValues - Array of ContentValues with ID for Youtubes, if not null, and full.
     * @return - Set of IDs for the Youtubes that were represented in the parameter
     */
    private static HashSet<String> makeYoutubeIncomingSet(ContentValues[] contentValues) {
        HashSet<String> incomingSet = null;

        if ((contentValues != null) && (contentValues.length > 0)) {
            incomingSet = new HashSet<>();

            for (int i = 0; i < contentValues.length; i++) {
                incomingSet.add(contentValues[i].getAsString(YoutubeEntry.YOUTUBE_ID));
            }

        }

        return incomingSet;
    }


    /**
     * CURRENTDBIDS - Returns a set of the IDs that exist in the entirety of the Databasae.
     * @param context - Needed for functions calls
     * @return - Set of ID in the database
     */
    public static Set<Long> currentDBIDs(Context context) {
        Set<Long> retVal = null;
        final int ID_INDEX = 0;

        String[] idRelevantColumns = {
                MovieEntry._ID
        };

        Cursor idCursor = context.getContentResolver().query(
                MovieEntry.CONTENT_URI,
                idRelevantColumns,
                null,
                null,
                null
        );

        if (idCursor != null) {
            retVal = new HashSet<>();

            if (idCursor.moveToFirst()) {

                do {
                    retVal.add(idCursor.getLong(ID_INDEX));
                } while(idCursor.moveToNext()) ;

            }

            idCursor.close();
        }

        return retVal;
    }


    /**
     * CURRENTDBIDSOFTYPE - Returns a set of the IDs that exist in the entirety of the Databasae with the current type.
     * @param context - Needed for functions calls
     * @param viewType - Should ONLY be either POPULAR or TOP_RATED
     * @return - Set of ID in the database of this particular type passed in
     */
    public static Set<Long> currentDBIDsOfType(Context context, String viewType) {
        Set<Long> retVal = new HashSet<>();
        final int ID_INDEX = 0;
        String typeOrderIn = getTypeOrderIn(context, viewType);

        String[] idRelevantColumns = {
                MovieEntry._ID
        };

        Cursor idCursor = context.getContentResolver().query(
                MovieEntry.CONTENT_URI,
                idRelevantColumns,
                typeOrderIn + " IS NOT NULL ",
                null,
                null
        );

        if (idCursor != null) {

            if (idCursor.moveToFirst()) {

                do {
                    retVal.add(idCursor.getLong(ID_INDEX));
                } while(idCursor.moveToNext()) ;

            }

            idCursor.close();
        }

        return retVal;
    }


    /**
     * INCOMINGIDS - Creates Set of IDs of the incoming movies
     * @param movieContentValues - Incoming movies data
     * @return - Set<Long> - Set of incoming Movie IDs
     */
    public static Set<Long> incomingIDs(ContentValues movieContentValues[]) {
        Set<Long> incomingMovieIds = new HashSet<>();

        for (int i = 0; i < movieContentValues.length; i++) {
            incomingMovieIds.add(movieContentValues[i].getAsLong(MovieEntry._ID));
        }

        return incomingMovieIds;
    }


    /**
     * CONNECTIONS - Tells whether the inputted ID has connections to types other than the one that
     * is inputted.  So, if it a Top Rated movie is also a Favorite or Popular.
     * @param context - Needed for function calls
     * @param anId - ID of movie under review.  Assumed to be a valid ID
     * @param aType - Type of the ID that we are looking for.  This is assumed to be correctly inputted
     * @return - boolean[] - Returns NULL if no other connections because all titles should have at least one,
     * and a 3-size array otherwise [POP] [TOP] [FAV]
     */
    public static boolean[] connections(Context context, Long anId, String aType) {
        boolean[] popTopFavoritesArr = null;

        String[] projection = new String[]{
                MovieEntry._ID,
                MovieEntry.ORIGINAL_TITLE,
                MovieEntry.POPULAR_ORDER_IN,
                MovieEntry.TOP_RATED_ORDER_IN,
                MovieEntry.FAVORITE_FLAG
        };

        String[] selectionArgs = new String[] {""+ anId};

        // Should return 1 value because of ID
        Cursor cursor = context.getContentResolver().query(
                MovieEntry.CONTENT_URI,
                projection,
                MovieEntry._ID + " = ? AND "
                + findOppositeTypeOrderIns(context, aType),
                selectionArgs,
                null
        );

        if (cursor != null) {

            if (cursor.moveToFirst()) {

                // Want to know what views are associated with this Cursor
                popTopFavoritesArr = typesForCursor(context, aType, cursor);
            }

            cursor.close();
        }

        return popTopFavoritesArr;
    }


    /**
     * TYPESFORCURSOR - Tells what types are connected to the passed in Cursor
     * @param context - Needed for function calls
     * @param aType - The ViewType of this Cursor
     * @param cursor - Will be searched for associated ViewTypes
     */
    private static boolean[] typesForCursor(Context context, String aType, Cursor cursor) {
        boolean[] popTopFavoritesArr = new boolean[3];

        int popIndex = cursor.getColumnIndex(MovieEntry.POPULAR_ORDER_IN);
        int topIndex = cursor.getColumnIndex(MovieEntry.TOP_RATED_ORDER_IN);
        int favIndex = cursor.getColumnIndex(MovieEntry.FAVORITE_FLAG);

        final int popularPosition = 0;
        final int topratedPosition = 1;
        final int favoritePosition = 2;
        // https://stackoverflow.com/questions/8063768/inserting-null-as-integer-value-into-a-database
        // https://stackoverflow.com/questions/18054182/getting-null-ints-from-sqlite-in-android

        // TODO: Need to avoid the TYPE THAT WE ARE
        if (aType.equals(context.getString(R.string.pref_sort_popular))) {
            popTopFavoritesArr[popularPosition] = true;
            popTopFavoritesArr[topratedPosition] = (!(cursor.isNull(topIndex)));
            popTopFavoritesArr[favoritePosition] = (cursor.getInt(favIndex) == FAVORITE_TRUE);
        } else if (aType.equals(context.getString(R.string.pref_sort_top_rated))) {
            popTopFavoritesArr[popularPosition] = (!(cursor.isNull(popIndex)));
            popTopFavoritesArr[topratedPosition] = true;
            popTopFavoritesArr[favoritePosition] = (cursor.getInt(favIndex) == FAVORITE_TRUE);
        } else if (aType.equals(context.getString(R.string.pref_sort_favorite))) {
            popTopFavoritesArr[popularPosition] = (!(cursor.isNull(popIndex)));
            popTopFavoritesArr[topratedPosition] = (!(cursor.isNull(topIndex)));
            popTopFavoritesArr[favoritePosition] = true;
        }

        return popTopFavoritesArr;
    }


    /**
     * DELETEUPDATEIDSOFTYPE - Will delete or update IDs in the passed in Set in the Database as necessary
     * @param context - Needed for function calls
     * @param viewType - What type we are working on now
     * @param deleteOfTypeSet - Set of IDs to be deleted or updated, if any
     * @param incomingMovieContentValues - Incoming list of movies
     */
    public static void deleteUpdateIDsOfType(Context context, String viewType,
                                             Set<Long> deleteOfTypeSet, ContentValues[] incomingMovieContentValues) {
        final int threshold = 1;  // Each Movie is at least its own type

        if ((deleteOfTypeSet != null) && (deleteOfTypeSet.size() > 0)) {

                for (Long deleteThisId : deleteOfTypeSet) {
                    boolean[] popTopFavArr = null;

                    popTopFavArr = connections(context, deleteThisId, viewType);
                    if (popTopFavArr != null) {  // Null condition should not occur

                            ContentValues eraseValues = new ContentValues();
                            eraseValues.putNull(getTypeOrderIn(context, viewType));
                            String where = MovieEntry._ID + " = ? ";
                            String[] whereArgs = new String[] {"" + deleteThisId};

                            context.getContentResolver().update(
                                    MovieEntry.CONTENT_URI,
                                    eraseValues,
                                    where,
                                    whereArgs
                            );

                    } else {  // There are no extra viewTypes for this ID
                        context.getContentResolver().delete(
                                MovieEntry.buildUriWithMovieId(deleteThisId),
                                null,
                                null
                        );
                    }

            }  // END OF FOR

        }

    }


    /**
     * UPDATEIDSAGAINSTWHOLEDB - Takes set of UpdateIDs and updates them in the DB, without respect to viewType.
     * The ViewType updated will automatically match the incoming movies viewType, and should leave the orderIn
     * information of the other viewTypes alone.  So, if Pop, only Pop orderIn will change.  Favorites is only
     * ever changed through Hearting movies, not incoming movies.
     * @param context - Needed for function calls
     * @param updateSet - Set of IDs of movies whose data need to be updated over the whole DB
     * @param movieContentValues - List of incoming movies of a certain type
     */
    public static void updateIDsAgainstWholeDB(Context context, Set<Long> updateSet,
                                               ContentValues[] movieContentValues) {

        if ((updateSet != null) && (updateSet.size() > 0)) {

            for (int i = 0; i < movieContentValues.length; i++) {

                Long thisID = movieContentValues[i].getAsLong(MovieEntry._ID);
                if (updateSet.contains(thisID)) {

                    context.getContentResolver().update(
                            MovieEntry.CONTENT_URI,
                            movieContentValues[i],
                            MovieEntry._ID + " = ? ",
                            new String[] {"" + thisID}
                    );

                }

            }

        }

    }


    /**
     * INSERTIDSOFTYPE - Inserts the IDs of a certain type into the Database.
     * @param context - Needed for function calls
     * @param needInsertSet - Set of IDs of Movies of a certain type that need insert
     * @param movieContentValues - Array of incoming movies of a certain type
     */
    public static void insertIDsOfType(Context context, Set<Long> needInsertSet,
                                       ContentValues[] movieContentValues) {

        if ((needInsertSet != null) && (needInsertSet.size() > 0)) {

            for (int i = 0; i < movieContentValues.length; i++) {

                Long thisId = movieContentValues[i].getAsLong(MovieEntry._ID);
                if (needInsertSet.contains(thisId)) {

                    context.getContentResolver().insert(
                            MovieEntry.CONTENT_URI,
                            movieContentValues[i]
                    );

                }

            }

        }

    }


    /**
     * USESTOREDREVIEWDATAINDB - Will use what is stored in the database to populate the Review list
     * @param currentMovieId - ID of the movie that we are supposed to have the reviews for
     * @param data - ContentValues ArrayList to be filled with the Reviews in the SQL DB, if any.  In order of Review ID.
     * @param context - Needed for function calls
     */
    public static void useStoredReviewDataInDB(ArrayList<ContentValues> data, long currentMovieId, Context context) {

        String[] projection = {
                ReviewEntry.REVIEW_ID,
                ReviewEntry.AUTHOR,
                ReviewEntry.CONTENT,
                MovieEntry.BACKDROP_PATH
        };
        String selection = ReviewEntry.REVIEW_ID + " = ? ";
        String[] selectionArgs = new String[] {"" + currentMovieId};

        Cursor sqlTransferCursor = null;
        try {
            sqlTransferCursor = context.getContentResolver()
                    .query(MovieContract.joineReviewEntry.CONTENT_URI,
                            projection,
                            selection,
                            selectionArgs,
                            ReviewEntry.REVIEW_ID);

            if ((sqlTransferCursor != null)
                    && (sqlTransferCursor.moveToFirst()) ) {

                int rIdIndex = sqlTransferCursor.getColumnIndex(ReviewEntry.REVIEW_ID);
                int authorIndex = sqlTransferCursor.getColumnIndex(ReviewEntry.AUTHOR);
                int contentIndex = sqlTransferCursor.getColumnIndex(ReviewEntry.CONTENT);
                int backdropPathIndex = sqlTransferCursor.getColumnIndex(MovieEntry.BACKDROP_PATH);

                do {

                    String r_id = sqlTransferCursor.getString(rIdIndex);
                    String author = sqlTransferCursor.getString(authorIndex);
                    String content = sqlTransferCursor.getString((contentIndex));
                    String backdropPath = sqlTransferCursor.getString(backdropPathIndex);

                    ContentValues contentValues = new ContentValues();
                    contentValues.put(ReviewEntry.REVIEW_ID, r_id);
                    contentValues.put(ReviewEntry.AUTHOR, author);
                    contentValues.put(ReviewEntry.CONTENT, content);
                    contentValues.put(MovieEntry.BACKDROP_PATH, backdropPath);

                    data.add(contentValues);

                } while(sqlTransferCursor.moveToNext());

            }

        } finally {  // Always close the Cursor
            if ((sqlTransferCursor != null) && (! sqlTransferCursor.isClosed())) {
                sqlTransferCursor.close();
            }
        }

    }


    /**
     * USESTOREDYOUTUBEDATAINDB - Will use what is stored in the database to populate the Youtube list
     * @param currentMovieId - ID of the movie that we are supposed to have the youtubes for
     * @param data - ContentValues ArrayList to be filled with the Youtubes in the SQL DB, if any.  In order of Youtube ID.
     * @param context - Needed for function calls
     */
    public static void useStoredYoutubeDataInDB(ArrayList<ContentValues> data, long currentMovieId, Context context) {

        String[] projection = {
                YoutubeEntry.YOUTUBE_ID,
                YoutubeEntry.KEY,
                YoutubeEntry.NAME,
                YoutubeEntry.TYPE,
                MovieEntry.ORIGINAL_TITLE
        };
        String selection = ReviewEntry.MOVIE_ID + " = ? ";
        String[] selectionArgs = new String[] {"" + currentMovieId};
        String orderBy = YoutubeEntry.YOUTUBE_ID;

        Cursor sqlTransferCursor = null;
        try {
            sqlTransferCursor = context.getContentResolver()
                    .query(MovieContract.JoinYoutubeEntry.CONTENT_URI,
                            projection,
                            selection,
                            selectionArgs,
                            orderBy);

            if ((sqlTransferCursor != null)
                    && (sqlTransferCursor.moveToFirst()) ) {

                int yIdIndex = sqlTransferCursor.getColumnIndex(YoutubeEntry.YOUTUBE_ID);
                int keyIndex = sqlTransferCursor.getColumnIndex(YoutubeEntry.KEY);
                int nameIndex = sqlTransferCursor.getColumnIndex(YoutubeEntry.NAME);
                int typeIndex = sqlTransferCursor.getColumnIndex(YoutubeEntry.TYPE);
                int titleIndex = sqlTransferCursor.getColumnIndex(MovieEntry.ORIGINAL_TITLE);

                do {

                    String y_id = sqlTransferCursor.getString(yIdIndex);
                    String key = sqlTransferCursor.getString(keyIndex);
                    String name = sqlTransferCursor.getString((nameIndex));
                    String type = sqlTransferCursor.getString(typeIndex);
                    String original_title = sqlTransferCursor.getString(titleIndex);

                    ContentValues contentValues = new ContentValues();
                    contentValues.put(YoutubeEntry.YOUTUBE_ID, y_id);
                    contentValues.put(YoutubeEntry.KEY, key);
                    contentValues.put(YoutubeEntry.NAME, name);
                    contentValues.put(YoutubeEntry.TYPE, type);
                    contentValues.put(MovieEntry.ORIGINAL_TITLE, original_title);

                    data.add(contentValues);

                } while(sqlTransferCursor.moveToNext());

            }

        } finally {  // Always close the Cursor
            if ((sqlTransferCursor != null) && (! sqlTransferCursor.isClosed())) {
                sqlTransferCursor.close();
            }
        }

    }


    /**
     * GETNUMBEROFTYPESFORID - Returns the number of true values in the array.
     * @param popTopFavArr - Array of booleans in order of Popular | Top Rated | Favorites.
     *                     True means that type is associated with this ID
     * @return Number of types associated with this ID
     */
    private static int getNumberOfTypesForID(boolean[] popTopFavArr) {
        int cnt = 0;

        for (int j = 0; j < popTopFavArr.length; j++) {
            if (popTopFavArr[j]) {
                cnt++;
            }
        }

        return cnt;
    }

}
