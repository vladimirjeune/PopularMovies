package app.com.vladimirjeune.popmovies.utilities;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import app.com.vladimirjeune.popmovies.R;
import app.com.vladimirjeune.popmovies.data.MovieContract.MovieEntry;

/**
 * Class to hold some of the functions used to reorder movies when new data comes in.
 * Created by vladimirjeune on 1/5/18.
 */

public final class MainLoadingUtils {

    private static final String TAG = MainLoadingUtils.class.getSimpleName();


    /**
     * GETTYPEORDERIN - Used to get the proper [TYPE]OrderIn depending on the boolean
     * @param isPopular - boolean - Does the User want Popular order, or Top-Rated order
     * @return - String - Proper key based on the boolean passed in
     */
    @Nullable
    public static String getTypeOrderIn(boolean isPopular) {
        return (isPopular) ? MovieEntry.POPULAR_ORDER_IN : MovieEntry.TOP_RATED_ORDER_IN;
    }


    /**
     * GETOLDPOSITIONOFNEWID - Returns the old Position of the movie with this ID
     * @param isPopular - Popular, or Top-Rated
     * @param idAndTitleOldCursor - Cursor - Place to find old ID
     * @param oldId - Long - Old Id we are looking for
     * @return - String - Order Index of the Type that the user is asking for.  Or "-1", if no match
     */
    @Nullable
    public static String getOldPositionOfNewId(boolean isPopular, final Cursor idAndTitleOldCursor, final Long oldId) {

        int retIndex = -1;
        String orderType = getTypeOrderIn(isPopular);

        if ((idAndTitleOldCursor != null)
                && (idAndTitleOldCursor.moveToFirst()) && (idAndTitleOldCursor.getCount() > 0)) {
            int idIndex = idAndTitleOldCursor.getColumnIndex(MovieEntry._ID);
            int orderTypeIndex = idAndTitleOldCursor.getColumnIndex(orderType);  // Will pick correct of Pop or Top index

            do {
                if (idAndTitleOldCursor.getLong(idIndex) == oldId) {
                    retIndex = idAndTitleOldCursor.getInt(orderTypeIndex);
                    return "" + retIndex;  // Found it, jump out.
                }
            } while (idAndTitleOldCursor.moveToNext());
        }
//        Log.d(TAG, "getOldPositionOfNewId: For some reason we did not find it. Old ID: " + oldId);
        return "" + retIndex;
    }


