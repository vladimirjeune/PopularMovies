package app.com.vladimirjeune.popmovies;

import android.os.Bundle;
import android.util.Log;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

import app.com.vladimirjeune.popmovies.data.MovieContract.YoutubeEntry;
import app.com.vladimirjeune.popmovies.utilities.NetworkUtils;

/**
 * Based on tutorial from:
 * http://www.androhub.com/how-to-show-youtube-video-thumbnail-play-youtube-video-in-android-app/
 */
public class YoutubePlayerActivity extends YouTubeBaseActivity implements YouTubePlayer.OnInitializedListener {
    private static final String TAG = YoutubePlayerActivity.class.getSimpleName();
    private static final int RECOVERY_DIALOG_REQUEST = 1;
    private String mMediaKey;
    private YouTubePlayerView mYouTubePlayerView;
    private String mKey ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube_player);

        mMediaKey = getIntent().getStringExtra(YoutubeEntry.KEY);

        mYouTubePlayerView = findViewById(R.id.youtube_player_view_player);

        mKey = NetworkUtils.obtainKeyOfType(this, NetworkUtils.YOUTUBE_DATA_V3_KEY);

        mYouTubePlayerView.initialize(mKey, this);

    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean wasRestored) {

        if (mMediaKey != null) {

            youTubePlayer.setFullscreen(true);
            if (!wasRestored) {  // Indicates initialization success

                // Setting Default Player Style, for now
                youTubePlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.MINIMAL);

                youTubePlayer.loadVideo(mMediaKey);
            } else {

                // Play automatically when rotated.  Otherwise user has to press play
                youTubePlayer.play();
            }
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {

        if (youTubeInitializationResult.isUserRecoverableError()) {
            youTubeInitializationResult.getErrorDialog(this, RECOVERY_DIALOG_REQUEST).show();
        } else {
            Log.e(TAG, "onInitializationFailure: Failed initialization for ID: " + mMediaKey);
        }
    }
}
