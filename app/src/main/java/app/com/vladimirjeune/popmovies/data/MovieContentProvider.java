package app.com.vladimirjeune.popmovies.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import app.com.vladimirjeune.popmovies.data.MovieContract.MovieEntry;
import app.com.vladimirjeune.popmovies.data.MovieContract.ReviewEntry;
import app.com.vladimirjeune.popmovies.data.MovieContract.YoutubeEntry;


/**
 * ContentProvider for Movie Database
 * Created by vladimirjeune on 12/6/17.
 */


public class MovieContentProvider extends ContentProvider {

    private static final String TAG = MovieContentProvider.class.getSimpleName();
    private MovieDBHelper mMovieDBHelper;

    // content://<package>/path/#
    private static final String ANYNUMBER = "/#";  // Should match any movie id.
    public static final int MOVIES = 100;
    public static final int MOVIES_WITH_ID = 101;
    public static final int REVIEWS = 200;
    public static final int REVIEWS_WITH_ID = 201;
    public static final int JOIN_REVIEWS_MOVIES = 299;  // Join with REVIEWS
    public static final int YOUTUBES = 300;
    public static final int YOUTUBES_WITH_ID = 301;
    public static final int JOIN_YOUTUBES_MOVIES = 399;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final String mFreeParameter = " = ? ";

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
        uriMatcher.addURI(MovieContract.CONTENT_AUTHORITY, MovieContract.PATH_REVIEWS, REVIEWS);
        uriMatcher.addURI(MovieContract.CONTENT_AUTHORITY, MovieContract.PATH_REVIEWS + ANYNUMBER
                , REVIEWS_WITH_ID);

        uriMatcher.addURI(MovieContract.CONTENT_AUTHORITY, MovieContract.PATH_JOIN_REVIEWS_MOVIES, JOIN_REVIEWS_MOVIES);

        uriMatcher.addURI(MovieContract.CONTENT_AUTHORITY, MovieContract.PATH_YOUTUBE, YOUTUBES);
        uriMatcher.addURI(MovieContract.CONTENT_AUTHORITY, MovieContract.PATH_YOUTUBE + ANYNUMBER
                , YOUTUBES_WITH_ID);

        uriMatcher.addURI(MovieContract.CONTENT_AUTHORITY, MovieContract.PATH_JOIN_YOUTUBE_MOVIES, JOIN_YOUTUBES_MOVIES);

