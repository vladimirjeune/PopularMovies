package app.com.vladimirjeune.popmovies.utilities;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;

import com.facebook.stetho.urlconnection.StethoURLConnectionManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.util.Scanner;

import app.com.vladimirjeune.popmovies.R;

/**
 * Created to communicate with the theMovieDb servers.
 * Created by vladimirjeune on 11/10/17.
 */

public final class NetworkUtils {
    private final static String TAG = NetworkUtils.class.getSimpleName();
    public static final String THEMOVIEDATABASE_KEY = "tmdb_key";
    public static final String YOUTUBE_DATA_V3_KEY = "youtube_v3_key";

    public final static String TMDB_BASE_URL = "https://api.themoviedb.org/3/movie";
    public final static String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p";
    public final static String YOUTUBE_BASE_URL = "https://www.youtube.com/";

    public final static String TMDB_POPULAR = "popular";
    public final static String TMDB_TOP_RATED = "top_rated";
    public final static String YOUTUBE_WATCH = "watch";

    public final static String YOUTUBE_VIDEO_PARAM = "v";

    // These are the values you want to use for our results.
    public final static String language = "en-US";
    public final static String page = "1";  // Not used for single MovieData

    // Queries
    public final static String TMDB_API_KEY = "api_key";  // Must get key from assets
    public final static String TMDB_LANGUAGE = "language";
    public final static String TMDB_PAGE = "page";
    private final static String TMDB_VIDEOS = "videos";
    private final static String TMDB_REVIEWS = "reviews";

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


    // TODO: Remove before submission
    private final static StethoURLConnectionManager stethoURLConnectionManager = new StethoURLConnectionManager(null);
    private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    private static final String GZIP_ENCODING = "gzip";

    /**
     * BUILDURLFORPOPULARORTOPRATED - The URLs for both endpoints is very similar.  Which one is picked
     * will be ultimately decided by what the user has set in his sort preferences.
     * @param context - Needed to obtain key
     * @param popularOrTop - String indicating what list the user wants
     * @return URL - URL that can be used to access appropriate JSON from theMovieDB
     */
    public static URL buildUrlForPopularOrTopRated(Context context, String popularOrTop) {

        // Favorites will not have URLs, and anything else is wrong
        if (
                ( ! popularOrTop.equals(context.getString(R.string.pref_sort_popular_value)))
                        && ( ! popularOrTop.equals(context.getString(R.string.pref_sort_top_rated_value)))
                ) {
            return null;
        }

        String whichEndpoint = TMDB_TOP_RATED;
        if (popularOrTop.equals(context.getString(R.string.pref_sort_default))) {
            whichEndpoint = TMDB_POPULAR;
        }

        Uri popularTopRatedUri = Uri.parse(TMDB_BASE_URL).buildUpon()
                .appendPath(whichEndpoint)
                .appendQueryParameter(TMDB_API_KEY, obtainKeyOfType(context, THEMOVIEDATABASE_KEY))
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
                .appendQueryParameter(TMDB_API_KEY, obtainKeyOfType(context, THEMOVIEDATABASE_KEY))
                .appendQueryParameter(TMDB_LANGUAGE, language)
                .build();
        try {
            URL singleMovieURL = new URL(singleMovieUri.toString());
            Log.d(TAG, "buildURLForSingleMovie() returned: " + movieId + "  [ " + singleMovieURL + " ]");
            Log.d(TAG, "END::buildUrlForSingleMovie: ");
            return singleMovieURL;
        } catch (MalformedURLException me) {
            me.printStackTrace();
            Log.d(TAG, "END::buildUrlForSingleMovie::MalformedURLException ");
            return null;
        }
    }


