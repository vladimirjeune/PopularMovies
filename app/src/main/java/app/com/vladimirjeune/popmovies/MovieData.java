package app.com.vladimirjeune.popmovies;

import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

/**
 * Temporary class to hold results of the JSON request
 * Created by vladimirjeune on 11/11/17.
 */

public class MovieData {

    public static final String TAG = MovieData.class.getSimpleName();

    // These may be needed for the Main page
    private  int movieId;  // SAVE AS INT
    private  String originalTitle;
    private  String posterPath ;  // Can be NULL

    // These will be needed for Details
    private  String synopsis;
    private  String releaseDate;  // DATE
    private double voterAverage;  // Make an FLOAT or DOUBLE for sorting
    private  String backdropPath;  // Can be NULL
    private double popularity;      // Make an FLOAT or DOUBLE for sorting

    // Requires a call to different endpoint, once we know movieId. But still for Details
    private  int runtime;  // Make an INT

    // Most pertinent information is in the results array
    private  String resultsList;

    // Status 7: Invalid Key, get Dialog?; Status 34: Resource not found, throw exception.
    private  int statusCode;
    private  String statusMessage = "status_message";

    private Drawable poster;     // Actual poster for this movie
    private Drawable backdrop; // Actual backdrop for this movie

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public String getPosterPath() {
        return posterPath;
    }

    /**
     * Set the path to the text given in the parameter if the parameter is not null
     * @param posterPath - Path to this movie's poster on theMovieDb's website
     */
    public void setPosterPath(@Nullable String posterPath) {
        if (posterPath != null) {
            this.posterPath = posterPath;
        }
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public double getVoterAverage() {
        return voterAverage;
    }

    public void setVoterAverage(double voterAverage) {
        this.voterAverage = voterAverage;
    }

    public String getBackdropPath() {
        return backdropPath;
    }

    /**
     * Set the path to the text given in the parameter if the parameter is not null
     * @param backdropPath - Path to this movie's backdrop on theMovieDb's website
     */
    public void setBackdropPath(@Nullable String backdropPath) {
        if (backdropPath != null) {
            this.backdropPath = backdropPath;
        }
    }

    public double getPopularity() {
        return popularity;
    }

    public void setPopularity(double popularity) {
        this.popularity = popularity;
    }

    public int getRuntime() {
        return runtime;
    }

    public void setRuntime(int runtime) {
        this.runtime = runtime;
    }

    public String getResultsList() {
        return resultsList;
    }

    public void setResultsList(String resultsList) {
        this.resultsList = resultsList;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public Drawable getPoster() {
        return poster;
    }

    public void setPoster(Drawable poster) {
        this.poster = poster;
    }

    public Drawable getBackdrop() {
        return backdrop;
    }

    public void setBackdrop(Drawable backdrop) {
        this.backdrop = backdrop;
    }

    public String toString() {
        return TAG + "[ID=" + movieId
                + "\nOriginal Title= " + originalTitle
                + "\nPosterPath= " + posterPath
                + "\nSynopsis= " + synopsis
                + "\nRelease Date= " + releaseDate
                + "\nVoter Average= " + voterAverage
                + "\nBackdropPath= " + backdropPath
                + "\nPopularity= " + popularity
                + "\nRuntime= " + runtime
                + "\nPoster= " + poster
                + "\nBackdrop= " + backdrop
                + "\nResults List= " + resultsList
                + "\nStatus Code= " + statusCode
                + "\nStatus Message= " + statusMessage
                + "]";
    }
}
