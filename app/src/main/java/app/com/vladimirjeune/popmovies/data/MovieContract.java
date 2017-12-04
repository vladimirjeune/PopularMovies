package app.com.vladimirjeune.popmovies.data;

import android.provider.BaseColumns;

/**
 * Contract class for Movie Database
 * Created by vladimirjeune on 11/27/17.
 */

public final class MovieContract {

    private MovieContract() {}  // You should not create one

    public static class MovieEntry implements BaseColumns {
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
    }

}
