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
    private String mMediaId;
    private YouTubePlayerView mYouTubePlayerView;
    private String mKey ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube_player);

        mMediaId = getIntent().getStringExtra(YoutubeEntry.YOUTUBE_ID);

        mYouTubePlayerView = findViewById(R.id.youtube_player_view_player);

        mKey = NetworkUtils.obtainKeyOfType(this, NetworkUtils.YOUTUBE_DATA_V3_KEY);

        mYouTubePlayerView.initialize(mKey, this);

    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean wasRestored) {


//        youTubePlayer.setFullscreenControlFlags(YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE);
//        youTubePlayer.setFullscreenControlFlags(YouTubePlayer.FULLSCREEN_FLAG_CONTROL_ORIENTATION);
//        youTubePlayer.setFullscreenControlFlags(YouTubePlayer.FULLSCREEN_FLAG_CONTROL_SYSTEM_UI);

        if (mMediaId != null) {

            youTubePlayer.setFullscreen(true);   // TODO: See if OK here.
            if (!wasRestored) {  // Indicates initialization success

                // Setting Default Player Style, for now
                youTubePlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.DEFAULT);

                youTubePlayer.loadVideo(mMediaId);
            } else {

                // Play automatically
                youTubePlayer.play();
            }
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {

        if (youTubeInitializationResult.isUserRecoverableError()) {
            youTubeInitializationResult.getErrorDialog(this, RECOVERY_DIALOG_REQUEST).show();
        } else {
            Log.e(TAG, "onInitializationFailure: Failed initialization for ID: " + mMediaId);
        }
    }
}
