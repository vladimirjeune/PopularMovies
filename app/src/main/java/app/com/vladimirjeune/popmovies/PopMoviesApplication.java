package app.com.vladimirjeune.popmovies;

import android.app.Application;
import com.facebook.stetho.Stetho;

/**
 * For use with Stetho
 * Created by vladimirjeune on 1/20/18.
 */

public class PopMoviesApplication extends Application {

        @Override
        public void onCreate() {
            super.onCreate();
            // TODO: Remove from application before submission.  Supposed to use debug Variant

            Stetho.initializeWithDefaults(this);
            // Your normal application code here.  See SampleDebugApplication for Stetho initialization.
        }

}
