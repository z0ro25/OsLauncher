package com.oslauncher.applauncher.themelauncher.views.dot_Indicator;

import android.animation.ArgbEvaluator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.oslauncher.applauncher.themelauncher.R;

public class DotIndicator extends View {

    private static final String TAG = "DotIndicatorCheck";

    @IntDef({RecyclerView.HORIZONTAL, RecyclerView.VERTICAL})
    public @interface Orientation {
    }

    private int infiniteDotCount;

    private final int dotMinimumSize;
    private final int dotNormalSize;
    private final int dotSelectedSize;
    private final int spaceBetweenDotCenters;
    private int visibleDotCount;
    private int visibleDotThreshold;
    private int orientation;

    private float visibleFramePosition;
    private float visibleFrameWidth;

    private float firstDotOffset;
    private SparseArray<Float> dotScale;

    private int itemCount;

    private final Paint paint;
    private final ArgbEvaluator colorEvaluator = new ArgbEvaluator();

    @ColorInt
    private int dotColor;

    @ColorInt
    private int selectedDotColor;

    private boolean looped;

    private Runnable attachRunnable;
    private PagerAttacker<?> currentAttacker;
    private boolean autoRtl = true;

    private boolean dotCountInitialized;

    public DotIndicator(Context context) {
        this(context, null);
    }

