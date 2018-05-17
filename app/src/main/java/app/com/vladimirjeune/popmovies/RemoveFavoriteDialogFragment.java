package app.com.vladimirjeune.popmovies;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 */
public class RemoveFavoriteDialogFragment extends DialogFragment {

    public static final String TAG = RemoveFavoriteDialogFragment.class.getSimpleName();
    public static final String REMOVEFAVORITEDIALOG_TAG = "REMOVE_FAVORITE_DIALOG";
    public static final String THEMOVIEID = "REMOVE_FAVORITE_DIALOG_ID";

    private TextView mFavoriteTextView;

    // Necessary for Event Callbacks, this fragment is sent if needed for querying
    public interface RemoveFavoriteListener {
        void onDialogAffirmativeClick(RemoveFavoriteDialogFragment dialogFragment);
        void onDialogNegativeClick(RemoveFavoriteDialogFragment dialogFragment);
    }

    // Used to deliver Action Events
    RemoveFavoriteListener mListener;

    public RemoveFavoriteDialogFragment() {
        // Required empty public constructor
    }


    public static RemoveFavoriteDialogFragment newInstance(long id) {

        Bundle args = new Bundle();
        args.putLong(THEMOVIEID, id);

        RemoveFavoriteDialogFragment fragment = new RemoveFavoriteDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach() called with: context = [" + context + "]");
        try {

        // The Context passed in should be our Activity.
        Activity activity = (Activity) context;
        mListener = (RemoveFavoriteListener) activity;
        } catch ( ClassCastException ce ) {
            throw new ClassCastException(getActivity().toString()
                    + " Either not an Activity, or does not implement callback interface");
        }

    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Help from example
        // https://stuff.mit.edu/afs/sipb/project/android/docs/guide/topics/ui/dialogs.html
        Log.d(TAG, "onCreateDialog() :BEGIN: called with: savedInstanceState = [" + savedInstanceState + "]");

        Context context = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppDialogTheme);

        builder.setMessage(R.string.dialog_remove_favorite)
                .setPositiveButton(R.string.dialog_remove_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mListener.onDialogAffirmativeClick(RemoveFavoriteDialogFragment.this);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mListener.onDialogNegativeClick(RemoveFavoriteDialogFragment.this);
                    }
                });

        return builder.create();
    }
}
