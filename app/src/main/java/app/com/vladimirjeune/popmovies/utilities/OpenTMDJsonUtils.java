package app.com.vladimirjeune.popmovies.utilities;

import android.content.ContentValues;
import android.content.Context;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import app.com.vladimirjeune.popmovies.data.MovieContract.MovieEntry;
import app.com.vladimirjeune.popmovies.data.MovieContract.ReviewEntry;
import app.com.vladimirjeune.popmovies.data.MovieContract.YoutubeEntry;


/**
 * Handles interactions with TheMovieDB's JSON data
 * Created by vladimirjeune on 11/10/17.
 */

public final class OpenTMDJsonUtils {

    private static final String TAG = OpenTMDJsonUtils.class.getSimpleName();
    // These may be needed for the Main page
    private static final String TMD_MOVIE_ID = "id";
    private static final String TMD_ORIGINAL_TITLE = "original_title";
    private static final String TMD_POSTER_PATH = "poster_path";  // Can be NULL

    // These will be needed for Details
    private static final String TMD_SYNOPSIS = "overview";
    private static final String TMD_RELEASE_DATE = "release_date";
    private static final String TMD_VOTER_AVERAGE = "vote_average";
    private static final String TMD_BACKDROP_PATH = "backdrop_path";  // Can be NULL
    private static final String TMD_POPULARITY = "popularity";

    // Requires a call to different endpoint, once we know movieId.
    private static final String TMD_RUNTIME = "runtime";

    // Most pertinent information is in the results array
    private static final String TMD_RESULTS_LIST = "results";

    // Status 7: Invalid Key, get Dialog?; Status 34: Resource not found, throw exception.
    private static final String TMD_STATUS_CODE = "status_code";
    private static final String TMD_STATUS_MESSAGE = "status_message";

    private static final int TMD_ERROR_STATUS_NO_KEY = 7;
    private static final int TMD_ERROR_STATUS_RESOURCE_NOT_FOUND = 34;

    private static final String TMD_REVIEW_RESULTS_LIST = "results";
    private static final String TMD_REVIEW_ID = "id";
    private static final String TMD_REVIEW_MOVIE_ID = "movie_id";
    private static final String TMD_REVIEW_AUTHOR = "author";
    private static final String TMD_REVIEW_CONTENT = "content";
    private static final String TMD_REVIEW_URL = "url";

    private static final String TMD_YOUTUBE_RESULTS_LIST = "results";
    private static final String TMD_YOUTUBE_ID = "id";
    private static final String TMD_YOUTUBE_MOVIE_ID = "movie_id";
    private static final String TMD_YOUTUBE_KEY = "key";
    private static final String TMD_YOUTUBE_NAME = "name";
    private static final String TMD_YOUTUBE_TUBE = "site";
    private static final String TMD_YOUTUBE_SIZE = "size";
    private static final String TMD_YOUTUBE_TYPE = "type";


    /**
     * GETPOPULARORTOPJSON - Can be used to get data from JSON for, 'popular', or, 'top-rated'
     * since they have the same schema.
     * *Note* - Neither endpoint have movie runtime.  That will have to be obtained from somewhere
     * else.
     * @param context - Necessary if we need to use the Utility functions
     * @param tmdJSONStr - The JSON data from the Database
     * @param isPopular - Whether this is a JSON for Popular or Top-Rated
     * @return - ArrayList of Movie data
     */
    public static ContentValues[] getPopularOrTopJSONContentValues(Context context, String tmdJSONStr
            , boolean isPopular) throws JSONException {

        ContentValues[] parsedContentValuesArray;
//        Log.d(TAG, "BEGIN::getPopularOrTopJSON: ");

        JSONObject movieJSONObject = new JSONObject(tmdJSONStr);
        if (isThereDataError(context, movieJSONObject)) {  // If there is a error in data; abort processing.
            return null;
        }


        // Loop thru data
        JSONArray movieJsonArray = movieJSONObject.getJSONArray(TMD_RESULTS_LIST);

        parsedContentValuesArray = new ContentValues[movieJsonArray.length()];

        for (int i = 0; i < movieJsonArray.length(); i++) {

            JSONObject movieJson = movieJsonArray.getJSONObject(i);

            int movieId = movieJson.getInt(TMD_MOVIE_ID);

            String movieOriginalTitle = movieJson.getString(TMD_ORIGINAL_TITLE);

            String moviePosterPath = movieJson.getString(TMD_POSTER_PATH);
            if (moviePosterPath != null) {
                moviePosterPath = moviePosterPath.substring(1);  // Removing preceding '/'
            }  // So, NULL will make it to the database

            String movieSynopsis = movieJson.getString(TMD_SYNOPSIS);

            // MMMM-YY-DD.  May save as MMMM-YY-DD 00:00:00
            String movieReleaseDate = movieJson.getString(TMD_RELEASE_DATE);

            Double movieVoterAverage = movieJson.getDouble(TMD_VOTER_AVERAGE);

            String movieBackdropPath = movieJson.getString(TMD_BACKDROP_PATH);  // Nulls will propagete to DB
            if (movieBackdropPath != null) {
                movieBackdropPath = movieBackdropPath.substring(1);  // Removing preceding slash
            }

            Double moviePopularity = movieJson.getDouble(TMD_POPULARITY);

            ContentValues contentValues = new ContentValues();
            contentValues.put(MovieEntry._ID, movieId);
            contentValues.put(MovieEntry.ORIGINAL_TITLE, movieOriginalTitle);
            contentValues.put(MovieEntry.SYNOPSIS, movieSynopsis);
            contentValues.put(MovieEntry.RELEASE_DATE, movieReleaseDate);
            contentValues.put(MovieEntry.VOTER_AVERAGE, movieVoterAverage);
            contentValues.put(MovieEntry.POPULARITY, moviePopularity);

            conditionalOptions(isPopular, i, moviePosterPath, movieBackdropPath, contentValues);

            // Set mostly complete new movie in position
            parsedContentValuesArray[i] = contentValues;

        }

//        Log.d(TAG, "END::getPopularOrTopJSON: ");
        return parsedContentValuesArray;
    }