    /**
     * CREATEARRAYLISTOFPAIRSFORPOSTERS - Creates the ArrayList needed for Posters
     * @param idsTitlesAndPosters - ArrayList<Pairs<Long, Pair<String, String>>> - Posters for MainActivity in order
     * @param cursorPosterPathsMovieIds - Cursor - Cursor from DB in order of Popularity or Rating
     */
    public static void createArrayListOfPairsForPosters(ArrayList<Pair<Long, Pair<String, String>>> idsTitlesAndPosters, Cursor cursorPosterPathsMovieIds) {
        // Create ArrayList of Pairs for posters
        if ((cursorPosterPathsMovieIds != null)
                && (cursorPosterPathsMovieIds.moveToFirst())) {
            int idIndex = cursorPosterPathsMovieIds.getColumnIndex(MovieEntry._ID);
            int titleIndex = cursorPosterPathsMovieIds.getColumnIndex(MovieEntry.ORIGINAL_TITLE);
            int posterPathIndex = cursorPosterPathsMovieIds.getColumnIndex(MovieEntry.POSTER_PATH);

            if ((-1 != idIndex) && (-1 != posterPathIndex) && (-1 != titleIndex)) {
                do {
                    long movieId = cursorPosterPathsMovieIds.getLong(idIndex);
                    String title = cursorPosterPathsMovieIds.getString(titleIndex);
                    String posterPath = cursorPosterPathsMovieIds.getString(posterPathIndex);

                    Pair<String, String> payload = new Pair<>(title, posterPath);
                    idsTitlesAndPosters.add(new Pair<>(movieId, payload));
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
            selection = MovieEntry.POPULAR_ORDER_IN;
        } else if (viewType.equals(context.getString(R.string.pref_sort_top_rated))) {
            orderByTypeIndex = MovieEntry.TOP_RATED_ORDER_IN;
            selection = MovieEntry.TOP_RATED_ORDER_IN;
        } else {
            orderByTypeIndex = MovieEntry.FAVORITE_ORDER_IN;
            selection = MovieEntry.FAVORITE_ORDER_IN;
        }

        String[] posterPathMovieIdColumns = {MovieEntry._ID, MovieEntry.ORIGINAL_TITLE, MovieEntry.POSTER_PATH};
        String selectionIsNotNull = selection + " IS NOT NULL ";
        // Trying to say SELECT movieId, original title, posterPath FROM movies WHERE selection IS NOT NULL ORDER BY xxxORDERIN
        // Give me 3 cols of all the movies that are POPULAR|TOPRATED and have them in the order they were downloaded(by pop or top)
        return context.getContentResolver().query(
                MovieEntry.CONTENT_URI,
                posterPathMovieIdColumns,
                selectionIsNotNull,
                null,
                orderByTypeIndex);
    }


    /**
     * MAKESETOFIDSFROMCURSOR - Create a Set of the IDs that are currently in the DB.
     * Will be used later to ensure proper updating when new data comes in.
     * No duplicates and proper updating.
     * @param idPos - Position of the id column
     * @param idAndTitleCursor - Cursor with old DB data
     * @param idOldSet - Set holding the IDs of the old DB movies.
     */
    public static void makeSetOfIdsFromCursor(int idPos, Cursor idAndTitleCursor, Set<Long> idOldSet) {
        // Make a Set of IDs you already have in DB.  Later, compare to incoming IDs
        if ((idAndTitleCursor != null) && (idAndTitleCursor.getCount() > 0)) {
            idAndTitleCursor.moveToFirst();
            do {
                idOldSet.add(idAndTitleCursor.getLong(idPos));
            } while (idAndTitleCursor.moveToNext());
        }
    }


    /**
     * GETSINGLEMOVIERUNTIMEFROMTMDB - Get the runtime for the movie with the given movieId.
     * Note: Makes network call.
     * @param aMovieId - Movie Id for movie we are getting the runtime for
     * @return int - Runtime of movie
     */
    private static int getSingleMovieRuntimeFromTMDB(String aMovieId, Context context) {
        int movieRuntime = 0;
//        Log.d(TAG, "BEGIN::getSingleMovieRuntimeFromTMDB: ");
        try {
            String receivedSingleMovieJSON = NetworkUtils
                    .getResponseFromHttpUrl(NetworkUtils
                            .buildUrlForSingleMovie(context, aMovieId));
//            Log.i(TAG, "getSingleMovieRuntimeFromTMDB: >>>" + receivedSingleMovieJSON + "<<<");

            movieRuntime = OpenTMDJsonUtils
                    .getRuntimeOfSingleMovie(receivedSingleMovieJSON);

//            Log.i(TAG, "getSingleMovieRuntimeFromTMDB: Runtime: " + movieRuntime + "\n");

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
//        Log.d(TAG, "END::getSingleMovieRuntimeFromTMDB: ");
        return movieRuntime;
    }


    /**
     * GETRUNTIMESFORMOVIESINLIST - Add the runtimes to the Movies that were just created from
     * JSON call that should precede this one.  Update database with runtimes
     * Note: Makes network call.  Calls DB
     * @param cursor - Holds data we need to look through
     */
    public static void getRuntimesForMoviesInList(Cursor cursor, Context context) {
//        Log.d(TAG, "BEGIN::getRuntimesForMoviesInList: ");
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
//        Log.d(TAG, "END::getRuntimesForMoviesInList: ");
    }


}
