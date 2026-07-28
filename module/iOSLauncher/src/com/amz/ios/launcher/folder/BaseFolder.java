package com.amz.ios.launcher.folder;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amz.ios.launcher.Alarm;
import com.amz.ios.launcher.DragController;
import com.amz.ios.launcher.DragLayer;
import com.amz.ios.launcher.DragSource;
import com.amz.ios.launcher.DragView;
import com.amz.ios.launcher.DropTarget;
import com.amz.ios.launcher.FolderIcon;
import com.amz.ios.launcher.IconCache;
import com.amz.ios.launcher.ItemInfo;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherModel;
import com.amz.ios.launcher.OnAlarmListener;
import com.amz.ios.launcher.R;

import java.util.ArrayList;

public class BaseFolder extends LinearLayout implements View.OnClickListener, View.OnFocusChangeListener, View.OnLongClickListener, DragSource, DropTarget {
    private static final String TAG = "BaseFolder";

    static final boolean DEBUG_DRAG = false;
    static final boolean DEBUG_ENABLE = false;
    protected static final int FOLDER_CLOSE_ANIMATE_DELAY = 80;
    protected static final int FOLDER_CLOSE_BACKGROUND_ANIMATE_DURATION = 150;
    protected static final int FOLDER_OPEN_BACKGROUND_ANIMATE_DELAY = 150;
    protected static final int FOLDER_OPEN_BACKGROUND_ANIMATE_DURATION = 120;
    protected static final int FULL_GROW = 0;
    protected static final int MAX_FOLDENAME_LEN = 9;
    protected static final int ON_EXIT_CLOSE_DELAY = 100;
    protected static final int PARTIAL_GROW = 1;
    protected static final int REORDER_ANIMATION_DURATION = 230;
    public static int SCROLL_ANIMATION_DURATION = 185;
    static final int STATE_NONE = -1;
    static final int STATE_SMALL = 0;
    static final int STATE_ANIMATING = 1;
    static final int STATE_OPEN = 2;
    private final int BATCH_EDIT_LEN = 10;

    protected static String sDefaultFolderName;
    protected static String sHintText;
    private float mBackgroundAlpha = 0.0f;
    protected Rect mBottomRect = new Rect();
    protected int mCellHeight;
    protected int mCellWidth;
    int[] mChildOffset = new int[2];
    protected View.OnClickListener mClickListener = new View.OnClickListener() {
        public void onClick(View view) {
            if (view != null) {
                String trim = ((TextView) view).getText().toString().trim();
                BaseFolder.this.mFolderName.setText(trim);
                BaseFolder.this.mFolderName.setSelection(trim.length());
            }
        }
    };
    protected View mCurrentDragView;
    protected boolean mDeleteFolderOnDropCompleted = false;
    protected DragController mDragController;
    protected boolean mDragInProgress = false;
    Rect mEditRect = new Rect();
    protected int[] mEmptyCell = new int[2];
    int[] mFirstChildCenter = new int[2];
    protected int mFoldAlphaAnimDuration = 145;
    protected int mFoldCloseAnimDuration = 180;
    protected int mFoldCloseBackDelay = 145;
    protected int mFoldScaleAnimDuration = 185;
    protected TextView mFoldShower;
    protected int mFolderContentPadding;
    protected FrameLayout mFolderHeader;
    int[] mFolderIconCenter = new int[2];
    protected FolderEditText mFolderName;
    protected int mFolderNameHeight;
    protected FrameLayout mFolderQuickTitleContainer;
    public FolderPagedView mFolderContent;
    int[] mFolderScrollPos = new int[2];
    protected final IconCache mIconCache;
    protected Drawable mIconDrawable;
    protected Rect mIconRect = new Rect();
    protected final LayoutInflater mInflater;
    protected InputMethodManager mInputMethodManager;
    protected boolean mIsEditingName = false;
    protected boolean mItemAddedBackToSelfViaIcon = false;
    protected ArrayList<View> mItemsInReadingOrder = new ArrayList<>();
    protected boolean mItemsInvalidated = false;
    protected Launcher mLauncher;
    protected int mMaxCountX;
    protected int mMaxCountY;
    protected int mMaxNumItems;
    protected boolean mMoveInValidArea = false;
    protected View.OnClickListener mNameClickListener = new View.OnClickListener() {
        public void onClick(View view) {
            if (view != null) {
                BaseFolder.this.startEditingFolderName();
            }
        }
    };
    protected float mNewAlpha;
    protected float mNewScaleX;
    protected float mNewScaleY;
    protected Rect mNewSize = new Rect();
    protected float mNewTranslationX;
    protected float mNewTranslationY;
    protected float mOldAlpha;
    protected float mOldScaleX;
    protected float mOldScaleY;
    protected float mOldTranslationX;
    protected float mOldTranslationY;
    protected Alarm mOnExitAlarm = new Alarm();
    public OnAlarmListener mOnExitAlarmListener = new OnAlarmListener() {
        @Override
        public void onAlarm(Alarm alarm) {
            BaseFolder.this.completeDragExit();
        }
    };
    protected boolean mOpenInDragMode = false;
    protected int[] mPreviousTargetCell = new int[2];
    protected boolean mRearrangeOnClose = false;

