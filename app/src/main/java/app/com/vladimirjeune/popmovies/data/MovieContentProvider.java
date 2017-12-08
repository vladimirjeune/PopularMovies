package app.com.vladimirjeune.popmovies.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * ContentProvider for Movie Data
 * Created by vladimirjeune on 12/6/17.
 */

public class MovieContentProvider extends ContentProvider {

    private MovieDBHelper mMovieDBHelper;

    // content://<package>/path/#
    public static final String ANYNUMBER = "/#";  // Should match any movie id.
    public static final int MOVIES = 100;
    public static final int MOVIES_WITH_ID = 101;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    @Override
    public boolean onCreate() {
        Context context = getContext();
        mMovieDBHelper = new MovieDBHelper(context);  // Need this to help with DB

        return true;  // Because we are now initialised
    }

    /**
     * BUILDURIMATCHER - Associates Uris with their int match
     * @return UriMatcher - Loaded up with the proper Uris to match against
     */
    public static UriMatcher buildUriMatcher() {
        final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        uriMatcher.addURI(MovieContract.CONTENT_AUTHORITY, MovieContract.PATH_MOVIES, MOVIES);
        uriMatcher.addURI(MovieContract.CONTENT_AUTHORITY, MovieContract.PATH_MOVIES + ANYNUMBER
                , MOVIES_WITH_ID);

        return uriMatcher;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] strings
            , @Nullable String s, @Nullable String[] strings1, @Nullable String s1) {





        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues
            , @Nullable String s, @Nullable String[] strings) {
        return 0;
    }
}
