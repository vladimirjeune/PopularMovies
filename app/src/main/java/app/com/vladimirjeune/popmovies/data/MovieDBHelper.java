package app.com.vladimirjeune.popmovies.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import app.com.vladimirjeune.popmovies.data.MovieContract.MovieEntry;

/**
 * Helps to create the database for the 1st time and upgrading it.
 * Created by vladimirjeune on 11/28/17.
 */

public class MovieDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "movie.db";

    private static final int DATABASE_VERSION = 3;

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
                MovieEntry.COLUMN_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP " +
                ");";

        sqLiteDatabase.execSQL(SQL_CREATE_MOVIE_TABLE);
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
        // Try this when you actually need to upgrade in the
        // wild: https://thebhwgroup.com/blog/how-android-sqlite-onupgrade
        final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS" +
                MovieEntry.TABLE_NAME;
        sqLiteDatabase.execSQL(SQL_DROP_TABLE);
        onCreate(sqLiteDatabase);
    }
}
