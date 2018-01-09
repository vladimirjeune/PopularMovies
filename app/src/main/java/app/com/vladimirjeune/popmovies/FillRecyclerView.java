package app.com.vladimirjeune.popmovies;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

/**
 * Using to fill RecyclerView during landscape mode. Modified from SquareIsland Blog's implementation
 * Created by vladimirjeune on 12/29/17.
 */

public class FillRecyclerView extends RecyclerView {  // Extended to get access to width
    private GridLayoutManager gridLayoutManager;
    private int columnWidth = -1;

    public FillRecyclerView(Context context) {
        super(context);
        init(context, null);
    }

    public FillRecyclerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);  // Let super have it first
        init(context, attributeSet);
    }

    public FillRecyclerView(Context context, AttributeSet attributeSet, int defStyle) {
        super(context, attributeSet, defStyle);
        init(context, attributeSet);
    }

    private void init(Context context, AttributeSet attributeSet) {
        if (attributeSet != null) {
            int [] attributeSetArray = {
                    android.R.attr.columnWidth
            };

            TypedArray typedArray = context.obtainStyledAttributes(attributeSet, attributeSetArray);
            int index = 0;
            int fallbackValue = -1;
            columnWidth = typedArray.getDimensionPixelSize(index, fallbackValue);  // Obtain the Col Width
            typedArray.recycle();  // These are a shared resource and MUST be recycled.
        }

        int wantedSpanCount = 1;  // Must make a GLManager here, or app will crash before getting to onMeasure
        gridLayoutManager = new GridLayoutManager(getContext(), wantedSpanCount);  // Not sure why necessary to getContext() here
        setLayoutManager(gridLayoutManager);
    }

    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);  // Let super measure, then get columnWidth

        if (columnWidth > 0) {
            int spanCount = Math.max(1, (getMeasuredWidth() / columnWidth));  // Span will either be 1, or greater
            gridLayoutManager.setSpanCount(spanCount);
        }
    }
}
