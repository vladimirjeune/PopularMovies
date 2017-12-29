package app.com.vladimirjeune.popmovies;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Using ItemDecoration for space. Modified from SquareIsland Blog's implemenation
 * Created by vladimirjeune on 12/29/17.
 */

public class SpaceDecoration extends RecyclerView.ItemDecoration {
    private int margin;


    public SpaceDecoration(Context context) {
        margin = context.getResources().getDimensionPixelSize(R.dimen.item_margin);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        outRect.set(margin, margin, margin, margin);
    }
}
