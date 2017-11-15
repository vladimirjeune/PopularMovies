package app.com.vladimirjeune.popmovies.utilities;

import android.content.Context;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import app.com.vladimirjeune.popmovies.MovieData;

/**
 * Handles interactions with TheMovieDB's JSON data
 * Created by vladimirjeune on 11/10/17.
 */

public final class OpenTMDJsonUtils {

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

    /**
     * GETPOPULARORTOPJSON - Can be used to get data from JSON for, 'popular', or, 'top-rated'
     * since they have the same schema.
     * *Note* - Neither endpoint have movie runtime.  That will have to be obtained from somewhere
     * else.
     * @param context - Necessary if we need to use the Utility functions
     * @param tmdJSONStr - The JSON dat from the Database
     * @return - ArrayList of Movie data
     */
    public static MovieData[] getPopularOrTopJSON(Context context, String tmdJSONStr) throws JSONException {
        MovieData[] parsedMovieDataArray;

        JSONObject movieJSONObject = new JSONObject(tmdJSONStr);
        if (isThereDataError(context, movieJSONObject)) {  // If there is a error in data; abort processing.
            return null;
        }


        // Loop thru data
        JSONArray movieJsonArray = movieJSONObject.getJSONArray(TMD_RESULTS_LIST);
        parsedMovieDataArray = new MovieData[movieJsonArray.length()];


        for (int i = 0; i < movieJsonArray.length(); i++) {
            MovieData movieData = new MovieData();

            JSONObject movieJson = movieJsonArray.getJSONObject(i);

            movieData.setMovieId(movieJson.getInt(TMD_MOVIE_ID));

            movieData.setOriginalTitle(movieJson.getString(TMD_ORIGINAL_TITLE));

            movieData.setPosterPath(movieJson.getString(TMD_POSTER_PATH));

            movieData.setSynopsis(movieJson.getString(TMD_SYNOPSIS));

            // MMMM-YY-DD.  May save as MMMM-YY-DD 00:00:00
            movieData.setReleaseDate(movieJson.getString(TMD_RELEASE_DATE));

            movieData.setVoterAverage(movieJson.getDouble(TMD_VOTER_AVERAGE));

            movieData.setBackdropPath(movieJson.getString(TMD_BACKDROP_PATH));

            movieData.setPopularity(movieJson.getDouble(TMD_POPULARITY));

            // Set mostly complete new movie in position
            parsedMovieDataArray[i] = movieData;

        }

        return parsedMovieDataArray;
    }

    /**
     * ISTHEREDATAERROR - If there is a status code in the JSON then there was a problem with the
     * data returned. Either the data is missing, or you do not have an API Key to access the information
     * from theMovieDb.com.
     * @param context - Needed to indicate to tester that they need to remember API Key
     * @param movieJSONObject - Searched for Status tag.
     * @return boolean - T means there is an error in the incoming data.  F means parsing of data can continue
     * @throws JSONException
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

    // Need a function to parse Movie JSON for Detail runtime.
    // If it has to be done in certain order, then make private
    /**
     * GETRUNTIMROFSINGLEMOVIE - Returns the runtime of a single movie whose info was obtained from the
     * movies endpoint.  Not the popular/ or toprated/ endpoints.  Those endpoints do not have runtime
     * in their schema.
     * @param context - Needed to access some functions
     * @param singleMovieJSONStr - JSON from individual movie from movie endpoint
     * @return int - Runtime of movie
     * @throws JSONException
     */
    public static int getRuntimeOfSingleMovie(Context context, String singleMovieJSONStr) throws JSONException {
        int runtimeOfMovie;

        JSONObject singleMovieJSON = new JSONObject(singleMovieJSONStr);

        runtimeOfMovie = singleMovieJSON.getInt(TMD_RUNTIME);

        return runtimeOfMovie;
    }

}