    /**
     * CONDITIONALOPTIONS - These values may or may not be available to be
     * incorporated into the database.  They may be NULL or not suitable for this database call.
     * @param isPopular - boolean - Whether this is for Popular or Top-Rated
     * @param index - int - Index of this movie as returned from tmdb
     * @param moviePosterPath - Path to movie onesheet on server.  Can be NULL
     * @param movieBackdropPath - Path to movie backdrop on server.  Can be NULL
     * @param contentValues - Values will be placed in here if present
     */
    private static void conditionalOptions(boolean isPopular, int index, String moviePosterPath
            , String movieBackdropPath, ContentValues contentValues) {
        if (moviePosterPath != null) {
            contentValues.put(MovieEntry.POSTER_PATH, moviePosterPath);  // Can be NULL
        }

        if (movieBackdropPath != null) {
            contentValues.put(MovieEntry.BACKDROP_PATH, movieBackdropPath);
        }

        // If this is Popular the order in popularity will be set.  Otherwise; Top-Rated
        if (isPopular) {
            contentValues.put(MovieEntry.POPULAR_ORDER_IN, index);
        } else {  // TODO: Make else if Top_Rated
            contentValues.put(MovieEntry.TOP_RATED_ORDER_IN, index);
        }
    }


    /**
     * ISTHEREDATAERROR - If there is a status code in the JSON then there was a problem with the
     * data returned. Either the data is missing, or you do not have an API Key to access the information
     * from theMovieDb.com.
     * @param context - Needed to indicate to tester that they need to remember API Key
     * @param movieJSONObject - Searched for Status tag.
     * @return boolean - T means there is an error in the incoming data.  F means parsing of data can continue
     * @throws JSONException - Something was wrong with the JSON
     */
    private static boolean isThereDataError(Context context, JSONObject movieJSONObject) throws JSONException {
        // Check that we are not in a bad status
        if (movieJSONObject.has(TMD_STATUS_CODE)) {
            int errorCode = movieJSONObject.getInt(TMD_STATUS_CODE);

            String errorMessage = "";
            switch (errorCode) {
                case TMD_ERROR_STATUS_NO_KEY:
                    errorMessage = movieJSONObject.getString(TMD_STATUS_MESSAGE);
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
                    return true;
                case TMD_ERROR_STATUS_RESOURCE_NOT_FOUND:
                    return true;
                default:
                    /*Server may be down*/
                    return true;
            }
        }
        return false;
    }


    /**
     * GETRUNTIMROFSINGLEMOVIE - Returns the runtime of a single movie whose info was obtained from the
     * movies endpoint.  Not the popular/ or toprated/ endpoints.  Those endpoints do not have runtime
     * in their schema.
     * @param singleMovieJSONStr - JSON from individual movie from movie endpoint
     * @return int - Runtime of movie
     * @throws JSONException
     */
    static int getRuntimeOfSingleMovie(String singleMovieJSONStr) throws JSONException {
        int runtimeOfMovie;

        JSONObject singleMovieJSON = new JSONObject(singleMovieJSONStr);

        runtimeOfMovie = singleMovieJSON.optInt(TMD_RUNTIME);  // optInt used so if does not exists or is null returns 0

        return runtimeOfMovie;
    }