    protected Alarm mReorderAlarm = new Alarm();
    protected final Alarm mOnScrollHintAlarm = new Alarm();
    protected final Alarm mScrollPauseAlarm = new Alarm();
    protected static final int REORDER_DELAY = 250;

    Rect mScrollRect = new Rect();
    protected int mShadowWidth = 0;
    protected int mState = -1;
    protected boolean mSuppressFolderDeletion = false;
    protected boolean mSuppressOnAdd = false;
    Rect mTailRect = new Rect();
    protected int[] mTargetCell = new int[2];
    protected Rect mTempRect = new Rect();
    protected TextWatcher mTextWatcher = new TextWatcher() {
        private int ch;
        private int delNums = 0;
        private int editEnd;
        private int editStart;

        public void afterTextChanged(Editable editable) {
            this.editStart = BaseFolder.this.mFolderName.getSelectionStart();
            this.editEnd = BaseFolder.this.mFolderName.getSelectionEnd();
            BaseFolder.this.mFolderName.removeTextChangedListener(BaseFolder.this.mTextWatcher);
            while (BaseFolder.this.calculateLength(editable.toString()) > 9) {
                this.ch = editable.toString().charAt(this.editStart - 1);
                if (this.ch < 55296 || this.ch > 57343) {
                    this.delNums = 1;
                } else {
                    this.delNums = 2;
                }
                editable.delete(this.editStart - this.delNums, this.editEnd);
                this.editStart--;
                this.editEnd--;
            }
            BaseFolder.this.mFolderName.addTextChangedListener(BaseFolder.this.mTextWatcher);
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }
    };
    private Toast mToast = null;
    protected Rect mTopRect = new Rect();
    Rect mValidAeraRect = new Rect();

    public BaseFolder(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setAlwaysDrawnWithCacheEnabled(false);
        this.mInflater = LayoutInflater.from(context);
        this.mIconCache = LauncherAppState.getInstance().getIconCache();
        Resources resources = getResources();
        this.mMaxCountX = resources.getInteger(R.integer.folder_max_count_x);
        this.mMaxCountY = resources.getInteger(R.integer.folder_max_count_y);
        this.mMaxNumItems = resources.getInteger(R.integer.folder_max_num_items);
        if (this.mMaxCountX < 0 || this.mMaxCountY < 0 || this.mMaxNumItems < 0) {
            this.mMaxCountX = LauncherModel.getCellCountX();
            this.mMaxCountY = LauncherModel.getCellCountY();
            this.mMaxNumItems = this.mMaxCountX * this.mMaxCountY;
        }
        this.mInputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (sDefaultFolderName == null) {
            sDefaultFolderName = resources.getString(R.string.folder_name);
        }
        if (sHintText == null) {
            sHintText = resources.getString(R.string.folder_hint_text);
        }
        this.mLauncher = (Launcher) context;
        setFocusableInTouchMode(true);
        this.mCellHeight = context.getResources().getDimensionPixelSize(R.dimen.app_folder_cell_height);
        this.mShadowWidth = context.getResources().getDimensionPixelSize(R.dimen.folder_shadow_width);
        this.mFolderContentPadding = context.getResources().getDimensionPixelSize(R.dimen.folder_content_padding);
        if (this.mCellWidth < 0 || this.mCellHeight < 0) {
            int i = this.mFolderContentPadding;
            int integer = context.getResources().getInteger(R.integer.folder_max_count_x);
            int i2 = i * 2;
            int i3 = i2 % integer;
            int i4 = i2 / integer;
            this.mCellWidth = Launcher.getCellWidth() - (i3 != 0 ? i4 + 1 : i4);
            this.mCellHeight = Launcher.getCellHeight();
        }
    }

