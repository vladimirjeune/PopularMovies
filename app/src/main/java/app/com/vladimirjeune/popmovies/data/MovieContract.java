package app.com.vladimirjeune.popmovies.data;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Contract class for Movie Database
 * Created by vladimirjeune on 11/27/17.
 */

public final class MovieContract {

    public static final String CONTENT_AUTHORITY = "app.com.vladimirjeune.popmovies";
    public static final String SCHEME = "content://";
    public static final Uri BASE_CONTENT_URI = Uri.parse(SCHEME + CONTENT_AUTHORITY);
    public static final String PATH_MOVIES = "movie";

    public static final String PATH_REVIEWS = "review";
    public static final String PATH_JOIN_REVIEWS_MOVIES = "review_movie_join";

    // TODO: Figure out how handle URI for JOIN content://app.com.vladimirjeune.popmovies/review_movie_join

    private MovieContract() {}  // You should not create one

    public static class MovieEntry implements BaseColumns {

        // Base content uri + path
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_MOVIES).build();

        public static final String TABLE_NAME = "movie";

        // We are using _ID from BaseColumns for the ID

        public static final String ORIGINAL_TITLE = "original_title";

        // String path to poster on theMovieDb
        public static final String POSTER_PATH = "poster_path";
        public static final String SYNOPSIS = "synopsis";

        // Could be stored as Date, or String
        public static final String RELEASE_DATE = "release_date";

        // Float representing what viewers rated the movie
        public static final String VOTER_AVERAGE = "voter_average";

        // String path to backdrop on theMovieDb
        public static final String BACKDROP_PATH = "backdrop_path";

        // Float representing how popular the movie is on the site
        public static final String POPULARITY = "popularity";

        // Integer
        public static final String RUNTIME = "runtime";

        // Bitmap, will be filled in using update at later time
        public static final String POSTER = "poster";

        // Bitmap, will be filled in using update at later time
        public static final String BACKDROP = "backdrop";

        // When we got this info
        public static final String COLUMN_TIMESTAMP = "timestamp";

        // Order of this movie in popular movies.  Was put in #th.  Null value means
        // movie is not in Popular movies
        public static final String POPULAR_ORDER_IN = "popular_order_in";

        // Order of this movie in Top Rated movies.  If both columns used,
        // means movie is in both categories.  Need to just update appropriate column
        // instead of insert because otherwise would duplicate data.
        public static final String TOP_RATED_ORDER_IN = "top_rated_order_in";

        // Order of this movie in favorites movies.  Was put in #th.  Null value means
        // movie is not in Favorite movies
        public static final String FAVORITE_FLAG = "favorite_in";

        /**
         * BUILDURIWITHMOVIEID - Will be used to query details about a single movie.
         * adds the id to the end of the movie content Uri path.
         * @param id - long - Movie ID
         * @return - Uri - [CONTENT_AUTHORITY] + ID
         */
        public static Uri buildUriWithMovieId(long id) {
            return CONTENT_URI.buildUpon().appendPath("" + id).build();
        }
    }


    public static class ReviewEntry implements BaseColumns {

        // Base content uri + path
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_REVIEWS).build();

        public static final String TABLE_NAME = "review";

        // We are using _ID from BaseColumns for the ID only for BaseColumns to still work(autoincrement).
        public static final String REVIEW_ID = "r_id";  // Too big for INT, but is real ID from website.

        public static final String AUTHOR = "author";

        public static final String CONTENT = "content";

        public static final String URL = "url";

        // Foreign Key to the Movie Table
        public static final String MOVIE_ID = "movie_id";

        /**
         * BUILDURIWITHREVIEWID - Will be used to query details about a single review.
         * adds the id to the end of the review content Uri path.
         * * The AutoIncrement ID is not the real ID from TMDb
         * @param id - long - Review ID
         * @return - Uri - [CONTENT_AUTHORITY] + ID
         */
        public static Uri buildUriWithReviewId(long id) {
            return CONTENT_URI.buildUpon().appendPath("" + id).build();
        }

    }


    // TODO: May require more than what is here, or may not be required at all.  Check.
    // No need to insert, update, or delete.  This is a created table
    // Just query.
    public static class JoinEntry implements BaseColumns {

        // Base content uri + path
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_JOIN_REVIEWS_MOVIES).build();

        // TODO: May not need these since these are from other tables.
        // From movie
        public static final String ORIGINAL_TITLE = "original_title";

        // String path to poster on theMovieDb
        public static final String POSTER_PATH = "poster_path";
        public static final String SYNOPSIS = "synopsis";

        // Could be stored as Date, or String
        public static final String RELEASE_DATE = "release_date";

        // Float representing what viewers rated the movie
        public static final String VOTER_AVERAGE = "voter_average";

        // String path to backdrop on theMovieDb
        public static final String BACKDROP_PATH = "backdrop_path";

        // Float representing how popular the movie is on the site
        public static final String POPULARITY = "popularity";

        // Integer
        public static final String RUNTIME = "runtime";

        // Bitmap, will be filled in using update at later time
        public static final String POSTER = "poster";

        // Bitmap, will be filled in using update at later time
        public static final String BACKDROP = "backdrop";

        // When we got this info
        public static final String COLUMN_TIMESTAMP = "timestamp";

        // Order of this movie in popular movies.  Was put in #th.  Null value means
        // movie is not in Popular movies
        public static final String POPULAR_ORDER_IN = "popular_order_in";

        // Order of this movie in Top Rated movies.  If both columns used,
        // means movie is in both categories.  Need to just update appropriate column
        // instead of insert because otherwise would duplicate data.
        public static final String TOP_RATED_ORDER_IN = "top_rated_order_in";

        // Order of this movie in favorites movies.  Was put in #th.  Null value means
        // movie is not in Favorite movies
        public static final String FAVORITE_FLAG = "favorite_in";


        // ----
        // From Review
        public static final String TABLE_NAME = "review";

        // We are using _ID from BaseColumns for the ID only for BaseColumns to still work(autoincrement).
        public static final String REVIEW_ID = "r_id";  // Too big for INT, but is real ID from website.

        public static final String AUTHOR = "author";

        public static final String CONTENT = "content";

        public static final String URL = "url";

        // Foreign Key to the Movie Table
        public static final String MOVIE_ID = "movie_id";



    }


}