    /**
     * GETREVIEWCONTENTVALUES - Get ContentValues for the Reviews of the movies of this ValueType
     * @param context - Needed for function calls
     * @param tmdReviewJSONStr - JSON for Reviews
     * @return - ContentValues[] of Reviews; may be 0 length, or null if there was an issue
     * @throws JSONException
     */
    public static ContentValues[] getReviewContentValues(Context context, long tmdMovieId,
                                                         String tmdReviewJSONStr ) throws JSONException {
        ContentValues[] parsedContentValuesArray;

        JSONObject reviewJSONObject = new JSONObject(tmdReviewJSONStr);

        if (isThereDataError(context, reviewJSONObject)) {
            return null;
        }

        JSONArray reviewJsonArray = reviewJSONObject.getJSONArray(TMD_REVIEW_RESULTS_LIST);

        parsedContentValuesArray = new ContentValues[reviewJsonArray.length()];

        for (int i = 0; i < reviewJsonArray.length(); i++) {

            JSONObject reviewJson = reviewJsonArray.getJSONObject(i);

            String reviewId = reviewJson.getString(TMD_REVIEW_ID);

            String reviewAuthor = reviewJson.getString(TMD_REVIEW_AUTHOR);

            String reviewContent = reviewJson.getString(TMD_REVIEW_CONTENT);

            String reviewURL = reviewJson.getString(TMD_REVIEW_URL);

            ContentValues contentValues = new ContentValues();

            contentValues.put(ReviewEntry.REVIEW_ID, reviewId);  // Actual TMDb ID, Our DB _id is autoincrement
            contentValues.put(ReviewEntry.MOVIE_ID, tmdMovieId);  // FK from movie database; passed in
            contentValues.put(ReviewEntry.AUTHOR, reviewAuthor);
            contentValues.put(ReviewEntry.CONTENT, reviewContent);
            contentValues.put(ReviewEntry.URL, reviewURL);

            parsedContentValuesArray[i] = contentValues;

        }

        return parsedContentValuesArray;

    }



    /**
     * GETYOUTUBECONTENTVALUES - Get ContentValues for the Youtubes of the movies of this ValueType
     * @param context - Needed for function calls
     * @param tmdYoutubeJSONStr - JSON for Reviews
     * @return - ContentValues[] of Reviews; may be 0 length, or null if there was an issue
     * @throws JSONException - Problem with JSON
     */
    public static ContentValues[] getYoutubeContentValues(Context context, long tmdMovieId,
                                                         String tmdYoutubeJSONStr ) throws JSONException {
        ContentValues[] parsedContentValuesArray;

        JSONObject youtubeJSONObject = new JSONObject(tmdYoutubeJSONStr);

        if (isThereDataError(context, youtubeJSONObject)) {
            return null;
        }

        JSONArray youtubeJSONArray = youtubeJSONObject.getJSONArray(TMD_YOUTUBE_RESULTS_LIST);

        parsedContentValuesArray = new ContentValues[youtubeJSONArray.length()];

        for (int i = 0; i < youtubeJSONArray.length(); i++) {

            JSONObject youtubeJsonObject = youtubeJSONArray.getJSONObject(i);

            String youtubeId = youtubeJsonObject.getString(TMD_YOUTUBE_ID);

            String youtubeKey = youtubeJsonObject.getString(TMD_YOUTUBE_KEY);

            String youtubeName = youtubeJsonObject.getString(TMD_YOUTUBE_NAME);

            String youtubeSite = youtubeJsonObject.getString(TMD_YOUTUBE_TUBE);  // If other than YTube, can't connect

            String youtubeSize = youtubeJsonObject.getString(TMD_YOUTUBE_SIZE);

            String youtubeType = youtubeJsonObject.getString(TMD_YOUTUBE_TYPE);

            ContentValues contentValues = new ContentValues();

            contentValues.put(YoutubeEntry.YOUTUBE_ID, youtubeId);  // Actual TMDb ID, Our DB _id is autoincrement
            contentValues.put(YoutubeEntry.MOVIE_ID, tmdMovieId);  // FK from movie database; passed in
            contentValues.put(YoutubeEntry.KEY, youtubeKey);
            contentValues.put(YoutubeEntry.NAME, youtubeName);
            contentValues.put(YoutubeEntry.TUBE, youtubeSite);
            contentValues.put(YoutubeEntry.SIZE, youtubeSize);
            contentValues.put(YoutubeEntry.TYPE, youtubeType);

            parsedContentValuesArray[i] = contentValues;

        }

        return parsedContentValuesArray;

    }

}