    public DotIndicator(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.dotIndicatorStyle);
    }

    public DotIndicator(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.DotIndicator, defStyleAttr, R.style.DotIndicator);
        dotColor = attributes.getColor(R.styleable.DotIndicator_di_dotColor, 0);
        selectedDotColor = attributes.getColor(R.styleable.DotIndicator_di_dotSelectedColor, dotColor);
        dotNormalSize = attributes.getDimensionPixelSize(R.styleable.DotIndicator_di_dotSize, 0);
        dotSelectedSize = attributes.getDimensionPixelSize(R.styleable.DotIndicator_di_dotSelectedSize, 0);
        int dotMinimumSize = attributes.getDimensionPixelSize(R.styleable.DotIndicator_di_dotMinimumSize, -1);
        this.dotMinimumSize = dotMinimumSize <= dotNormalSize ? dotMinimumSize : -1;

        spaceBetweenDotCenters = attributes.getDimensionPixelSize(R.styleable.DotIndicator_di_dotSpacing, 0) + dotNormalSize;
        looped = attributes.getBoolean(R.styleable.DotIndicator_di_looped, false);
        int visibleDotCount = attributes.getInt(R.styleable.DotIndicator_di_visibleDotCount, 0);
        setVisibleDotCount(visibleDotCount);
        visibleDotThreshold = attributes.getInt(R.styleable.DotIndicator_di_visibleDotThreshold, 2);
        orientation = attributes.getInt(R.styleable.DotIndicator_di_orientation, RecyclerView.HORIZONTAL);

        attributes.recycle();

        paint = new Paint();
        paint.setAntiAlias(true);

        if (isInEditMode()) {
            setDotCount(visibleDotCount);
            onPageScrolled(visibleDotCount / 2, 0);
        }
    }

    public void setLooped(boolean looped) {
        Log.d(TAG, "setLooped: ");
        this.looped = looped;
        reattach();
        invalidate();
    }

    /**
     * @return not selected dot color
     */
    @ColorInt
    public int getDotColor() {
        Log.d(TAG, "getDotColor: '");
        return dotColor;
    }

    /**
     * Sets dot color
     *
     * @param color dot color
     */
    public void setDotColor(@ColorInt int color) {
        Log.d(TAG, "setDotColor: ");
        this.dotColor = color;
        invalidate();
    }

    /**
     * @return the selected dot color
     */
    @ColorInt
    public int getSelectedDotColor() {
        return selectedDotColor;
    }

    /**
     * Sets selected dot color
     *
     * @param color selected dot color
     */
    public void setSelectedDotColor(@ColorInt int color) {
        Log.d(TAG, "setSelectedDotColor: ");
        this.selectedDotColor = color;
        invalidate();
    }

    /**
     * Maximum number of dots which will be visible at the same time.
     * If pager has more pages than visible_dot_count, indicator will scroll to show extra dots.
     * Must be odd number.
     *
     * @return visible dot count
     */
    public int getVisibleDotCount() {
        Log.d(TAG, "getVisibleDotCount: ");
        return visibleDotCount;
    }

    /**
     * Sets visible dot count. Maximum number of dots which will be visible at the same time.
     * If pager has more pages than visible_dot_count, indicator will scroll to show extra dots.
     * Must be odd number.
     *
     * @param visibleDotCount visible dot count
     * @throws IllegalStateException when pager is already attached
     */
    public void setVisibleDotCount(int visibleDotCount) {
        Log.d(TAG, "setVisibleDotCount: ");
        if (visibleDotCount % 2 == 0) {
            throw new IllegalArgumentException("visibleDotCount must be odd");
        }
        this.visibleDotCount = visibleDotCount;
        this.infiniteDotCount = visibleDotCount + 2;

        if (attachRunnable != null) {
            reattach();
        } else {
            requestLayout();
        }
    }

    /**
     * The minimum number of dots which should be visible.
     * If pager has less pages than visibleDotThreshold, no dots will be shown.
     *
     * @return visible dot threshold.
     */
    public int getVisibleDotThreshold() {
        return visibleDotThreshold;
    }

    /**
     * Sets the minimum number of dots which should be visible.
     * If pager has less pages than visibleDotThreshold, no dots will be shown.
     *
     * @param visibleDotThreshold visible dot threshold.
     */
    public void setVisibleDotThreshold(int visibleDotThreshold) {
        this.visibleDotThreshold = visibleDotThreshold;
        if (attachRunnable != null) {
            reattach();
        } else {
            requestLayout();
        }
    }

    /**
     * The visible orientation of the dots
     *
     * @return dot orientation (RecyclerView.HORIZONTAL, RecyclerView.VERTICAL)
     */
    @Orientation
    public int getOrientation() {
        return orientation;
    }

    /**
     * Set the dot orientation
     *
     * @param orientation dot orientation (RecyclerView.HORIZONTAL, RecyclerView.VERTICAL)
     */
    public void setOrientation(@Orientation int orientation) {
        Log.d(TAG, "setOrientation: ");
        this.orientation = orientation;
        if (attachRunnable != null) {
            reattach();
        } else {
            requestLayout();
        }
    }

    /**
     * Attaches indicator to ViewPager2
     *
     * @param pager pager to attach
     */
    public void attachToPager(@NonNull ViewPager2 pager) {
        Log.d(TAG, "attachToPager: ");
        attachToPager(pager, new ViewPager2Attacker());
    }

    public <T> void attachToPager(@NonNull final T pager, @NonNull final PagerAttacker<T> attacker) {
        Log.d(TAG, "attachToPager: ");
        detachFromPager();
        attacker.attachToPager(this, pager);
        currentAttacker = attacker;

        attachRunnable = () -> {
            itemCount = -1;
            attachToPager(pager, attacker);
        };
    }

    /**
     * Detaches indicator from pager.
     */
    public void detachFromPager() {
        Log.d(TAG, "detachFromPager: ");
        if (currentAttacker != null) {
            currentAttacker.detachFromPager();
            currentAttacker = null;
            attachRunnable = null;
            autoRtl = true;
        }
        dotCountInitialized = false;
    }

    /**
     * Detaches indicator from pager and attaches it again.
     * It may be useful for refreshing after adapter count change.
     */
    public void reattach() {
        Log.d(TAG, "reattach: ");
        if (attachRunnable != null) {
            attachRunnable.run();
            invalidate();
        }
    }

    public void onPageScrolled(int page, float offset) {
        Log.d(TAG, "onPageScrolled: ");
        if (offset < 0 || offset > 1) {
            throw new IllegalArgumentException("Offset must be [0, 1]");
        } else if (page < 0 || page != 0 && page >= itemCount) {
            throw new IndexOutOfBoundsException("page must be [0, adapter.getItemCount())");
        }

        if (!looped || itemCount <= visibleDotCount && itemCount > 1) {
            dotScale.clear();

            if (orientation == LinearLayout.HORIZONTAL) {
                scaleDotByOffset(page, offset);

                if (page < itemCount - 1) {
                    scaleDotByOffset(page + 1, 1 - offset);
                } else if (itemCount > 1) {
                    scaleDotByOffset(0, 1 - offset);
                }
            } else { // Vertical orientation
                scaleDotByOffset(page - 1, offset);
                scaleDotByOffset(page, 1 - offset);
            }

            invalidate();
        }
        if (orientation == LinearLayout.HORIZONTAL) {
            adjustFramePosition(offset, page);
        } else {
            adjustFramePosition(offset, page - 1);
        }
        invalidate();
    }

    /**
     * Sets dot count
     *
     * @param count new dot count
     */
    public void setDotCount(int count) {
        initDots(count);
    }

    /**
     * Sets currently selected position (according to your pager's adapter)
     *
     * @param position new current position
     */
    public void setCurrentPosition(int position) {
        Log.d(TAG, "setCurrentPosition: ");
        if (position != 0 && (position < 0 || position >= itemCount)) {
            throw new IndexOutOfBoundsException("Position must be [0, adapter.getItemCount()]");
        }
        if (itemCount == 0) {
            return;
        }
        adjustFramePosition(0, position);
        updateScaleInIdleState(position);
    }

    /**
     * Sets Rtl direction availability when the view has Rtl direction.
     * autoRtl is on by default.
     *
     * @param autoRtl false means rtl direction doesn't be apply even if view direction is Rtl.
     */
    public void setAutoRtl(boolean autoRtl) {
        Log.d(TAG, "setAutoRtl: ");
        this.autoRtl = autoRtl;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.d(TAG, "onMeasure: ");
        // Width
        int measuredWidth;
        // Height
        int measuredHeight;

        if (orientation == LinearLayoutManager.HORIZONTAL) {
            // We ignore widthMeasureSpec because width is based on visibleDotCount
            if (isInEditMode()) {
                // Maximum width with all dots visible
                measuredWidth = (visibleDotCount - 1) * spaceBetweenDotCenters + dotSelectedSize;
            } else {
                measuredWidth = itemCount >= visibleDotCount ? (int) visibleFrameWidth : (itemCount - 1) * spaceBetweenDotCenters + dotSelectedSize;
            }
            int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);

            // Height
            int desiredHeight = dotSelectedSize;

            switch (heightMode) {
                case MeasureSpec.EXACTLY:
                    measuredHeight = heightSize;
                    break;
                case MeasureSpec.AT_MOST:
                    measuredHeight = Math.min(desiredHeight, heightSize);
                    break;
                case MeasureSpec.UNSPECIFIED:
                default:
                    measuredHeight = desiredHeight;
            }
        } else {
            if (isInEditMode()) {
                measuredHeight = (visibleDotCount - 1) * spaceBetweenDotCenters + dotSelectedSize;
            } else {
                measuredHeight = itemCount >= visibleDotCount ? (int) visibleFrameWidth : (itemCount - 1) * spaceBetweenDotCenters + dotSelectedSize;
            }

            int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);

            // Width
            int desiredWidth = dotSelectedSize;

            switch (widthMode) {
                case MeasureSpec.EXACTLY:
                    measuredWidth = widthSize;
                    break;
                case MeasureSpec.AT_MOST:
                    measuredWidth = Math.min(desiredWidth, widthSize);
                    break;
                case MeasureSpec.UNSPECIFIED:
                default:
                    measuredWidth = desiredWidth;
            }
        }
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "onDraw: ");
        int dotCount = getDotCount();
        if (dotCount < visibleDotThreshold) {
            return;
        }

        // Some empirical coefficients
        float scaleDistance = (spaceBetweenDotCenters + (dotSelectedSize - dotNormalSize) / 2) * 0.7f;
        float smallScaleDistance = dotSelectedSize / 2;
        float centerScaleDistance = 6f / 7f * spaceBetweenDotCenters;

        int firstVisibleDotPos = (int) (visibleFramePosition - firstDotOffset) / spaceBetweenDotCenters;
        int lastVisibleDotPos = firstVisibleDotPos + (int) (visibleFramePosition + visibleFrameWidth - getDotOffsetAt(firstVisibleDotPos)) / spaceBetweenDotCenters;

        Log.d(TAG, "onDraw 1: " + firstVisibleDotPos + "===" + lastVisibleDotPos);
        // If real dots count is less than we can draw inside visible frame, we move lastVisibleDotPos
        // to the last item
        if (firstVisibleDotPos == 0 && lastVisibleDotPos + 1 > dotCount) {
            lastVisibleDotPos = dotCount - 1;
        }

        Log.d(TAG, "onDraw 2: " + lastVisibleDotPos);

        for (int i = firstVisibleDotPos; i <= lastVisibleDotPos; i++) {
            float dot = getDotOffsetAt(i);
            if (dot >= visibleFramePosition && dot < visibleFramePosition + visibleFrameWidth) {
                float diameter;
                float scale = 1;

                // Calculate scale according to current page position
                if (looped && itemCount > visibleDotCount) {
                    float frameCenter = visibleFramePosition + visibleFrameWidth / 2;
                    if (dot >= frameCenter - centerScaleDistance && dot <= frameCenter) {
                        scale = (dot - frameCenter + centerScaleDistance) / centerScaleDistance;
                    } else if (dot > frameCenter && dot < frameCenter + centerScaleDistance) {
                        scale = 1 - (dot - frameCenter) / centerScaleDistance;
                    } else {
                        scale = 0;
                    }
                } else {
                    scale = getDotScaleAt(i);
                }
                diameter = dotNormalSize + (dotSelectedSize - dotNormalSize) * scale;

                // Additional scale for dots at corners
                if (itemCount > visibleDotCount) {
                    float currentScaleDistance;
                    if (!looped && (i == 0 || i == dotCount - 1)) {
                        currentScaleDistance = smallScaleDistance;
                    } else {
                        currentScaleDistance = scaleDistance;
                    }

                    int size = getWidth();
                    if (orientation == LinearLayoutManager.VERTICAL) {
                        size = getHeight();
                    }
                    if (dot - visibleFramePosition < currentScaleDistance) {
                        float calculatedDiameter = diameter * (dot - visibleFramePosition) / currentScaleDistance;
                        if (calculatedDiameter <= dotMinimumSize) {
                            diameter = dotMinimumSize;
                        } else if (calculatedDiameter < diameter) {
                            diameter = calculatedDiameter;
                        }
                    } else if (dot - visibleFramePosition > size - currentScaleDistance) {
                        float calculatedDiameter = diameter * (-dot + visibleFramePosition + size) / currentScaleDistance;
                        if (calculatedDiameter <= dotMinimumSize) {
                            diameter = dotMinimumSize;
                        } else if (calculatedDiameter < diameter) {
                            diameter = calculatedDiameter;
                        }
                    }
                }

                paint.setColor(calculateDotColor(scale));
                if (orientation == LinearLayoutManager.HORIZONTAL) {
                    float cx = dot - visibleFramePosition;
                    if (autoRtl && isRtl()) {
                        cx = getWidth() - cx;
                    }
                    canvas.drawCircle(cx, getMeasuredHeight() / 2, diameter / 2, paint);
                } else {
                    canvas.drawCircle(getMeasuredWidth() / 2, dot - visibleFramePosition, diameter / 2, paint);

                }
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private boolean isRtl() {
        Log.d(TAG, "isRtl: ");
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    @ColorInt
    private int calculateDotColor(float dotScale) {
        Log.d(TAG, "calculateDotColor: ");
        return (Integer) colorEvaluator.evaluate(dotScale, dotColor, selectedDotColor);
    }

    private void updateScaleInIdleState(int currentPos) {
        Log.d(TAG, "updateScaleInIdleState: ");
        if (!looped || itemCount < visibleDotCount) {
            dotScale.clear();
            dotScale.put(currentPos, 1f);
            invalidate();
        }
    }

    private void initDots(int itemCount) {
        Log.d(TAG, "initDots: ");
        if (this.itemCount == itemCount && dotCountInitialized) {
            return;
        }
        this.itemCount = itemCount;
        dotCountInitialized = true;
        dotScale = new SparseArray<>();

        if (itemCount < visibleDotThreshold) {
            requestLayout();
            invalidate();
            return;
        }

        firstDotOffset = (looped && this.itemCount > visibleDotCount) ? 0 : dotSelectedSize / 2f;
        visibleFrameWidth = (visibleDotCount - 1) * spaceBetweenDotCenters + dotSelectedSize;

        requestLayout();
        invalidate();
    }

    private int getDotCount() {
        Log.d(TAG, "getDotCount: ");
        if (looped && itemCount > visibleDotCount) {
            return infiniteDotCount;
        } else {
            return itemCount;
        }
    }

    private void adjustFramePosition(float offset, int pos) {
        Log.d(TAG, "adjustFramePosition: ");
        if (itemCount <= visibleDotCount) {
            // Without scroll
            visibleFramePosition = 0;
        } else if (!looped) {
            // Not looped with scroll
            float center = getDotOffsetAt(pos) + spaceBetweenDotCenters * offset;
            visibleFramePosition = center - visibleFrameWidth / 2;

            // Block frame offset near start and end
            int firstCenteredDotIndex = visibleDotCount / 2;
            float lastCenteredDot = getDotOffsetAt(getDotCount() - 1 - firstCenteredDotIndex);
            if (visibleFramePosition + visibleFrameWidth / 2 < getDotOffsetAt(firstCenteredDotIndex)) {
                visibleFramePosition = getDotOffsetAt(firstCenteredDotIndex) - visibleFrameWidth / 2;
            } else if (visibleFramePosition + visibleFrameWidth / 2 > lastCenteredDot) {
                visibleFramePosition = lastCenteredDot - visibleFrameWidth / 2;
            }
        } else {
            // Looped with scroll
            float center = getDotOffsetAt(infiniteDotCount / 2) + spaceBetweenDotCenters * offset;
            visibleFramePosition = center - visibleFrameWidth / 2;
        }
    }

    private void scaleDotByOffset(int position, float offset) {
        Log.d(TAG, "scaleDotByOffset: " + position);
        if (dotScale == null || getDotCount() == 0) {
            return;
        }
        setDotScaleAt(position, 1 - Math.abs(offset));
    }

    private float getDotOffsetAt(int index) {
        Log.d(TAG, "getDotOffsetAt: ");
        return firstDotOffset + index * spaceBetweenDotCenters;
    }

    private float getDotScaleAt(int index) {
        Log.d(TAG, "getDotScaleAt: ");
        Float scale = dotScale.get(index);
        if (scale != null) {
            return scale;
        }
        return 0;
    }

    private void setDotScaleAt(int index, float scale) {
        Log.d(TAG, "setDotScaleAt: ");
        if (scale == 0) {
            dotScale.remove(index);
        } else {
            dotScale.put(index, scale);
        }
    }

    public interface PagerAttacker<T> {

        void attachToPager(@NonNull DotIndicator indicator, @NonNull T pager);

        void detachFromPager();
    }
}