        return uriMatcher;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection
            , @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String orderBy) {

        Cursor retCursor = null;

        switch (sUriMatcher.match(uri)) {
            case MOVIES:
                retCursor = mMovieDBHelper.getReadableDatabase()
                        .query(MovieEntry.TABLE_NAME
                                , projection
                                , selection
                                , selectionArgs
                                , null
                                , null
                                , orderBy);
                break;
            case MOVIES_WITH_ID:
                long idSelection = Long.parseLong(uri.getLastPathSegment());
                String[] idArgs = {String.valueOf(idSelection)};

                retCursor = mMovieDBHelper.getReadableDatabase()
                        .query(MovieEntry.TABLE_NAME
                        , projection
                        , MovieEntry._ID + mFreeParameter
                        , idArgs
                        , null
                        , null
                        , orderBy);
                break;
            case REVIEWS:
                retCursor = mMovieDBHelper.getReadableDatabase()
                        .query(ReviewEntry.TABLE_NAME
                                , projection
                                , selection
                                , selectionArgs
                                , null
                                , null
                                , orderBy);
                break;
            case REVIEWS_WITH_ID:
                long idReviewSelection = Long.parseLong(uri.getLastPathSegment());
                String[] idReviewArgs = {String.valueOf(idReviewSelection)};

                retCursor = mMovieDBHelper.getReadableDatabase()
                        .query(ReviewEntry.TABLE_NAME
                                , projection
                                , ReviewEntry._ID + mFreeParameter
                                , idReviewArgs
                                , null
                                , null
                                , orderBy);
                break;
            case JOIN_REVIEWS_MOVIES:
                retCursor = queryReviewJoin(uri, projection, selection, selectionArgs, orderBy);
                break;
            case YOUTUBES:
                retCursor = mMovieDBHelper.getReadableDatabase()
                        .query(YoutubeEntry.TABLE_NAME
                                , projection
                                , selection
                                , selectionArgs
                                , null
                                , null
                                , orderBy);
                break;
            case YOUTUBES_WITH_ID:
                long idYoutubeSelection = Long.parseLong(uri.getLastPathSegment());
                String[] idYoutubeArgs = {String.valueOf(idYoutubeSelection)};

                retCursor = mMovieDBHelper.getReadableDatabase()
                        .query(YoutubeEntry.TABLE_NAME
                                , projection
                                , YoutubeEntry._ID + mFreeParameter
                                , idYoutubeArgs
                                , null
                                , null
                                , orderBy);
                break;
            case JOIN_YOUTUBES_MOVIES:
                retCursor = queryYoutubeJoin(uri, projection, selection, selectionArgs, orderBy);
                break;
            default:
                throw new UnsupportedOperationException("Uri not recognized <" + uri + ">");
        }

        retCursor.setNotificationUri(getContext().getContentResolver(), uri);

        return retCursor;
    }


    /**
     * QUERYREVIEWJOIN - Used to make query against joined Review+Movie table
     * @param uri - If there was a specific Id; we could extract it from the URI
     * @param projection - What to return
     * @param selection - Where
     * @param selectionArgs - Where arguments, used to keep from SQL Injection
     * @param orderBy - How to return results
     * @return - Cursor - Of joined table query
     */
    private Cursor queryReviewJoin(@NonNull Uri uri, @Nullable String[] projection,
                                   @Nullable String selection, @Nullable String[] selectionArgs,
                                   @Nullable String orderBy) {
        SQLiteQueryBuilder sqLiteQueryBuilder = new SQLiteQueryBuilder();
        String movieAlias = "movie_entry";  // No spaces in case of need of '.'
        String reviewAlias = "review_entry";

        sqLiteQueryBuilder.setTables(
                MovieEntry.TABLE_NAME + " AS " +
                movieAlias +
                " INNER JOIN " + ReviewEntry.TABLE_NAME
                + " AS " +
                reviewAlias +
                " ON ( " +
                movieAlias +
                "."
                + MovieEntry._ID + " = " +
                reviewAlias +
                "."
                + ReviewEntry.MOVIE_ID
                + " ) ");

        return sqLiteQueryBuilder.query(
                mMovieDBHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                orderBy);

    }

    /**
     * QUERYYOUTUBEJOIN - Used to make query against joined Youtube+Movie table
     * @param uri - If there was a specific Id; we could extract it from the URI
     * @param projection - What to return
     * @param selection - Where
     * @param selectionArgs - Where arguments, used to keep from SQL Injection
     * @param orderBy - How to return results
     * @return - Cursor - Of joined table query
     */
    private Cursor queryYoutubeJoin(@NonNull Uri uri, @Nullable String[] projection,
                                   @Nullable String selection, @Nullable String[] selectionArgs,
                                   @Nullable String orderBy) {
        SQLiteQueryBuilder sqLiteQueryBuilder = new SQLiteQueryBuilder();
        String movieAlias = "movie_entry";  // No spaces in case of need of '.'
        String youtubeAlias = "youtube_entry";

        sqLiteQueryBuilder.setTables(
                MovieEntry.TABLE_NAME + " AS " +
                movieAlias +
                " INNER JOIN " + YoutubeEntry.TABLE_NAME
                + " AS " +
                youtubeAlias +
                " ON ( " +
                movieAlias +
                "."
                + MovieEntry._ID + " = " +
                youtubeAlias +
                "."
                + YoutubeEntry.MOVIE_ID
                + " ) ");

        return sqLiteQueryBuilder.query(
                mMovieDBHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                orderBy);

    }

    /**
     * GETTYPE - For completeness.  getType() handles requests for the MIME type of data
     * We are working with two types of data:
     *  1) a directory and 2) a single row of data.
     * This method will not be used in our app, but gives a way to standardize the data formats
     * that your provider accesses, and this can be useful for data organization.
     * For now, this method will not be used but will be provided for completeness.
     * @param uri
     * @return
     */
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {

        switch (sUriMatcher.match(uri)) {
            case MOVIES:
                return "vnd.android.cursor.dir" + "/" + MovieContract.CONTENT_AUTHORITY + "/"
                        + MovieContract.PATH_MOVIES;
            case MOVIES_WITH_ID:
                return "vnd.android.cursor.item" + "/" + MovieContract.CONTENT_AUTHORITY + "/"
                        + MovieContract.PATH_MOVIES;
            case REVIEWS:
                return "vnd.android.cursor.dir" + "/" + MovieContract.CONTENT_AUTHORITY + "/"
                        + MovieContract.PATH_REVIEWS;
            case REVIEWS_WITH_ID:
                return "vnd.android.cursor.item" + "/" + MovieContract.CONTENT_AUTHORITY + "/"
                        + MovieContract.PATH_REVIEWS;
            default:
                throw new UnsupportedOperationException("Unknown Uri: " + uri);
        }

    }

    /**
     * INSERT - Insert what is in ContentValues into the DB
     * @param uri - The Uri to table we want to insert into.
     * @param contentValues - Holds data we want to insert into DB
     * @return - Uri - If successful; it will be the Uri passed in with the new ID appended.
     * -1 if unsuccessful.
     */
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {

        Uri retUri = null;

        switch (sUriMatcher.match(uri)) {
            case MOVIES:
                long newId = mMovieDBHelper.getWritableDatabase().insert(MovieEntry.TABLE_NAME,
                        null,
                        contentValues);

                if (newId != -1) {  // If no error, then return the amended Uri
                    retUri = uri.buildUpon().appendPath("" + newId).build();
//                    Log.d(TAG, "insert: Uri: " + retUri);
                } else {
                    throw new SQLException("Insert failed for Uri: " + uri + ": " + contentValues);  // If null, should just print it out.
                }
                break;
            case REVIEWS:
                long newReviewId = mMovieDBHelper.getWritableDatabase().insert(ReviewEntry.TABLE_NAME,
                        null,
                        contentValues);

                if (newReviewId != -1) {  // If no error, then return the amended Uri
                    retUri = uri.buildUpon().appendPath("" + newReviewId).build();
                } else {
                    throw new SQLException("Insert failed for Uri: " + uri);
                }
                break;
            case YOUTUBES:
                long newYoutubeId = mMovieDBHelper.getWritableDatabase().insert(YoutubeEntry.TABLE_NAME,
                        null,
                        contentValues);

                if (newYoutubeId != -1) {  // If no error, then return the amended Uri
                    retUri = uri.buildUpon().appendPath("" + newYoutubeId).build();
                } else {
                    throw new SQLException("Insert failed for Uri: " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown Uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);  // Important, so Resolver knows to update DB and UI

        return retUri;
    }


    /**
     * BULKINSERT - Inserts a group of ContentValues into the database
     * @param uri - Uri for the table to insert into
     * @param contentValues - Array of data to be inserted
     * @return - int - Number of rows that were successfully inserted
     */
    @Nullable
    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] contentValues) {

        int rowsInserted = 0;
        final SQLiteDatabase movieDb = mMovieDBHelper.getWritableDatabase();  // Get DB

        switch (sUriMatcher.match(uri)) {  // You would only BulkInsert into a table
            case MOVIES:
                movieDb.beginTransaction();
                try {

                    for (int i = 0; i < contentValues.length; i++) {
                        long id = movieDb.insert(MovieEntry.TABLE_NAME, null, contentValues[i]);

                        if (id != -1) {
                            rowsInserted++;
                        }
                    }

                    movieDb.setTransactionSuccessful();
                } finally {
                    movieDb.endTransaction();
                }
                if (rowsInserted > 0) {
                    getContext().getContentResolver().notifyChange(uri, null);  // Important for CursorLoader
                }
                return rowsInserted;
            case REVIEWS:
                movieDb.beginTransaction();
                Log.d(TAG, "bulkInsert() REVIEW: called with: uri = [" + uri + "], contentValues = [" + contentValues + "]");
                try {

                    for (int i = 0; i < contentValues.length; i++) {
                        long id = movieDb.insert(ReviewEntry.TABLE_NAME, null, contentValues[i]);

                        if (id != -1) {
                            rowsInserted++;
                        }
                    }

                    movieDb.setTransactionSuccessful();
                } finally {
                    movieDb.endTransaction();
                }
                if (rowsInserted > 0) {
                    getContext().getContentResolver().notifyChange(uri, null);  // Important for CursorLoader
                }
                return rowsInserted;
            case YOUTUBES:
                movieDb.beginTransaction();
                Log.d(TAG, "bulkInsert() YOUTUBE: called with: uri = [" + uri + "], contentValues = [" + contentValues + "]");
                try {

                    for (int i = 0; i < contentValues.length; i++) {
                        long id = movieDb.insert(YoutubeEntry.TABLE_NAME, null, contentValues[i]);

                        if (id != -1) {
                            rowsInserted++;
                        }
                    }

                    movieDb.setTransactionSuccessful();
                } finally {
                    movieDb.endTransaction();
                }
                if (rowsInserted > 0) {
                    getContext().getContentResolver().notifyChange(uri, null);  // Important for CursorLoader
                }
                return rowsInserted;
            default:  // Just run the normal one
                return super.bulkInsert(uri, contentValues);
        }

    }

    /**
     * DELETE - Deletes either a single row, or the entire table depending on the Uri
     * @param uri - Describes whether to delete an row or the table depending on its form
     * @param selection - Where, but that should be given in the Uri
     * @param selectionArgs - Where args, but that should be given in the Uri
     * @return int - Rows deleted successfully.
     */
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        // Need count of rows deleted, and will delete whole table and single rows.
        int rowsDeleted = 0;

            // This is the only way we can delete the table and get rows back.
            // According to documentation
            if (null == selection) {
                selection = "1";
            }

        switch (sUriMatcher.match(uri)) {

            // Deleting entire table will delete both types.  Be careful
            case MOVIES:
                rowsDeleted = mMovieDBHelper.getWritableDatabase()
                        .delete(MovieEntry.TABLE_NAME
                                , selection
                                , selectionArgs);  // Want to delete everything
                break;
            case MOVIES_WITH_ID:
                long movieId = Long.parseLong(uri.getLastPathSegment());

                rowsDeleted = mMovieDBHelper.getWritableDatabase()
                        .delete(MovieEntry.TABLE_NAME
                                , MovieEntry._ID + mFreeParameter
                                , new String[] {"" + movieId});
                break;
            case REVIEWS:
                rowsDeleted = mMovieDBHelper.getWritableDatabase()
                        .delete(ReviewEntry.TABLE_NAME
                                , selection
                                , selectionArgs);  // Want to delete everything
                break;
            case REVIEWS_WITH_ID:
                long reviewId = Long.parseLong(uri.getLastPathSegment());  // Autoincrement ID, not the same as Real ID from web

                rowsDeleted = mMovieDBHelper.getWritableDatabase()
                        .delete(ReviewEntry.TABLE_NAME
                                , ReviewEntry._ID + mFreeParameter
                                , new String[] {"" + reviewId});
                break;
            case YOUTUBES:
                rowsDeleted = mMovieDBHelper.getWritableDatabase()
                        .delete(YoutubeEntry.TABLE_NAME
                                , selection
                                , selectionArgs);  // Want to delete everything
                break;
            case YOUTUBES_WITH_ID:
                long youtubeId = Long.parseLong(uri.getLastPathSegment());  // Autoincrement ID, not the same as Real ID from web

                rowsDeleted = mMovieDBHelper.getWritableDatabase()
                        .delete(YoutubeEntry.TABLE_NAME
                                , YoutubeEntry._ID + mFreeParameter
                                , new String[] {"" + youtubeId});
                break;
            default:
                throw new UnsupportedOperationException("Unknown Uri: " + uri);

        }

        // Very important for Loader
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    /**
     * UPDATE - Updates a single row by the id in the Uri
     * @param uri - Indicates what row to update
     * @param contentValues - Map of keys to values of things to be updated
     * @param selection - Where clause criterion
     * @param selectionArgs - Actual value of parameter to match in Where clause
     * @return - int - Number of rows that were updated.
     */
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues
            , @Nullable String selection, @Nullable String[] selectionArgs) {


        int rowsUpdated = 0;

        switch (sUriMatcher.match(uri)) {
            case MOVIES_WITH_ID:
                long updateId = Long.parseLong(uri.getLastPathSegment());
                rowsUpdated = mMovieDBHelper.getWritableDatabase().update(
                        MovieEntry.TABLE_NAME
                        ,contentValues
                        ,MovieEntry._ID + mFreeParameter
                        ,new String[] {"" + updateId});
                break;
            case MOVIES:
                rowsUpdated = mMovieDBHelper.getWritableDatabase().update(
                        MovieEntry.TABLE_NAME,
                        contentValues,
                        selection,
                        selectionArgs
                );
                break;
            case REVIEWS_WITH_ID:
                long updateReviewId = Long.parseLong(uri.getLastPathSegment());
                rowsUpdated = mMovieDBHelper.getWritableDatabase().update(
                        ReviewEntry.TABLE_NAME
                        ,contentValues
                        ,ReviewEntry._ID + mFreeParameter
                        ,new String[] {"" + updateReviewId});
                break;
            case REVIEWS:
                rowsUpdated = mMovieDBHelper.getWritableDatabase().update(
                        ReviewEntry.TABLE_NAME,
                        contentValues,
                        selection,
                        selectionArgs
                );
                break;
            case YOUTUBES_WITH_ID:
                long updateYoutubeId = Long.parseLong(uri.getLastPathSegment());
                rowsUpdated = mMovieDBHelper.getWritableDatabase().update(
                        YoutubeEntry.TABLE_NAME
                        ,contentValues
                        ,YoutubeEntry._ID + mFreeParameter
                        ,new String[] {"" + updateYoutubeId});
                break;
            case YOUTUBES:
                rowsUpdated = mMovieDBHelper.getWritableDatabase().update(
                        YoutubeEntry.TABLE_NAME,
                        contentValues,
                        selection,
                        selectionArgs
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }

}
