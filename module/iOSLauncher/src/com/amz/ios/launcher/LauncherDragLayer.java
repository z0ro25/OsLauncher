package com.amz.ios.launcher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.customview.widget.ViewDragHelper;

import com.amz.ios.launcher.applibrary.AppsLibraryLayout;
import com.amz.ios.launcher.leftpage.views.CustomContentView;

public class LauncherDragLayer extends FrameLayout {

    private AppsLibraryLayout mAppsLibrary;
    private BlurScreenLayout mBlurLayout;
    private CustomContentView mLeftPage;
    private ViewDragHelper mDragHelperAppLibrary;
    private ViewDragHelper.Callback mDragHelperCallbackAppsLibrary;
    private ViewDragHelper mDragHelperLeftPage;
    private ViewDragHelper.Callback mDragHelperCallbackLeftPage;
    private View mDragLayer;
    private GestureDetector mGestureDetector;
    private int mHeight;
    private int mWidth;
    private int mLeftOfAppsLibrary;
    private int mLeftOfLeftPage;
    private PanelSlideListener mPanelSlideListener;
    private int mRange;
    private PageState mStatusAppsLibrary;
    private PageState mStatusLeftPage;

    public interface PanelSlideListener{
        void onAppsLibraryClosed();
        void onAppsLibraryOpened();
        void onAppsLibrarySlide(float f);
        void onLeftPageClosed();
        void onLeftPageOpened();
        void onLeftPageSlide(float f);
    }

    public enum PageState {
        DRAG,
        OPEN,
        CLOSE
    }

    public LauncherDragLayer(@NonNull Context context) {
        this(context,null);
    }

