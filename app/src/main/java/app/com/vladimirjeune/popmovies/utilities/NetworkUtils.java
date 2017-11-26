package app.com.vladimirjeune.popmovies.utilities;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import app.com.vladimirjeune.popmovies.R;

/**
 * Created to communicate with the theMovieDb servers.
 * Created by vladimirjeune on 11/10/17.
 */

public final class NetworkUtils {
    public final static String TAG = NetworkUtils.class.getSimpleName();
    public final static String TMDB_BASE_URL = "https://api.themoviedb.org/3/movie";
    public final static String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p";

    public final static String TMDB_POPULAR = "popular";
    public final static String TMDB_TOP_RATED = "top_rated";

    // These are the values you want to use for our results.
    public final static String language = "en-US";
    public final static String page = "1";  // Not used for single MovieData

    // Queries
    public final static String TMDB_API_KEY = "api_key";  // Must get key from assets
    public final static String TMDB_LANGUAGE = "language";
    public final static String TMDB_PAGE = "page";

    // Sizes for Posters and Backdrops, not all sizes are used
    public final static String TMDB_IMAGE_W92 = "w92";
    public final static String TMDB_IMAGE_W154 = "w154";
    public final static String TMDB_IMAGE_W185 = "w185";
    public final static String TMDB_IMAGE_W342 = "w342";
    public final static String TMDB_IMAGE_W500 = "w500";
    public final static String TMDB_IMAGE_W780 = "w780";
    public final static String TMDB_IMAGE_ORIGINAL = "original";

    // Temp
    public final static String TEST_MOVIE_LIST = "testJSON";
    public final static String TEST_SINGLE_MOVIE = "testSingleMovieJSON";

    /**
     * BUILDURLFORPOPULARORTOPRATED - The URLs for both endpoints is very similar.  Which one is picked
     * will be ultimately decided by what the user has set in his sort preferences.
     * @param context - Needed to obtain key
     * @return URL - URL that can be used to access appropriate JSON from theMovieDB
     */
    public static URL buildUrlForPopularOrTopRated(Context context, String popularOrTop) {
        // There will eventually be a SharedPref to get which one it is.  But not yet

        String whichEndpoint = TMDB_TOP_RATED;
        if (popularOrTop.equals(context.getString(R.string.pref_sort_default))) {
            whichEndpoint = TMDB_POPULAR;
        }

        Uri popularTopRatedUri = Uri.parse(TMDB_BASE_URL).buildUpon()
                .appendPath(whichEndpoint)
                .appendQueryParameter(TMDB_API_KEY, obtainTMDKey(context))
                .appendQueryParameter(TMDB_LANGUAGE, language)
                .appendQueryParameter(TMDB_PAGE, page)
                .build();
        try {
            Log.d(TAG, "buildUrlForPopularOrTopRated: " + popularTopRatedUri.toString());
            URL popularTopRatedURL = new URL(popularTopRatedUri.toString());
            Log.d(TAG, "buildUrlForPopularOrTopRated() returned: " + popularTopRatedURL);
            return popularTopRatedURL;
        } catch (MalformedURLException me) {
            me.printStackTrace();
            Log.d(TAG, "buildUrlForPopularOrTopRated: MalformedURLException()");
            return null;
        }

    }

    /**
     * BUILDURLFORSINGLEMOVIE - Builds a URL to get the JSON for a single movie from theMovieDb.
     * @param context -
     * @param movieId - ID obtained from initial call of top-rated or popular movies.
     * @return URL - Properly formatted URL for a single movie with the appropriate id from the parameter list
     */
    public static URL buildUrlForSingleMovie(Context context, String movieId) {
        Log.d(TAG, "BEGIN::buildUrlForSingleMovie: ");
        Uri singleMovieUri = Uri.parse(TMDB_BASE_URL)
                .buildUpon()
                .appendPath(movieId)
                .appendQueryParameter(TMDB_API_KEY, obtainTMDKey(context))
                .appendQueryParameter(TMDB_LANGUAGE, language)
                .build();
        try {
            URL singleMovieURL = new URL(singleMovieUri.toString());
            Log.d(TAG, "buildURLForSingleMovie() returned: " + singleMovieURL);
            Log.d(TAG, "END::buildUrlForSingleMovie: ");
            return singleMovieURL;
        } catch (MalformedURLException me) {
            me.printStackTrace();
            Log.d(TAG, "END::buildUrlForSingleMovie::MalformedURLException ");
            return null;
        }
    }