    private long calculateLength(CharSequence charSequence) {
        double d = 0.0d;
        for (int i = 0; i < charSequence.length(); i++) {
            char charAt = charSequence.charAt(i);
            d += (charAt <= 0 || charAt >= 127) ? 1.0d : 0.5d;
        }
        return Math.round(d);
    }

    public static BaseFolder fromXml(Context context) {
        return null;
    }

    public void animateBackground(boolean z) {
        float startValue;
        int delay = 150;
        float endValue = 0.0f;
        final Drawable background = getBackground();
        Log.w(TAG, "Folder:animateBackground   open = " + z + ", background = " + background);
        if (background != null) {
            int duration = FOLDER_OPEN_BACKGROUND_ANIMATE_DURATION;
            if (!z) {
                startValue = this.mBackgroundAlpha;
                duration = 150;
                delay = 0;
            } else {
                this.mBackgroundAlpha = 0.0f;
                startValue = 0.0f;
                endValue = 1.0f;
            }
            ValueAnimator ofFloat = ValueAnimator.ofFloat(startValue, endValue);
            ofFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                /* class com.ios.home.BaseFolder.AnonymousClass3 */

                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    BaseFolder.this.mBackgroundAlpha = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    if (background != null) {
                        background.setAlpha((int) (BaseFolder.this.mBackgroundAlpha * 255.0f));
                    }
                }
            });
            ofFloat.setDuration((long) duration);
            ofFloat.setStartDelay((long) delay);
            ofFloat.start();
        }
    }

    public void animateClosed() {
    }

    public void animateOpen() {
    }

    public void arrangeChildren(ArrayList<View> arrayList) {
    }

    public void bind(ItemInfo itemInfo) {
    }

    public void cancelExitAlarm() {
        if (this.mOnExitAlarm != null) {
            this.mOnExitAlarm.cancelAlarm();
        }
    }

    public void centerAboutIcon() {
    }

    public void completeDragExit() {
    }

    public boolean createAndAddApps(ItemInfo itemInfo) {
        return true;
    }

    public String cutString(String str) {
        return str.length() > 10 ? str.substring(0, 10) + "..." : str;
    }

    public void dismissEditingName() {
        this.mInputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
        doneEditingFolderName(true);
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        return true;
    }

    public void doneEditingFolderName(boolean z) {
    }

    public boolean findAndSetEmptyCells(ItemInfo itemInfo) {
        return false;
    }

    public boolean folderIsAnimating() {
        return false;
    }

    public Drawable getDragDrawable() {
        return this.mIconDrawable;
    }

    public float[] getDragViewVisualCenter(int i, int i2, int i3, int i4, DragView dragView, float[] fArr) {
        if (fArr == null) {
            fArr = new float[2];
        }
        fArr[0] = (float) ((i - i3) + (dragView.getDragRegion().width() / 2));
        fArr[1] = (float) ((i2 - i4) + (dragView.getDragRegion().height() / 2));
        return fArr;
    }

    //    @Override
    public DropTarget getDropTargetDelegate(DropTarget.DragObject dragObject) {
        return null;
    }

    public View getEditTextRegion() {
        return this.mFolderName;
    }

    public void getFolderEffectiveRegion(Rect rect, boolean z) {
        if (rect == null) {
            rect = new Rect();
        } else {
            rect.set(0, 0, 0, 0);
        }
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        if (dragLayer != null) {
            if (this.mFolderHeader != null && z) {
                dragLayer.getDescendantRectRelativeToSelf(this.mFolderHeader, this.mEditRect);
                rect.union(this.mEditRect);
            }
            if (this.mFolderContent != null) {
                dragLayer.getDescendantRectRelativeToSelf(this.mFolderContent, this.mScrollRect);
                rect.union(this.mScrollRect);
            }
        }
    }

    public FolderIcon getFolderIcon() {
        return null;
    }

    @Override
    public void getHitRect(Rect rect) {
        if (this.mOpenInDragMode) {
            DragLayer dragLayer = this.mLauncher.getDragLayer();
            if (dragLayer != null) {
                dragLayer.getDescendantRectRelativeToSelf(this, rect);
                return;
            }
            return;
        }
        getFolderEffectiveRegion(rect, true);
    }

    public ItemInfo getInfo() {
        return null;
    }

    public View getItemAt(int i) {
        return null;
    }

    public int getItemCount() {
        return 0;
    }

    public ArrayList<View> getItemsInReadingOrder() {
        return getItemsInReadingOrder(true);
    }

    public ArrayList<View> getItemsInReadingOrder(boolean z) {
        return null;
    }

    public View getNameShowRegion() {
        return this.mFoldShower;
    }

    public boolean isEditingName() {
        return this.mIsEditingName;
    }

    public boolean isFull() {
        return false;
    }

    public void makeToast(int i) {
        if (this.mToast != null) {
            this.mToast.cancel();
            this.mToast = null;
        }
        this.mToast = Toast.makeText(getContext(), i, Toast.LENGTH_SHORT);
        this.mToast.show();
    }

    public void notifyDrop() {
        if (this.mDragInProgress) {
            this.mItemAddedBackToSelfViaIcon = true;
        }
    }

    public void onAdd(ItemInfo itemInfo) {
    }

    public void onClick(View view) {
    }

    public void onCloseComplete() {
    }

    @Override
    public void onFlingToDelete(DragObject dragObject, PointF vec) {

    }

    @Override
    public boolean acceptDrop(DragObject dragObject) {
        return false;
    }

    @Override
    public void prepareAccessibilityDrop() {

    }

    @Override
    public void getHitRectRelativeToDragLayer(Rect outRect) {
        if (this.mOpenInDragMode) {
            DragLayer dragLayer = this.mLauncher.getDragLayer();
            if (dragLayer != null) {
                dragLayer.getDescendantRectRelativeToSelf(this, outRect);
                return;
            }
            return;
        }
        getFolderEffectiveRegion(outRect, true);

    }

    @Override
    public void getLocationInDragLayer(int[] loc) {

    }

    @Override
    public void onDragOver(DropTarget.DragObject dragObject) {
        if (this.mOpenInDragMode) {
            getFolderEffectiveRegion(this.mValidAeraRect, true);
            if (dragObject.dragComplete || !this.mMoveInValidArea || this.mValidAeraRect.contains(dragObject.x, dragObject.y)) {
                this.mOnExitAlarm.cancelAlarm();
            } else if (!this.mOnExitAlarm.alarmPending()) {
                this.mOnExitAlarm.setOnAlarmListener(this.mOnExitAlarmListener);
                this.mOnExitAlarm.setAlarm(500);
            }
        }
        Rect rect = new Rect();
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        if (dragLayer != null) {
            dragLayer.getDescendantRectRelativeToSelf(this.mFolderName, rect);
        }
        dragObject.y = (dragObject.y - this.mFolderNameHeight) - rect.top;
    }

    @Override
    public void onDragExit(DragObject dragObject) {

    }

    @Override
    public boolean isDropEnabled() {
        return false;
    }

    @Override
    public void onDrop(DragObject dragObject) {

    }

    @Override
    public void onDragEnter(DragObject dragObject) {

    }

    public void onFinishInflate() {
        super.onFinishInflate();
    }

    //    @Override
    public void onFlingToDelete(DropTarget.DragObject dragObject, int i, int i2, PointF pointF) {
    }

    @Override
    public void onFlingToDeleteCompleted() {
    }

    @Override
    public void onDropCompleted(View target, DragObject d, boolean isFlingToDelete, boolean success) {

    }

    public void onFocusChange(View view, boolean z) {
        if (view == this.mFolderName && z) {
            new Handler().postDelayed(new Runnable() {
                /* class com.ios.home.BaseFolder.AnonymousClass5 */

                public void run() {
                    BaseFolder.this.mInputMethodManager.showSoftInput(BaseFolder.this.mFolderName, 0);
                }
            }, 100);
        }
    }

    public void onItemsChanged() {
        updateTextViewFocus();
    }

    public boolean onLongClick(View view) {
        return true;
    }

    public void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
    }

    public void onRemove(ItemInfo itemInfo) {
    }

    public void onTitleChanged(CharSequence charSequence) {
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        return true;
    }

    public void positionAndSizeAsIcon() {
    }

    //    @Override
    public void preOnDrop(DropTarget.DragObject dragObject) {
        onDragExit(dragObject);
    }

    public boolean readingOrderGreaterThan(int[] iArr, int[] iArr2) {
        return iArr[1] > iArr2[1] || (iArr[1] == iArr2[1] && iArr[0] > iArr2[0]);
    }

    //    @Override
    public void removeDragViewApp(Object obj) {
    }

    public void replaceFolderWithFinalItem(boolean z) {
    }

    //    @Override
    public void restoreDragInfo(DragObject dragObject) {
    }

    public void sendCustomAccessibilityEvent(int i, String str) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager.isEnabled()) {
            AccessibilityEvent obtain = AccessibilityEvent.obtain(i);
            onInitializeAccessibilityEvent(obtain);
            obtain.getText().add(str);
            accessibilityManager.sendAccessibilityEvent(obtain);
        }
    }

    public void setBackground(Drawable drawable) {
        super.setBackground(drawable);
        animateBackground(true);
    }

    public void setDragController(DragController dragController) {
        this.mDragController = dragController;
        mFolderContent.setup(dragController);
    }

    public void setFolderIcon(FolderIcon baseFolderIcon) {
    }

    public void setLauncher(Launcher launcher) {
        this.mLauncher = launcher;
    }

    public void setupContentDimensions(int i) {
    }

    public void setupContentForNumItems(int i) {
        setupContentDimensions(i);
        if (((DragLayer.LayoutParams) getLayoutParams()) == null) {
            DragLayer.LayoutParams layoutParams = new DragLayer.LayoutParams(0, 0);
            layoutParams.customPosition = true;
            setLayoutParams(layoutParams);
        }
        centerAboutIcon();
    }

    public void startEditingFolderName() {
        this.mFolderName.setHint("");
        this.mIsEditingName = true;
    }

    @Override
    public boolean supportsFlingToDelete() {
        return false;
    }

    @Override
    public boolean supportsAppInfoDropTarget() {
        return false;
    }

    @Override
    public boolean supportsDeleteDropTarget() {
        return false;
    }

    @Override
    public float getIntrinsicIconScaleFactor() {
        return 0;
    }

    public void updateItemLocationsInDatabase() {
    }

    public void updateTextViewFocus() {
        View itemAt = getItemAt(getItemCount() - 1);
        getItemAt(getItemCount() - 1);
        if (itemAt != null) {
            this.mFolderName.setNextFocusDownId(itemAt.getId());
            this.mFolderName.setNextFocusRightId(itemAt.getId());
            this.mFolderName.setNextFocusLeftId(itemAt.getId());
            this.mFolderName.setNextFocusUpId(itemAt.getId());
        }
    }
}