    public LauncherDragLayer(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public LauncherDragLayer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr,0);
    }

    public LauncherDragLayer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mStatusAppsLibrary = PageState.CLOSE;
        this.mStatusLeftPage = PageState.CLOSE;
        config();
        setListeners();
    }

    void setListeners(){

        this.mDragHelperCallbackAppsLibrary = new AppLibraryDragCallBack();
        this.mDragHelperCallbackLeftPage = new LeftPageDragCallBack();

        this.mDragHelperAppLibrary = ViewDragHelper.create(this,0.5f,mDragHelperCallbackAppsLibrary);
        this.mDragHelperLeftPage = ViewDragHelper.create(this,0.5f,mDragHelperCallbackLeftPage);

        this.mGestureDetector = new GestureDetector(
            getContext(),
            new GestureDetector.SimpleOnGestureListener(){
                @Override
                public boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                    return Math.abs(distanceY) <= Math.abs(distanceX);
                }
            }
        );
    }

    void config(){
        setWillNotDraw(false);
    }

    public void dispatchDragLeftPageEvent(int i) {

        if (this.mPanelSlideListener == null) {
            return;
        }
        this.mPanelSlideListener.onLeftPageSlide(Math.abs(i) * 1.f / this.mRange);
        PageState state = this.mStatusLeftPage;

        if (state != getStatusLeftPage() && this.mStatusLeftPage == PageState.CLOSE) {
            this.mPanelSlideListener.onLeftPageClosed();
        } else if (state == getStatusLeftPage() || this.mStatusLeftPage != PageState.OPEN) {

        } else {
            this.mPanelSlideListener.onLeftPageOpened();
        }
    }

    public void dispatchDragAppsLibraryEvent(int i) {
        if (this.mPanelSlideListener == null) {
            return;
        }
        this.mPanelSlideListener.onAppsLibrarySlide(Math.abs(i) * 1.f / this.mRange);
        PageState state = this.mStatusAppsLibrary;
        if (state != getStatusAppLibrary() && this.mStatusAppsLibrary == PageState.CLOSE) {
            this.mPanelSlideListener.onAppsLibraryClosed();
        } else if (state == getStatusAppLibrary() || this.mStatusAppsLibrary != PageState.OPEN) {
        } else {
            this.mPanelSlideListener.onAppsLibraryOpened();
        }
    }

    public PageState getStatusAppLibrary() {
        int i = this.mLeftOfAppsLibrary;
        this.mStatusAppsLibrary = i == 0 ? PageState.CLOSE : i == this.mRange ? PageState.OPEN : PageState.DRAG;
        return this.mStatusAppsLibrary;
    }

    public PageState getStatusLeftPage() {
        int i = this.mLeftOfLeftPage;
        this.mStatusLeftPage = i == 0 ? PageState.OPEN : i == (-this.mRange) ? PageState.CLOSE : PageState.DRAG;
        return this.mStatusLeftPage;
    }

    public boolean isAppsLibraryClosed() {
        View view = this.mAppsLibrary;
        return view != null && this.mWidth - Math.abs(view.getLeft()) == 0;
    }

    public boolean isAppsLibraryOpening(int i) {
        View view = this.mAppsLibrary;
        return view != null && Math.abs(view.getLeft()) < i;
    }

    public boolean isLayoutRtlSupport() {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    public boolean isLeftPageClosed() {
        View view = this.mLeftPage;
        return view != null && this.mWidth - Math.abs(view.getLeft()) == 0;
    }

    public boolean isLeftPageOpened() {
        View view = this.mLeftPage;
        return view != null && Math.abs(view.getLeft()) == 0;
    }

    public boolean isLeftPageOpening(int x) {
        View view = this.mLeftPage;
        return view != null && Math.abs(view.getLeft()) < x;
    }

    public void closeAppsLibrary() {
        closeAppsLibrary(true);
    }

    public void closeAppsLibrary(boolean z) {
        boolean isLayoutRtlSupport = isLayoutRtlSupport();

        if (z) {
            if (this.mDragHelperAppLibrary.smoothSlideViewTo(
                    this.mAppsLibrary,
                    isLayoutRtlSupport ? -mRange : mRange,
                    0)
            ) {
                postInvalidateOnAnimation();
                return;
            }
            return;
        }

        View view = this.mAppsLibrary;
        int i = this.mRange;
        if (isLayoutRtlSupport) {
            i = -i;
        }
        view.layout(i, 0, isLayoutRtlSupport ? 0 : this.mRange * 2, this.mHeight);
        dispatchDragAppsLibraryEvent(isLayoutRtlSupport ? 0 : this.mRange);
    }

    public void closeLeftPage() {
        closeLeftPage(true);
    }

    public void closeLeftPage(boolean z) {
        boolean isLayoutRtlSupport = isLayoutRtlSupport();

        if (z) {
            if (this.mDragHelperLeftPage.smoothSlideViewTo(this.mLeftPage, isLayoutRtlSupport ? this.mRange : -this.mRange, 0)) {
                postInvalidateOnAnimation();
                return;
            }
            return;
        }
        View view = this.mLeftPage;
        int i = this.mRange;
        if (!isLayoutRtlSupport) {
            i = -i;
        }
        view.layout(i, 0, isLayoutRtlSupport ? this.mRange * 2 : 0, this.mHeight);
        dispatchDragLeftPageEvent(isLayoutRtlSupport ? 0 : -mRange);
    }

    public void moveToX(int x, boolean z) {
        if (!z) {
            int i2 = isLayoutRtlSupport() ? -(this.mWidth + x) : this.mWidth - x;
            this.mAppsLibrary.layout(i2, 0, this.mWidth + i2, this.mHeight);
            dispatchDragAppsLibraryEvent(i2);
            return;
        }
        int i3 = isLayoutRtlSupport() ? this.mWidth - x : -(this.mWidth + x);
        this.mLeftPage.layout(i3, 0, this.mWidth + i3, this.mHeight);
        dispatchDragLeftPageEvent(i3);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mDragLayer = getChildAt(0);
        this.mBlurLayout = (BlurScreenLayout) getChildAt(1);
        this.mAppsLibrary = (AppsLibraryLayout) getChildAt(2);
        this.mLeftPage = (CustomContentView) getChildAt(3);
        this.mDragLayer.setClickable(true);
        this.mAppsLibrary.setClickable(true);
        this.mLeftPage.setClickable(true);

        this.mLeftPage.addOnLayoutChangeListener(new View.OnLayoutChangeListener(){
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                mLeftOfLeftPage = left;
            }
        });

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        // Don't intercept horizontal swipes (to open the apps library / left page)
        // while the search view is showing, otherwise it slides out on top of search.
        if (isSearchViewShowing()) {
            this.mDragHelperAppLibrary.cancel();
            this.mDragHelperLeftPage.cancel();
            return false;
        }
        if (actionMasked != MotionEvent.ACTION_CANCEL && actionMasked != MotionEvent.ACTION_UP) {
            return (this.mDragHelperAppLibrary.shouldInterceptTouchEvent(motionEvent) || this.mDragHelperLeftPage.shouldInterceptTouchEvent(motionEvent)) && this.mGestureDetector.onTouchEvent(motionEvent);
        }
        // UP/CANCEL: nếu 1 pane đang KẸT giữa chừng (fling bị cắt ngang / gesture bị hệ thống hủy),
        // cho nó TRƯỢT về mép gần nhất để callback kết thúc (Opened/Closed) LUÔN bắn -> Launcher reset
        // alpha/scale về đúng trạng thái nghỉ (fix bug fling qua lại mất sạch app + dock). Nếu đã khởi
        // động settle thì KHÔNG cancel (cancel() sẽ hủy animation -> kẹt lại y như cũ).
        if (settleDraggingPanesToNearestEdge()) {
            return false;
        }
        this.mDragHelperAppLibrary.cancel();
        this.mDragHelperLeftPage.cancel();
        return false;
    }

    /**
     * Cho pane (App Library / negative page) đang ở trạng thái DRAG trượt về MÉP GẦN NHẤT bằng đúng
     * đường animation (open/close), đảm bảo dispatch chạy tới trạng thái tận cùng để callback
     * Opened/Closed bắn -> Launcher khôi phục alpha/scale về đúng trạng thái nghỉ. Trả true nếu có
     * ít nhất 1 pane được khởi động settle (khi đó caller KHÔNG được gọi cancel()).
     */
    private boolean settleDraggingPanesToNearestEdge() {
        boolean settled = false;
        if (mAppsLibrary != null && getStatusAppLibrary() == PageState.DRAG) {
            // mLeftOfAppsLibrary: 0 = MỞ hẳn, mRange = home. Về mép gần hơn.
            if (Math.abs(mLeftOfAppsLibrary) * 2 >= mRange) closeAppsLibrary(); // gần home -> về home
            else openAppsLibrary();                                            // gần mở  -> mở hẳn
            settled = true;
        }
        if (mLeftPage != null && getStatusLeftPage() == PageState.DRAG) {
            // mLeftOfLeftPage: 0 = MỞ hẳn, -mRange = home.
            if (Math.abs(mLeftOfLeftPage) * 2 >= mRange) closeLeftPage(); // gần home -> về home
            else openLeftPage();                                          // gần mở  -> mở hẳn
            settled = true;
        }
        return settled;
    }

    private boolean isSearchViewShowing() {
        Context context = getContext();
        if (context instanceof Launcher) {
            return ((Launcher) context).isOpeningSearchView();
        }
        return false;
    }

    @Override
    public void onLayout(boolean z, int left, int top, int right, int bottom) {
        this.mDragLayer.layout(0, 0, this.mWidth, this.mHeight);
        this.mBlurLayout.layout(0, 0, this.mWidth, this.mHeight);

        this.mAppsLibrary.layout(
                this.mAppsLibrary.getLeft(),
                0,
                this.mAppsLibrary.getLeft() + this.mWidth,
                this.mHeight
        );

        this.mLeftPage.layout(
                this.mLeftPage.getLeft(),
                0,
                this.mLeftPage.getLeft() + this.mWidth,
                this.mHeight
        );

        // GHI CHÚ: KHÔNG layout anchor apps_library_frost_bg (child index 4) full-screen ở đây. Nếu
        // làm vậy nó thành view TRÊN CÙNG phủ toàn màn -> ViewDragHelper.findTopChildUnder trả về nó
        // thay vì mAppsLibrary -> tryCaptureView từ chối -> KHÔNG vuốt-đóng App Library được. Anchor
        // giữ 0x0 (vô hại cho drag); AppLibraryBlurView tự lấy kích thước màn từ root view để dựng blur.
    }

    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        mWidth = mDragLayer.getMeasuredWidth();
        mHeight = mDragLayer.getMeasuredHeight();
        mRange = mWidth;
        mLeftOfAppsLibrary = isLayoutRtlSupport() ? -mWidth : mWidth;
        mLeftOfLeftPage = isLayoutRtlSupport() ? mWidth : -mWidth;
        mDragLayer.layout(0, 0, mWidth, mHeight);
        mBlurLayout.layout(0, 0, mWidth, mHeight);
        mAppsLibrary.layout(mLeftOfAppsLibrary, 0, mWidth + mLeftOfAppsLibrary, mHeight);
        mLeftPage.layout(mLeftOfLeftPage, 0, mWidth + mLeftOfLeftPage, mHeight);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (isSearchViewShowing()) {
            this.mDragHelperAppLibrary.cancel();
            this.mDragHelperLeftPage.cancel();
            return false;
        }
        try {
            this.mDragHelperAppLibrary.processTouchEvent(motionEvent);
            this.mDragHelperLeftPage.processTouchEvent(motionEvent);
            return true;
        } catch (Exception e2) {
            e2.printStackTrace();
            return true;
        }
    }

    public void openAppsLibrary() {
        openAppsLibrary(true);
    }

    public void openAppsLibrary(boolean z) {
        if (!z) {
            this.mAppsLibrary.layout(0, 0, this.mWidth, this.mHeight);
            dispatchDragAppsLibraryEvent(isLayoutRtlSupport() ? this.mRange : 0);
        } else if (this.mDragHelperAppLibrary.smoothSlideViewTo(this.mAppsLibrary, 0, 0)) {
            postInvalidateOnAnimation();
        }
    }

    public void openLeftPage() {
        openLeftPage(true);
    }

    public void openLeftPage(boolean z) {
        if (!z) {
            this.mLeftPage.layout(0, 0, this.mWidth, this.mHeight);
            dispatchDragLeftPageEvent(isLayoutRtlSupport() ? mRange : 0);
        } else if (this.mDragHelperLeftPage.smoothSlideViewTo(this.mLeftPage, 0, 0)) {
            postInvalidateOnAnimation();
        }
    }

    public void setPanelSlideListener(PanelSlideListener listener) {
        this.mPanelSlideListener = listener;
    }

    class AppLibraryDragCallBack extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            return child == mAppsLibrary;
        }

        @Override
        public int getViewHorizontalDragRange(@NonNull View child) {
            return LauncherDragLayer.this.mWidth;
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            if (isLayoutRtlSupport()) {
                if (changedView == mAppsLibrary)
                    mLeftOfAppsLibrary = left;
                else
                    mLeftOfAppsLibrary -= left;

                if (mLeftOfAppsLibrary <= 0) {
                    if (mLeftOfAppsLibrary < (-mRange)) {
                        mLeftOfAppsLibrary = - mRange;
                    }
                }
                else mLeftOfAppsLibrary = 0;
            } else {
                if (changedView == mAppsLibrary)
                    mLeftOfAppsLibrary = left;
                else
                    mLeftOfAppsLibrary += left;
                if (mLeftOfAppsLibrary >= 0) {
                    if (mLeftOfAppsLibrary > mRange)
                        mLeftOfAppsLibrary = mRange;
                }
                else mLeftOfAppsLibrary = 0;
            }
            if (changedView == mDragLayer) {
                mDragLayer.layout(0, 0, mWidth, mHeight);
                mAppsLibrary.layout(mLeftOfAppsLibrary, 0, mWidth + mLeftOfAppsLibrary, mHeight);
            }
            dispatchDragAppsLibraryEvent(
                    mLeftOfAppsLibrary
            );
        }

        @Override
        public int clampViewPositionHorizontal(@NonNull View view, int left, int dx) {
            if (!isLayoutRtlSupport()) {
                int paddingLeft = getPaddingLeft();
                return Math.min(Math.max(left, paddingLeft), mRange + paddingLeft);
            }
            int width = getWidth() - (mAppsLibrary.getWidth() + getPaddingRight());
            return Math.max(Math.min(left, width), width - mRange);
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            boolean isRtlSupport = isLayoutRtlSupport();
            double left = mLeftOfAppsLibrary;
            double range = mRange;
            int directX = xvel > 0 ? 1 : (xvel == 0 ? 0 : -1);
            if (!isRtlSupport){
                left = Math.abs(left);
                if (directX > 0)
                    closeAppsLibrary();
                else if (directX == 0){
                    if (releasedChild == mAppsLibrary){
                        int compareValue = Double.compare(left,range * .3);
                        if (compareValue == 1) closeAppsLibrary();
                        else openAppsLibrary();
                    }
                    else if (releasedChild == mDragLayer){
                        int compareValue = Double.compare(left,range * .7);
                        if (compareValue <= 0) openAppsLibrary();
                        else closeAppsLibrary();
                    }
                    else
                        openAppsLibrary();
                }
                else openAppsLibrary();
            }
            else {
                if (directX < 0)
                    closeAppsLibrary();
                else if (directX == 0){
                    if (releasedChild == mAppsLibrary){
                        int switchValue = Double.compare(range, left * 0.7);
                        if(switchValue >= 0)
                            openAppsLibrary();
                        else
                            closeAppsLibrary();
                    }
                    else if (releasedChild == mDragLayer){
                        int switchValue = Double.compare(range, left * .3);
                        if (switchValue >= 0)
                            openAppsLibrary();
                        else closeAppsLibrary();
                    }
                    else openAppsLibrary();
                }
                else openAppsLibrary();
            }

        }

    }

    class LeftPageDragCallBack extends ViewDragHelper.Callback{

        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            return child == mLeftPage;
        }

        @Override
        public int getViewHorizontalDragRange(@NonNull View child) {
            return mWidth;
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            if (isLayoutRtlSupport()) {
                if (changedView == mLeftPage) {
                    mLeftOfLeftPage = left;
                } else {
                    mLeftOfLeftPage += left;
                }
                if (mLeftOfLeftPage >= 0) {
                    if (mLeftOfLeftPage > mRange) {
                        mLeftOfLeftPage = mRange;
                    }
                }
                else mLeftOfLeftPage = 0;
            } else {
                if (changedView == mLeftPage) {
                    mLeftOfLeftPage = left;
                } else {
                    mLeftOfLeftPage -= left;
                }
                if (mLeftOfLeftPage <= 0) {
                    if (mLeftOfLeftPage < (-mRange)) {
                        mLeftOfLeftPage = -mRange;
                    }
                }
                else mLeftOfLeftPage = 0;
            }

            if (changedView == mDragLayer) {
                mDragLayer.layout(0, 0, mWidth, mHeight);
                mLeftPage.layout(mLeftOfLeftPage, 0, mWidth + mLeftOfLeftPage, mHeight);
            }

            dispatchDragLeftPageEvent(mLeftOfLeftPage);
        }

        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            if (isLayoutRtlSupport()) {
                int paddingLeft = getPaddingLeft();
                return Math.min(Math.max(left, paddingLeft), mRange + paddingLeft);
            }
            else {
                int width = getWidth() - (mLeftPage.getWidth() + getPaddingRight());
                return Math.max(Math.min(left, width), width - mRange);
            }
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            boolean isRtlSupport = isLayoutRtlSupport();
            double left = mLeftOfLeftPage;
            double range = mRange;

            int directX = (xvel > 0 ? 1 : (xvel == 0 ? 0 : -1));

            if (isRtlSupport){
                if (directX > 0)
                    openLeftPage();
                else if (directX == 0){
                    if (releasedChild == mLeftPage){
                        int comparedValue = Double.compare(left, range * 0.3);
                        if (comparedValue > 0)
                            closeLeftPage();
                        else openLeftPage();
                    }
                    else if (releasedChild == mDragLayer){
                        int comparedValue = Double.compare(left, range * 0.7);
                        if (comparedValue > 0)
                            closeLeftPage();
                        else openLeftPage();
                    }
                    else
                        openLeftPage();
                }
                else openLeftPage();
            }
            else {
                left = Math.abs(left);
                if (directX > 0)
                    openLeftPage();
                else if (directX == 0){
                    if (releasedChild == mLeftPage){
                        int compareValue = Double.compare(left, range * .3);
                        if (compareValue < 0)
                            closeLeftPage();
                        else openLeftPage();
                    }
                    else if (releasedChild == mDragLayer){
                        int compareValue = Double.compare(left, range * .7);
                        if (compareValue >= 0)
                            openLeftPage();
                        else closeLeftPage();
                    }
                }
                else
                    closeLeftPage();
            }

        }
    }

    @Override
    public void postInvalidateOnAnimation() {
        super.postInvalidateOnAnimation();
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (this.mDragHelperLeftPage.continueSettling(true)){
            postInvalidateOnAnimation();
        }
        if (this.mDragHelperAppLibrary.continueSettling(true)){
            postInvalidateOnAnimation();
        }
    }
}