    /**
     * BUILDURLFORIMAGE - Builds URL for an image from the inputted path.
     * @param aPath - Path that came from theMovieDb of the form "/{letter|number}+"
     * @return URL - URL to request correct image from theMovieDb
     */
    public static URL buildURLForImage(String aPath) {
        Uri imageUri = Uri.parse(TMDB_IMAGE_BASE_URL)
                .buildUpon()
                .appendPath(TMDB_IMAGE_W342)
                .appendEncodedPath(aPath)
                .build();
        try {
            URL imageURL = new URL(imageUri.toString());
            Log.i(TAG, "buildURLForImage: " + imageURL);
            return imageURL;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * BUILDURLFORIMAGE - Builds URL for an image from the inputted path at the
     * allowed size specified.
     * @param aPath - Path that came from theMovieDb of the form "/{letter|number}+"
     * @param aSize - One of the allowed size descriptors.  Can be accessed by
     *              NetworkUtils.TMDB_IMAGE_W+ or NetworkUtils.TMDB_IMAGE_ORIGINAL
     *              for the largest size.  If you want the default, you can
     *              call buildURLForImage(String), will call with default size
     *              of w185(185x278 for posters, 185x104 for backdrops).
     * @return URL - URL to request correct image from theMovieDb
     */
    public static URL buildURLForImageOfSize(String aPath, String aSize) {
        Uri imageUri = Uri.parse(TMDB_IMAGE_BASE_URL)
                .buildUpon()
                .appendPath(aSize)
                .appendPath(aPath)
                .build();
        try {
            URL imageURL = new URL(imageUri.toString());
            Log.i(TAG, "buildURLForImage: " + imageURL);
            return imageURL;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * GETRESPONSEFROMHTTPURL - This method returns the entire result from the HTTP response.
     *
     * @param url The URL to fetch the HTTP response from.
     * @return The contents of the HTTP response, null if no response
     * @throws IOException Related to network and stream reading
     */
    public static String getResponseFromHttpUrl(URL url) throws IOException {
        Log.d(TAG, "BEGIN::getResponseFromHttpUrl: ");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            String response = null;
            if (hasInput) {
                response = scanner.next();
            }
            scanner.close();
            return response;
        } finally {
            Log.d(TAG, "END::getResponseFromHttpUrl: ");
            urlConnection.disconnect();
        }
    }

    /**
     * OBTAINTMDKEY - Reads the apiTMD.key file in the assets directory so that we can
     * access: <a href="https://www.themoviedb.org/">https://www.themoviedb.org/</a> with
     * the assigned key.  Keys can be acquired at the site.  Then the string can be placed in
     * a file with the appropriate file name.
     * @return - String: The TheMovieDb API Key needed to access the database.
     */
    public static String obtainTMDKey(Context context) {
        // In order for the movie requests to work we must obtain key from file in assets
        try {

            AssetManager assetManager = context.getAssets();  // File is kept in the asset folder

            Scanner scanner = new Scanner(assetManager.open("apiTmd.key"));

            return scanner.next();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException io ) {
            io.printStackTrace();
        }
        return null;
    }

    /**
     * OBTAINTEMPJSON - A temporary function to present theMovieDb JSON.
     * @param context - Needed to get the File
     * @return - String - Example JSON from website
     */
    public static String obtainTempJSON(Context context, final String typeOfFile) {
        // In order for the movie requests to work we must obtain key from file in assets
        try {
            AssetManager assetManager = context.getAssets();  // File is kept in the asset folder

            Scanner scanner = new Scanner(assetManager.open(typeOfFile));

            StringBuilder retVal = new StringBuilder();
//            Toast.makeText(context, retVal, Toast.LENGTH_LONG).show();
            while (scanner.hasNextLine()) {
                retVal.append(scanner.nextLine());
            }

            return retVal.toString();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException io ) {
            io.printStackTrace();
        }
        return null;
    }



}
