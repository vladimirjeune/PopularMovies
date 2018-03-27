package app.com.vladimirjeune.popmovies.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import app.com.vladimirjeune.popmovies.data.MovieContract.MovieEntry;
import app.com.vladimirjeune.popmovies.data.MovieContract.ReviewEntry;

/**
 * Helps to create the database for the 1st time and upgrading it.
 * Created by vladimirjeune on 11/28/17.
 */

public class MovieDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "movie.db";

    private static final int DATABASE_VERSION = 6;

    public MovieDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_MOVIE_TABLE = "CREATE TABLE " +
                MovieEntry.TABLE_NAME + " (" +
                MovieEntry._ID + " INTEGER PRIMARY KEY NOT NULL, " +  // Will be MovieID
                MovieEntry.ORIGINAL_TITLE + " TEXT NOT NULL, " +
                MovieEntry.POSTER_PATH + " TEXT, " +
                MovieEntry.SYNOPSIS + " TEXT NOT NULL, " +
                MovieEntry.RELEASE_DATE + " INTEGER NOT NULL, " +  // Will read with SELECT date(########, 'unixepoch');
                MovieEntry.VOTER_AVERAGE + " FLOAT NOT NULL, " +
                MovieEntry.BACKDROP_PATH + " TEXT, " +
                MovieEntry.POPULARITY + " FLOAT NOT NULL, " +
                MovieEntry.RUNTIME + " INTEGER, " +
                MovieEntry.POSTER + " BLOB, " +
                MovieEntry.BACKDROP + " BLOB, " +
                MovieEntry.POPULAR_ORDER_IN + " INTEGER, " +  // THE ORDER POP MOVIES WERE ENTERED, NULL means not Pop
                MovieEntry.TOP_RATED_ORDER_IN + " INTEGER, " +  // The order Top Rated movies were entered.  Null means not TR
                MovieEntry.FAVORITE_FLAG + " INTEGER DEFAULT 0, " +  // 1 Means order alphabetically.  0 means not Favorite
                MovieEntry.COLUMN_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP " +
                ");";

        final String SQL_CREATE_REVIEW_TABLE = "CREATE TABLE " +
                ReviewEntry.TABLE_NAME + " ( " +
                ReviewEntry._ID + " INTEGER PRIMARY KEY NOT NULL, " +  // Will be ReviewID
                ReviewEntry.AUTHOR + " TEXT NOT NULL, " +
                ReviewEntry.CONTENT + " TEXT NOT NULL, " +
                ReviewEntry.URL + " TEXT NOT NULL, " +
                ReviewEntry.MOVIE_ID + " INTEGER NOT NULL, " +
                " FOREIGN KEY ( " + ReviewEntry.MOVIE_ID + " ) " +
                " REFERENCES " + MovieEntry.TABLE_NAME + " ( " + MovieEntry._ID + " ) " +
                " ON DELETE CASCADE " + // Foreign Key to Movie Table.  Delete of Movie items auto-delete Review Children
                " ); ";

        sqLiteDatabase.execSQL(SQL_CREATE_MOVIE_TABLE);  // Parent Table
        sqLiteDatabase.execSQL(SQL_CREATE_REVIEW_TABLE); // Review Table

    }

    /**
     * ONUPGRADE - Called when version# of DB becomes larger than version on the device.
     * For now we drop and call onCreate.  Later, we may need to do an Alter call as needed
     * so we do not lose the users data while modifying the database structure.
     * @param sqLiteDatabase - Database to needing upgrade
     * @param oldVersion - Version number currently on
     * @param newVersion - Version number to get to
     */
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // TODO: Try this when you actually need to upgrade in the
        // wild: https://thebhwgroup.com/blog/how-android-sqlite-onupgrade

        // Child table must be deleted before parent.  Review has FK relationship to Movie
        final String SQL_DROP_REVIEW_TABLE = " DROP TABLE IF EXISTS " +
                ReviewEntry.TABLE_NAME;

        final String SQL_DROP_MOVIE_TABLE = " DROP TABLE IF EXISTS " +
                MovieEntry.TABLE_NAME;

        sqLiteDatabase.execSQL(SQL_DROP_REVIEW_TABLE);  // Child tables must be deleted 1st so no constraint problems
        sqLiteDatabase.execSQL(SQL_DROP_MOVIE_TABLE);

        onCreate(sqLiteDatabase);
    }
}
