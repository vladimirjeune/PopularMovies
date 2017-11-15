package app.com.vladimirjeune.popmovies.utilities;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created to communicate with the theMovieDb servers.
 * Created by vladimirjeune on 11/10/17.
 */

public final class NetworkUtils {
    public final static String TAG = NetworkUtils.class.getSimpleName();

    // Temp
    public final static String TEST_MOVIE_LIST = "testJSON";
    public final static String TEST_SINGLE_MOVIE = "testSingleMovieJSON";

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

//            Toast.makeText(this, mTMDKey, Toast.LENGTH_LONG).show();

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
