package org.envirocar.app.views.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.envirocar.app.R;
import org.jetbrains.annotations.NotNull;

import static com.mapbox.android.gestures.Utils.dpToPx;

public class RoundedBottomPanelLayout extends ViewGroup implements SlidingUpPanelLayout.PanelSlideListener {

    private Context context;

    private boolean didAddScrollListener = false;
    private RecyclerView recyclerView;
    private RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NotNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            invalidate();
        }
    };

    private GradientDrawable shadowGradient;
    private int shadowHeight = (int) dpToPx(6f);

    int handleHeight = (int) dpToPx(32f);
    private Path handlePath = new Path();
    private boolean isHandlePathInvalid = true;
    private Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private RectF handleBarRect = new RectF();
    private float handleBarWidth = dpToPx(24f);
    private float handleBarHeight = dpToPx(5f);
    private Paint handleBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean didAddSlideListener = false;
    private SlidingUpPanelLayout slidingUpPanelLayout = null;

    private int originalPanelHeight = -1;
    private float slideOffsetPx = Float.POSITIVE_INFINITY;
    private float previousSlideOffsetPx = Float.POSITIVE_INFINITY;

    public RoundedBottomPanelLayout(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public RoundedBottomPanelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public RoundedBottomPanelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init();
    }

    public RoundedBottomPanelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.context = context;
        init();
    }

    public void init() {
        shadowGradient = (GradientDrawable) context.getDrawable(R.drawable.shadow_bottom);
        handlePaint.setStyle(Paint.Style.FILL);
        handlePaint.setColor(Color.WHITE);

        handleBarPaint.setStyle(Paint.Style.FILL);
        handleBarPaint.setColor(0xffdde4ef);

        ViewCompat.setElevation(this, dpToPx(16f));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setConvexPath(handlePath);

                }
            });
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int childCount = getChildCount();
        if (childCount > 2 || childCount < 1) {
            throw new IllegalArgumentException("RoundedBottomPanel must have 1 or 2 children");
        }

        int maxHeight = MeasureSpec.getSize(heightMeasureSpec);

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);

            LayoutParams layoutParams = child.getLayoutParams();
            if (layoutParams.height == LayoutParams.MATCH_PARENT) {
                measureChild(child, widthMeasureSpec, MeasureSpec.makeMeasureSpec(maxHeight - handleHeight, MeasureSpec.EXACTLY));
            } else {
                measureChild(child, widthMeasureSpec, MeasureSpec.makeMeasureSpec(maxHeight - handleHeight, MeasureSpec.AT_MOST));
            }
        }

        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);

        if (childCount > 1) {
            View firstChild = getChildAt(0);
            shadowGradient.setBounds(
                    0,
                    handleHeight + firstChild.getMeasuredHeight(),
                    getMeasuredWidth(),
                    handleHeight + firstChild.getMeasuredHeight() + shadowHeight
            );
        }

        float handleHeightF = (float) handleHeight;

        handlePath.reset();
        handlePath.moveTo(0f, handleHeightF);
        handlePath.quadTo(0f, 0f, handleHeightF, 0f);
        handlePath.lineTo(getMeasuredWidth() - handleHeightF, 0f);
        handlePath.quadTo(getMeasuredWidth(), 0f, getMeasuredWidth(), handleHeightF);
        handlePath.close();

        isHandlePathInvalid = true;

        float left = (getMeasuredWidth() - handleBarWidth) / 2f;
        float top = (handleHeight - handleBarHeight) / 2f;
        handleBarRect.set(left, top, left + handleBarWidth, top + handleBarHeight);

        if (originalPanelHeight == -1) {
            originalPanelHeight = getMeasuredHeight();
        }
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View firstChild = getChildAt(0);
        int firstChildHeight = Math.min(firstChild.getMeasuredHeight(), getMeasuredHeight() - handleHeight);

        int childCount = getChildCount();
        if (childCount > 1) {
            firstChild.layout(0, handleHeight, getMeasuredWidth(), handleHeight + firstChildHeight);
        } else {
            firstChild.layout(0, handleHeight, getMeasuredWidth(), getMeasuredHeight());
        }

        if (childCount > 1) {
            View secondChild = getChildAt(1);
            secondChild.layout(0, handleHeight + firstChild.getMeasuredHeight(), getMeasuredWidth(), getMeasuredHeight());

            addScrollListenerIfNeeded();
        }

        addSlideListenerIfNeeded();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        RecyclerView recyclerView = this.recyclerView;
        if (didAddScrollListener && recyclerView != null) {
            if (recyclerView.canScrollVertically(-1)) {
                shadowGradient.draw(canvas);
            }
        }

        if (didAddSlideListener) {
            float handleHeightF = (float) handleHeight;

            float offset = Math.min(slideOffsetPx, handleHeightF);

            handlePath.reset();
            handlePath.moveTo(0f, handleHeightF);
            handlePath.lineTo(0f, offset);
            handlePath.quadTo(0f, 0f, offset, 0f);
            handlePath.lineTo(getMeasuredWidth() - offset, 0f);
            handlePath.quadTo(getMeasuredWidth(), 0f, getMeasuredWidth(), offset);
            handlePath.lineTo(getMeasuredWidth(), handleHeightF);
            handlePath.close();


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                invalidateOutline();
            }

            isHandlePathInvalid = false;

            previousSlideOffsetPx = slideOffsetPx;
        }

        canvas.drawPath(handlePath, handlePaint);
        canvas.drawRoundRect(handleBarRect, handleBarHeight / 2f, handleBarHeight / 2f, handleBarPaint);

    }

    private void addScrollListenerIfNeeded() {
        int childCount = getChildCount();
        if (!didAddScrollListener && childCount > 1) {
            View secondChild = getChildAt(1);
            if (secondChild instanceof RecyclerView) {
                recyclerView = (RecyclerView) secondChild;
                ((RecyclerView) secondChild).addOnScrollListener(scrollListener);
                didAddScrollListener = true;
            }
        }
    }

    private void addSlideListenerIfNeeded() {
        if (!didAddSlideListener) {
            slidingUpPanelLayout = (SlidingUpPanelLayout) getParent();
            slidingUpPanelLayout.addPanelSlideListener(this);
            didAddSlideListener = true;
        }
    }

    private boolean shouldRedrawHandle() {
        return previousSlideOffsetPx != slideOffsetPx && previousSlideOffsetPx < handleHeight
                || isHandlePathInvalid;
    }


    @Override
    public void onPanelSlide(@NonNull View panel, float slideOffset) {
        slideOffsetPx = (originalPanelHeight * (1 - slideOffset));
        if (shouldRedrawHandle()) {
            invalidate();
        }
        previousSlideOffsetPx = slideOffsetPx;
    }

    @Override
    public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
        if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
            // If this happened due to a quick slide, such that slideOffsetPx became 0 instantly without
            // affecting the value of previousSlideOffsetPx, we still need to update the handle path,
            // so we set previousSlideOffsetPx to something other than 0, so that the handle is redrawn.
            previousSlideOffsetPx = 1f;
            slideOffsetPx = 0f;
            invalidate();
        }
    }
}