    /**
     * BUILDURLFORVIDEOS - Builds URL to get JSON for list of videos associated with inputted ID
     * @param context - Needed for some function calls
     * @param anId - ID of the movie to get information about
     * @return - URL - URL to access JSON for videos
     */
    public static URL buildURLforVideos(Context context, String anId) {
        Uri videosUri = Uri.parse(TMDB_BASE_URL)
                .buildUpon()
                .appendPath(anId)
                .appendPath(TMDB_VIDEOS)
                .appendQueryParameter(TMDB_API_KEY, obtainKeyOfType(context, THEMOVIEDATABASE_KEY))
                .appendQueryParameter(TMDB_LANGUAGE, language)
                .appendQueryParameter(TMDB_PAGE, page)
                .build();

        try {
            URL videosURL = new URL(videosUri.toString());
            Log.d(TAG, "buildURLforVideos() called with: context = [" + context + "], anId = [" + anId + "]"
            + "  returned: [" + videosURL + "]");
            return videosURL;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * BUILDURLFORYOUTUBE - Builds a URL for a Youtube video using the Key parameter.
     * @param key - ID for the Youtube video we want an URL for
     * @return - URL - For specific Youtube video
     */
    public static URL buildURLforYoutube(String key) {
        Uri youtubeUri = Uri.parse(YOUTUBE_BASE_URL)
                .buildUpon()
                .appendPath(YOUTUBE_WATCH)
                .appendQueryParameter(YOUTUBE_VIDEO_PARAM, key)
                .build();

        try {
            URL youtubeURL = new URL(youtubeUri.toString());
            Log.d(TAG, "buildURLforYoutube() called with result: " + youtubeURL);

            return youtubeURL;

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * BUILDURLFORREVIEWS - Builds the URL for the JSON of the Reviews for the given ID
     * @param context - Needed for some function calls
     * @param anId - ID of the movie for which we want the Reviews, if any are available
     * @return - URL - URL for the Reviews for the given ID
     */
    public static URL buildURLForReviews(Context context, String anId) {
        Uri reviewsURI = Uri.parse(TMDB_BASE_URL)
                .buildUpon()
                .appendPath(anId)
                .appendPath(TMDB_REVIEWS)
                .appendQueryParameter(TMDB_API_KEY, obtainKeyOfType(context, THEMOVIEDATABASE_KEY))
                .appendQueryParameter(TMDB_LANGUAGE, language)
                .appendQueryParameter(TMDB_PAGE, page)
                .build();

        try {
            URL reviewsURL = new URL(reviewsURI.toString());
            Log.d(TAG, "buildURLForReviews() called with: context = [" + context + "], anId = [" + anId + "]"
            + " returned [" + reviewsURL + "]");
            return reviewsURL;
        } catch (MalformedURLException e) {
            e.printStackTrace();
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
//            Log.i(TAG, "buildURLForImage: " + imageURL);
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
     *              of w342.
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
//            Log.i(TAG, "buildURLForImage: " + imageURL);
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
//        Log.d(TAG, "BEGIN::getResponseFromHttpUrl: ");
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
//            Log.d(TAG, "END::getResponseFromHttpUrl: ");
            urlConnection.disconnect();
        }
    }

    /**
     * OBTAINTMDKEY - Reads the apiTMD.key file in the assets directory so that we can
     * access: <a href="https://www.themoviedb.org/">https://www.themoviedb.org/</a> with
     * the assigned key.  Keys can be acquired at the site.  Then the string can be placed in
     * a file with the appropriate file name.
     * @param context - Needed for function calls
     * @param key - Type of key needed
     * @return - String: The TheMovieDb API Key needed to access the database.
     */
    public static String obtainKeyOfType(Context context, String key) {
        // In order for the movie requests to work we must obtain key from file in assets
        try {

            AssetManager assetManager = context.getAssets();  // File is kept in the asset folder
            Scanner scanner;


            if (key.equals(NetworkUtils.THEMOVIEDATABASE_KEY)) {
                scanner = new Scanner(assetManager.open("apiTmd.key"));
            } else {
                scanner = new Scanner(assetManager.open("youtubeDataAPI.key"));
            }

            return scanner.next();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException io ) {
            io.printStackTrace();
        }
        return null;
    }

    /**
     * DOWEHAVEINTERNET - Checks for internet access without asking for extra permissions
     * Should be run on a background thread.
     * @return boolean - Whether we have access or not
     */
    public static boolean doWeHaveInternet()  {
        try {
            final int timeoutInMilliseconds = 1500;
            final Socket socket = new Socket();
            final String hostname = "8.8.8.8";
            final int port = 53;

            final SocketAddress socketAddress = new InetSocketAddress(hostname, port);

            socket.connect(socketAddress, timeoutInMilliseconds);
            socket.close();
            return true;
        } catch (IOException e) {
            return false;  // We weren't able to make the connection
        }

    }


    // TODO: Remove before submission.  For Stetho
    private static void requestDecompression(HttpURLConnection conn) {
        conn.setRequestProperty(HEADER_ACCEPT_ENCODING, GZIP_ENCODING);
    }

}
