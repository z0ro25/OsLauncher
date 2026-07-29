
package com.amz.ios.launcher;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Advanceable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.amz.ios.blurkit.BlurKit;
import com.amz.ios.database.HiddenAppManager;
import com.amz.ios.gpuimage.GausianBlur;
import com.amz.ios.ioslite.common.ContextHelper;
import com.amz.ios.ioslite.common.LiteAction;
import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.Router;
import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;
import com.amz.ios.ioslite.common.analytics.UMEventConstants;
import com.amz.ios.ioslite.common.anim.PropertyHolderUtis;
import com.amz.ios.ioslite.common.debug.DebugUtil;
import com.amz.ios.ioslite.common.debug.MemoryDumpActivity;
import com.amz.ios.ioslite.common.debug.WeightWatcher;
import com.amz.ios.ioslite.common.launcher.LauncherRouter;
import com.amz.ios.ioslite.common.launcher.LauncherRouter.LauncherDelegate;
import com.amz.ios.ioslite.common.launcher.LauncherSettingCallback;
import com.amz.ios.ioslite.common.launcher.LauncherSettingSubject;
import com.amz.ios.ioslite.common.launcher.LauncherStateManager;
import com.amz.ios.ioslite.common.launcher.LauncherWallpaperManager;
import com.amz.ios.ioslite.common.launcher.LeftCustomContentCallbacks;
import com.amz.ios.ioslite.common.update.VersionUpdateManager;
import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.ioslite.common.util.CommonUtilities;
import com.amz.ios.ioslite.common.util.DeviceInfoUtil;
import com.amz.ios.ioslite.common.util.FileUtil;
import com.amz.ios.ioslite.common.util.PermissionUtil;
import com.amz.ios.launcher.anim.explosion.ExplosionField;
import com.amz.ios.launcher.applibrary.AppCategory;
import com.amz.ios.launcher.applibrary.AppsLibraryLayout;
import com.amz.ios.launcher.applibrary.DragAppsLibraryLayout;
import com.amz.ios.launcher.applibrary.OpenedLibraryView;
import com.amz.ios.launcher.assembly.LeftCustomContentUtil;
import com.amz.ios.launcher.assembly.SearchWidgetUtil;
import com.amz.ios.launcher.awareness.AppUsagesModel;
import com.amz.ios.launcher.awareness.SensorGestureModel;
import com.amz.ios.launcher.awareness.UnreadCallbacks;
import com.amz.ios.launcher.awareness.UnreadLoaderCompact;
import com.amz.ios.launcher.bounce.BouncyRecyclerView;
import com.amz.ios.launcher.compat.AppWidgetManagerCompat;
import com.amz.ios.launcher.compat.LauncherActivityInfoCompat;
import com.amz.ios.launcher.compat.LauncherAppsCompat;
import com.amz.ios.launcher.compat.LauncherAppsCompatVO;
import com.amz.ios.launcher.compat.UserHandleCompat;
import com.amz.ios.launcher.compat.UserManagerCompat;
import com.amz.ios.launcher.config.GestureEventModel;
import com.amz.ios.launcher.config.LauncherSettingActivity;
import com.amz.ios.launcher.config.Settings;
import com.amz.ios.launcher.dialog.CommonDialog;
import com.amz.ios.launcher.folder.Folder;
import com.amz.ios.launcher.leftpage.custom.CustomZoomButton;
import com.amz.ios.launcher.leftpage.database.WidgetInfo;
import com.amz.ios.launcher.leftpage.drawables.ClockDrawable;
import com.amz.ios.launcher.leftpage.views.CustomContentView;
import com.amz.ios.launcher.logging.FileLog;
import com.amz.ios.launcher.model.WidgetsModel;
import com.amz.ios.launcher.notification.NotificationListener;
import com.amz.ios.launcher.popup.PopupContainerWithArrow;
import com.amz.ios.launcher.popup.PopupDataProvider;
import com.amz.ios.launcher.provider.AppTypeProvider;
import com.amz.ios.launcher.searchview.SearchPullDetector;
import com.amz.ios.launcher.searchview.SearchViewLayout;
import com.amz.ios.launcher.shortcut.BatterySave;
import com.amz.ios.launcher.shortcut.Discovery;
import com.amz.ios.launcher.shortcut.Theme;
import com.amz.ios.launcher.shortcut.Wallpaper;
import com.amz.ios.launcher.shortcuts.DeepShortcutManager;
import com.amz.ios.launcher.shortcuts.ShortcutKey;
import com.amz.ios.launcher.slideup.SlidingUpPanelLayout;
import com.amz.ios.launcher.states.InternalStateHandler;
import com.amz.ios.launcher.theme.ThemeManager;
import com.amz.ios.launcher.util.AdminManager;
import com.amz.ios.launcher.util.BlurWallpaperProvider;
import com.amz.ios.launcher.util.ComponentKey;
import com.amz.ios.launcher.util.CubicInterpolate;
import com.amz.ios.launcher.util.ItemInfoMatcher;
import com.amz.ios.launcher.util.LongArrayMap;
import com.amz.ios.launcher.util.MultiHashMap;
import com.amz.ios.launcher.util.PackageUserKey;
import com.amz.ios.launcher.util.SystemUiController;
import com.amz.ios.launcher.util.Themes;
import com.amz.ios.launcher.util.Thunk;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.launcher.widget.PendingAddShortcutInfo;
import com.amz.ios.launcher.widget.PendingAddWidgetInfo;
import com.amz.ios.launcher.widget.SlidingUpWidgetsCellAppStyle;
import com.amz.ios.launcher.widget.WidgetHostViewLoader;
import com.amz.ios.launcher.widget.WidgetsContainerView;
import com.amz.ios.rate.LauncherSharePrefUtils;
import com.amz.ios.rate.RatingDialog;
import com.amz.varunjohn1990.iosdialogs4android.IOSDialog;
import com.amz.varunjohn1990.iosdialogs4android.IOSDialogButton;
import com.amz.varunjohn1990.iosdialogs4android.IOSDialogMultiOptionsListeners;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.ios.boot.iosboot.LauncherGuideManager;

import org.litepal.LitePal;
import org.litepal.LitePalDB;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Launcher extends LauncherBaseActivity implements View.OnClickListener,
        OnLongClickListener,
        LauncherModel.Callbacks,
        View.OnTouchListener,
        PagedView.PageSwitchListener,
        LauncherProviderChangeListener,
        LauncherDelegate,
        LauncherSettingCallback,
        UnreadCallbacks,
        AppUsagesModel.Callbacks,
        SearchViewLayout.SearchViewLayoutDelegate,
        LauncherDragLayer.PanelSlideListener {

    static final String TAG = "LauncherTag";
    static final boolean LOGD = false;
    static final int SEARCH_ACTIVITY_REQUEST_CODE = 109;
    static final boolean PROFILE_STARTUP = false;
    static final boolean DEBUG_WIDGETS = false;
    static final boolean DEBUG_STRICT_MODE = false;
    static final boolean DEBUG_RESUME_TIME = false;
    static final boolean DEBUG_DUMP_LOG = false;
    public static final String PREF_KEY = "com.aisoft.iosLauncher";
    static final boolean ENABLE_DEBUG_INTENTS = false; // allow DebugIntents to run

    static int PERMISSION_ALL = 100;
    static final String CUSTOM_CONTENT_WIDGET_DB_NAME = "widgetinfo";

    private static final int REQUEST_CREATE_SHORTCUT = 1;
    private static final int REQUEST_CREATE_APPWIDGET = 5;
    private static final int REQUEST_PICK_APPWIDGET = 9;
    private static final int REQUEST_PICK_WALLPAPER = 10;

    private static final int REQUEST_BIND_APPWIDGET = 11;
    private static final int REQUEST_RECONFIGURE_APPWIDGET = 12;

    private static final float BOUNCE_ANIMATION_TENSION = 1.3f;

    public static final String DOWNLOAD_FOLDER_NAME = "IOSOS";

    // IntentStarter uses request codes starting with this. This must be greater than all activity request codes used internally.
    protected static final int REQUEST_LAST = 100;

    static final int SCREEN_COUNT = 5;

    static final String INTENT_EXTREA_LAUNCHE_CUSTOM_SCREEN = "launch_custom_screen";
    static final String INTENT_EXTREA_LAUNCHE_NEW_THEME = "launch_apply_theme";

    // To turn on these properties, type
    // adb shell setprop log.tag.PROPERTY_NAME [VERBOSE | SUPPRESS]
    static final String DUMP_STATE_PROPERTY = "launcher_dump_state";

    // The Intent extra that defines whether to ignore the launch animation
    static final String INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION = "com.amz.ios.launcher.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION";
    // Type: int
    private static final String RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen";
    // Type: int
    private static final String RUNTIME_STATE = "launcher.state";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_CONTAINER = "launcher.add_container";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_SCREEN = "launcher.add_screen";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_CELL_X = "launcher.add_cell_x";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_CELL_Y = "launcher.add_cell_y";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_SPAN_X = "launcher.add_span_x";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_SPAN_Y = "launcher.add_span_y";
    // Type: parcelable
    private static final String RUNTIME_STATE_PENDING_ADD_WIDGET_INFO = "launcher.add_widget_info";
    // Type: parcelable
    private static final String RUNTIME_STATE_PENDING_ADD_WIDGET_ID = "launcher.add_widget_id";
    // Type: int[]
    private static final String RUNTIME_STATE_VIEW_IDS = "launcher.view_ids";

    private static final String QSB_WIDGET_ID = "qsb_widget_id";

    private static final String QSB_WIDGET_PROVIDER = "qsb_widget_provider";

    public static final String USER_HAS_MIGRATED = "launcher.user_migrated_from_old_data";
    public boolean mBeginDragLibraryPage;
    public boolean mBeginDragLeftPage;
    public LauncherAppWidgetHostView mOpenAppWidgetHostView = null;

    private Handler collapseNotificationHandler;
    private ThreadPoolExecutor mThreadExecutor;
    private BlurWallpaperProvider mBlurWallpaperProvider;
    private BlurScreenLayout mBlurBackgroundView;
    private BlurScreenLayout mFloatingMenuBlurBg;
    private CustomZoomButton mAddWidgetBtn;
    private CustomZoomButton mAddWidgetDoneBtn;
    public SlidingUpWidgetsCellAppStyle mWidgetsAppStyle;
    public WidgetsContainerView mWidgetsView;

    boolean currentFocus;

    // To keep track of activity's foreground/background status
    boolean isPaused;

    public boolean showingFloatingMenu = false;
    public boolean mIsShaking = false;

    /**
     * The different states that Launcher can be in.
     */
    enum State {
        NONE, WORKSPACE, APPS, APPS_SPRING_LOADED, WIDGETS, WIDGETS_SPRING_LOADED
    }

    @Thunk
    State mState = State.WORKSPACE;
    @Thunk
    LauncherStateTransitionAnimation mStateTransitionAnimation;

    private boolean mIsSafeModeEnabled;
    private boolean mEnableIOSKnow;
    private boolean mEnableRecentlyApps;
    private LauncherGuideManager mLauncherGuideManager;

    public static final int APP_ANIM_SYSTEM = 0;
    public static final int APP_ANIM_IN_RIGHT = 1;
    public static final int APP_ANIM_IN_LEFT = 2;
    public static final int APP_ANIM_IN_UP = 3;
    public static final int APP_ANIM_IN_BOTTOM = 4;
    public static final int APP_ANIM_ZOOM = 5;
    public static final int APP_ANIM_ROTATE = 6;
    public static final int APP_ANIM_FADE = 7;

    private int mAppAnimationStyle;
    private int mTempAppAnimationStyle = -1;

    private int mWorkSpacePageIndex = -1;

    static final int APPWIDGET_HOST_ID = 1024;
    public static final int EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT = 300;
    private static final int ON_ACTIVITY_RESULT_ANIMATION_DELAY = 500;
    private static final int ACTIVITY_START_DELAY = 1000;

    private HashMap<Integer, Integer> mItemIdToViewId = new HashMap<Integer, Integer>();
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    // How long to wait before the new-shortcut animation automatically pans the workspace
    private static int NEW_APPS_PAGE_MOVE_DELAY = 500;
    private static int NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS = 5;
    @Thunk
    static int NEW_APPS_ANIMATION_DELAY = 500;

    private final BroadcastReceiver mCloseSystemDialogsReceiver = new CloseSystemDialogsIntentReceiver();

    private LayoutInflater mInflater;
    private LoadingView mLoadingView;
    private View mWallpaperBackground;

    @Thunk
    Workspace mWorkspace;
    private LauncherRootView mLauncherView;
    private View mPageIndicators;

    @Thunk
    DragLayer mDragLayer;
    private DragController mDragController;
    private View mWeightWatcher;
    private ExplosionField mDeleteExplosionField;

    private AppWidgetManagerCompat mAppWidgetManager;
    private LauncherAppWidgetHost mAppWidgetHost;

    @Thunk
    ItemInfo mPendingAddInfo = new ItemInfo();
    private LauncherAppWidgetProviderInfo mPendingAddWidgetInfo;
    private int mPendingAddWidgetId = -1;

    private int[] mTmpAddItemCellCoordinates = new int[2];

    private WorkspaceRootView mWorkspaceRootView;

    //DockBar Control
    private Hotseat mHotseat;

    //Launcher Settings Pannel
    public View mPageIndicatorContainer;
    //    private SearchDropTargetBar mSearchDropTargetBar;
    public DragAppsLibraryLayout mDragAppsLibraryLayout;
    public BlurScreenLayout mSliderBlurBg;

    public AppsLibraryLayout mAppsLibraryLayout;
    public CustomContentView mCustomContentView;
    public SearchViewLayout mSearchViewLayout;

    // Main container view for the all apps screen.

    SearchPullDetector mSearchPullDetector;

    // Main container view and the model for the widget tray screen.
    @Thunk
    WidgetsModel mWidgetsModel;

    private boolean mAutoAdvanceRunning = false;
    private AppWidgetHostView mQsb;

    private Bundle mSavedState;
    // We set the state in both onCreate and then onNewIntent in some cases, which causes both
    // scroll issues (because the workspace may not have beenf measured yet) and extra work.
    // Instead, just save the state that we need to restore Launcher to, and commit it in onResume.
    private State mOnResumeState = State.NONE;
    private boolean mOnResumeToCusContentScreen = false;

    @Thunk
    boolean mWorkspaceLoading = true;

    private boolean mPaused = true;
    private boolean mRestoring;
    private boolean mWaitingForResult;
    private boolean mOnResumeNeedsLoad;
    private boolean mIsGuideMode;
    private boolean mResetDesktopLoad;
    private boolean mHasLoaderCompletedOnce;

    private ArrayList<Runnable> mBindOnResumeCallbacks = new ArrayList<Runnable>();
    private ArrayList<Runnable> mOnResumeCallbacks = new ArrayList<Runnable>();

    private Bundle mSavedInstanceState;

    private AppUsagesModel mAppUsagesModel;
    private LauncherModel mModel;
    public IconCache mIconCache;
    @Thunk
    boolean mUserPresent = true;
    private boolean mVisible = false;
    private boolean mHasFocus = false;
    private boolean mAttached = false;

    private SensorGestureModel mSensorGestureModel;

    private PopupDataProvider mPopupDataProvider;

    private LauncherClings mClings;
    private View.OnTouchListener mHapticFeedbackTouchListener;

    // Related to the auto-advancing of widgets
    private final int ADVANCE_MSG = 1;
    private final int mAdvanceInterval = 20000;
    private final int mAdvanceStagger = 250;
    private long mAutoAdvanceSentTime;
    private long mAutoAdvanceTimeLeft = -1;
    @Thunk
    HashMap<View, AppWidgetProviderInfo> mWidgetsToAdvance = new HashMap<View, AppWidgetProviderInfo>();
    private final ArrayList<Integer> mSynchronouslyBoundPages = new ArrayList<Integer>();
    private static final boolean DISABLE_SYNCHRONOUS_BINDING_CURRENT_PAGE = false;

    static final ArrayList<String> sDumpLogs = new ArrayList<String>();
    static Date sDateStamp = new Date();
    static DateFormat sDateFormat =
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    static long sRunStart = System.currentTimeMillis();

    // We only want to get the SharedPreferences once since it does an FS stat each time we get
    // it from the context.
    private SharedPreferences mSharedPrefs;

    // Holds the page that we need to animate to, and the icon views that we need to animate up
    // when we scroll to that page on resume.
    @Thunk
    ImageView mFolderIconImageView;
    private Bitmap mFolderIconBitmap;
    private Canvas mFolderIconCanvas;
    private Rect mRectForFolderAnimation = new Rect();

    private DeviceProfile mDeviceProfile;

    // This is set to the view that launched the activity that navigated the user away from
    // launcher. Since there is no callback for when the activity has finished launching, enable
    // the press state and keep this reference to reset the press state when we return to launcher.
    private BubbleTextView mWaitingForResume;

    protected static HashMap<String, IOSAppWidget> sIOSAppWidgets =
            new HashMap<String, IOSAppWidget>();

    private static final boolean ENABLE_CUSTOM_WIDGET_TEST = false;

    static {
        if (ENABLE_CUSTOM_WIDGET_TEST) {
            sIOSAppWidgets.put(DummyWidget.class.getName(), new DummyWidget());
        }
    }

    @Thunk
    Runnable mBuildLayersRunnable = new Runnable() {
        public void run() {
            if (mWorkspace != null) {
                mWorkspace.buildPageHardwareLayers();
            }
        }
    };

    Runnable mOpenWidgetViewRunnable = new Runnable() {
        @Override
        public void run() {
            cancelShakingAnimation();
            mWidgetsView.requestFocus();
            mWidgetsView.setVisibility(View.VISIBLE);
            mWidgetsView.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
        }
    };

    private static PendingAddArguments sPendingAddItem;

    @Thunk
    static class PendingAddArguments {
        int requestCode;
        Intent intent;
        long container;
        long screenId;
        int cellX;
        int cellY;
        int appWidgetId;
    }

    private Stats mStats;
    FocusIndicatorView mFocusHandler;
    private boolean mRotationEnabled = false;
    private float mCurrentFontScale;
    private ArrayList<ItemInfo> listHiddenApp = new ArrayList<>();

    private BroadcastReceiver updateHiddenAppBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("UPDATE_UNHIDDENAPP")) {
                mModel.forceReload();
            }
        }
    };

    public static OpenedLibraryView viewItem4GroupAppLibrary;

    private ReviewManager manager;
    private ReviewInfo reviewInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG_STRICT_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        Log.i(TAG, "onCreate");
        HiddenAppManager.INSTANCE.initDataBase(this);
        listHiddenApp = (ArrayList<ItemInfo>) HiddenAppManager.INSTANCE.getAllHiddenApp();
        Log.d(TAG, "onCreatexxx: " + listHiddenApp.size());
        for (ItemInfo info : listHiddenApp) {
            Log.d(TAG, "onCreatexxx: " + info.getTargetComponent());
        }

        getWindow().setExitTransition(null);
        getWindow().setEnterTransition(null);
        getWindow().setAllowReturnTransitionOverlap(false);
        getWindow().setAllowEnterTransitionOverlap(false);
        getWindow().setTransitionBackgroundFadeDuration(0L);
        Log.e("alshdflkasdf", "onCreate: ");
        hideNavigationBar(getWindow());
        getWindow().setStatusBarColor(0);
        getWindow().setNavigationBarColor(Color.parseColor("#01ffffff"));

        useDB();

        mSavedState = savedInstanceState;
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        mThreadExecutor = new ThreadPoolExecutor(availableProcessors + 1, (availableProcessors * 2) + 1, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue());
        mBlurWallpaperProvider = new BlurWallpaperProvider(this);
        super.onCreate(mSavedState);
        mOnResumeState = State.WORKSPACE;
        initApp();
        checkPermissionGranted();
        mSearchPullDetector = new SearchPullDetector(this, mDeviceProfile.heightPx);

        Log.d(TAG, "onCreate()");

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("UPDATE_UNHIDDENAPP");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(updateHiddenAppBroadcast, intentFilter, RECEIVER_EXPORTED);
        } else registerReceiver(updateHiddenAppBroadcast, intentFilter);

        LauncherSharePrefUtils.increaseCountOpenApp(this);
    }


    public void showRateDialog() {
        final RatingDialog ratingDialog = new RatingDialog(this);
        ratingDialog.init(new RatingDialog.OnPress() {
            @Override
            public void send(float star) {
                ratingDialog.dismiss();
                Toast.makeText(Launcher.this, getString(R.string.rate_success), Toast.LENGTH_SHORT).show();
                LauncherSharePrefUtils.forceRated(Launcher.this);
            }

            @Override
            public void rating(float star) {
                manager = ReviewManagerFactory.create(Launcher.this);
                Task<ReviewInfo> request = manager.requestReviewFlow();
                request.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        reviewInfo = task.getResult();
                        Task<Void> flow = manager.launchReviewFlow(Launcher.this, reviewInfo);
                        flow.addOnSuccessListener(aVoid -> {
                            LauncherSharePrefUtils.forceRated(Launcher.this);
                            ratingDialog.dismiss();
                        });
                    } else {
                        ratingDialog.dismiss();
                    }
                });
            }

            @Override
            public void cancel() {
            }

            @Override
            public void later() {
                ratingDialog.dismiss();
            }
        });
        ratingDialog.show();
    }


    public void useDB() {
        LitePal.initialize(this);
        LitePalDB db = new LitePalDB(CUSTOM_CONTENT_WIDGET_DB_NAME, 1);
        db.addClassName(WidgetInfo.class.getName());
        LitePal.use(db);
    }

    public ThreadPoolExecutor getThreadExecutor() {
        if (mThreadExecutor == null) {
            int availableProcessors = Runtime.getRuntime().availableProcessors();
            mThreadExecutor = new ThreadPoolExecutor(availableProcessors + 1, (availableProcessors * 2) + 1, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue());
        }
        return mThreadExecutor;
    }

    public BlurWallpaperProvider getBlurWallpaperProvider() {
        if (mBlurWallpaperProvider == null)
            mBlurWallpaperProvider = new BlurWallpaperProvider(this);
        return mBlurWallpaperProvider;
    }

    public void hideNavigationBar(Window window) {
        Log.e("alshdflkasdf", "hideNavigationbar");
        View decorView = window.getDecorView();
        WindowInsetsControllerCompat windowInsetsController;
        windowInsetsController = Build.VERSION.SDK_INT >= 30
                ? ViewCompat.getWindowInsetsController(decorView) : new WindowInsetsControllerCompat(window, decorView);

        if (windowInsetsController == null) {
            return;
        }

        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars());
        windowInsetsController.hide(WindowInsetsCompat.Type.systemGestures());

        decorView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            new Handler().postDelayed(()->{
                WindowInsetsControllerCompat windowInsetsController1;
                windowInsetsController1 = Build.VERSION.SDK_INT >= 30
                        ? ViewCompat.getWindowInsetsController(decorView) : new WindowInsetsControllerCompat(window, decorView);

                if (windowInsetsController1 == null) {
                    return;
                }
                windowInsetsController1.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                windowInsetsController1.hide(WindowInsetsCompat.Type.navigationBars());
                windowInsetsController1.hide(WindowInsetsCompat.Type.systemGestures());
            },3000);
        });
    }

    private void checkPermissionGranted() {
        ArrayList<String> permissionList = new ArrayList<>();

//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
//                == PackageManager.PERMISSION_DENIED) {
//            permissionList.add(Manifest.permission.READ_CALL_LOG);
//        }
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//                == PackageManager.PERMISSION_DENIED) {
//            permissionList.add(Manifest.permission.CAMERA);
//        }
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
//                == PackageManager.PERMISSION_DENIED) {
//            permissionList.add(Manifest.permission.READ_PHONE_STATE);
//        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
//                == PackageManager.PERMISSION_DENIED) {
//            permissionList.add(Manifest.permission.READ_CALENDAR);
//        }
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
//                == PackageManager.PERMISSION_DENIED) {
//            permissionList.add(Manifest.permission.READ_CONTACTS);
//        }
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.EXPAND_STATUS_BAR)
//                == PackageManager.PERMISSION_DENIED) {
//            permissionList.add(Manifest.permission.EXPAND_STATUS_BAR);
//        }

        if (permissionList.size() > 0) {
            String[] strPermissions = new String[permissionList.size()];
            strPermissions = permissionList.toArray(strPermissions);
            permissionList.clear();
            ActivityCompat.requestPermissions(this, strPermissions, PERMISSION_ALL);
            return;
        }

//        initApp();
    }

    //todo start app
    private void initApp() {
        mSharedPrefs = getSharedPreferences(LauncherAppState.getSharedPreferencesKey(),
                Context.MODE_PRIVATE);
        BlurKit.init(this.getApplicationContext());
        mLauncherGuideManager = LauncherGuideManager.getInstance(this.getApplicationContext());
        showFirstRunActivity();
        LauncherAppState.setApplicationContext(getApplicationContext());
        LauncherAppState app = LauncherAppState.getInstance();

        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mDeviceProfile = app.getInvariantDeviceProfile().portraitProfile;
        mIsSafeModeEnabled = getPackageManager().isSafeMode();
        mModel = app.setLauncher(this);
        mIconCache = app.getIconCache();
        mAppUsagesModel = app.getAppUsagesModel();
        mAppUsagesModel.initialize(this);

        mDragController = new DragController(this);
        mInflater = getLayoutInflater();
        mStateTransitionAnimation = new LauncherStateTransitionAnimation(this);

        mStats = new Stats(this);

        mAppWidgetManager = AppWidgetManagerCompat.getInstance(this);

        mAppWidgetHost = new LauncherAppWidgetHost(this, APPWIDGET_HOST_ID);
        mAppWidgetHost.startListening();

        // If we are getting an onCreate, we can actually preempt onResume and unset mPaused here,
        // this also ensures that any synchronous binding below doesn't re-trigger another
        // LauncherModel load.
        mPaused = false;

        mRotationEnabled = getResources().getBoolean(R.bool.allow_rotation);

        if (!mRotationEnabled) {
            mRotationEnabled = Utilities.isAllowRotationPrefEnabled(this);
        }

        setOrientation();

        if (PROFILE_STARTUP) {
            android.os.Debug.startMethodTracing(FileUtil.getTraceFilesDir() + "/launcher");
        }

        setContentView(R.layout.launcher);

        setupViews();
        mDeviceProfile.layout(this);

        mCurrentFontScale = getResources().getConfiguration().fontScale;

        mPopupDataProvider = new PopupDataProvider(this);

        restoreState(mSavedState);

        if (PROFILE_STARTUP) {
            android.os.Debug.stopMethodTracing();
        }

        if (!mRestoring) {
            DebugUtil.debugLaunch(TAG, "startLoader");
            if (DISABLE_SYNCHRONOUS_BINDING_CURRENT_PAGE) {
                // If the user leaves launcher, then we should just load items asynchronously when
                // they return.
                mModel.startLoader(PagedView.INVALID_RESTORE_PAGE);
            } else {
                // We only load the page synchronously if the user rotates (or triggers a
                // configuration change) while launcher is in the foreground
                mModel.startLoader(mWorkspace.getRestorePage());
            }
        }

        IntentFilter filter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        ContextHelper.registerReceiver(this, mCloseSystemDialogsReceiver, filter);

        if (DeviceInfoUtil.isIOSOs()) {
            mSensorGestureModel = new SensorGestureModel(this);
        }
        mAppAnimationStyle = Settings.getAppAnimationStyle(this);
        mEnableRecentlyApps = Partner.getBoolean(this, Partner.FEATURE_ALL_APP_RECENTLY_APPS_ENABLE);
        LauncherRouter.setLauncherDelegate(this);

        // FIXME: 2023.11.11 KDH AppsDragLayout Set
        mDragAppsLibraryLayout = findViewById(R.id.sliding_pane_layout);
        mDragAppsLibraryLayout.setPanelSlideListener(this);

        mAppsLibraryLayout = findViewById(R.id.apps_library_layout);
        mCustomContentView = findViewById(R.id.left_page);
        mSliderBlurBg = findViewById(R.id.blur_apps_library_background);

        mSearchViewLayout = findViewById(R.id.search_view);
        mSearchViewLayout.setSearchViewLayoutDelegate(this);

        mBlurBackgroundView = findViewById(R.id.blur_background);
        mFloatingMenuBlurBg = new BlurScreenLayout(this, null);
        mFloatingMenuBlurBg.setClickable(true);
        mFloatingMenuBlurBg.setFocusable(true);
        mFloatingMenuBlurBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "closeFloatingMenu mFloatingMenuBlurBg.setOnClickListener: ");
                closeFloatingMenu();
                if (isOpeningFolder()) {
                    mWorkspace.getOpenFolder().showPopUpItemAfterClose();
                }
            }
        });

//        mFloatingMenuBlurBg.mHandler1.obtainMessage(2,false).sendToTarget();

//        BlurScreenLayout.a(mFloatingMenuBlurBg,null);

        mAddWidgetBtn = findViewById(R.id.add_widgets);
        mAddWidgetBtn.setOnTouchListener(getHapticFeedbackTouchListener());
        mAddWidgetBtn.setOnLongClickListener(v -> {
                    v.performClick();
                    return false;
                }
        );

        mAddWidgetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditMenu(v);
            }
        });

        mAddWidgetDoneBtn = findViewById(R.id.add_widgets_done);
        mAddWidgetDoneBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cancelShakingAnimation();
                    }
                }
        );

        mWidgetsAppStyle = findViewById(R.id.sliding_up_widgets_app_style);
        mWidgetsView = findViewById(R.id.widgets_view);

//        if(this.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED){
//                this.requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG},1);
//                Log.d("585=====>xiaopeng","Manifest.permission.READ_CALL_LOG.......");
//        }
//        ios.gejun remove permission
//        checkAndRequestPermission();
//        SwitchResponseSubject.registerObserver(getApplicationContext(), this);
//        NetworkManager.handleNetConnect(getApplicationContext());

        if (Partner.getBoolean(this, Partner.FEATURE_ICON_UNREAD_SUPPORT, true)) {
            mUnreadLoader = UnreadLoaderCompact.getInstance(this);
            // initialize unread loader
            if (mUnreadLoader != null) {
//                mUnreadLoader.initInitFlag();
                mUnreadLoader.initialize(this, this);
                mUnreadLoader.loadAndInitUnreadShortcuts();
            }
        }

        getSystemUiController().updateUiState(SystemUiController.UI_STATE_BASE_WINDOW, Themes.getAttrBoolean(this, R.attr.isWorkspaceDarkText));

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onCreate(mSavedState);
        }

        this.m_imgFolderBlurBg = (ImageView) findViewById(R.id.launcher_img_folder_bg_blur);
        GausianBlur.getInstance().setup(this);
    }

    @Override
    public void onSettingsChanged(String settings, boolean value) {
    }

    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onLauncherProviderChange() {

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onLauncherProviderChange();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        dispatchDeviceProfileChanged();
        super.onConfigurationChanged(newConfig);
    }

    /**
     * To be overridden by subclasses to populate the custom content container and call
     * {@link #addToCustomContentPage}. This will only be invoked if
     */
    protected void populateCustomContentContainer() {
        View leftScreenPage = null;
        LeftCustomContentCallbacks callback = null;
        try {
            leftScreenPage = getInflater().inflate(R.layout.left_custom_content_view, null);
            callback = (LeftCustomContentCallbacks) leftScreenPage;
        } catch (Exception e) {
            Log.e(TAG, "populateCustomContentContainer fail", e);
        }

        if (leftScreenPage != null && callback != null) {
            addToCustomContentPage(leftScreenPage, callback, "leftscreen");
        }
    }

    /**
     * Invoked by subclasses to signal a change to the addCustomContentToLeft value to
     * ensure the custom content page is added or removed if necessary.
     */
    protected void invalidateHasCustomContentToLeft(boolean enable) {
        if (mWorkspace == null || mWorkspace.getScreenOrder().isEmpty()) {
            // Not bound yet, wait for bindScreens to be called.
            return;
        }

        if (enable) {
            if (!mWorkspace.hasCustomContent() && LeftCustomContentUtil.hasCustomContentToLeft(this)) {
                // Create the custom content page and call the subclass to populate it.
                mWorkspace.createCustomContentContainer();
                populateCustomContentContainer();
            }
        } else {
            if (mWorkspace.hasCustomContent()) {
                mWorkspace.removeCustomContentPage();
            }
        }
    }

    public static Launcher getLauncher(Context context) {
        if (context instanceof Launcher) {
            return (Launcher) context;
        }
        return ((Launcher) ((ContextWrapper) context).getBaseContext());
    }

    public Stats getStats() {
        return mStats;
    }

    public LayoutInflater getInflater() {
        return mInflater;
    }

    public boolean isDraggingEnabled() {
        // We prevent dragging when we are loading the workspace as it is possible to pick up a view
        // that is subsequently removed from the workspace in startBinding().
        return !isWorkspaceLoading();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static int generateViewId() {
        if (Utilities.ATLEAST_JB_MR1) {
            return View.generateViewId();
        } else {
            // View.generateViewId() is not available. The following fallback logic is a copy
            // of its implementation.
            for (; ; ) {
                final int result = sNextGeneratedId.get();
                // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
                int newValue = result + 1;
                if (newValue > 0x00FFFFFF)
                    newValue = 1; // Roll over to 1, not 0.
                if (sNextGeneratedId.compareAndSet(result, newValue)) {
                    return result;
                }
            }
        }
    }

    public int getViewIdForItem(ItemInfo info) {
        // This cast is safe given the > 2B range for int.
        int itemId = (int) info.id;
        if (mItemIdToViewId.containsKey(itemId)) {
            return mItemIdToViewId.get(itemId);
        }
        int viewId = generateViewId();
        mItemIdToViewId.put(itemId, viewId);
        return viewId;
    }

    /**
     * Returns whether we should delay spring loaded mode -- for shortcuts and widgets that have
     * a configuration step, this allows the proper animations to run after other transitions.
     */
    private long completeAdd(PendingAddArguments args) {
        long screenId = args.screenId;
        if (args.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            // When the screen id represents an actual screen (as opposed to a rank) we make sure
            // that the drop page actually exists.
            screenId = ensurePendingDropLayoutExists(args.screenId);
        }

        switch (args.requestCode) {
            case REQUEST_CREATE_SHORTCUT:
                completeAddShortcut(args.intent, args.container, screenId, args.cellX,
                        args.cellY);
                break;
            case REQUEST_CREATE_APPWIDGET:
                completeAddAppWidget(args.appWidgetId, args.container, screenId, null, null);
                break;
            case REQUEST_RECONFIGURE_APPWIDGET:
                completeRestoreAppWidget(args.appWidgetId);
                break;
        }
        // Before adding this resetAddInfo(), after a shortcut was added to a workspace screen,
        // if you turned the screen off and then back while in All Apps, Launcher would not
        // return to the workspace. Clearing mAddInfo.container here fixes this issue
        resetAddInfo();
        return screenId;
    }

    private void handleActivityResult(
            final int requestCode, final int resultCode, final Intent data) {
        // Reset the startActivity waiting flag
        setWaitingForResult(false);
        final int pendingAddWidgetId = mPendingAddWidgetId;
        mPendingAddWidgetId = -1;

        Runnable exitSpringLoaded = new Runnable() {
            @Override
            public void run() {
                exitSpringLoadedDragModeDelayed((resultCode != RESULT_CANCELED),
                        EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT, null);
            }
        };

        if (requestCode == REQUEST_BIND_APPWIDGET) {
            // This is called only if the user did not previously have permissions to bind widgets
            final int appWidgetId = data != null ?
                    data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) : -1;
            if (resultCode == RESULT_CANCELED) {
                completeTwoStageWidgetDrop(RESULT_CANCELED, appWidgetId);
                if (!mWorkspace.isInOverviewMode()) {
                    mWorkspace.removeExtraEmptyScreenDelayed(true, exitSpringLoaded,
                            ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
                }
            } else if (resultCode == RESULT_OK) {
                addAppWidgetImpl(appWidgetId, mPendingAddInfo, null,
                        mPendingAddWidgetInfo, ON_ACTIVITY_RESULT_ANIMATION_DELAY);

                // When the user has granted permission to bind widgets, we should check to see if
                // we can inflate the default search bar widget.
                getOrCreateQsbBar();
            }
            return;
        } else if (requestCode == REQUEST_PICK_WALLPAPER) {
            if (resultCode == RESULT_OK && mWorkspace.isInOverviewMode()) {
                showWorkspace(false);
            }
            return;
        }

        boolean isWidgetDrop = (
                requestCode == REQUEST_PICK_APPWIDGET ||
                        requestCode == REQUEST_CREATE_APPWIDGET);

        final boolean workspaceLocked = isWorkspaceLocked();
        // We have special handling for widgets
        if (isWidgetDrop) {
            final int appWidgetId;
            int widgetId = data != null ? data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                    : -1;
            if (widgetId < 0) {
                appWidgetId = pendingAddWidgetId;
            } else {
                appWidgetId = widgetId;
            }

            final int result;
            if (appWidgetId < 0 || resultCode == RESULT_CANCELED) {
                Log.e(TAG, "Error: appWidgetId (EXTRA_APPWIDGET_ID) was not " +
                        "returned from the widget configuration activity.");
                result = RESULT_CANCELED;
                completeTwoStageWidgetDrop(result, appWidgetId);
                final Runnable onComplete = new Runnable() {
                    @Override
                    public void run() {
                        exitSpringLoadedDragModeDelayed(false, 0, null);
                    }
                };
                if (workspaceLocked) {
                    // No need to remove the empty screen if we're mid-binding, as the
                    // the bind will not add the empty screen.
                    mWorkspace.postDelayed(onComplete, ON_ACTIVITY_RESULT_ANIMATION_DELAY);
                } else {
                    if (!mWorkspace.isInOverviewMode()) {
                        mWorkspace.removeExtraEmptyScreenDelayed(true, onComplete,
                                ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
                    }
                }
            } else {
                if (!workspaceLocked) {
                    if (mPendingAddInfo.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                        // When the screen id represents an actual screen (as opposed to a rank)
                        // we make sure that the drop page actually exists.
                        mPendingAddInfo.screenId =
                                ensurePendingDropLayoutExists(mPendingAddInfo.screenId);
                    }
                    final CellLayout dropLayout = mWorkspace.getScreenWithId(mPendingAddInfo.screenId);

                    dropLayout.setDropPending(true);
                    final Runnable onComplete = new Runnable() {
                        @Override
                        public void run() {
                            completeTwoStageWidgetDrop(resultCode, appWidgetId);
                            dropLayout.setDropPending(false);
                        }
                    };
                    if (!mWorkspace.isInOverviewMode()) {
                        mWorkspace.removeExtraEmptyScreenDelayed(true, onComplete,
                                ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
                    } else {
                        onComplete.run();
                    }
                } else {
                    PendingAddArguments args = preparePendingAddArgs(requestCode, data, appWidgetId,
                            mPendingAddInfo);
                    sPendingAddItem = args;
                }
            }
            return;
        }

        if (requestCode == REQUEST_RECONFIGURE_APPWIDGET) {
            if (resultCode == RESULT_OK) {
                // Update the widget view.
                PendingAddArguments args = preparePendingAddArgs(requestCode, data,
                        pendingAddWidgetId, mPendingAddInfo);
                if (workspaceLocked) {
                    sPendingAddItem = args;
                } else {
                    completeAdd(args);
                }
            }
            // Leave the widget in the pending state if the user canceled the configure.
            return;
        }

        // The pattern used here is that a user PICKs a specific application,
        // which, depending on the target, might need to CREATE the actual target.

        // For example, the user would PICK_SHORTCUT for "Music playlist", and we
        // launch over to the Music app to actually CREATE_SHORTCUT.
        if (resultCode == RESULT_OK && mPendingAddInfo.container != ItemInfo.NO_ID) {
            final PendingAddArguments args = preparePendingAddArgs(requestCode, data, -1,
                    mPendingAddInfo);
            if (isWorkspaceLocked()) {
                sPendingAddItem = args;
            } else {
                completeAdd(args);
                if (!mWorkspace.isInOverviewMode()) {
                    mWorkspace.removeExtraEmptyScreenDelayed(true, exitSpringLoaded,
                            ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            if (!mWorkspace.isInOverviewMode()) {
                mWorkspace.removeExtraEmptyScreenDelayed(true, exitSpringLoaded,
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
            }
        }
        mDragLayer.clearAnimatedView();

    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        if (requestCode == SEARCH_ACTIVITY_REQUEST_CODE) {
            hideBlurBg();
        }

        handleActivityResult(requestCode, resultCode, data);
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onActivityResult(requestCode, resultCode, data);
        }
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN && resultCode == RESULT_OK) {
            lock();
        }
    }

    /**
     * @Override for MNC
     */
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        int perMissionGranted = 0;
        int openSetting = 0;
        int perDenied = 0;
        if (requestCode == PERMISSION_ALL) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    perMissionGranted++;
                } else {
                    perDenied++;
                    if (!shouldShowRequestPermissionRationale(permissions[i])) {
                        openSetting++;
                    }
                }
            }

            if (perMissionGranted != permissions.length) {
                if (openSetting != perDenied) {
                    checkPermissionGranted();
                } else {
//                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                    intent.setData(Uri.parse("package:" + getPackageName()));
//                    startActivity(intent);
                }
            }
//            initApp();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private PendingAddArguments preparePendingAddArgs(int requestCode, Intent data, int
            appWidgetId, ItemInfo info) {
        PendingAddArguments args = new PendingAddArguments();
        args.requestCode = requestCode;
        args.intent = data;
        args.container = info.container;
        args.screenId = info.screenId;
        args.cellX = info.cellX;
        args.cellY = info.cellY;
        args.appWidgetId = appWidgetId;
        return args;
    }

    /**
     * Check to see if a given screen id exists. If not, create it at the end, return the new id.
     *
     * @param screenId the screen id to check
     * @return the new screen, or screenId if it exists
     */
    private long ensurePendingDropLayoutExists(long screenId) {
        CellLayout dropLayout =
                mWorkspace.getScreenWithId(screenId);
        if (dropLayout == null) {
            // it's possible that the add screen was removed because it was
            // empty and a re-bind occurred
            mWorkspace.addExtraEmptyScreen();
            return mWorkspace.commitExtraEmptyScreen(Workspace.EXTRA_EMPTY_SCREEN_ID1);
        } else {
            return screenId;
        }
    }

    @Thunk
    void completeTwoStageWidgetDrop(final int resultCode, final int appWidgetId) {
        CellLayout cellLayout =
                mWorkspace.getScreenWithId(mPendingAddInfo.screenId);
        Runnable onCompleteRunnable = null;
        int animationType = 0;

        AppWidgetHostView boundWidget = null;
        if (resultCode == RESULT_OK) {
            animationType = Workspace.COMPLETE_TWO_STAGE_WIDGET_DROP_ANIMATION;
            final AppWidgetHostView layout = mAppWidgetHost.createView(this, appWidgetId,
                    mPendingAddWidgetInfo);
            boundWidget = layout;
            onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    completeAddAppWidget(appWidgetId, mPendingAddInfo.container,
                            mPendingAddInfo.screenId, layout, null);

                    if (!mWorkspace.isInOverviewMode()) {
                        exitSpringLoadedDragModeDelayed((resultCode != RESULT_CANCELED),
                                EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT, null);
                    }
                }
            };
        } else if (resultCode == RESULT_CANCELED) {
            mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            animationType = Workspace.CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION;
        }
        if (mDragLayer.getAnimatedView() != null) {
            mWorkspace.animateWidgetDrop(mPendingAddInfo, cellLayout,
                    (DragView) mDragLayer.getAnimatedView(), onCompleteRunnable,
                    animationType, boundWidget, true);
        } else if (onCompleteRunnable != null) {
            // The animated view may be null in the case of a rotation during widget configuration
            onCompleteRunnable.run();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirstFrameAnimatorHelper.setIsVisible(false);
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onStop();
        }
        NotificationListener.removeNotificationsChangedListener();
    }

//    public void releaseUnreadLoader() {
//        if (mUnreadLoader != null) {
//            mUnreadLoader.onCancel(this);
//        }
//    }

    @Override
    protected void onStart() {
        super.onStart();
        FirstFrameAnimatorHelper.setIsVisible(true);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onStart();
        }

        if (mUnreadLoader != null) {
            mUnreadLoader.onCancel(this);
            mUnreadLoader.initialize(this, this);
        }

        if (!isWorkspaceLoading()) {
            NotificationListener.setNotificationsChangedListener(mPopupDataProvider);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume Start");
        long startTime = 0;
        // Đã tắt dialog "Rate us" hiện lên khi mở launcher theo yêu cầu.

        isPaused = false;

        if (DEBUG_RESUME_TIME) {
            startTime = System.currentTimeMillis();
            Log.v(TAG, "Launcher.onResume()");
        }

        if (mLauncherCallbacks != null) {
            try {
                ObjectAnimator objectAnimator = mStateTransitionAnimation.mCloseAnimator;
                if ((objectAnimator == null || (!objectAnimator.isRunning() && !mStateTransitionAnimation.mCloseAnimator.isStarted())) && mStateTransitionAnimation.mCloseAnimIV != null) {
                    int[] locations = mStateTransitionAnimation.mViewLocations;
                    if (locations[0] != 0 && locations[1] != 0) {
                        PropertyValuesHolder xValue = PropertyValuesHolder.ofFloat(View.X, locations[0]);
                        PropertyValuesHolder yValue = PropertyValuesHolder.ofFloat(View.Y, mStateTransitionAnimation.mViewLocations[1]);
                        PropertyValuesHolder scaleXValue = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f);
                        PropertyValuesHolder scaleYValue = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f);
                        PropertyValuesHolder alphaValue = PropertyValuesHolder.ofFloat(View.ALPHA, 0.0f, 1.0f);
                        if (mStateTransitionAnimation.mCloseAnimator == null || !mStateTransitionAnimation.mCloseAnimator.isRunning()) {
                            mStateTransitionAnimation.mCloseAnimator = ObjectAnimator.ofPropertyValuesHolder(mStateTransitionAnimation.mCloseAnimIV, xValue, yValue, scaleXValue, scaleYValue, alphaValue);
                            mStateTransitionAnimation.mCloseAnimator.setDuration(386L);
                            mStateTransitionAnimation.mCloseAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
                            mStateTransitionAnimation.mCloseAnimator.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationCancel(Animator animation) {
                                    mStateTransitionAnimation.mViewLocations[0] = 0;
                                    mStateTransitionAnimation.mViewLocations[1] = 0;
                                    mStateTransitionAnimation.mCloseAnimIV.setVisibility(View.INVISIBLE);
                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    onAnimationCancel(animation);
                                }

                                @Override
                                public void onAnimationStart(Animator animation) {
                                    mStateTransitionAnimation.mCloseAnimIV.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                        mStateTransitionAnimation.mCloseAnimator.start();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Log.d(TAG, "onResume(): mOnResumeState = " + mOnResumeState);

        // Restore the previous launcher state
        if (mOnResumeState == State.WORKSPACE) {
            dismissWorkspace(null);
            if (mOnResumeToCusContentScreen) {
                moveToCustomContentScreen(true, 200);
                mOnResumeToCusContentScreen = false;
            }
        } else if (mOnResumeState == State.APPS) {
            boolean launchedFromApp = (mWaitingForResume != null);
            // Don't update the predicted apps if the user is returning to launcher in the apps
            // view after launching an app, as they may be depending on the UI to be static to
            // switch to another app, otherwise, if it was
            //showAppsView(false, false, !launchedFromApp, false, false);
        }

        mOnResumeState = State.NONE;

        mPaused = false;
        if (mRestoring || mOnResumeNeedsLoad) {
            setWorkspaceLoading(true);

            // If we're starting binding all over again, clear any bind calls we'd postponed in
            // the past (see waitUntilResume) -- we don't need them since we're starting binding
            // from scratch again
            mBindOnResumeCallbacks.clear();

            mModel.startLoader(PagedView.INVALID_RESTORE_PAGE);
            mRestoring = false;
            mOnResumeNeedsLoad = false;
        }
        if (mBindOnResumeCallbacks.size() > 0) {
            // We might have postponed some bind calls until onResume (see waitUntilResume) --
            // execute them here
            long startTimeCallbacks = 0;
            if (DEBUG_RESUME_TIME) {
                startTimeCallbacks = System.currentTimeMillis();
            }

            for (int i = 0; i < mBindOnResumeCallbacks.size(); i++) {
                mBindOnResumeCallbacks.get(i).run();
            }
            mBindOnResumeCallbacks.clear();
            if (DEBUG_RESUME_TIME) {
                Log.d(TAG, "Time spent processing callbacks in onResume: " +
                        (System.currentTimeMillis() - startTimeCallbacks));
            }
        }
        if (mOnResumeCallbacks.size() > 0) {
            for (int i = 0; i < mOnResumeCallbacks.size(); i++) {
                mOnResumeCallbacks.get(i).run();
            }
            mOnResumeCallbacks.clear();
        }

        // It is possible that widgets can receive updates while launcher is not in the foreground.
        // Consequently, the widgets will be inflated in the orientation of the foreground activity
        // (framework issue). On resuming, we ensure that any widgets are inflated for the current
        // orientation.
        getWorkspace().reinflateWidgetsIfNecessary();
        reinflateQSBIfNecessary(false);

        if (DEBUG_RESUME_TIME) {
            Log.d(TAG, "Time spent in onResume: " + (System.currentTimeMillis() - startTime));
        }

        if (mWorkspace.getCustomContentCallbacks() != null) {
            // If we are resuming and the custom content is the current page, we call onShow().
            // It is also poassible that onShow will instead be called slightly after first layout
            // if PagedView#setRestorePage was set to the custom content page in onCreate().
            if (mWorkspace.isOnOrMovingToCustomContent()) {
                mWorkspace.getCustomContentCallbacks().onShow(true);
            }
        }
        updateInteraction(Workspace.State.NORMAL, mWorkspace.getState());
        mWorkspace.onResume();

        if (!isWorkspaceLoading()) {
            // Process any items that were added while Launcher was away.
            InstallShortcutReceiver.disableAndFlushInstallQueue(this);
            UninstallShortcutReceiver.disableAndFlushUninstallQueue(this);

            // Refresh shortcuts if the permission changed.
            mModel.refreshShortcutsIfRequired();
        }

        if (!mLauncherGuideManager.hasRunFirstRunActivity()) {
            mLauncherGuideManager.showFirstRunActivity(this);
        } else {
            showGuideActivityAndDialog();
        }

        if (mSensorGestureModel != null) {
            mSensorGestureModel.onResume();
        }

        if (mLauncherCallbacks != null) {
//            checkHideNavigation();
            mLauncherCallbacks.onResume();
        }

        Log.i(TAG, "onResume End");
    }

    @Override
    protected void onPause() {
        Log.e("alshdflkasdf", "onPause: ");
        // Ensure that items added to Launcher are queued until Launcher returns
        InstallShortcutReceiver.enableInstallQueue();
        UninstallShortcutReceiver.enableUninstallQueue();
        enterAppAnimation();
        super.onPause();
        mPaused = true;

        isPaused = true;

        mDragController.cancelDrag();
        mDragController.resetLastGestureUpTime();

        // We call onHide() aggressively. The custom content callbacks should be able to
        // debounce excess onHide calls.
        if (mWorkspace.getCustomContentCallbacks() != null) {
            mWorkspace.getCustomContentCallbacks().onHide();
        }

        if (mSensorGestureModel != null) {
            mSensorGestureModel.onPause();
        }

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onPause();
        }
    }


    public void addToCustomContentPage(View customContent,
                                       LeftCustomContentCallbacks callbacks, String description) {
        mWorkspace.addToCustomContentPage(customContent, callbacks, description);
    }

    // The custom content needs to offset its content to account for the QSB
    public int getTopOffsetForCustomContent() {
        return mWorkspace.getPaddingTop();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        // Flag the loader to stop early before switching
        if (mModel.isCurrentCallbacks(this)) {
            mModel.stopLoader();
        }
        //TODO(hyunyoungs): stop the widgets loader when there is a rotation.

        return Boolean.TRUE;
    }

    // We can't hide the IME if it was forced open.  So don't bother
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.e("alshdflkasdf", "onWindowFocusChanged: ");
//        mHasFocus = hasFocus;
//        if (hasFocus) {
//            checkHideNavigation();
//        }

//        currentFocus = hasFocus;

//        if (!hasFocus) {
//            // Method that handles loss of window focus
//            collapseNow();
//        }
    }

    public void checkHideNavigation() {
        try {
            final View decorView = getWindow().getDecorView();
            decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(final int visibility) {
                    decorView.postDelayed(
                            new Runnable() {
                                @Override
                                public void run() {
                                    if ((visibility & 4) == 0) {
                                        decorView.setSystemUiVisibility(4866);
                                    }
                                }
                            }, 3000
                    );
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void collapseNow() {

        // Initialize 'collapseNotificationHandler'
        if (collapseNotificationHandler == null) {
            collapseNotificationHandler = new Handler();
        }

        // If window focus has been lost && activity is not in a paused state
        // Its a valid check because showing of notification panel
        // steals the focus from current activity's window, but does not
        // 'pause' the activity
        if (!currentFocus && !isPaused) {

            // Post a Runnable with some delay - currently set to 300 ms
            collapseNotificationHandler.post(new Runnable() {
                @Override
                public void run() {

                    // Use reflection to trigger a method from 'StatusBarManager'
                    Object statusBarService = getSystemService(Context.STATUS_BAR_SERVICE);
                    Class<?> statusBarManager = null;

                    try {
                        statusBarManager = Class.forName("android.app.StatusBarManager");
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                    Method collapseStatusBar = null;

                    try {
                        // Prior to API 17, the method to call is 'collapse()'
                        // API 17 onwards, the method to call is `collapsePanels()`

                        if (Build.VERSION.SDK_INT > 24) {
                            collapseStatusBar = statusBarManager.getMethod("collapsePanels");
                        } else {
                            collapseStatusBar = statusBarManager.getMethod("collapse");
                        }
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }

                    if (collapseStatusBar != null) {
                        collapseStatusBar.setAccessible(true);
                        try {
                            collapseStatusBar.invoke(statusBarService);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // Check if the window focus has been returned
                    // If it hasn't been returned, post this Runnable again
                    // Currently, the delay is 100 ms. You can change this
                    // value to suit your needs.
                    if (!currentFocus && !isPaused) {
                        collapseNotificationHandler.post(this);
                    }

                }
            });
        }
    }

    private boolean acceptFilter() {
        final InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        return !inputManager.isFullscreenMode();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        final boolean handled = super.onKeyDown(keyCode, event);
        // Eat the long press event so the keyboard doesn't come up.
        if (keyCode == KeyEvent.KEYCODE_MENU && event.isLongPress()) {
            return true;
        }
        return handled;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_F11 == keyCode && mState == State.WORKSPACE && !isFolderOpen() && Settings.isFingerprintEnable(this)) {
            mWorkspace.exitWidgetResizeMode();
            mWorkspace.scrollRight();
        }

        return super.onKeyUp(keyCode, event);
    }

    /**
     * Given the integer (ordinal) value of a State enum instance, convert it to a variable of type
     * State
     */
    private static State intToState(int stateOrdinal) {
        State state = State.WORKSPACE;
        final State[] stateValues = State.values();
        for (int i = 0; i < stateValues.length; i++) {
            if (stateValues[i].ordinal() == stateOrdinal) {
                state = stateValues[i];
                break;
            }
        }
        return state;
    }

    /**
     * Restores the previous state, if it exists.
     *
     * @param savedState The previous state.
     */
    @SuppressWarnings("unchecked")
    private void restoreState(Bundle savedState) {
        if (savedState == null) {
            return;
        }

        State state = intToState(savedState.getInt(RUNTIME_STATE, State.WORKSPACE.ordinal()));
        if (state == State.APPS || state == State.WIDGETS) {
            mOnResumeState = state;
        }

        int currentScreen = savedState.getInt(RUNTIME_STATE_CURRENT_SCREEN,
                PagedView.INVALID_RESTORE_PAGE);
        if (currentScreen != PagedView.INVALID_RESTORE_PAGE) {
            mWorkspace.setRestorePage(currentScreen);
        }

        final long pendingAddContainer = savedState.getLong(RUNTIME_STATE_PENDING_ADD_CONTAINER, -1);
        final long pendingAddScreen = savedState.getLong(RUNTIME_STATE_PENDING_ADD_SCREEN, -1);

        if (pendingAddContainer != ItemInfo.NO_ID && pendingAddScreen > -1) {
            mPendingAddInfo.container = pendingAddContainer;
            mPendingAddInfo.screenId = pendingAddScreen;
            mPendingAddInfo.cellX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_CELL_X);
            mPendingAddInfo.cellY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_CELL_Y);
            mPendingAddInfo.spanX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_X);
            mPendingAddInfo.spanY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y);
            AppWidgetProviderInfo info = savedState.getParcelable(
                    RUNTIME_STATE_PENDING_ADD_WIDGET_INFO);
            mPendingAddWidgetInfo = info == null ?
                    null : LauncherAppWidgetProviderInfo.fromProviderInfo(this, info);

            mPendingAddWidgetId = savedState.getInt(RUNTIME_STATE_PENDING_ADD_WIDGET_ID);
            setWaitingForResult(true);
            mRestoring = true;
        }

        mItemIdToViewId = (HashMap<Integer, Integer>)
                savedState.getSerializable(RUNTIME_STATE_VIEW_IDS);
    }

    /**
     * Finds all the views we need and configure them properly.
     */
    private void setupViews() {
        final DragController dragController = mDragController;
        mLauncherView = (LauncherRootView) findViewById(R.id.launcher);
        mWallpaperBackground = findViewById(R.id.background_wallpaper);
        mWorkspaceRootView = (WorkspaceRootView) findViewById(R.id.workspace_root_view);
        mFocusHandler = (FocusIndicatorView) findViewById(R.id.focus_indicator);
        mDragLayer = (DragLayer) findViewById(R.id.drag_layer);

        mWorkspace = (Workspace) mDragLayer.findViewById(R.id.workspace);
        mWorkspace.setPageSwitchListener(this);

        mPageIndicators = mDragLayer.findViewById(R.id.page_indicator);
        mPageIndicatorContainer = findViewById(R.id.page_indicator);


        mLoadingView = (LoadingView) findViewById(R.id.loading_view);

        mLauncherView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        // Setup the drag layer
        mDragLayer.setup(this, dragController);
        mDeleteExplosionField = new ExplosionField(this);
        mDragLayer.addView(mDeleteExplosionField, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // Setup the hotseat
        mHotseat = (Hotseat) findViewById(R.id.hotseat);
        if (mHotseat != null) {
            mHotseat.setOnLongClickListener(this);
        }

        // Setup the workspace
        mWorkspace.setHapticFeedbackEnabled(false);
        mWorkspace.setOnLongClickListener(this);
        mWorkspace.setup(dragController);
        dragController.addDragListener(mWorkspace);

        // Get the search/delete bar
//        mSearchDropTargetBar = (SearchDropTargetBar)mDragLayer.findViewById(R.id.search_drop_target_bar);

        // if gauss wallpaper has created , set as background;
        applyGaussWallpaperBackground();

        // Setup the drag controller (drop targets have to be added in reverse order in priority)
        dragController.setDragScoller(mWorkspace);
        dragController.setScrollView(mDragLayer);
        dragController.setMoveTarget(mWorkspace);
        dragController.addDropTarget(mWorkspace);
        /*
        if (mSearchDropTargetBar != null) {
            mSearchDropTargetBar.setup(this, dragController);
            getOrCreateQsbBar();
        }

         */

        toggleWeightWatcher(Settings.isMemWatcherEnabled(this));
    }

    public PopupDataProvider getPopupDataProvider() {
        return mPopupDataProvider;
    }

    /**
     * Creates a view representing a shortcut.
     *
     * @param info The data structure describing the shortcut.
     */
    View createShortcut(ShortcutInfo info) {
        return createShortcut((ViewGroup) mWorkspace.getChildAt(mWorkspace.getCurrentPage()), info);
    }

    /**
     * Creates a view representing a shortcut inflated from the specified resource.
     *
     * @param parent The group the shortcut belongs to.
     * @param info   The data structure describing the shortcut.
     * @return A View inflated from layoutResId.
     */
    public View createShortcut(ViewGroup parent, ShortcutInfo info) {
        BubbleTextView favorite = (BubbleTextView) mInflater.inflate(R.layout.app_icon,
                parent, false);
        favorite.applyFromShortcutInfo(info, mIconCache);
        favorite.setCompoundDrawablePadding(mDeviceProfile.iconDrawablePaddingPx);
        favorite.setOnClickListener(this);
        favorite.setOnFocusChangeListener(mFocusHandler);
        return favorite;
    }

    /**
     * Add a shortcut to the workspace.
     *
     * @param data The intent describing the shortcut.
     */
    private void completeAddShortcut(Intent data, long container, long screenId, int cellX,
                                     int cellY) {
        int[] cellXY = mTmpAddItemCellCoordinates;
        int[] touchXY = mPendingAddInfo.dropPos;
        CellLayout layout = getCellLayout(container, screenId);

        ShortcutInfo info = null;

        if (Utilities.ATLEAST_OREO) {
            info = LauncherAppsCompatVO.createShortcutInfoFromPinItemRequest(
                    this, LauncherAppsCompatVO.getPinItemRequest(data), 0);
        }

        if (info == null) {
            ComponentName componentName = data.getComponent();
            if (componentName != null && componentName.getPackageName().equals(getPackageName())) {
                info = mModel.infoFromIOSShortcutIntent(this, data);
            } else {
                info = InstallShortcutReceiver.fromShortcutIntent(this, data);
            }
        }

        if (info == null) {
            return;
        }
        final View view = createShortcut(info);

        boolean foundCellSpan = false;
        // First we check if we already know the exact location where we want to add this item.
        if (cellX >= 0 && cellY >= 0) {
            cellXY[0] = cellX;
            cellXY[1] = cellY;
            foundCellSpan = true;

            // If appropriate, either create a folder or add to an existing folder
            if (mWorkspace.createUserFolderIfNecessary(view, container, layout, cellXY, 0,
                    true, null, null)) {
                return;
            }
            DropTarget.DragObject dragObject = new DropTarget.DragObject();
            dragObject.dragInfo = info;
            if (mWorkspace.addToExistingFolderIfNecessary(view, layout, cellXY, 0, dragObject,
                    true)) {
                return;
            }
        } else if (touchXY != null) {
            // when dragging and dropping, just find the closest free spot
            int[] result = layout.findNearestVacantArea(touchXY[0], touchXY[1], 1, 1, cellXY);
            foundCellSpan = (result != null);
        } else {
            foundCellSpan = layout.findCellForSpan(cellXY, 1, 1);
        }

        if (!foundCellSpan) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }

        LauncherModel.addItemToDatabase(this, info, container, screenId, cellXY[0], cellXY[1]);

        if (!mRestoring) {
            mWorkspace.addInScreen(view, container, screenId, cellXY[0], cellXY[1], 1, 1,
                    isWorkspaceLocked());
        }
        resetAddInfo();

        if (mWorkspace.isInOverviewMode()) {
//            mWorkspace.addExtraEmptyScreenInOverviewMode();
        }
    }

    /**
     * Add a widget to the workspace.
     *
     * @param appWidgetId The app widget id
     */
    @Thunk
    void completeAddAppWidget(int appWidgetId, long container, long screenId,
                              AppWidgetHostView hostView, LauncherAppWidgetProviderInfo appWidgetInfo) {

        ItemInfo info = mPendingAddInfo;
        if (appWidgetInfo == null) {
            appWidgetInfo = LauncherAppWidgetProviderInfo.fromProviderInfo(this,
                    mAppWidgetManager.getAppWidgetInfo(appWidgetId));
        }

        if (appWidgetInfo.isIOSWidget) {
            appWidgetId = LauncherAppWidgetInfo.IOS_WIDGET_ID;
        }

        LauncherAppWidgetInfo launcherInfo;
        launcherInfo = new LauncherAppWidgetInfo(appWidgetId, appWidgetInfo.provider);
        launcherInfo.spanX = info.spanX;
        launcherInfo.spanY = info.spanY;
        launcherInfo.minSpanX = info.minSpanX;
        launcherInfo.minSpanY = info.minSpanY;
        launcherInfo.user = mAppWidgetManager.getUser(appWidgetInfo);

        LauncherModel.addItemToDatabase(this, launcherInfo,
                container, screenId, info.cellX, info.cellY);

        if (!mRestoring) {
            if (hostView == null) {
                // Perform actual inflation because we're live
                launcherInfo.hostView = mAppWidgetHost.createView(this, appWidgetId,
                        appWidgetInfo);
            } else {
                // The AppWidgetHostView has already been inflated and instantiated
                launcherInfo.hostView = hostView;
            }
            launcherInfo.hostView.setTag(launcherInfo);
            launcherInfo.hostView.setVisibility(View.VISIBLE);
            launcherInfo.notifyWidgetSizeChanged(this);

            mWorkspace.addInScreen(launcherInfo.hostView, container, screenId, info.cellX,
                    info.cellY, launcherInfo.spanX, launcherInfo.spanY, isWorkspaceLocked());

            addWidgetToAutoAdvanceIfNeeded(launcherInfo.hostView, appWidgetInfo);
        }
        resetAddInfo();

        if (mWorkspace.isInOverviewMode()) {
//            mWorkspace.addExtraEmptyScreenInOverviewMode();
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mUserPresent = false;
                mDragLayer.clearAllResizeFrames();
                updateAutoAdvanceState();
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                mUserPresent = true;
                updateAutoAdvanceState();
            } else if (ENABLE_DEBUG_INTENTS && DebugIntents.DELETE_DATABASE.equals(action)) {
                mModel.resetLoadedState(false, true);
                mModel.startLoader(PagedView.INVALID_RESTORE_PAGE,
                        LauncherModel.LOADER_FLAG_CLEAR_WORKSPACE);
            } else if (ENABLE_DEBUG_INTENTS && DebugIntents.MIGRATE_DATABASE.equals(action)) {
                mModel.resetLoadedState(false, true);
                mModel.startLoader(PagedView.INVALID_RESTORE_PAGE, LauncherModel.LOADER_FLAG_CLEAR_WORKSPACE | LauncherModel.LOADER_FLAG_MIGRATE_SHORTCUTS);
            }
        }
    };


    /**
     * Initializes the device profile based off of the launcher app state and screen orientation
     *
     * @param app The launcher app state
     */
    public void initializeDeviceProfile(LauncherAppState app) {
        // Load configuration-specific DeviceProfile
//        mDeviceProfile = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? app.getInvariantDeviceProfile().landscapeProfile : app.getInvariantDeviceProfile().portraitProfile;
        mDeviceProfile = app.getInvariantDeviceProfile().portraitProfile;
        mModel = app.setLauncher(this);
        mIconCache = app.getIconCache();
        mIconCache.flush();
    }


    Runnable mReloadLauncherRunnable = new Runnable() {
        @Override
        public void run() {
            reloadLauncher(mResetDesktopLoad);
        }
    };

    /**
     * Re-initializes the device profile and layout and reloads the workspace  as
     * appropriate
     *
     * @param resetDesktop Indicates whether the grid should be resized
     */
    public void reloadLauncher(boolean resetDesktop) {
        Log.d("reloadLauncher", "start");
        if (waitUntilResume(mReloadLauncherRunnable, true)) {
            mResetDesktopLoad = resetDesktop;
            return;
        }

        Log.d(TAG, "reloadLauncher - LauncherAppState.getInstance");
        // Re-initialize device profile
        LauncherAppState app = LauncherAppState.getInstance();
        app.initInvariantDeviceProfile();
        initializeDeviceProfile(app);

        mDeviceProfile.layout(this);

        // Reload
        int page, flag;
        if (resetDesktop) {
            page = PagedView.INVALID_RESTORE_PAGE;
            flag = LauncherModel.LOADER_FLAG_CLEAR_WORKSPACE;
        } else {
            page = PagedView.INVALID_RESTORE_PAGE;
            flag = LauncherModel.LOADER_FLAG_NONE;
        }

        mModel.resetLoadedState(true, true);
        mModel.startLoader(page, flag);

        mWorkspace.updateCustomContentVisibility();
        reinflateQSBIfNecessary(true);
    }


    public void updateIconBadges(final Set<PackageUserKey> updatedBadges) {
        Runnable r = () -> {
            mWorkspace.updateIconBadges(updatedBadges);

            PopupContainerWithArrow popup = PopupContainerWithArrow.getOpen(Launcher.this);
            if (popup != null) {
                popup.updateNotificationHeader(updatedBadges);
            }
        };
        if (!waitUntilResume(r)) {
            r.run();
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.e("alshdflkasdf", "onAttachedToWindow: ");
        // Listen for broadcasts related to user-presence
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        // For handling managed profiles
        if (ENABLE_DEBUG_INTENTS) {
            filter.addAction(DebugIntents.DELETE_DATABASE);
            filter.addAction(DebugIntents.MIGRATE_DATABASE);
        }
        ContextHelper.registerReceiver(this, mReceiver, filter);
        FirstFrameAnimatorHelper.initializeDrawListener(getWindow().getDecorView());
        setupTransparentSystemBarsForLollipop();
        mAttached = true;
        mVisible = true;
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onAttachedToWindow();
        }
    }

    /**
     * t
     * Sets up transparent navigation and status bars in Lollipop.
     * This method is a no-op for other platform versions.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setupTransparentSystemBarsForLollipop() {
        if (Utilities.ATLEAST_LOLLIPOP) {
            Window window = getWindow();
            window.getAttributes().systemUiVisibility |=
                    (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
//            window.setNavigationBarColor(Color.TRANSPARENT);
        }
    }


    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mVisible = false;
        Log.e("alshdflkasdf", "onDetachedFromWindow: ");
        if (mAttached) {
            unregisterReceiver(mReceiver);
            mAttached = false;
        }
        updateAutoAdvanceState();

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onDetachedFromWindow();
        }
    }

    public void onWindowVisibilityChanged(int visibility) {
        Log.e("alshdflkasdf", "onWindowVisibilityChanged: ");
        mVisible = visibility == View.VISIBLE;
        updateAutoAdvanceState();
        // The following code used to be in onResume, but it turns out onResume is called when
        // you're in All Apps and click home to go to the workspace. onWindowVisibilityChanged
        // is a more appropriate event to handle
        if (mVisible) {
            if (!mWorkspaceLoading) {
                final ViewTreeObserver observer = mWorkspace.getViewTreeObserver();
                // We want to let Launcher draw itself at least once before we force it to build
                // layers on all the workspace pages, so that transitioning to Launcher from other
                // apps is nice and speedy.
                observer.addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
                    private boolean mStarted = false;

                    public void onDraw() {
                        if (mStarted)
                            return;
                        mStarted = true;
                        // We delay the layer building a bit in order to give
                        // other message processing a time to run.  In particular
                        // this avoids a delay in hiding the IME if it was
                        // currently shown, because doing that may involve
                        // some communication back with the app.
                        mWorkspace.postDelayed(mBuildLayersRunnable, 500);
                        final ViewTreeObserver.OnDrawListener listener = this;
                        mWorkspace.post(new Runnable() {
                            public void run() {
                                if (mWorkspace != null &&
                                        mWorkspace.getViewTreeObserver() != null) {
                                    mWorkspace.getViewTreeObserver().
                                            removeOnDrawListener(listener);
                                }
                            }
                        });
                        return;
                    }
                });
            }
        }
    }

    @Thunk
    void sendAdvanceMessage(long delay) {
        mHandler.removeMessages(ADVANCE_MSG);
        Message msg = mHandler.obtainMessage(ADVANCE_MSG);
        mHandler.sendMessageDelayed(msg, delay);
        mAutoAdvanceSentTime = System.currentTimeMillis();
    }

    @Thunk
    void updateAutoAdvanceState() {
        Log.e("alshdflkasdf", "updateAutoAdvanceState: ");

        boolean autoAdvanceRunning = mVisible && mUserPresent && !mWidgetsToAdvance.isEmpty();
        if (autoAdvanceRunning != mAutoAdvanceRunning) {
            mAutoAdvanceRunning = autoAdvanceRunning;
            if (autoAdvanceRunning) {
                long delay = mAutoAdvanceTimeLeft == -1 ? mAdvanceInterval : mAutoAdvanceTimeLeft;
                sendAdvanceMessage(delay);
            } else {
                if (!mWidgetsToAdvance.isEmpty()) {
                    mAutoAdvanceTimeLeft = Math.max(0, mAdvanceInterval -
                            (System.currentTimeMillis() - mAutoAdvanceSentTime));
                }
                mHandler.removeMessages(ADVANCE_MSG);
                mHandler.removeMessages(0); // Remove messages sent using postDelayed()
            }
        }
    }

    @Thunk
    final Handler mHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case ADVANCE_MSG:
                    int i = 0;
                    for (View key : mWidgetsToAdvance.keySet()) {
                        final View v = key.findViewById(mWidgetsToAdvance.get(key).autoAdvanceViewId);
                        final int delay = mAdvanceStagger * i;
                        if (v instanceof Advanceable) {
                            mHandler.postDelayed(new Runnable() {
                                public void run() {
                                    ((Advanceable) v).advance();
                                }
                            }, delay);
                        }
                        i++;
                    }
                    sendAdvanceMessage(mAdvanceInterval);
                    break;
                default:
                    break;
            }

            return true;
        }
    });


    void addWidgetToAutoAdvanceIfNeeded(View hostView, AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo == null || appWidgetInfo.autoAdvanceViewId == -1)
            return;
        View v = hostView.findViewById(appWidgetInfo.autoAdvanceViewId);
        if (v instanceof Advanceable) {
            mWidgetsToAdvance.put(hostView, appWidgetInfo);
            ((Advanceable) v).fyiWillBeAdvancedByHostKThx();
            updateAutoAdvanceState();
        }
    }

    void removeWidgetToAutoAdvance(View hostView) {
        if (mWidgetsToAdvance.containsKey(hostView)) {
            mWidgetsToAdvance.remove(hostView);
            updateAutoAdvanceState();
        }
    }

    public void removeAppWidget(LauncherAppWidgetInfo launcherInfo) {
        removeWidgetToAutoAdvance(launcherInfo.hostView);
        launcherInfo.hostView = null;
    }

    public void showOutOfSpaceMessage(boolean isHotseatLayout) {
        int strId = (isHotseatLayout ? R.string.hotseat_out_of_space : R.string.out_of_space);
        Toast.makeText(this, getString(strId), Toast.LENGTH_SHORT).show();
    }

    public Rect getInsets() {
        return mLauncherView.getInsets();
    }

    public DragLayer getDragLayer() {
        return mDragLayer;
    }

    public WorkspaceRootView getWorkspaceRootView() {
        return mWorkspaceRootView;
    }

    public ExplosionField getDeleteExplosionField() {
        return mDeleteExplosionField;
    }

    public Workspace getWorkspace() {
        return mWorkspace;
    }

    public Hotseat getHotseat() {
        return mHotseat;
    }

    public View getWallpaperBackgroud() {
        return mWallpaperBackground;
    }

    /*
    public SearchDropTargetBar getSearchDropTargetBar() {
        return mSearchDropTargetBar;
    }*/

    public LauncherAppWidgetHost getAppWidgetHost() {
        return mAppWidgetHost;
    }

    public LauncherModel getModel() {
        return mModel;
    }

    public SharedPreferences getSharedPrefs() {
        return mSharedPrefs;
    }

    public DeviceProfile getDeviceProfile() {
        return mDeviceProfile;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public Bundle getActivityLaunchOptions(View v) {
        Log.e("alshdflkasdf", "getActivityLaunchOptions: ");

        if (Utilities.ATLEAST_MARSHMALLOW) {
            int left = 0, top = 0;
            int width = v.getMeasuredWidth(), height = v.getMeasuredHeight();
            if (v instanceof CustomTextView) {
                // Launch from center of icon, not entire view
                Drawable icon = Workspace.getTextViewIcon((CustomTextView) v);
                if (icon != null) {
                    Rect bounds = icon.getBounds();
                    left = (width - bounds.width()) / 2;
                    top = v.getPaddingTop();
                    width = bounds.width();
                    height = bounds.height();
                }
            }
            return ActivityOptions.makeClipRevealAnimation(v, left, top, width, height).toBundle();
        } else if (Utilities.ATLEAST_LOLLIPOP_MR1) {
            // On L devices, we use the device default slide-up transition.
            // On L MR1 devices, we use a custom version of the slide-up transition which
            // doesn't have the delay present in the device default.
            return ActivityOptions.makeCustomAnimation(
                    this, R.anim.task_open_enter, R.anim.no_anim).toBundle();
        }
        return null;
    }

    public Rect getViewBounds(View v) {
        int[] pos = new int[2];
        v.getLocationOnScreen(pos);
        return new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight());
    }

    public ThemeManager getThemeManager() {
        return LauncherAppState.getInstance().getThemeManager();
    }

    public void closeSystemDialogs() {
        getWindow().closeAllPanels();

        // Whatever we were doing is hereby canceled.
        setWaitingForResult(false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(TAG, "onNewIntent: " + intent);
        Log.e("alshdflkasdf", "onNewIntent: ");
        long startTime = 0;
        if (DEBUG_RESUME_TIME) {
            startTime = System.currentTimeMillis();
        }
        super.onNewIntent(intent);
        LauncherSharePrefUtils.increaseCountOpenApp(this);

        // Close the menu
        boolean alreadyOnHome = mHasFocus && (
                (
                        intent.getFlags() &
                                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                        != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        boolean isActionMain = Intent.ACTION_MAIN.equals(intent.getAction());
        boolean launchToCustomScreen = intent.getBooleanExtra(INTENT_EXTREA_LAUNCHE_CUSTOM_SCREEN, false);

        Log.i(TAG, "onNewIntent custom --- " + launchToCustomScreen);

        boolean restartRequest = intent.getBooleanExtra(LauncherRouter.LAUNCHER_RESTART_REQUEST, false);
        if (restartRequest) {
            restartSelf();
        }

        boolean resetRequest = intent.getBooleanExtra(LauncherRouter.LAUNCHER_RESET_REQUEST, false);
        if (resetRequest) {
            reloadLauncher(true/*resetDesktop*/);
        }

        boolean launchToNewTheme = checkAndApplyTheme(intent);
        if (launchToNewTheme) {
            mOnResumeNeedsLoad = true;
            showLoadingViewAndHideLauncher();
            if (launchToCustomScreen) {
                throw new RuntimeException("launchToNewTheme and launchToCustomScreen at same time");
            }
        }

        boolean internalStateHandled = InternalStateHandler.handleNewIntent(this, intent, isStarted());

        if (isActionMain) {
            // also will cancel mWaitingForResult.
            closeSystemDialogs();
            hidePanel();

            if (mWorkspace == null) {
                // Can be cases where mWorkspace is null, this prevents a NPE
                return;
            }
            // In all these cases, only animate if we're already on home
            mWorkspace.exitWidgetResizeMode();
            AbstractFloatingView.closeAllOpenViews(this, alreadyOnHome);
            Log.d(TAG, "AbstractFloatingView.closeAllOpenViews onNewIntent: ");
            exitSpringLoadedDragMode();

            boolean success = closeFolder();

            // If we are already on home, then just animate back to the workspace, otherwise, just wait until onResume to set the state back to Workspace
            if (alreadyOnHome) {
                if (!success)
                    showWorkspace(true);
            } else {
                mOnResumeState = State.WORKSPACE;
                if (launchToCustomScreen) {
                    mOnResumeToCusContentScreen = true;
                }
            }

            final View v = getWindow().peekDecorView();
            if (v != null && v.getWindowToken() != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }

            if (mLauncherCallbacks != null) {
                mLauncherCallbacks.onHomeIntent(internalStateHandled);
            }


            if (mWorkspace.getState() == Workspace.State.NORMAL && isTidyUping()) {
                endTidyUp();
            }
        }

        // Defer moving to the default screen until after we callback to the LauncherDelegate
        // as slow logic in the callbacks eat into the time the scroller expects for the snapToPage
        // animation.
        if (isActionMain) {
            if (alreadyOnHome && mState == State.WORKSPACE && !mWorkspace.isTouchActive() &&
                    !isFolderOpen()) {
                moveWorkspaceToDefaultScreen(true);
            }
        }

        if (DEBUG_RESUME_TIME) {
            Log.d(TAG, "Time spent in onNewIntent: " + (System.currentTimeMillis() - startTime));
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);

        for (int page : mSynchronouslyBoundPages) {
            mWorkspace.restoreInstanceStateForChild(page);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.e("alshdflkasdf", "onSaveInstanceState: ");

        if (mWorkspace.getChildCount() > 0) {
            outState.putInt(RUNTIME_STATE_CURRENT_SCREEN, mWorkspace.getCurrentPageOffsetFromCustomContent());
        }
        super.onSaveInstanceState(outState);
        Log.d("onSaveInstanceState", "value = " + mState.ordinal());
        outState.putInt(RUNTIME_STATE, mState.ordinal());
        // We close any open folder since it will not be re-opened, and we need to make sure this state is reflected.
//        closeFolder();
        AbstractFloatingView.closeAllOpenViews(this, false);
        Log.d(TAG, "AbstractFloatingView.closeAllOpenViews onSaveInstanceState: ");

        if (mPendingAddInfo.container != ItemInfo.NO_ID && mPendingAddInfo.screenId > -1 && mWaitingForResult) {
            outState.putLong(RUNTIME_STATE_PENDING_ADD_CONTAINER, mPendingAddInfo.container);
            outState.putLong(RUNTIME_STATE_PENDING_ADD_SCREEN, mPendingAddInfo.screenId);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_X, mPendingAddInfo.cellX);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_Y, mPendingAddInfo.cellY);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_X, mPendingAddInfo.spanX);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y, mPendingAddInfo.spanY);
            outState.putParcelable(RUNTIME_STATE_PENDING_ADD_WIDGET_INFO, mPendingAddWidgetInfo);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_WIDGET_ID, mPendingAddWidgetId);
        }

        // Save the current widgets tray?
        // TODO(hyunyoungs)
        outState.putSerializable(RUNTIME_STATE_VIEW_IDS, mItemIdToViewId);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("alshdflkasdf", "onDestroy: ");
        // Remove all pending runnables
        mHandler.removeMessages(ADVANCE_MSG);
        mHandler.removeMessages(0);
        mWorkspace.removeCallbacks(mBuildLayersRunnable);

        if (isTidyUping()) {
            this.mWorkspace.endTidyUp();
        }

        // Stop callbacks from LauncherModel
        LauncherAppState app = (LauncherAppState.getInstance());

        // It's possible to receive onDestroy after a new Launcher activity has
        // been created. In this case, don't interfere with the new Launcher.
        if (mModel.isCurrentCallbacks(this)) {
            mModel.stopLoader();
            app.setLauncher(null);
        }

        try {
            mAppWidgetHost.stopListening();
        } catch (NullPointerException ex) {
            Log.w(TAG, "problem while stopping AppWidgetHost during Launcher destruction", ex);
        }
        mAppWidgetHost = null;

        mWidgetsToAdvance.clear();

        TextKeyListener.getInstance().release();

        unregisterReceiver(mCloseSystemDialogsReceiver);

        mDragLayer.clearAllResizeFrames();
        ((ViewGroup) mWorkspace.getParent()).removeAllViews();
        mWorkspace.removeAllWorkspaceScreens();
        mWorkspace = null;
        mDragController = null;

        LauncherAnimUtils.onDestroyActivity();

        if (mUnreadLoader != null) {
            mUnreadLoader.onCancel(this);
        }
//        SwitchResponseSubject.unRegisterObserver(this);

        LauncherSettingSubject.unRegister();

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onDestroy();
        }

        unregisterReceiver(updateHiddenAppBroadcast);
    }

    public DragController getDragController() {
        return mDragController;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        onStartForResult(requestCode);
        Log.e("alshdflkasdf", "startActivityForResult: ");
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void startIntentSenderForResult(IntentSender intent, int requestCode,
                                           Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) {
        onStartForResult(requestCode);
        try {
            super.startIntentSenderForResult(intent, requestCode,
                    fillInIntent, flagsMask, flagsValues, extraFlags, options);
        } catch (IntentSender.SendIntentException e) {
            throw new ActivityNotFoundException();
        }
    }

    private void onStartForResult(int requestCode) {
        if (requestCode >= 0) {
            setWaitingForResult(true);
        }
    }


    public void startSearchFromAllApps(View v, Intent searchIntent, String searchQuery) {
        // If not handled, then just start the provided search intent
        startActivitySafely(v, searchIntent, null);
    }

    @Override
    public void startSearch(String initialQuery, boolean selectInitialQuery,
                            Bundle appSearchData, boolean globalSearch) {
        if (appSearchData == null) {
            appSearchData = new Bundle();
            appSearchData.putString("source", "launcher-search");
        }

        if (mLauncherCallbacks == null ||
                !mLauncherCallbacks.startSearch(initialQuery, selectInitialQuery, appSearchData)) {
            // Starting search from the callbacks failed. Start the default global search.
            super.startSearch(initialQuery, selectInitialQuery, appSearchData, true);
        }

        // We need to show the workspace after starting the search
        showWorkspace(true);
    }

    public boolean isOnCustomContent() {
        return mWorkspace.isOnOrMovingToCustomContent();
    }

    public boolean isOnAppsLibrary() {
        return this.mAppsLibraryLayout.getLeft() == 0;
    }

    public boolean isOnDefaultScreen() {
        return mWorkspace != null && mWorkspace.isOnDefaultPage();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
//        if (!isOnCustomContent()) {
//            // Close any open folders
//            closeFolder();
//            // Stop resizing any widgets
//            mWorkspace.exitWidgetResizeMode();
//            if (mState == State.WORKSPACE) {
//                // Show the main menu dialog
//                showPanel();
//            } else {
//                showWorkspace(true);
//            }
//        }

        return false;
    }

    public boolean isWorkspaceLocked() {
        return mWorkspaceLoading || mWaitingForResult;
    }

    public boolean isWorkspaceLoading() {
        return mWorkspaceLoading;
    }

    private void setWorkspaceLoading(boolean value) {
        boolean isLocked = isWorkspaceLocked();
        mWorkspaceLoading = value;
        if (isLocked != isWorkspaceLocked()) {
            onWorkspaceLockedChanged();
        }
    }

    private void setWaitingForResult(boolean value) {
        boolean isLocked = isWorkspaceLocked();
        mWaitingForResult = value;
        if (isLocked != isWorkspaceLocked()) {
            onWorkspaceLockedChanged();
        }
    }

    protected void onWorkspaceLockedChanged() {
    }

    private void resetAddInfo() {
        mPendingAddInfo.container = ItemInfo.NO_ID;
        mPendingAddInfo.screenId = -1;
        mPendingAddInfo.cellX = mPendingAddInfo.cellY = -1;
        mPendingAddInfo.spanX = mPendingAddInfo.spanY = -1;
        mPendingAddInfo.minSpanX = mPendingAddInfo.minSpanY = 1;
        mPendingAddInfo.dropPos = null;
    }

    void addAppWidgetImpl(final int appWidgetId, final ItemInfo info, final
    AppWidgetHostView boundWidget, final LauncherAppWidgetProviderInfo appWidgetInfo) {
        addAppWidgetImpl(appWidgetId, info, boundWidget, appWidgetInfo, 0);
    }

    void addAppWidgetImpl(final int appWidgetId, final ItemInfo info,
                          final AppWidgetHostView boundWidget, final LauncherAppWidgetProviderInfo appWidgetInfo,
                          int delay) {
        if (appWidgetInfo.configure != null) {

            mPendingAddWidgetInfo = appWidgetInfo;
            mPendingAddWidgetId = appWidgetId;

            // Launch over to configure widget, if needed
            mAppWidgetManager.startConfigActivity(appWidgetInfo, appWidgetId, this,
                    mAppWidgetHost, REQUEST_CREATE_APPWIDGET);

        } else {
            // Otherwise just add it
            Runnable onComplete = new Runnable() {
                @Override
                public void run() {
                    // Exit spring loaded mode if necessary after adding the widget
                    exitSpringLoadedDragModeDelayed(true, EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT,
                            null);
                }
            };
            completeAddAppWidget(appWidgetId, info.container, info.screenId, boundWidget,
                    appWidgetInfo);

            if (!mWorkspace.isInOverviewMode()) {
                mWorkspace.removeExtraEmptyScreenDelayed(true, onComplete, delay, false);
            }
        }
    }

    public void addPendingItem(PendingAddItemInfo info, long container, long screenId,
                               int[] cell, int spanX, int spanY) {
        switch (info.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_IOS_APPWIDGET:
            case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                int span[] = new int[2];
                span[0] = spanX;
                span[1] = spanY;
                addAppWidgetFromDrop((PendingAddWidgetInfo) info,
                        container, screenId, cell, span);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                processShortcutFromDrop(info.componentName, container, screenId, cell);
                break;
            default:
                throw new IllegalStateException("Unknown item type: " + info.itemType);
        }
    }

    /**
     * Process a shortcut drop.
     *
     * @param componentName The name of the component
     * @param screenId      The ID of the screen where it should be added
     * @param cell          The cell it should be added to, optional
     */
    private void processShortcutFromDrop(ComponentName componentName, long container, long screenId,
                                         int[] cell) {
        resetAddInfo();
        mPendingAddInfo.container = container;
        mPendingAddInfo.screenId = screenId;
        mPendingAddInfo.dropPos = null;

        if (cell != null) {
            mPendingAddInfo.cellX = cell[0];
            mPendingAddInfo.cellY = cell[1];
        }


        if (componentName.getPackageName().equals(getPackageName())) {
            addIOSShortcutFromDrop(componentName, container, screenId, cell);
        } else {
            Intent createShortcutIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
            createShortcutIntent.setComponent(componentName);
            Utilities.startActivityForResultSafely(this, createShortcutIntent, REQUEST_CREATE_SHORTCUT);
        }
    }


    /**
     * Process a ios shortcut drop.
     *
     * @param componentName The name of the component
     * @param screenId      The ID of the screen where it should be added
     * @param cell          The cell it should be added to, optional
     */
    private void addIOSShortcutFromDrop(ComponentName componentName, long container, long screenId,
                                        int[] cell) {
        Runnable exitSpringLoaded = new Runnable() {
            @Override
            public void run() {
                exitSpringLoadedDragModeDelayed(true,
                        EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT, null);
            }
        };

        Intent data = new Intent().setComponent(componentName);

        completeAddShortcut(data, container, screenId, cell[0],
                cell[1]);

        if (!mWorkspace.isInOverviewMode()) {
            mWorkspace.removeExtraEmptyScreenDelayed(true, exitSpringLoaded,
                    ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
        }
    }

    /**
     * Process a widget drop.
     *
     * @param info     The PendingAppWidgetInfo of the widget being added.
     * @param screenId The ID of the screen where it should be added
     * @param cell     The cell it should be added to, optional
     */
    private void addAppWidgetFromDrop(PendingAddWidgetInfo info, long container, long screenId,
                                      int[] cell, int[] span) {
        resetAddInfo();
        mPendingAddInfo.container = info.container = container;
        mPendingAddInfo.screenId = info.screenId = screenId;
        mPendingAddInfo.dropPos = null;
        mPendingAddInfo.minSpanX = info.minSpanX;
        mPendingAddInfo.minSpanY = info.minSpanY;

        if (cell != null) {
            mPendingAddInfo.cellX = cell[0];
            mPendingAddInfo.cellY = cell[1];
        }
        if (span != null) {
            mPendingAddInfo.spanX = span[0];
            mPendingAddInfo.spanY = span[1];
        }

        AppWidgetHostView hostView = info.boundWidget;
        int appWidgetId;
        if (hostView != null) {
            appWidgetId = hostView.getAppWidgetId();

            addAppWidgetImpl(appWidgetId, info, hostView, info.info);

            // Clear the boundWidget so that it doesn't get destroyed.
            info.boundWidget = null;
        } else {
            // In this case, we either need to start an activity to get permission to bind
            // the widget, or we need to start an activity to configure the widget, or both.
            appWidgetId = getAppWidgetHost().allocateAppWidgetId();
            Bundle options = info.bindOptions;


            boolean success;
            if (info.info.isIOSWidget && info.info.configure == null) {
                success = true;
            } else {
                success = mAppWidgetManager.bindAppWidgetIdIfAllowed(
                        appWidgetId, info.info, options);
            }

            if (success) {
                addAppWidgetImpl(appWidgetId, info, null, info.info);
            } else {
                mPendingAddWidgetInfo = info.info;
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.componentName);
                mAppWidgetManager.getUser(mPendingAddWidgetInfo)
                        .addToIntent(intent, AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE);
                // TODO: we need to make sure that this accounts for the options bundle.
                // intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options);
                startActivityForResult(intent, REQUEST_BIND_APPWIDGET);
            }
        }
    }

    public void addAppWidgetFromScreenEditView(PendingAddWidgetInfo info) {
        processAddItemFromScreenEditView(info);
    }


    public void addAppShortcutFromScreenEditView(PendingAddShortcutInfo info) {
        processAddItemFromScreenEditView(info);
    }

    private void processAddItemFromScreenEditView(PendingAddItemInfo info) {
        int span[] = new int[2];
        span[0] = info.spanX;
        span[1] = info.spanY;

        CellLayout cellLayout = mWorkspace.getCurrentDropLayout();
        long currentScreenId = mWorkspace.getIdForScreen(cellLayout);
        if (cellLayout.isEmpty()) {
            if (currentScreenId == Workspace.EXTRA_EMPTY_SCREEN_ID1) {
                currentScreenId = mWorkspace.commitExtraEmptyScreen(Workspace.EXTRA_EMPTY_SCREEN_ID1);
            }
        }

        int[] pixelXY = cellLayout.getFirstVacant(info.spanX, info.spanY);
        int[] cellXY = cellLayout.findNearestVacantArea(pixelXY[0], pixelXY[1], info.spanX, info.spanY, new int[2]);

        if (cellXY[0] >= 0 && cellXY[1] >= 0) {
            if (info instanceof PendingAddShortcutInfo) {
                PendingAddShortcutInfo shortcutInfo = (PendingAddShortcutInfo) info;
                processShortcutFromDrop(shortcutInfo.componentName, LauncherSettings.Favorites.CONTAINER_DESKTOP, currentScreenId, cellXY);
            } else if (info instanceof PendingAddWidgetInfo) {
                PendingAddWidgetInfo widgetInfo = (PendingAddWidgetInfo) info;
                addAppWidgetFromDrop(widgetInfo, LauncherSettings.Favorites.CONTAINER_DESKTOP, currentScreenId, cellXY, span);
            }

            if (cellLayout.isNullScreen()) {
                cellLayout.setNullScreen(false);
            }

        } else {
            showOutOfSpaceMessage(false);
        }
    }

    FolderIcon addFolder(String folderName, CellLayout layout, long container, final long screenId, int cellX,
                         int cellY) {
        final FolderInfo folderInfo = new FolderInfo();
        folderInfo.title = folderName;

        // Update the model
        LauncherModel.addItemToDatabase(Launcher.this, folderInfo, container, screenId,
                cellX, cellY);

        // Create the view
        FolderIcon newFolder =
                FolderIcon.fromXml(R.layout.folder_icon, this, layout, folderInfo, mIconCache);
        mWorkspace.addInScreen(newFolder, container, screenId, cellX, cellY, 1, 1,
                isWorkspaceLocked());
        // Force measure the new folder icon
        CellLayout parent = mWorkspace.getParentCellLayoutForView(newFolder);
        parent.getShortcutsAndWidgets().measureChild(newFolder);
        return newFolder;
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HOME:
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (DebugUtil.isPropertyEnabled(DUMP_STATE_PROPERTY)) {
                        dumpState();
                    }
                    break;
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HOME:
                    return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {

        if (viewItem4GroupAppLibrary != null) {
            viewItem4GroupAppLibrary.hide();
        }

        if (mLauncherCallbacks != null && mLauncherCallbacks.handleBackPressed()) {
            return;
        }

        if (isOnCustomContent() && (mWorkspace.mCustomContentCallbacks != null && mWorkspace.mCustomContentCallbacks.onBackPressed())) {
            return;
        }

        if (isFolderOpen()) {
            Folder folder = mWorkspace.getOpenFolder();
            if (folder != null) {
                if (folder.isEditingName()) {
                    folder.cancelEditingFolderName();
                    return;
                }
                if (DragLayer.sStartTidyUpInFolder) {
                    endTidyUp();
                    return;
                }

                closePopUp();
                closeFolder();
            }

            return;
        }

        if (mDragController.isDragging()) {
            mDragController.cancelDrag();
            return;
        }

        int width = mDeviceProfile.getCurrentWidth();

        boolean flag = false;

        if (mDragAppsLibraryLayout.isAppsLibraryOpening(width)) {
//                mAppsLibraryLayout.r();
            mDragAppsLibraryLayout.closeAppsLibrary();
            flag = true;
        }
        if (mDragAppsLibraryLayout.isLeftPageOpening(width)) {
            mDragAppsLibraryLayout.closeLeftPage();
            flag = true;
        }

        if (mSearchViewLayout != null && mSearchViewLayout.isOpened()) {
            this.mSearchViewLayout.hideSearchView();
            flag = true;
        }

        if (isOpeningFloatingMenu()) {
            Log.d(TAG, "closeFloatingMenu onBackPress: ");
            closeFloatingMenu();
            flag = true;
        }

        if (this.mBlurBackgroundView.getBackground() != null) {
            this.mBlurBackgroundView.clear(false);
            flag = true;
        }

        if (this.mFloatingMenuBlurBg.getBackground() != null) {
            this.mFloatingMenuBlurBg.clear(true);
            flag = true;
        }

        if (flag)
            return;

        closePopUp();

        if (isWidgetsViewVisible()) {
            closeWidgetViewWithAnimation();
            return;
        }

        if (isAppsViewVisible()) {
            showWorkspace(true);
        } else if (mWorkspace.isInOverviewMode()) {
            dismissWorkspace(mWorkspace.getDefaultPage());
        } else if (mDragLayer.hasResizeFrames()) {
            mWorkspace.exitWidgetResizeMode();
        } else {
            moveWorkspaceToDefaultScreen(true);
            // Back button is a no-op here, but give at least some feedback for the button press
            mWorkspace.showOutlinesTemporarily();
        }
    }


    public void setToPageOfFolder(FolderInfo info) {
        if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            mWorkspace.setToScreenId(info.screenId);
        }
    }

    /**
     * Re-listen when widget host is reset.
     */
    @Override
    public void onAppWidgetHostReset() {
        if (mAppWidgetHost != null) {
            mAppWidgetHost.startListening();
        }
    }

    public void removeEmptyScreens(CellLayout clickedCell, boolean isFoceRemove) {
        int nCurrentPage = 0;

        if (clickedCell != null) {
            for (int i = 0; i < mWorkspace.getChildCount(); i++) {
                CellLayout page = (CellLayout) mWorkspace.getChildAt(i);
                if (page == clickedCell) {
                    break;
                }

                if (!page.isNullScreen())
                    nCurrentPage++;
            }
        } else {
            nCurrentPage = mWorkspace.getCurrentPage();
        }

        mWorkspace.stripEmptyScreens(isFoceRemove);
        mWorkspace.setCurrentPage(nCurrentPage);
        LauncherModel.cleanEmptyWorkspacesFromDatabase(this);
    }

    public void dismissWorkspace(CellLayout clickedCell) {
        boolean isRemove = clickedCell == null;
        if (clickedCell == null) {
            clickedCell = mWorkspace.findCurrentPage();
            if (clickedCell == null)
                return;
        }
        removeEmptyScreens(clickedCell, isRemove);
        showWorkspace(true);
        mWorkspace.updateAllPageIndicatorMarker();
        endTidyUp();
    }

    /**
     * Launches the intent referred by the clicked shortcut.
     *
     * @param v The view representing the clicked shortcut.
     */
    //todo on click
    public void onClick(View v) {
        // Make sure that rogue clicks don't get through while allapps is launching, or after the
        // view has detached (it's possible for this to happen if the view is removed mid touch).

        Object tag = v.getTag();
        Log.d(TAG, "onClick");
        if (tag == null && this.mIsShaking) {
            cancelShakingAnimation();
        } else if (tag == null && mWorkspace.isInOverviewMode()) {
            showWorkspace(true);
        }

        if (v.getWindowToken() == null) {
            return;
        }

        if (!mWorkspace.isFinishedSwitchingState()) {
            return;
        }

        if (v instanceof CellLayout) {
            CellLayout screenCell = (CellLayout) v;
            if (screenCell.isNullScreen()) {
                screenCell.setNullScreen(false);
            } else {
                if (screenCell.getAppChildCount() <= 0) {
                    mWorkspace.fadeAndRemoveScreen(screenCell, null);
                    mWorkspace.updateAllPageIndicatorMarker();
                    return;
                }
            }

            mWorkspace.updateAllPageIndicatorMarker();
            dismissWorkspace(screenCell);
            return;
        }

        if (tag instanceof ShortcutInfo) {
            if (((ShortcutInfo) tag).itemType != LauncherSettings.Favorites.ITEM_TYPE_VIRTUAL_APP) {
                onClickAppShortcut(v);
            }
        } else if (tag instanceof FolderInfo) {
            if (v instanceof FolderIcon && !isFolderOpen()) {
                onClickFolderIcon(v);
                AnalyticsDelegate.onLauncherEvent(this, UMEventConstants.SMART_SORT_EVENT, UMEventConstants.DESKTOP_FOLDER_CLICK, ((FolderInfo) tag).folderCategoryType + "");
            }
        } else if (tag instanceof AppInfo) {
            AnalyticsDelegate.onAllAppsEvent(this, UMEventConstants.ALLAPPS_PERAPP_CLICK);
            startAppShortcutOrInfoActivity(v);
        } else if (tag instanceof LauncherAppWidgetInfo) {
            if (v instanceof PendingAppWidgetHostView) {
                onClickPendingWidget((PendingAppWidgetHostView) v);
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    /**
     * Event handler for the app widget view which has not fully restored.
     */
    public void onClickPendingWidget(final PendingAppWidgetHostView v) {
        if (mIsSafeModeEnabled) {
            Toast.makeText(this, R.string.safemode_widget_error, Toast.LENGTH_SHORT).show();
            return;
        }

        final LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) v.getTag();
        if (v.isReadyForClickSetup()) {
            int widgetId = info.appWidgetId;
            AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(widgetId);
            if (appWidgetInfo != null) {
                mPendingAddWidgetInfo = LauncherAppWidgetProviderInfo.fromProviderInfo(
                        this, appWidgetInfo);
                mPendingAddInfo.copyFrom(info);
                mPendingAddWidgetId = widgetId;

                AppWidgetManagerCompat.getInstance(this).startConfigActivity(appWidgetInfo,
                        info.appWidgetId, this, mAppWidgetHost, REQUEST_RECONFIGURE_APPWIDGET);
            }
        } else if (info.installProgress < 0) {
            // The install has not been queued
            final String packageName = info.providerName.getPackageName();
            showBrokenAppInstallDialog(packageName,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            startActivitySafely(v, LauncherModel.getMarketIntent(packageName), info);
                        }
                    });
        } else {
            // Download has started.
            final String packageName = info.providerName.getPackageName();
            startActivitySafely(v, LauncherModel.getMarketIntent(packageName), info);
        }
    }

    private void showBrokenAppInstallDialog(final String packageName,
                                            DialogInterface.OnClickListener onSearchClickListener) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.abandoned_promises_title)
                .setMessage(R.string.abandoned_promise_explanation)
                .setPositiveButton(R.string.abandoned_search, onSearchClickListener)
                .setNeutralButton(R.string.abandoned_clean_this,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                final UserHandleCompat user = UserHandleCompat.myUserHandle();
                                mWorkspace.removeAbandonedPromise(packageName, user);
                            }
                        })
                .create().show();
        return;
    }

    /**
     * Event handler for an app shortcut click.
     *
     * @param v The view that was clicked. Must be a tagged with a {@link ShortcutInfo}.
     */
    protected void onClickAppShortcut(final View v) {
        if (LOGD)
            Log.d(TAG, "onClickAppShortcut");
        Object tag = v.getTag();
        if (!(tag instanceof ShortcutInfo)) {
            throw new IllegalArgumentException("Input must be a Shortcut");
        }

        // Open shortcut
        final ShortcutInfo shortcut = (ShortcutInfo) tag;

        if (shortcut.isDisabled != 0) {
            if ((shortcut.isDisabled & ShortcutInfo.FLAG_DISABLED_SUSPENDED) != 0
                    || (shortcut.isDisabled & ShortcutInfo.FLAG_DISABLED_QUIET_USER) != 0) {
                // Launch activity anyway, framework will tell the user why the app is suspended.
            } else {
                int error = R.string.activity_not_available;
                if ((shortcut.isDisabled & ShortcutInfo.FLAG_DISABLED_SAFEMODE) != 0) {
                    error = R.string.safemode_shortcut_error;
                }
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        final Intent intent = shortcut.intent;

        // Check for special shortcuts
        if (handlerIOSShortcutClick(intent)) {
            return;
        }

        // Check for abandoned promise
        if ((v instanceof BubbleTextView)
                && shortcut.isPromise()
                && !shortcut.hasStatusFlag(ShortcutInfo.FLAG_INSTALL_SESSION_ACTIVE)) {
            showBrokenAppInstallDialog(
                    shortcut.getTargetComponent().getPackageName(),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            startAppShortcutOrInfoActivity(v);
                        }
                    });
            return;
        }

        // Start activities
        startAppShortcutOrInfoActivity(v);
    }


    private boolean handlerIOSShortcutClick(Intent intent) {
        if (intent.getComponent() != null && intent.getComponent().getPackageName().equals(getPackageName())) {
            final String shortcutClass = intent.getComponent().getClassName();
            if (shortcutClass.equals(MemoryDumpActivity.class.getName())) {
                MemoryDumpActivity.startDump(this);
                return true;
            } else if (shortcutClass.equals(Wallpaper.class.getName())) {
                Router.startWallpaperActivity(this);
                return true;
            } else if (shortcutClass.equals(Theme.class.getName())) {
                Router.startThemeClubActivity(this);
                return true;
            } else if (shortcutClass.equals(BatterySave.class.getName())) {
                Router.startBatterySaveActivity(this);
                return true;
            } else if (shortcutClass.equals(Discovery.class.getName())) {
                Router.startDiscoveryActivity(this);
                return true;
            }
        }
        return false;
    }

    private ComponentName mCN;
    private DevicePolicyManager mDPM;
    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
    private static final String EMUI_LOCK_PKG = "com.android.systemui";
    private static final String EMUI_LOCK_CLS = "com.huawei.keyguard.onekeylock.OneKeyLockActivity";

    public void setLockApp() {
        mCN = new ComponentName(this, AdminManager.class); // Receiver, not Activity!
        mDPM = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);

        if (isHuaweiNougat()) {
            launchEmuiLockActivity();
            finish();
        } else if (isAdminActive()) {
            lock();
            finish();
        } else {
            enableAppAsAdministrator();
        }
    }

    private void lock() {
        i("[lock] lockNow");
        if (mDPM != null)
            mDPM.lockNow();
    }

    private void enableAppAsAdministrator() {
        i("[enableAppAsAdministrator] startActivityForResult: requestCode=%d", REQUEST_CODE_ENABLE_ADMIN);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mCN);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.receiver_expl));
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
    }

    private void launchEmuiLockActivity() {
        i("[launchEmuiLockActivity] startActivity");
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(EMUI_LOCK_PKG, EMUI_LOCK_CLS));
        startActivity(intent);
    }

    private static void i(final String s) {
        Log.i(TAG, s);
    }

    private static void i(final String fmt, final Object... args) {
        i(String.format(fmt, args));
    }

    private static boolean isHuaweiNougat() {
        return Build.MANUFACTURER.equalsIgnoreCase("huawei")
                && Build.VERSION.SDK_INT == Build.VERSION_CODES.N;
    }

    private boolean isAdminActive() {
        return mDPM.isAdminActive(mCN);
    }

    public void handleGestureInfo(GestureEventModel.GestureInfo info, int direction) {
        Intent actionIntent = info.getActionIntent();
        if (actionIntent == null) {
            return;
        }

        if (info.getGestureEvent() == GestureEventModel.GESTURE_SWIPE_UP) {
            setTempAppAnimationStyle(Launcher.APP_ANIM_IN_UP);
        } else if (info.getGestureEvent() == GestureEventModel.GESTURE_SWIPE_DOWN) {
            setTempAppAnimationStyle(Launcher.APP_ANIM_IN_BOTTOM);
        } else if (info.getGestureEvent() == GestureEventModel.GESTURE_SWIPE_OBLIQUELY) {
        }

        if (handlerIOSShortcutClick(actionIntent)) {
            return;
        }

        String actionUri = info.getActionUri();

        if (actionUri.contentEquals(GestureEventModel.GESTURE_ACTION_URI_NO)) {
            return;
        } else if (actionUri.contentEquals(GestureEventModel.GESTURE_ACTION_URI_ALARM)) {
            //FunctionUtil.getInstance(getApplicationContext()).OpenNotify();
            return;
        } else if (actionUri.contentEquals(GestureEventModel.GESTURE_ACTION_URI_SEARCH)) {//
//            setTempAppAnimationStyle(Launcher.APP_ANIM_IN_UP);
//            startSearchActivity();
            return;
        }

        CommonUtilities.startActivitySafely(this, actionIntent);
    }

    @Thunk
    void startAppShortcutOrInfoActivity(View v) {
        Object tag = v.getTag();
        final ShortcutInfo shortcut;
        final Intent intent;

        if (mLauncherCallbacks != null) {
            ObjectAnimator animator = LauncherAnimUtils.ofPropertyValuesHolder(getDragLayer(), PropertyValuesHolder.ofFloat("scaleX", 0.95f), PropertyValuesHolder.ofFloat("scaleY", 0.95f));
            animator.setStartDelay(0L);
            animator.setDuration(238L);
            animator.setInterpolator(new DecelerateInterpolator(0.8f));
            animator.start();
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    Launcher.this.resumeNormalHomeState();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    Launcher.this.resumeNormalHomeState();
                }
            });
        }


        if (tag instanceof ShortcutInfo) {
            shortcut = (ShortcutInfo) tag;
            intent = shortcut.intent;
            int[] pos = new int[2];
            v.getLocationOnScreen(pos);
            intent.setSourceBounds(new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight()));

            if (shortcut.newInstalled) {
                shortcut.newInstalled = false;
                final ArrayList<ShortcutInfo> updates = new ArrayList<>();
                updates.add(shortcut);
                mWorkspace.updateShortcuts(updates);
            }
        } else if (tag instanceof AppInfo) {
            shortcut = null;
            intent = ((AppInfo) tag).intent;

            AppInfo appInfo = (AppInfo) tag;
            if (appInfo.newInstalled) {
                appInfo.newInstalled = false;
            }
        } else {
            throw new IllegalArgumentException("Input must be a Shortcut or AppInfo");
        }

        boolean success = startActivitySafely(v, intent, tag);
        mAppUsagesModel.onLaunch(this, intent.getComponent());
        mStats.recordLaunch(v, intent, shortcut);
        LauncherModel.updateCalledTimeAndCountItemInDatabase(this, shortcut);
    }

    public void resumeNormalHomeState() {
        getDragLayer().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (getDragLayer() != null) {
                        getDragLayer().setScaleX(1.0f);
                        getDragLayer().setScaleY(1.0f);
                    }
                } catch (Throwable th) {
                    th.getMessage();
                }
            }
        }, 889L);
    }

    /**
     * Event handler for a folder icon click.
     *
     * @param v The view that was clicked. Must be an instance of {@link FolderIcon}.
     */
    //todo folder click
    protected void onClickFolderIcon(View v) {
        if (LOGD)
            Log.d(TAG, "onClickFolder");
        if (!(v instanceof FolderIcon)) {
            throw new IllegalArgumentException("Input must be a FolderIcon");
        }

        // TODO(sunnygoyal): Re-evaluate this code.
        FolderIcon folderIcon = (FolderIcon) v;
        final FolderInfo info = folderIcon.getFolderInfo();

        openFolder(folderIcon);
    }

    /**
     * Event handler for the wallpaper picker button that appears after a long press
     * on the home screen.
     */
    protected void onClickWallpaperPicker(View v) {
        if (LOGD)
            Log.d(TAG, "onClickWallpaperPicker");
        // Open the system wallpaper picker. Do NOT restrict to our own package
        // (setPackage(getPackageName())) — this app has no SET_WALLPAPER activity,
        // so that would throw ActivityNotFoundException. Wrap in a chooser and
        // guard with resolveActivity so we never crash if no picker is present.
        Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
        Intent chooser = Intent.createChooser(intent, getString(R.string.wallpaper_button_text));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(chooser, REQUEST_PICK_WALLPAPER);
        } else {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Event handler for a click on the settings button that appears after a long press
     * on the home screen.
     */
    protected void onClickSettingsButton(View v) {
        if (LOGD)
            Log.d(TAG, "onClickSettingsButton");
        startActivity(new Intent(this, LauncherSettingActivity.class));
    }

    protected void startSysSettings(View v) {
        Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Utilities.startActivitySafely(this, intent);
    }

    public View.OnTouchListener getHapticFeedbackTouchListener() {
        if (mHapticFeedbackTouchListener == null) {
            mHapticFeedbackTouchListener = new View.OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    }
                    return false;
                }
            };
        }
        return mHapticFeedbackTouchListener;
    }

    public void onDragStarted(View view) {
        if (isOnCustomContent()) {
            // Custom content screen doesn't participate in drag and drop. If on custom
            // content screen, move to default.
            moveWorkspaceToDefaultScreen(false);
        }
    }

    /**
     * Called when the user stops interacting with the launcher.
     * This implies that the user is now on the homescreen and is not doing housekeeping.
     */
    protected void onInteractionEnd() {
    }

    /**
     * Called when the user starts interacting with the launcher.
     * The possible interactions are:
     * - open all apps
     * - reorder an app shortcut, or a widget
     * - open the overview mode.
     * This is a good time to stop doing things that only make sense
     * when the user is on the homescreen and not doing housekeeping.
     */
    protected void onInteractionBegin() {
    }

    /**
     * Updates the interaction state.
     */
    public void updateInteraction(Workspace.State fromState, Workspace.State toState) {
        // Only update the interacting state if we are transitioning to/from a view with an
        // overlay
        boolean fromStateWithOverlay = fromState != Workspace.State.NORMAL;
        boolean toStateWithOverlay = toState != Workspace.State.NORMAL;
        if (toStateWithOverlay) {
            onInteractionBegin();
        } else if (fromStateWithOverlay) {
            onInteractionEnd();
        }
    }

    void startApplicationDetailsActivity(ComponentName componentName, UserHandleCompat user) {
        try {
            LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(this);
            launcherApps.showAppDetailsForProfile(componentName, user);
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have permission to launch settings");
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Unable to launch settings");
        }
    }

    // returns true if the activity was started
    boolean startApplicationUninstallActivity(ComponentName componentName, int flags,
                                              UserHandleCompat user) {
        if ((flags & AppInfo.DOWNLOADED_FLAG) == 0) {
            // System applications cannot be installed. For now, show a toast explaining that.
            // We may give them the option of disabling apps this way.
            int messageId = R.string.uninstall_system_app_text;
            Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
            return false;
        } else {
            String packageName = componentName.getPackageName();
//            String className = componentName.getClassName();
//            Intent intent = new Intent(
//                    Intent.ACTION_DELETE, Uri.fromParts("package", packageName, className));
            Intent intent = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + packageName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            if (user != null) {
                user.addToIntent(intent, Intent.EXTRA_USER);
            }
            startActivity(intent);
            return true;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean startActivity(View v, Intent intent, Object tag) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            boolean useLaunchAnimation = (v != null) &&
                    !intent.hasExtra(INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION);
            LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(this);
            UserManagerCompat userManager = UserManagerCompat.getInstance(this);

            UserHandleCompat user = null;
            if (intent.hasExtra(AppInfo.EXTRA_PROFILE)) {
                long serialNumber = intent.getLongExtra(AppInfo.EXTRA_PROFILE, -1);
                user = userManager.getUserForSerialNumber(serialNumber);
            }

            Bundle optsBundle = null;
            if (useLaunchAnimation) {
                ActivityOptions opts = null;
                if (BuildUtil.ATLEAST_MARSHMALLOW) {
                    int left = 0, top = 0;
                    int width = v.getMeasuredWidth(), height = v.getMeasuredHeight();
                    if (v instanceof CustomTextView) {
                        // Launch from center of icon, not entire view
                        Drawable icon = Workspace.getTextViewIcon((CustomTextView) v);
                        if (icon != null) {
                            Rect bounds = icon.getBounds();
                            left = (width - bounds.width()) / 2;
                            top = v.getPaddingTop();
                            width = bounds.width();
                            height = bounds.height();
                        }
                        /*
                        if (!(v.getParent().getParent().getParent() instanceof FolderPagedView)) {
                            this.mStateTransitionAnimation.updateCloseAnimView(v, icon);
                        }

                         */
                    }
                    opts = ActivityOptions.makeClipRevealAnimation(v, left, top, width, height);
                } else if (!Utilities.ATLEAST_LOLLIPOP) {
                    // Below L, we use a scale up animation
                    opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0,
                            v.getMeasuredWidth(), v.getMeasuredHeight());
                } else if (Utilities.ATLEAST_LOLLIPOP_MR1) {
                    // On L devices, we use the device default slide-up transition.
                    // On L MR1 devices, we a custom version of the slide-up transition which
                    // doesn't have the delay present in the device default.
                    opts = ActivityOptions.makeCustomAnimation(this,
                            R.anim.task_open_enter, R.anim.no_anim);
                }
                optsBundle = opts != null ? opts.toBundle() : null;
            }

            if (user == null || user.equals(UserHandleCompat.myUserHandle())) {
                // Could be launching some bookkeeping activity
                startActivity(intent, optsBundle);
            } else {
                // TODO Component can be null when shortcuts are supported for secondary user
                launcherApps.startActivityForProfile(intent.getComponent(), user,
                        intent.getSourceBounds(), optsBundle);
            }
            return true;
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have the permission to launch " + intent +
                    ". Make sure to create a MAIN intent-filter for the corresponding activity " +
                    "or use the exported attribute for this activity. "
                    + "tag=" + tag + " intent=" + intent, e);
        }
        return false;
    }

    public boolean startActivitySafely(View v, Intent intent, Object tag) {
        boolean success = false;
        if (mIsSafeModeEnabled && !Utilities.isSystemApp(this, intent)) {
            Toast.makeText(this, R.string.safemode_shortcut_error, Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            if (Utilities.ATLEAST_MARSHMALLOW
                    && (tag instanceof ShortcutInfo)
                    && (((ShortcutInfo) tag).itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT
                    || ((ShortcutInfo) tag).itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT)
                    && !((ShortcutInfo) tag).isPromise()) {
                boolean useLaunchAnimation = (v != null) &&
                        !intent.hasExtra(INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION);
                Bundle optsBundle = useLaunchAnimation ? getActivityLaunchOptions(v) : null;
                // Shortcuts need some special checks due to legacy reasons.
                startShortcutIntentSafely(intent, optsBundle, (ItemInfo) tag);
            } else {
                success = startActivity(v, intent, tag);
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Unable to launch. tag=" + tag + " intent=" + intent, e);
        }
        return success;
    }

    public boolean startActivitySafely(Intent intent, Object obj) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(intent);
            return true;
        } catch (SecurityException e) {
            //clearDarkEffect();
            Toast.makeText(this, (int) R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have the permission to launch " + intent + ". Make sure to create a MAIN intent-filter for the corresponding activity " + "or use the exported attribute for this activity. " + "tag=" + obj + " intent=" + intent, e);
            return false;
        } catch (ActivityNotFoundException e2) {
            //clearDarkEffect();
//            if (LauncherModel.isExternalAppAvailable()) {
//                Toast.makeText(this, (int) R.string.activity_not_found, 0).show();
//            } else {
//                Toast.makeText(this, (int) R.string.activity_unavailable, 0).show();
//            }
            Log.e(TAG, "Unable to launch. tag=" + obj + " intent=" + intent, e2);
            return false;
        }
    }

    private void startShortcutIntentSafely(Intent intent, Bundle optsBundle, ItemInfo info) {
        try {
            StrictMode.VmPolicy oldPolicy = StrictMode.getVmPolicy();
            try {
                // Temporarily disable deathPenalty on all default checks. For eg, shortcuts
                // containing file Uri's would cause a crash as penaltyDeathOnFileUriExposure
                // is enabled by default on NYC.
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll()
                        .penaltyLog().build());

                if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT) {
                    String id = ((ShortcutInfo) info).getDeepShortcutId();
                    String packageName = intent.getPackage();
                    DeepShortcutManager.getInstance(this).startShortcut(
                            packageName, id, intent.getSourceBounds(), optsBundle, info.user.getUser());
                } else {
                    // Could be launching some bookkeeping activity
                    startActivity(intent, optsBundle);
                }
            } finally {
                StrictMode.setVmPolicy(oldPolicy);
            }
        } catch (SecurityException e) {
            // Due to legacy reasons, direct call shortcuts require Launchers to have the
            // corresponding permission. Show the appropriate permission prompt if that
            // is the case.
            if (intent.getComponent() == null
                    && Intent.ACTION_CALL.equals(intent.getAction())) {
                PermissionUtil.checkSelfPermissions(this, Manifest.permission.CALL_PHONE);
            }
        }
    }

    /**
     * This method draws the FolderIcon to an ImageView and then adds and positions that ImageView
     * in the DragLayer in the exact absolute location of the original FolderIcon.
     */
    private void copyFolderIconToImage(FolderIcon fi) {
        final int width = fi.getMeasuredWidth();
        final int height = fi.getMeasuredHeight();

        // Lazy load ImageView, Bitmap and Canvas
        if (mFolderIconImageView == null) {
            mFolderIconImageView = new ImageView(this);
        }
        if (mFolderIconBitmap == null || mFolderIconBitmap.getWidth() != width ||
                mFolderIconBitmap.getHeight() != height) {
            mFolderIconBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mFolderIconCanvas = new Canvas(mFolderIconBitmap);
        }

        DragLayer.LayoutParams lp;
        if (mFolderIconImageView.getLayoutParams() instanceof DragLayer.LayoutParams) {
            lp = (DragLayer.LayoutParams) mFolderIconImageView.getLayoutParams();
        } else {
            lp = new DragLayer.LayoutParams(width, height);
        }

        // The layout from which the folder is being opened may be scaled, adjust the starting
        // view size by this scale factor.
        float scale = mDragLayer.getDescendantRectRelativeToSelf(fi, mRectForFolderAnimation);
        lp.customPosition = true;
        lp.x = mRectForFolderAnimation.left;
        lp.y = mRectForFolderAnimation.top;
        lp.width = (int) (scale * width);
        lp.height = (int) (scale * height);

        mFolderIconCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        fi.draw(mFolderIconCanvas);
        mFolderIconImageView.setImageBitmap(mFolderIconBitmap);
        if (fi.getFolder() != null) {
            mFolderIconImageView.setPivotX(fi.getFolder().getPivotXForIconAnimation());
            mFolderIconImageView.setPivotY(fi.getFolder().getPivotYForIconAnimation());
        }
        // Just in case this image view is still in the drag layer from a previous animation,
        // we remove it and re-add it.
        if (mDragLayer.indexOfChild(mFolderIconImageView) != -1) {
            mDragLayer.removeView(mFolderIconImageView);
        }
        mDragLayer.addView(mFolderIconImageView, lp);
        if (fi.getFolder() != null) {
            fi.getFolder().bringToFront();
        }
    }

    /**
     * Opens the user folder described by the specified tag. The opening of the folder
     * is animated relative to the specified View. If the View is null, no animation
     * is played.
     *
     * @param folderIcon The FolderInfo describing the folder to open.
     */
    public void openFolder(FolderIcon folderIcon) {
        Folder folder = folderIcon.mFolder;
        folder.mInfo.opened = true;

        if (isTidyUping()) {
            DragLayer.sStartTidyUpInFolder = true;
            this.mWorkspace.stopShakeAnimations();
        }
        if (folder.getParent() == null) {
            DragLayer.LayoutParams layoutParams = new DragLayer.LayoutParams(DragLayer.LayoutParams.MATCH_PARENT, DragLayer.LayoutParams.MATCH_PARENT);
            layoutParams.topMargin = 0;
            layoutParams.gravity = Gravity.CENTER;
            this.mDragLayer.addView(folder, layoutParams);
            this.mDragController.addDropTarget(folder);
        } else {
            Log.w(TAG, "Opening folder (" + folder + ") which already has a parent (" + folder.getParent() + ").");
        }

        showFolderBlurBackground();
        folder.animateOpen();
    }

    private void showFolderBlurBackground() {
        if (this.mBlurBackgroundView != null) {
            try {
                this.mBlurBackgroundView.mHandler1.obtainMessage(2, null).sendToTarget();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void closeFolder(Folder folder) {
        folder.getInfo().opened = false;
        folder.animateClosed();

        if (this.mBlurBackgroundView.getBackground() != null) {
            this.mBlurBackgroundView.clear(false);
        }

        if (isTidyUping()) {
            this.mWorkspace.beginShakeAnimations(true);
        } else if (this.mWorkspace.getState() == Workspace.State.OVERVIEW) {
            startTidyUp();
        }
    }


    public boolean closeFolder() {
        Folder openFolder = this.mWorkspace.getOpenFolder();
        if (openFolder != null) {
            if (openFolder.isEditingName()) {
                openFolder.dismissEditingName();
                if (isTidyUping() && DragLayer.sStartTidyUpInFolder) {
                    endTidyUp();
                }
            }
            closeFolder(openFolder);

            return true;
        }

        return false;
    }

    public boolean showPannel() {
        if (!mWorkspace.isInOverviewMode()) {
            if (!mWorkspace.isTouchActive()) {
                showPanel(true);
                mWorkspace.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void popWorkspace(boolean isShowOverViewPanel) {
        mWorkspace.startTidyUp();
        showPanel(isShowOverViewPanel);
        mWorkspace.buildPageHardwareLayers();
    }


    //todo on long click
    public boolean onLongClick(View v) {
        if (!isDraggingEnabled()) {
            return false;
        }

        if (isWorkspaceLocked())
            return false;

        if (mState != State.WORKSPACE)
            return false;

        if (v instanceof FolderIcon && isFolderOpen()) {
            return false;
        }

        if (v instanceof Workspace) {
            if (this.mWorkspace.isInOverviewMode()) {
                return false;
            }
            showPanel(true);
            this.mWorkspace.performHapticFeedback(0, 1);
            return true;
        }

        /*
        CellLayout.CellInfo longClickCellInfo = null;
        View itemUnderLongClick = null;

        if (this.mIsShaking || this.showingFloatingMenu || !((v instanceof BubbleTextView) || (v instanceof LauncherAppWidgetHostView))){
            closeFloatingMenu();
            if (!(v.getTag() instanceof AppInfo)){
                longClickCellInfo = null;
            }
            else if (!this.mIsShaking){
                onShakingAllApps();
                return true;
            }
            else {
                ItemInfo info = (ItemInfo) v.getTag();
                longClickCellInfo = new CellLayout.CellInfo(v, info);
                resetAddInfo();
                itemUnderLongClick = v;
            }

            if(!mDragController.isDragging()){
                if (longClickCellInfo == null){
                    this.mWorkspace.performHapticFeedback(0,REQUEST_CREATE_SHORTCUT);
                    if (!this.mWorkspace.isInOverviewMode()){
                        showOverviewMode(true,true);
                        return true;
                    }
                    else {
                        mWorkspace.startReordering(v);
                    }
                }
                else {
                    if (!(itemUnderLongClick instanceof Folder)) {
                        openFloatingMenu(v);
                        this.mWorkspace.showInfo(longClickCellInfo);
                    }
                }
            }
        }


         */
        CellLayout.CellInfo longClickCellInfo = null;
        View itemUnderLongClick = null;
        if (v.getTag() instanceof ItemInfo) {
            ItemInfo info = (ItemInfo) v.getTag();
            longClickCellInfo = new CellLayout.CellInfo(v, info);
            itemUnderLongClick = longClickCellInfo.cell;
            resetAddInfo();

            if (info instanceof LauncherAppWidgetInfo) {
                mOpenAppWidgetHostView = (LauncherAppWidgetHostView) v;
                openFloatingMenu(v);
                mWorkspace.showInfo(longClickCellInfo);
                mWorkspace.startTidyUp();
                return true;
            }
        }


        // The hotseat touch handling does not go through Workspace, and we always allow long press on hotseat items.
        final boolean inHotseat = isHotseatLayout(v);
        if (!mDragController.isDragging()) {
            if (itemUnderLongClick == null) {
                // User long pressed on empty space
                mWorkspace.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                if (mWorkspace.isInOverviewMode()) {
                    mWorkspace.startReordering(v);
                } else {
                    popWorkspace(true);
                }
            } else {
                //todo show menu
                Log.e(TAG, "open menu");
                openFloatingMenu(v);
                mWorkspace.showInfo(longClickCellInfo);
                mWorkspace.startTidyUp();
            }
        }

        return true;
    }

    //todo edit item in screen
    public void onEditHomeScreen() {
        Log.d(TAG, "closeFloatingMenu onEditHomeScreen: ");
        closeFloatingMenu();
        if (this.mWorkspace.isInOverviewMode()) {
            return;
        }
        showPanel(true);
        this.mWorkspace.performHapticFeedback(0, 1);
    }

    public void closePopUp() {
        AbstractFloatingView topView = AbstractFloatingView.getTopOpenView(this);
        if (topView != null) {
            if (topView.getActiveTextView() != null) {
                topView.getActiveTextView().dispatchBackKey();
            } else {
                topView.close(true);
            }
            return;
        }
    }

    public void startWorkspaceMoveState() {
        mStateTransitionAnimation.startAnimationToWorkspace(mState, mWorkspace.getState(),
                Workspace.State.SPRING_LOADED,
                WorkspaceStateTransitionAnimation.SCROLL_TO_CURRENT_PAGE, true,
                null /* onCompleteRunnable */);
    }

    private static String[] mFixedWidgets;

    private boolean isFixedWidget(Context context, String packageName) {
        if (!TextUtils.isEmpty(packageName)) {
            if (mFixedWidgets == null) {
                mFixedWidgets = Partner.getStringArray(context, Partner.DEF_WIDGET_FIXED_LIST);
            }

            if (mFixedWidgets == null) {
                return false;
            }

            for (int i = 0; i < mFixedWidgets.length; i++) {
                if (packageName.equals(mFixedWidgets[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean isHotseatLayout(View layout) {
        return mHotseat != null && layout != null &&
                (layout instanceof CellLayout) && (layout == mHotseat.getLayout());
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
    }

    @Override
    public boolean onCreateThumbnail(Bitmap outBitmap, Canvas canvas) {
        return super.onCreateThumbnail(outBitmap, canvas);
    }

    /**
     * Returns the CellLayout of the specified container at the specified screen.
     */
    public CellLayout getCellLayout(long container, long screenId) {
        if (container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            if (mHotseat != null) {
                return mHotseat.getLayout();
            } else {
                return null;
            }
        } else {
            return mWorkspace.getScreenWithId(screenId);
        }
    }

    public boolean isAppsViewVisible() {
        return (mState == State.APPS) || (mOnResumeState == State.APPS);
    }

    /*
    public boolean isWidgetsViewVisible() {
        return (mState == State.WIDGETS) || (mOnResumeState == State.WIDGETS);
    }
     */

    public boolean isFolderOpen() {
        Folder openFolder = this.mWorkspace.getOpenFolder();
        return openFolder != null;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // The widget preview db can result in holding onto over
            // 3MB of memory for caching which isn't necessary.
            SQLiteDatabase.releaseMemory();

            // This clears all widget bitmaps from the widget tray
            // TODO(hyunyoungs)
        }

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onTrimMemory(level);
        }
    }

    public boolean showWorkspace(boolean animated) {
        return showWorkspace(WorkspaceStateTransitionAnimation.SCROLL_TO_CURRENT_PAGE, animated, null);
    }

    public boolean showWorkspace(boolean animated, Runnable onCompleteRunnable) {
        return showWorkspace(WorkspaceStateTransitionAnimation.SCROLL_TO_CURRENT_PAGE, animated,
                onCompleteRunnable);
    }

    protected boolean showWorkspace(int snapToPage, boolean animated) {
        return showWorkspace(snapToPage, animated, null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }

    boolean showWorkspace(int snapToPage, boolean animated, Runnable onCompleteRunnable) {
        boolean changed = mState != State.WORKSPACE || mWorkspace.getState() != Workspace.State.NORMAL;
        if (changed) {
            mWorkspace.setVisibility(View.VISIBLE);
            mStateTransitionAnimation.startAnimationToWorkspace(mState, mWorkspace.getState(),
                    Workspace.State.NORMAL, snapToPage, animated, onCompleteRunnable);
        }

        // Change the state *after* we've called all the transition code
        mState = State.WORKSPACE;

        // Resume the auto-advance of widgets
        mUserPresent = true;
        updateAutoAdvanceState();

        if (changed) {
            mWorkspace.stripEmptyScreens(false);
            // Send an accessibility event to announce the context change
            getWindow().getDecorView().sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        }

        return changed;
    }

    void showPanel(boolean isShowOverViewPanel) {
        AnalyticsDelegate.onLauncherLongClickEvent(this, UMEventConstants.LONGCLICK_ENTER);
        showOverviewMode(isShowOverViewPanel, true);
    }

    void hidePanel() {
    }

    private void showLoadingViewAndHideLauncher() {
        mDragLayer.setVisibility(View.INVISIBLE);
        showLoadingDialog();
    }

    private void hideLoadingViewAndShowLauncher() {
        mDragLayer.setVisibility(View.VISIBLE);
        hideLoadingDialog();
    }

    private void showLoadingDialog() {
        if (!mLoadingView.isVisible()) {
            mLoadingView.show();
        }
    }

    private void hideLoadingDialog() {
        if (mLoadingView.isVisible()) {
            mLoadingView.dismiss();
        }
    }

    private void showOverviewMode(boolean isShowOverViewPanel, boolean animated) {
        /*
        if (mWorkspace.getState() == Workspace.State.OVERVIEW)
            return;

        if (isShowOverViewPanel) {
            mOverviewPanel.onShow();
        } else {
            mOverviewPanel.setVisibility(View.INVISIBLE);
        }

        mWorkspace.setVisibility(View.VISIBLE);

        boolean isAddedFirstEmptyPage = mWorkspace.addExtraEmptyScreenInOverviewMode();
        mWorkspace.forceLayout();
        int currentPageIndex = mWorkspace.getCurrentPage();

        if (isAddedFirstEmptyPage) {
            currentPageIndex += 1;
            mWorkspace.moveToScreen(currentPageIndex, false);
        }

        mOverviewPanel.synchronizeHomeBtn();
        */

        /*
        mStateTransitionAnimation.startAnimationToWorkspace(mState, mWorkspace.getState(),
                Workspace.State.OVERVIEW,
                currentPageIndex, animated,
                null);

        mState = State.WORKSPACE;
        mWorkspace.beginShakeAnimations(true);

         */

        if (isShaking()) {
            cancelShakingAnimation();
        } else {
            onShakingAllApps();
        }
    }

    public void onShakingAllApps() {
        if (this.mIsShaking) {
            return;
        }
        this.mIsShaking = true;
        if (this.mWorkspace != null) {
            this.mAddWidgetBtn.setVisibility(View.VISIBLE);
            this.mAddWidgetDoneBtn.setVisibility(View.VISIBLE);
            int[] iArr = new int[2];
            this.mAddWidgetBtn.getLocationOnScreen(iArr);
            this.mAddWidgetDoneBtn.getLocationInWindow(iArr);
            this.mWorkspace.getPageIndicator().disableSearch();
            this.mWorkspace.startTidyUp();
        }
    }

    public void cancelShakingAnimation() {
        if (isShaking()) {
            mIsShaking = false;
            dismissEditMenu();
            if (mWorkspace != null) {
                mAddWidgetDoneBtn.setVisibility(View.GONE);
                mAddWidgetBtn.setVisibility(View.GONE);
                mWorkspace.endTidyUp();
            }
            PageIndicator pageIndicator = this.mWorkspace.getPageIndicator();
            pageIndicator.removeCallbacks(pageIndicator.mSearchAnimRunnable);
            pageIndicator.postOnAnimationDelayed(pageIndicator.mSearchAnimRunnable, 1999L);
        }
    }

    /**
     * Shows the apps view.
     */
    /*
    void showAppsView(boolean animated, boolean resetListToTop, boolean updateNewInstallApps, boolean focusSearchBar, boolean updateAdvertise) {
        if (resetListToTop) {
            mAppsView.scrollToTop();
        }

        mAppsView.refresh();

        if (updateNewInstallApps) {
            updateNewInstallApps();
        }

        mAppsView.startAppmanagerAnimation();
        showApps(State.APPS, animated, focusSearchBar);
    }

     */

    /**
     * Sets up the transition to show the apps/widgets view.
     *
     * @return whether the current from and to state allowed this operation
     */
    // TODO: calling method should use the return value so that when {@code false} is returned
    // the workspace transition doesn't fall into invalid state.
    /*
    private boolean showApps(State toState, boolean animated, boolean focusSearchBar) {
        if (mState != State.WORKSPACE && mState != State.APPS_SPRING_LOADED &&
                mState != State.WIDGETS_SPRING_LOADED) {
            return false;
        }
        if (toState != State.APPS) {
            return false;
        }

        mStateTransitionAnimation.startAnimationToAllApps(mWorkspace.getState(), animated, focusSearchBar);

        // Change the state *after* we've called all the transition code
        mState = toState;

        // Pause the auto-advance of widgets until we are out of AllApps
        mUserPresent = false;
        updateAutoAdvanceState();
        closeFolder();
        AbstractFloatingView.closeAllOpenViews(this);
        showBlurBackgroundForAllApps();
        // Send an accessibility event to announce the context change
        getWindow().getDecorView().sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        return true;
    }

     */

    /**
     * Updates the workspace and interaction state on state change, and return the animation to this
     * new state.
     */
    public Animator startWorkspaceStateChangeAnimation(Workspace.State toState, int toPage,
                                                       boolean animated, HashMap<View, Integer> layerViews) {
        Workspace.State fromState = mWorkspace.getState();
        Animator anim = mWorkspace.setStateWithAnimation(toState, toPage, animated, layerViews);
        updateInteraction(fromState, toState);
        return anim;
    }

    public void enterSpringLoadedDragMode() {
        if (LOGD)
            Log.d(TAG, String.format("enterSpringLoadedDragMode [mState=%s", mState.name()));
        if (mState == State.WORKSPACE || mState == State.APPS_SPRING_LOADED ||
                mState == State.WIDGETS_SPRING_LOADED) {
            return;
        }

        mStateTransitionAnimation.startAnimationToWorkspace(mState, mWorkspace.getState(),
                Workspace.State.SPRING_LOADED,
                WorkspaceStateTransitionAnimation.SCROLL_TO_CURRENT_PAGE, true /* animated */,
                null /* onCompleteRunnable */);
        mState = isAppsViewVisible() ? State.APPS_SPRING_LOADED : State.WIDGETS_SPRING_LOADED;
    }

    public void exitSpringLoadedDragModeDelayed(final boolean successfulDrop, int delay,
                                                final Runnable onCompleteRunnable) {
        if (mState != State.APPS_SPRING_LOADED && mState != State.WIDGETS_SPRING_LOADED)
            return;

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (successfulDrop) {
                    // TODO(hyunyoungs): verify if this hack is still needed, if not, delete.
                    //
                    // Before we show workspace, hide all apps again because
                    // exitSpringLoadedDragMode made it visible. This is a bit hacky; we should
                    // clean up our state transition functions
                    showWorkspace(true, onCompleteRunnable);
                } else {
                    exitSpringLoadedDragMode();
                }
            }
        }, delay);
    }

    void exitSpringLoadedDragMode() {
        if (mState == State.APPS_SPRING_LOADED) {
            //showAppsView(true, false, false, false, false);
        }
        mAddWidgetDoneBtn.setVisibility(View.GONE);
        mAddWidgetBtn.setVisibility(View.GONE);
    }

    public View getOrCreateQsbBar() {
        if (!SearchWidgetUtil.shouldShow(this)) {
            return null;
        }

        if (mQsb == null) {
            LauncherAppWidgetProviderInfo searchProvider = SearchWidgetUtil.getSearchWidgetProvider(this);
            if (searchProvider == null) {
                return null;
            }

            Bundle opts = new Bundle();
            opts.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
                    AppWidgetProviderInfo.WIDGET_CATEGORY_SEARCHBOX);
            SharedPreferences sp = getSharedPreferences(
                    LauncherAppState.getSharedPreferencesKey(), MODE_PRIVATE);
            int widgetId;

            if (searchProvider.isIOSWidget) {
                widgetId = LauncherAppWidgetInfo.IOS_WIDGET_ID;
            } else {
                widgetId = sp.getInt(QSB_WIDGET_ID, -1);
                AppWidgetProviderInfo widgetInfo = mAppWidgetManager.getAppWidgetInfo(widgetId);
                if (!searchProvider.provider.flattenToString().equals(
                        sp.getString(QSB_WIDGET_PROVIDER, null))
                        || (widgetInfo == null)
                        || !widgetInfo.provider.equals(searchProvider.provider)) {
                    // A valid widget is not already bound.
                    if (widgetId > -1) {
                        mAppWidgetHost.deleteAppWidgetId(widgetId);
                        widgetId = -1;
                    }

                    // Try to bind a new widget
                    widgetId = mAppWidgetHost.allocateAppWidgetId();

                    if (!AppWidgetManagerCompat.getInstance(this)
                            .bindAppWidgetIdIfAllowed(widgetId, searchProvider, opts)) {
                        mAppWidgetHost.deleteAppWidgetId(widgetId);
                        widgetId = -1;
                    }

                    sp.edit()
                            .putInt(QSB_WIDGET_ID, widgetId)
                            .putString(QSB_WIDGET_PROVIDER, searchProvider.provider.flattenToString())
                            .commit();
                }
                mAppWidgetHost.setQsbWidgetId(widgetId);
            }


            if (widgetId != -1) {
                mQsb = mAppWidgetHost.createView(this, widgetId, searchProvider);
                mQsb.setId(R.id.qsb_widget);
                mQsb.updateAppWidgetOptions(opts);
                mQsb.setPadding(0, 0, 0, 0);
                //mSearchDropTargetBar.addQsbSearchBar(mQsb);
            }
        }
        return mQsb;
    }

    private void reinflateQSBIfNecessary(boolean forceUpdate) {
        boolean requiredInflate = mQsb instanceof LauncherAppWidgetHostView &&
                ((LauncherAppWidgetHostView) mQsb).isReinflateRequired();
        if (forceUpdate || requiredInflate) {
            //mSearchDropTargetBar.removeQsbSearchBar();
            mQsb = null;
            getOrCreateQsbBar();
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        final boolean result = super.dispatchPopulateAccessibilityEvent(event);
        final List<CharSequence> text = event.getText();
        text.clear();
        // Populate event with a fake title based on the current state.
        if (mState == State.APPS) {
            text.add(getString(R.string.all_apps_button_label));
        } else if (mState == State.WIDGETS) {
            text.add(getString(R.string.widget_button_text));
        } else if (mWorkspace != null) {
            text.add(mWorkspace.getCurrentPageDescription());
        } else {
            text.add(getString(R.string.all_apps_home_button_label));
        }
        return result;
    }

    /**
     * Receives notifications when system dialogs are to be closed.
     */
    @Thunk
    class CloseSystemDialogsIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            closeSystemDialogs();
        }
    }

    /**
     * If the activity is currently paused, signal that we need to run the passed Runnable
     * in onResume.
     * <p>
     * This needs to be called from incoming places where resources might have been loaded
     * while the activity is paused. That is because the Configuration (e.gf., rotation)  might be
     * wrong when we're not running, and if the activity comes back to what the configuration was
     * when we were paused, activity is not restarted.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     *
     * @return {@code true} if we are currently paused. The caller might be able to skip some work
     */
    @Thunk
    boolean waitUntilResume(Runnable run, boolean deletePreviousRunnables) {
        if (mPaused && !mIsGuideMode) {
            if (LOGD)
                Log.d(TAG, "Deferring update until onResume");
            if (deletePreviousRunnables) {
                while (mBindOnResumeCallbacks.remove(run)) {
                }
            }
            mBindOnResumeCallbacks.add(run);
            return true;
        } else {
            return false;
        }
    }

    private boolean waitUntilResume(Runnable run) {
        return waitUntilResume(run, false);
    }

    public void addOnResumeCallback(Runnable run) {
        mOnResumeCallbacks.add(run);
    }


    /**
     * If the activity is currently paused, signal that we need to re-run the loader
     * in onResume.
     * <p>
     * This needs to be called from incoming places where resources might have been loaded
     * while we are paused.  That is becaues the Configuration might be wrong
     * when we're not running, and if it comes back to what it was when we
     * were paused, we are not restarted.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     *
     * @return true if we are currently paused.  The caller might be able to
     * skip some work in that case since we will come back again.
     */
    public boolean setLoadOnResume() {
        if (mPaused) {
            if (LOGD)
                Log.d(TAG, "setLoadOnResume");
            mOnResumeNeedsLoad = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public int getCurrentWorkspaceScreen() {
        if (mWorkspace != null) {
            return mWorkspace.getCurrentPage();
        } else {
            return SCREEN_COUNT / 2;
        }
    }

    /**
     * Refreshes the shortcuts shown on the workspace.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void startBinding() {
        Log.d(TAG, "AbstractFloatingView.closeAllOpenViews startBinding: ");
        AbstractFloatingView.closeAllOpenViews(this);

        /**
         *  ShowWorkspace here, fix bug: on all apps view, go system setting, change language;
         *  Then : all apps ,workspace show at same time;
         *
         *  Real cause not found, this is a temp solution, need continue modify;
         *
         *  huangshuai add (2016.12.02)
         */
        //showWorkspace(false);
        dismissWorkspace(null);

        setWorkspaceLoading(true);

        // If we're starting binding all over again, clear any bind calls we'd postponed in
        // the past (see waitUntilResume) -- we don't need them since we're starting binding
        // from scratch again
        mBindOnResumeCallbacks.clear();

        // Clear the workspace because it's going to be rebound
        mWorkspace.clearDropTargets();
        mWorkspace.removeAllWorkspaceScreens();

        mWidgetsToAdvance.clear();
        if (mHotseat != null) {
            mHotseat.resetLayout();
        }
    }

    @Override
    public void bindScreens(ArrayList<Long> orderedScreenIds) {
        bindAddScreens(orderedScreenIds);

        // If there are no screens, we need to have an empty screen
        if (orderedScreenIds.size() == 0) {
            mWorkspace.addExtraEmptyScreen();
        }

        // Create the custom content page (this call updates mDefaultScreen which calls
        // setCurrentPage() so ensure that all pages are added before calling this).
        if (LeftCustomContentUtil.shouldShow(this)) {
            mWorkspace.createCustomContentContainer();
            populateCustomContentContainer();
            startNewspageApp();
        }
    }

    @Override
    public void bindAddScreens(ArrayList<Long> orderedScreenIds) {
        int count = orderedScreenIds.size();
        for (int i = 0; i < count; i++) {
            mWorkspace.insertNewWorkspaceScreenBeforeEmptyScreen(orderedScreenIds.get(i));
        }
    }

    private void toggleWeightWatcher(boolean show) {
        if (show) {
            if (mWeightWatcher == null) {
                mWeightWatcher = new WeightWatcher(this);
                mWeightWatcher.setAlpha(0.5f);
                mLauncherView.addView(mWeightWatcher,
                        new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT,
                                Gravity.BOTTOM)
                );
            }
            mWeightWatcher.setVisibility(View.VISIBLE);
        } else {
            if (mWeightWatcher != null) {
                mLauncherView.removeView(mWeightWatcher);
                mWeightWatcher = null;
            }
        }
    }

    public void bindAppsAdded(final ArrayList<Long> newScreens,
                              final ArrayList<ItemInfo> addNotAnimated,
                              final ArrayList<ItemInfo> addAnimated,
                              final ArrayList<AppInfo> addedApps) {
        Runnable r = new Runnable() {
            public void run() {
                bindAppsAdded(newScreens, addNotAnimated, addAnimated, addedApps);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        // Add the new screens
        if (newScreens != null) {
            bindAddScreens(newScreens);
        }

        // We add the items without animation on non-visible pages, and with
        // animations on the new page (which we will try and snap to).
        Log.e("adadadada", "bindAppsAdded: ");
        if (addNotAnimated != null && !addNotAnimated.isEmpty()) {
            bindItems(addNotAnimated, 0,
                    addNotAnimated.size(), false);
        }
        if (addAnimated != null && !addAnimated.isEmpty()) {
            bindItems(addAnimated, 0,
                    addAnimated.size(), true);
        }

        // Remove the extra empty screen
        //mWorkspace.removeExtraEmptyScreen(false, false);

        if (addedApps != null && mAppsLibraryLayout != null) {
            try {
                ArrayList<AppCategory> arrayList5 = mAppsLibraryLayout.mCategories;
                if (arrayList5 == null || arrayList5.size() != 10) {
                    // App Library CHƯA dựng category. Khi cold-load, apps được giao qua
                    // bindAppsAdded (không qua bindAllApplications) nên setApps() chưa từng
                    // chạy -> trước đây return ở đây khiến App Library trống trơn tới khi có
                    // reload khác. Thay vì bỏ, gộp addedApps vào danh sách tổng rồi dựng
                    // category ngay từ danh sách đó.
                    if (allApp == null) {
                        allApp = new ArrayList<>();
                    }
                    for (AppInfo ai : addedApps) {
                        if (ai != null && !allApp.contains(ai)) {
                            allApp.add(ai);
                        }
                    }
                    mAppsLibraryLayout.setApps(allApp);
                    if (mSearchViewLayout != null) {
                        mSearchViewLayout.setApps(allApp);
                    }
                    return;
                }
                // Category đã có sẵn: cập nhật tăng dần + đồng bộ danh sách tổng.
                if (allApp == null) {
                    allApp = new ArrayList<>();
                }
                for (AppInfo ai : addedApps) {
                    if (ai != null && !allApp.contains(ai)) {
                        allApp.add(ai);
                    }
                }
                Iterator<AppInfo> it = addedApps.iterator();
                while (it.hasNext()) {
                    AppInfo next = it.next();
                    if (next != null) {
                        int type = mAppsLibraryLayout.getAppType(next);
                        if (type >= 0) {
                            if (mAppsLibraryLayout.mCategories.get(type) != null) {
                                mAppsLibraryLayout.mCategories.get(type).mApps.add(next);
                                mAppsLibraryLayout.mAppLibraryAdapter.notifyItemChanged(type);
                                mAppsLibraryLayout.mSearchResultAdapter.notifyDataSetChanged();
                            }
                        }
                        if (mAppsLibraryLayout.mCategories.get(9) != null) {
                            mAppsLibraryLayout.mCategories.get(9).mApps.add(next);
                            mAppsLibraryLayout.mAppLibraryAdapter.notifyItemChanged(9);
                            mAppsLibraryLayout.mSearchResultAdapter.notifyDataSetChanged();
                        }
                    }
                }
            } catch (Throwable th) {
                th.getMessage();
            }
        }
    }

    /**
     * Bind the items start-end from the list.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindItems(final ArrayList<ItemInfo> shortcuts, final int start, final int end,
                          final boolean forceAnimateIcons) {
        Runnable r = new Runnable() {
            public void run() {
                bindItems(shortcuts, start, end, forceAnimateIcons);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        Log.e("adadadada", "bind: ");
        // Get the list of added shortcuts and intersect them with the set of shortcuts here
        final AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();
        final Collection<Animator> bounceAnims = new ArrayList<Animator>();
        final boolean animateIcons = forceAnimateIcons;
        Workspace workspace = mWorkspace;
        long newShortcutsScreenId = -1;
        for (int i = start; i < end; i++) {
            final ItemInfo item = shortcuts.get(i);
            //check if is hidden app -> not show app on screen
            boolean isHiddenApp = HiddenAppManager.INSTANCE.isHidden(item);
            if (!isHiddenApp) {
                // Short circuit if we are loading dock items for a configuration which has no dock
                if (item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT && mHotseat == null) {
                    continue;
                }

                final View view;
                switch (item.itemType) {
                    case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                    case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                    case LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT:
                    case LauncherSettings.Favorites.ITEM_TYPE_VIRTUAL_APP:
                        ShortcutInfo info = (ShortcutInfo) item;
                        view = createShortcut(info);

                        if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                            CellLayout cl = mWorkspace.getScreenWithId(item.screenId);
                            if (cl != null && cl.isOccupied(item.cellX, item.cellY)) {
                                View v = cl.getChildAt(item.cellX, item.cellY);
                                Object tag = v.getTag();
                                String desc = "Collision while binding workspace item: " + item
                                        + ". Collides with " + tag;
                                if (LauncherAppState.isDogfoodBuild()) {
                                    throw (new RuntimeException(desc));
                                } else {
                                    Log.d(TAG, desc);
                                }

                                mModel.addWorkspaceItemFromFolder(this, item, item.screenId);
                                continue;
                            }
                        }
                        break;
                    case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                        view = FolderIcon.fromXml(R.layout.folder_icon, this,
                                (ViewGroup) workspace.getChildAt(workspace.getCurrentPage()),
                                (FolderInfo) item, mIconCache);
                        break;
                    default:
                        throw new RuntimeException("Invalid Item Type");
                }

                workspace.addInScreenFromBind(view, item.container, item.screenId, item.cellX,
                        item.cellY, 1, 1);
                if (animateIcons) {
                    // Animate all the applications up now
                    view.setAlpha(0f);
                    view.setScaleX(0f);
                    view.setScaleY(0f);
                    bounceAnims.add(createNewAppBounceAnimation(view, i));
                    newShortcutsScreenId = item.screenId;
                }
            }
        }

        if (animateIcons) {
            // Chạy bounce animation cho app mới cài, nhưng KHÔNG kéo màn hình sang
            // page của app đó. Trước đây khi page đầu đã đầy, app mới rơi xuống page
            // cuối và snapToPage() tự cuộn tới page cuối lúc mở launcher. Bỏ snapToPage
            // để luôn ở lại page mặc định (page đầu, ngay dưới 2 widget).
            if (newShortcutsScreenId > -1) {
                final Runnable startBounceAnimRunnable = new Runnable() {
                    public void run() {
                        anim.playTogether(bounceAnims);
                        anim.start();
                    }
                };
                mWorkspace.postDelayed(startBounceAnimRunnable, NEW_APPS_ANIMATION_DELAY);
            }
        }
        workspace.requestLayout();
    }

    /**
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindFolders(final LongArrayMap<FolderInfo> folders) {
        Runnable r = new Runnable() {
            public void run() {
                bindFolders(folders);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
    }

    /**
     * Add the views for a widget to the workspace.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppWidget(final LauncherAppWidgetInfo item) {
        Runnable r = new Runnable() {
            public void run() {
                bindAppWidget(item);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        final long start = DEBUG_WIDGETS ? SystemClock.uptimeMillis() : 0;
        if (DEBUG_WIDGETS) {
            Log.d(TAG, "bindAppWidget: " + item);
        }
        final Workspace workspace = mWorkspace;

        LauncherAppWidgetProviderInfo appWidgetInfo =
                LauncherModel.getProviderInfo(this, item.providerName, item.user);

        if (!mIsSafeModeEnabled
                && ((item.restoreStatus & LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY) == 0)
                && (item.restoreStatus != LauncherAppWidgetInfo.RESTORE_COMPLETED)) {

            if (appWidgetInfo == null) {
                if (DEBUG_WIDGETS) {
                    Log.d(TAG, "Removing restored widget: id=" + item.appWidgetId
                            + " belongs to component " + item.providerName
                            + ", as the povider is null");
                }
                LauncherModel.deleteItemFromDatabase(this, item);
                return;
            }

            // If we do not have a valid id, try to bind an id.
            if ((item.restoreStatus & LauncherAppWidgetInfo.FLAG_ID_NOT_VALID) != 0) {
                // Note: This assumes that the id remap broadcast is received before this step.
                // If that is not the case, the id remap will be ignored and user may see the
                // click to setup view.
                PendingAddWidgetInfo pendingInfo = new PendingAddWidgetInfo(this, appWidgetInfo, null);
                pendingInfo.spanX = item.spanX;
                pendingInfo.spanY = item.spanY;
                pendingInfo.minSpanX = item.minSpanX;
                pendingInfo.minSpanY = item.minSpanY;
                Bundle options = null;
                WidgetHostViewLoader.getDefaultOptionsForWidget(this, pendingInfo);

                int newWidgetId = mAppWidgetHost.allocateAppWidgetId();
                boolean success = mAppWidgetManager.bindAppWidgetIdIfAllowed(
                        newWidgetId, appWidgetInfo, options);

                // TODO consider showing a permission dialog when the widget is clicked.
                if (!success) {
                    mAppWidgetHost.deleteAppWidgetId(newWidgetId);
                    if (DEBUG_WIDGETS) {
                        Log.d(TAG, "Removing restored widget: id=" + item.appWidgetId
                                + " belongs to component " + item.providerName
                                + ", as the launcher is unable to bing a new widget id");
                    }
                    LauncherModel.deleteItemFromDatabase(this, item);
                    return;
                }

                item.appWidgetId = newWidgetId;

                // If the widget has a configure activity, it is still needs to set it up, otherwise
                // the widget is ready to go.
                item.restoreStatus = (appWidgetInfo.configure == null)
                        ? LauncherAppWidgetInfo.RESTORE_COMPLETED
                        : LauncherAppWidgetInfo.FLAG_UI_NOT_READY;

                LauncherModel.updateItemInDatabase(this, item);
            } else if (((item.restoreStatus & LauncherAppWidgetInfo.FLAG_UI_NOT_READY) != 0)
                    && (appWidgetInfo.configure == null)) {
                // If the ID is already valid, verify if we need to configure or not.
                item.restoreStatus = LauncherAppWidgetInfo.RESTORE_COMPLETED;
                LauncherModel.updateItemInDatabase(this, item);
            }
        }

        if (!mIsSafeModeEnabled && item.restoreStatus == LauncherAppWidgetInfo.RESTORE_COMPLETED) {
            final int appWidgetId = item.appWidgetId;
            if (DEBUG_WIDGETS) {
                Log.d(TAG, "bindAppWidget: id=" + item.appWidgetId + " belongs to component "
                        + appWidgetInfo.provider);
            }

            item.hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
            item.minSpanX = appWidgetInfo.minSpanX;
            item.minSpanY = appWidgetInfo.minSpanY;
        } else {
            appWidgetInfo = null;
            PendingAppWidgetHostView view = new PendingAppWidgetHostView(this, item,
                    mIsSafeModeEnabled);
            view.updateIcon(mIconCache);
            item.hostView = view;
            item.hostView.updateAppWidget(null);
            item.hostView.setOnClickListener(this);
        }

        item.hostView.setTag(item);
        item.onBindAppWidget(this);

        workspace.addInScreen(item.hostView, item.container, item.screenId, item.cellX,
                item.cellY, item.spanX, item.spanY, false);
        if (!item.isIOSWidget()) {
            addWidgetToAutoAdvanceIfNeeded(item.hostView, appWidgetInfo);
        }

        workspace.requestLayout();

        if (DEBUG_WIDGETS) {
            Log.d(TAG, "bound widget id=" + item.appWidgetId + " in "
                    + (SystemClock.uptimeMillis() - start) + "ms");
        }
    }

    /**
     * Restores a pending widget.
     *
     * @param appWidgetId The app widget id
     */
    private void completeRestoreAppWidget(final int appWidgetId) {
        LauncherAppWidgetHostView view = mWorkspace.getWidgetForAppWidgetId(appWidgetId);
        if ((view == null) || !(view instanceof PendingAppWidgetHostView)) {
            Log.e(TAG, "Widget update called, when the widget no longer exists.");
            return;
        }

        LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) view.getTag();
        info.restoreStatus = LauncherAppWidgetInfo.RESTORE_COMPLETED;

        mWorkspace.reinflateWidgetsIfNecessary();
        LauncherModel.updateItemInDatabase(this, info);
    }

    public void onPageBoundSynchronously(int page) {
        mSynchronouslyBoundPages.add(page);
//        hideLoadingViewAndShowLauncher();
    }

    /**
     * Callback saying that there aren't any more items to bind.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void finishBindingItems() {
        Runnable r = new Runnable() {
            public void run() {
                finishBindingItems();
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        if (mSavedState != null) {
            if (!mWorkspace.hasFocus()) {
                mWorkspace.getChildAt(mWorkspace.getCurrentPage()).requestFocus();
            }
            mSavedState = null;
        }

        mWorkspace.restoreInstanceStateForRemainingPages();

        setWorkspaceLoading(false);

        // Remove the extra empty screen
        mWorkspace.removeExtraEmptyScreen(false, false);

        //change Gauss image
        LauncherAppState appState = LauncherAppState.getInstance();
        appState.onWallpaperChanged();

        // If we received the result of any pending adds while the loader was running (e.g. the
        // widget configuration forced an orientation change), process them now.
        if (sPendingAddItem != null) {
            final long screenId = completeAdd(sPendingAddItem);

            // TODO: this moves the user to the page where the pending item was added. Ideally,
            // the screen would be guaranteed to exist after bind, and the page would be set through
            // the workspace restore process.
            mWorkspace.post(new Runnable() {
                @Override
                public void run() {
                    mWorkspace.snapToScreenId(screenId);
                }
            });
            sPendingAddItem = null;
        }

        InstallShortcutReceiver.disableAndFlushInstallQueue(this);
        UninstallShortcutReceiver.disableAndFlushUninstallQueue(this);

        NotificationListener.setNotificationsChangedListener(mPopupDataProvider);

        if (mUnreadLoadCompleted) {
            bindWorkspaceUnreadInfo();
        }

        // Khôi phục các page đã ẩn (kiểu iOS): tháo khỏi view tree nhưng giữ app trong DB.
        restoreHiddenPages();

        mBindingWorkspaceFinished = true;
    }

    private void sendLoadingCompleteBroadcastIfNecessary() {
        Intent intent = new Intent(LiteAction.ACTION_LAUNCHER_LOAD_COMPLETE);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private ValueAnimator createNewAppBounceAnimation(View v, int i) {
        ValueAnimator bounceAnim = LauncherAnimUtils.ofPropertyValuesHolder(v,
                PropertyValuesHolder.ofFloat("alpha", 1f),
                PropertyValuesHolder.ofFloat("scaleX", 1f),
                PropertyValuesHolder.ofFloat("scaleY", 1f));
        bounceAnim.setDuration(InstallShortcutReceiver.NEW_SHORTCUT_BOUNCE_DURATION);
        bounceAnim.setStartDelay(i * InstallShortcutReceiver.NEW_SHORTCUT_STAGGER_DELAY);
        bounceAnim.setInterpolator(new OvershootInterpolator(BOUNCE_ANIMATION_TENSION));
        return bounceAnim;
    }

    public boolean useVerticalBarLayout() {
        return mDeviceProfile.isVerticalBarLayout();
    }

    protected Rect getSearchBarBounds() {
        return mDeviceProfile.getSearchBarBounds(Utilities.isRtl(getResources()));
    }

    public void bindSearchProviderChanged() {
        /*
        if (mSearchDropTargetBar == null) {
            return;
        }
        if (mQsb != null) {
            mSearchDropTargetBar.removeView(mQsb);
            mQsb = null;
        }

         */
        getOrCreateQsbBar();
    }

    /**
     * A runnable that we can dequeue and re-enqueue when all applications are bound (to prevent
     * multiple calls to bind the same list.)
     */
    @Thunk
    ArrayList<AppInfo> mTmpAppsList;

    ArrayList<AppInfo> allApp;
    private Runnable mBindAllApplicationsRunnable = new Runnable() {
        public void run() {
            bindAllApplications(mTmpAppsList);
            mTmpAppsList = null;
        }
    };

    /**
     * Add the icons for all apps.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     */
    //todo bind all apps
    public void bindAllApplications(final ArrayList<AppInfo> apps) {
        if (waitUntilResume(mBindAllApplicationsRunnable, true)) {
            mTmpAppsList = apps;
            return;
        }

        if (mAppsLibraryLayout != null) {
            Log.e(TAG, "bindAllApplications: " + apps.toString());
            allApp = apps;
            mAppsLibraryLayout.setApps(apps);
        }

        if (mSearchViewLayout != null) {
            mSearchViewLayout.setApps(apps);
        }

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.bindAllApplications(apps);
        }
    }

    public void addHiddenApp(ItemInfo itemInfo) {
        Log.d(TAG, "addHiddenApp: " + itemInfo.getTargetComponent().getPackageName());
        //todo save to db && remove shortcut from home view
        itemInfo.setPackageName(itemInfo.getTargetComponent().getPackageName());
        HiddenAppManager.INSTANCE.hideApp(itemInfo);
        mWorkspace.removeShortcutInfo(itemInfo);
    }


    @Override
    public void bindDeepShortcutMap(MultiHashMap<ComponentKey, String> deepShortcutMapCopy) {
        mPopupDataProvider.setDeepShortcutMap(deepShortcutMapCopy);
    }

    @Override
    public void bindWorkspaceComponentsRemoved(final ItemInfoMatcher matcher) {
        Runnable r = new Runnable() {
            public void run() {
                bindWorkspaceComponentsRemoved(matcher);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        mWorkspace.removeItemsByMatcher(matcher);
        mDragController.onAppsRemoved(matcher);
    }

    /**
     * A package was updated.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppsUpdated(final ArrayList<AppInfo> apps) {
        Runnable r = new Runnable() {
            public void run() {
                bindAppsUpdated(apps);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
    }

    @Override
    public void bindWidgetsRestored(final ArrayList<LauncherAppWidgetInfo> widgets) {
        Runnable r = new Runnable() {
            public void run() {
                bindWidgetsRestored(widgets);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        mWorkspace.widgetsRestored(widgets);
    }

    /**
     * Some shortcuts were updated in the background.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public void bindShortcutsChanged(final ArrayList<ShortcutInfo> updated,
                                     final ArrayList<ShortcutInfo> removed, final UserHandleCompat user) {
        Runnable r = new Runnable() {
            public void run() {
                bindShortcutsChanged(updated, removed, user);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        if (!updated.isEmpty()) {
            mWorkspace.updateShortcuts(updated);
        }

        if (!removed.isEmpty()) {
            HashSet<ComponentName> removedComponents = new HashSet<ComponentName>();
            HashSet<ShortcutKey> removedDeepShortcuts = new HashSet<>();
            for (ShortcutInfo si : removed) {
                if (si.itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT) {
                    removedDeepShortcuts.add(ShortcutKey.fromItemInfo(si));
                } else {
                    removedComponents.add(si.getTargetComponent());
                }
            }

            if (!removedComponents.isEmpty()) {
                mWorkspace.removeItemsByComponentName(removedComponents, user);
                // Notify the drag controller
                mDragController.onAppsRemoved(new ArrayList<String>(), removedComponents);
            }

            if (!removedDeepShortcuts.isEmpty()) {
                ItemInfoMatcher matcher = ItemInfoMatcher.ofShortcutKeys(removedDeepShortcuts);
                mWorkspace.removeItemsByMatcher(matcher);
                mDragController.onAppsRemoved(matcher);
            }
        }
    }

    /**
     * Update the state of a package, typically related to install state.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public void bindRestoreItemsChange(final HashSet<ItemInfo> updates) {
        Runnable r = new Runnable() {
            public void run() {
                bindRestoreItemsChange(updates);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        mWorkspace.updateRestoreItems(updates);
    }

    /**
     * A package was uninstalled.  We take both the super set of packageNames
     * in addition to specific applications to remove, the reason being that
     * this can be called when a package is updated as well.  In that scenario,
     * we only remove specific components from the workspace, where as
     * package-removal should clear all items by package name.
     *
     * @param reason if non-zero, the icons are not permanently removed, rather marked as disabled.
     *               Implementation of the method from LauncherModel.Callbacks.
     */

    @Override
    public void bindComponentsRemoved(final ArrayList<String> packageNames,
                                      final ArrayList<AppInfo> appInfos,
                                      final UserHandleCompat user,
                                      final int reason) {
        Runnable r = new Runnable() {
            public void run() {
                bindComponentsRemoved(packageNames, appInfos, user, reason);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        if (reason == 0) {
            HashSet<ComponentName> removedComponents = new HashSet<ComponentName>();
            for (AppInfo info : appInfos) {
                removedComponents.add(info.componentName);
            }
            if (!packageNames.isEmpty()) {
                mWorkspace.removeItemsByPackageName(packageNames, user);
            }
            if (!removedComponents.isEmpty()) {
                mWorkspace.removeItemsByComponentName(removedComponents, user);
            }
            // Notify the drag controller
            mDragController.onAppsRemoved(packageNames, removedComponents);

        } else {
            mWorkspace.disableShortcutsByPackageName(packageNames, user, reason);
        }

        if (mAppsLibraryLayout != null) {
            for (AppInfo next : appInfos) {
                if (next != null) {
                    if (mAppsLibraryLayout.mCategories.get(0).mApps.contains(next)) {
                        mAppsLibraryLayout.mCategories.get(0).mApps.remove(next);
                        mAppsLibraryLayout.mSearchResultAdapter.notifyDataSetChanged();
                        mAppsLibraryLayout.mAppLibraryAdapter.notifyItemChanged(0);
                    }
                    int appType = mAppsLibraryLayout.getAppType(next);
                    if (appType > 0) {
                        if (mAppsLibraryLayout.mCategories.get(appType) != null) {
                            mAppsLibraryLayout.mCategories.get(appType).mApps.remove(next);
                            mAppsLibraryLayout.mSearchResultAdapter.notifyDataSetChanged();
                            mAppsLibraryLayout.mAppLibraryAdapter.notifyItemChanged(appType);
                        }
                    } else if (mAppsLibraryLayout.mCategories.get(0) != null && mAppsLibraryLayout.mCategories.get(9) != null) {
                        mAppsLibraryLayout.mCategories.get(9).mApps.remove(next);
                        mAppsLibraryLayout.mAppLibraryAdapter.notifyItemChanged(9);
                        mAppsLibraryLayout.mSearchResultAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }

    @Override
    public void removeShortcutById(final long id) {
        Runnable r = new Runnable() {
            public void run() {
                removeShortcutById(id);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        mWorkspace.removeShortcutById(id);
    }

    private Runnable mBindPackagesUpdatedRunnable = new Runnable() {
        public void run() {
            bindAllPackages(mWidgetsModel);
        }
    };

    @Override
    public void bindAllPackages(final WidgetsModel model) {

        if (model == null) return;

        if (waitUntilResume(mBindPackagesUpdatedRunnable, true)) {
            mWidgetsModel = model;
            return;
        }
        //todo notify wallpaperchange
        notifyWidgetProvidersChanged();
    }

    Map<ShortcutInfo, FolderInfo> mAddCategoryAppMap = new HashMap<>(3);
    private Runnable mBindAddCategoryAppsRunnable = new Runnable() {
        public void run() {
            addAndbindCategoryApps(mAddCategoryAppMap);
        }
    };

    @Override
    public void addAndbindCategoryApps(Map<ShortcutInfo, FolderInfo> categoryMapMap) {
        mAddCategoryAppMap.putAll(categoryMapMap);
        if (waitUntilResume(mBindAddCategoryAppsRunnable, true)) {
            return;
        }

        if (mClings == null) {
            mClings = new LauncherClings(this);
        }

        mClings.showCategoryFolderCling(mAddCategoryAppMap);
        mAddCategoryAppMap.clear();
    }

    public boolean showFirstRunActivity() {
        if (!mLauncherGuideManager.hasRunFirstRunActivity()) {
            mIsGuideMode = true;
            mLauncherGuideManager.showFirstRunActivity(this);
            return true;
        }
        return false;
    }


    public void showGuideActivityAndDialog() {
        if (VersionUpdateManager.hasNewVersionChecked() && !VersionUpdateManager.hasNewVersionPrompted()) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showNewsVersionPromptDialog();
                }
            }, 100);

            return;
        }

        if (Partner.getBoolean(this, Partner.DEF_USER_GUIDE_ENABLE)) {
            if (mLauncherGuideManager.hasCompleteGuide()) {
                return;
            }

            if (!mLauncherGuideManager.hasRunSpeedGuideActivity()) {
                return;
            }

            if (!mLauncherGuideManager.hasRunNewVersionInfoActivity()) {
                mLauncherGuideManager.showNewVersionInfoActivity(this);
                return;
            }

            mLauncherGuideManager.markGuideComplete();
        }
    }

    ObjectAnimator mWorkspaceAnimator;

    public void animateLauncherEndClean() {
        mDragLayer.setBlockTouch(false);
        Animator animator = ObjectAnimator.ofPropertyValuesHolder(mDragLayer,
                PropertyHolderUtis.scaleX(0.1F, 1.0F),
                PropertyHolderUtis.scaleY(0.1F, 1.0F),
                PropertyHolderUtis.alpha(0.0F, 1.0F));
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(100L);
        animator.start();
    }

    public void animateLauncherStartClean(int centerX, int centerY) {
        mDragLayer.setPivotX(centerX);
        mDragLayer.setPivotY(centerY);
        Animator animator = ObjectAnimator.ofPropertyValuesHolder(mDragLayer,
                PropertyHolderUtis.scaleX(1.0F, 0.1F),
                PropertyHolderUtis.scaleY(1.0F, 0.1F),
                PropertyHolderUtis.alpha(1.0F, 0.0F));
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(200L);
        animator.start();
    }

    // TODO: These method should be a part of LauncherSearchCallback
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ItemInfo createAppDragInfo(Intent appLaunchIntent) {
        // Called from search suggestion
        UserHandleCompat user = null;
        if (Utilities.ATLEAST_LOLLIPOP) {
            UserHandle userHandle = appLaunchIntent.getParcelableExtra(Intent.EXTRA_USER);
            if (userHandle != null) {
                user = UserHandleCompat.fromUser(userHandle);
            }
        }
        return createAppDragInfo(appLaunchIntent, user);
    }

    // TODO: This method should be a part of LauncherSearchCallback
    public ItemInfo createAppDragInfo(Intent intent, UserHandleCompat user) {
        if (user == null) {
            user = UserHandleCompat.myUserHandle();
        }

        // Called from search suggestion, add the profile extra to the intent to ensure that we
        // can launch it correctly
        LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(this);
        LauncherActivityInfoCompat activityInfo = launcherApps.resolveActivity(intent, user);
        if (activityInfo == null) {
            return null;
        }
        return new AppInfo(this, activityInfo, user, mIconCache);
    }

    // TODO: This method should be a part of LauncherSearchCallback
    public ItemInfo createShortcutDragInfo(Intent shortcutIntent, CharSequence caption,
                                           Bitmap icon) {
        return new ShortcutInfo(shortcutIntent, caption, caption, icon,
                UserHandleCompat.myUserHandle());
    }

    // TODO: This method should be a part of LauncherSearchCallback
    public void startDrag(View dragView, ItemInfo dragInfo, DragSource source) {
        dragView.setTag(dragInfo);
        mWorkspace.onExternalDragStartedWithItem(dragView);
        mWorkspace.beginExternalDragShared(dragView, source);
    }

    protected void moveWorkspaceToDefaultScreen(boolean animate) {
        if (animate) {
            mWorkspace.post(new Runnable() {
                @Override
                public void run() {
                    mWorkspace.moveToDefaultScreen(true);
                }
            });
        } else {
            mWorkspace.moveToDefaultScreen(false);
        }
    }

    protected void moveToCustomContentScreen(boolean animate, long delay) {
        Log.d(TAG, "AbstractFloatingView.closeAllOpenViews moveToCustomContentScreen: ");
        AbstractFloatingView.closeAllOpenViews(this, false);
        if (animate) {
            mWorkspace.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mWorkspace.moveToCustomContentScreen(true);
                }
            }, delay);
        } else {
            mWorkspace.moveToCustomContentScreen(animate);
        }
    }

    public void setWorkspaceScrollEffect(String effectName) {
        mWorkspace.setScrollEffectFromString(effectName);
        mWorkspace.showScrollEffectAnimation();
    }

    @Override
    public void onPageSwitch(View newPage, int newPageIndex) {

    }

    @Override
    public void onPageBeginMoving() {
        if (isOnDefaultScreen() && mWorkspace.isInNormalMode()) {
        }
    }

    @Override
    public void onPageEndMoving() {
        if (mWorkSpacePageIndex != mWorkspace.getCurrentPage()) {
            mWorkSpacePageIndex = mWorkspace.getCurrentPage();
            LauncherWallpaperManager.setScreenCount(mWorkspace.hasCustomContent() ? mWorkspace.getPageCount() - 1 : mWorkspace.getPageCount());
            LauncherWallpaperManager.setScreenCurrentPosition(mWorkspace.hasCustomContent() ? mWorkSpacePageIndex - 1 : mWorkSpacePageIndex);
            LauncherStateManager.notifyPageSwitch();
        }
    }

    /**
     * Returns a FastBitmapDrawable with the icon, accurately sized.
     */
    public FastBitmapDrawable createIconDrawable(Bitmap icon) {

        Bitmap newBmp = Bitmap.createBitmap(icon.getWidth(), icon.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newBmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        ClockDrawable.drawBgAndReturnSave(
                canvas,
                icon,
                newBmp.getWidth(),
                newBmp.getHeight(),
                paint
        );

        FastBitmapDrawable d = new FastBitmapDrawable(newBmp);
        d.setFilterBitmap(true);
        resizeIconDrawable(d);
        return d;
    }

    public FastBitmapDrawable createIconDrawableWithNoBg(Bitmap icon) {

        Bitmap newBmp = Bitmap.createBitmap(icon.getWidth(), icon.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newBmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setStrokeWidth(0.0f);

        ClockDrawable.drawBgAndReturnSave(
                canvas,
                icon,
                newBmp.getWidth(),
                newBmp.getHeight(),
                null
        );

        FastBitmapDrawable d = new FastBitmapDrawable(newBmp);
        d.setFilterBitmap(true);
        resizeIconDrawable(d);
        return d;
    }

    /**
     * Resizes an icon drawable to the correct icon size.
     */
    public void resizeIconDrawable(Drawable icon) {
        icon.setBounds(0, 0, mDeviceProfile.iconSizePx, mDeviceProfile.iconSizePx);
    }

    public void applyGaussWallpaperBackground() {
        BitmapDrawable gaussWallpaper = mModel.getGaussWallpaperDrawable();
    }


    @Override
    public void onLoadStart(boolean hasLoad) {
        if (!hasLoad && Partner.getBoolean(this, Partner.DEF_LAUNCHER_LOADING_VIEW_ENABLE, true)) {
            showLoadingViewAndHideLauncher();
        }
    }

    @Override
    public void onLoadComplete() {
        if (mIsGuideMode) {
            mIsGuideMode = false;
        }
        hideLoadingViewAndShowLauncher();
        if (!mHasLoaderCompletedOnce) {
            DebugUtil.debugLaunch(TAG, "hasLoaderCompletedOnce");
            sendLoadingCompleteBroadcastIfNecessary();
            mHasLoaderCompletedOnce = true;
        }
    }

    public boolean hasLoaderCompletedOnce() {
        return mHasLoaderCompletedOnce;
    }

    /*
     *服务器获取数据回调
     */
//    @Override
//    public void onSwitchCallback(HashMap<String,SwitchResponseBean.ServerResponseBean> response) {
//        if(response.containsKey(NetworkManager.SWITCH_NEWS_PAGER)){
//            Log.d(TAG,"switch:".concat(NetworkManager.SWITCH_NEWS_PAGER).concat(":").concat(String.valueOf(response.get(NetworkManager.SWITCH_NEWS_PAGER))));
//
//            boolean isSwitchNewsPagerHidden = response.get(NetworkManager.SWITCH_NEWS_PAGER).getFlag() == 0;
//            if (isSwitchNewsPagerHidden) {
//                Settings.setLeftPageEnabled(getApplicationContext(), true);
//            }
//            Settings.setNewsPageSwitchEnable(getApplicationContext(), !isSwitchNewsPagerHidden);
//        }
//
//        if (response.containsKey(NetworkManager.SWITCH_FOLDER_DISCOVERY)) {
//            Log.d(TAG, "switch:".concat(NetworkManager.SWITCH_FOLDER_DISCOVERY).concat(":").concat(String.valueOf(response.get(NetworkManager.SWITCH_FOLDER_DISCOVERY))));
//            boolean isFolderDiscoveryEnable = response.get(NetworkManager.SWITCH_FOLDER_DISCOVERY).getFlag() == 1;
//            Settings.setFolderDiscoveryFeatureEnable(getApplicationContext(), isFolderDiscoveryEnable);
//            String networkFolderDiscoveryListStr = response.get(NetworkManager.SWITCH_FOLDER_DISCOVERY).getValue();
//            if (!TextUtils.isEmpty(networkFolderDiscoveryListStr)) {
//                Settings.setNetworkFolderDiscoveryListStr(getApplicationContext(), networkFolderDiscoveryListStr);
//            }
//        }
//
//        if (response.containsKey(NetworkManager.SWITCH_NOT_UNINSTALL_APP_LIST)) {
//            Log.d(TAG, "switch:".concat(NetworkManager.SWITCH_NOT_UNINSTALL_APP_LIST).concat(":").concat(String.valueOf(response.get(NetworkManager.SWITCH_NOT_UNINSTALL_APP_LIST))));
//            String notUninstallStr = response.get(NetworkManager.SWITCH_NOT_UNINSTALL_APP_LIST).getValue();
//            if (!TextUtils.isEmpty(notUninstallStr)) {
//                Settings.setNotUninstallAppListStr(getApplicationContext(), notUninstallStr);
//            }
//        }
//
//        if (response.containsKey(NetworkManager.DESKTOP_SHORTCUT_BLACKLIST)) {
//            Log.d(TAG, "switch:".concat(NetworkManager.DESKTOP_SHORTCUT_BLACKLIST).concat(":").concat(String.valueOf(response.get(NetworkManager.DESKTOP_SHORTCUT_BLACKLIST))));
//            boolean isNetworkShortcutBlacklistEnable = response.get(NetworkManager.DESKTOP_SHORTCUT_BLACKLIST).getFlag() == 1;
//            Settings.setNetworkShortcutBlacklistEnabled(getApplicationContext(),isNetworkShortcutBlacklistEnable);
//            String shortcutBlacklistStr = response.get(NetworkManager.DESKTOP_SHORTCUT_BLACKLIST).getValue();
//            if (!TextUtils.isEmpty(shortcutBlacklistStr)) {
//                Settings.setNetworkShortcutBlacklistStr(getApplicationContext(), shortcutBlacklistStr);
//            }
//        }
//    }
//
//    @Override
//    public List<String> onRequestKey() {
//        List<String> list = new ArrayList<>();
//        list.add(NetworkManager.SWITCH_NEWS_PAGER);
//        list.add(NetworkManager.SWITCH_FOLDER_DISCOVERY);
//        list.add(NetworkManager.SWITCH_NOT_UNINSTALL_APP_LIST);
//        list.add(NetworkManager.DESKTOP_SHORTCUT_BLACKLIST);
//        return list;
//    }

    /**
     * 桌面设置回调接口
     */
    @Override
    public void onLauncherSettingChanged(String key) {
        // 桌面负一屏
        if (key.equals(Settings.PREFER_LEFT_PAGE_ENABLE)) {
            final boolean enable = Settings.isLeftPageEnabled(this);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    invalidateHasCustomContentToLeft(enable);
                }
            });
        }

        // 桌面文件夹底部探索
        if (key.equals(Settings.PREFER_FOLDER_DISCOVERY_USER_ENABLED) || key.equals(Settings.PREFER_FOLDER_DISCOVERY_FEATURE_ENABLED)) {
            final boolean enable = Settings.isFolderDiscoveryEnable(this);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                }
            });
        }

        // 桌面搜索框
        if (key.equals(Settings.PREFER_SEARCH_BAR_ENABLE)) {
            reloadLauncher(false/*resetDesktop*/);
        }

        // 应用切换动画
        if (key.equals(Settings.PREFER_APP_ANIMATION_STYLE)) {
            mAppAnimationStyle = Settings.getAppAnimationStyle(this);
        }

        // Workspace 字体大小
        if (key.equals(Settings.PREFER_WORKSPACE_TEXT_SIZE_SCALE)) {
            float textSizeScale = Settings.getWorkspaceTextSizeScale(this);
            if (textSizeScale != mDeviceProfile.workspaceIconTextSizeScale) {
                scaleWorkspaceIconTextSize(textSizeScale);
            }
        }

        if (key.equals(Settings.PREFER_WORKSPACE_TEXT_FONT)) {
            int textFont = Settings.getWorkspaceTextFont(this);
            if (textFont != mDeviceProfile.workspaceIconTextFont) {
                scaleWorkspaceIconTextFont(textFont);
            }
        }

        // Workspace 图标大小
        if (key.equals(Settings.PREFER_WORKSPACE_ICON_SIZE_SCALE)) {
            scaleWorkspaceIcon();
            float iconSizeScale = Settings.getWorkspaceIconSizeScale(this);
            if (iconSizeScale != mDeviceProfile.workspaceIconSizeScale) {
                scaleWorkspaceIcon();
            }
        }

        // Droptarget 删除动画
        if (key.equals(Settings.PREFER_DROP_TARGET_ANIM_STYLE)) {
            setDeleteDropTargetAnim(Settings.getDropTargetAnimStyle(this));
        }

        // 更改桌面行列数
        if (key.equals(Settings.PREFER_DESKTOP_GRID)) {
            reloadLauncher(true/*resetDesktop*/);
        }

        // 是否允许壁纸滚动
        if (key.equals(Settings.PREFER_WALLPAPER_SCROLL_ENABLE)) {
            mWorkspace.changeWallpaperScroll();
        }

        // Workspace 字体颜色
        if (key.equals(Settings.PREFER_WORKSPACE_ICON_LABEL_COLOR)) {
            changeWorkspaceIconTextColor(Settings.getWorkspaceLabelColor(this));
        }

        //  智能整理
        if (key.equals(Settings.PREFER_CATEGORY_NEW_APPS_ENABLE)) {
            final boolean enable = Settings.isNewAppsCategoryEnable(this);
            LauncherAppState.setNewAppsCategotyEnable(enable);
        }


        // 内存检测窗口
        if (key.equals(Settings.PREFER_MEMORY_WATCHER_ENABLE)) {
            toggleWeightWatcher(Settings.isMemWatcherEnabled(this));
        }

        // IOSKnow
        if (key.equals(Settings.PREFER_SWING_VIEW_ENABLE)) {
            mEnableIOSKnow = Settings.isSwingViewEnabled(this);
            if (mEnableIOSKnow) {
            } else {
            }
        }

        if (key.equals(Settings.PREFER_WORKSPACE_SCROLL_EFFECT)) {
            this.setWorkspaceScrollEffect(Settings.getWorkspaceScrollEffect(this));
        }
    }

    /**
     * Implements LauncherDelegate;
     */
    @Override
    public void scrollLauncherToDefaultScreen(boolean animate) {
        moveWorkspaceToDefaultScreen(animate);
    }

    public void startClockApp() {
        startAppForSpeicType(AppTypeParser.APP_TYPE_CLOCK);
    }


    public void startCalendarApp() {
        startAppForSpeicType(AppTypeParser.APP_TYPE_CALENDAR);
    }

    private void startAppForSpeicType(String appType) {
        AppTypeProvider appTypeProvider = AppTypeProvider.getAppTypeProvider();
        if (appTypeProvider != null) {
            ComponentName cn = appTypeProvider.getComponentNameForAppType(appType);
            final Intent intent = new Intent(Intent.ACTION_MAIN, null)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(cn)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            Utilities.startActivitySafely(this, intent);
        }
    }


    private boolean checkAndApplyTheme(Intent intent) {
        String themePkgName = intent.getStringExtra(INTENT_EXTREA_LAUNCHE_NEW_THEME);
        return LauncherAppState.getInstance().applyNewTheme(themePkgName);
    }


    /**
     * Show new version upgrade dialog;
     */
    private void showNewsVersionPromptDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.update_self_dialog_title);
        builder.setMessage(R.string.update_self_dialog_content);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                VersionUpdateManager.setNewVersionPromptedFlag();
                VersionUpdateManager.updateApp();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                VersionUpdateManager.setNewVersionPromptedFlag();
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);

        if (args.length > 0 && TextUtils.equals(args[0], "--all")) {
            writer.println(prefix + "Workspace Items");
            for (int i = mWorkspace.numCustomPages(); i < mWorkspace.getPageCount(); i++) {
                writer.println(prefix + "  Homescreen " + i);

                ViewGroup layout = ((CellLayout) mWorkspace.getPageAt(i)).getShortcutsAndWidgets();
                for (int j = 0; j < layout.getChildCount(); j++) {
                    Object tag = layout.getChildAt(j).getTag();
                    if (tag != null) {
                        writer.println(prefix + "    " + tag.toString());
                    }
                }
            }

            writer.println(prefix + "  Hotseat");
            ViewGroup layout = mHotseat.getLayout().getShortcutsAndWidgets();
            for (int j = 0; j < layout.getChildCount(); j++) {
                Object tag = layout.getChildAt(j).getTag();
                if (tag != null) {
                    writer.println(prefix + "    " + tag.toString());
                }
            }

            try {
                FileLog.flushAll(writer);
            } catch (Exception e) {
                // Ignore
            }
        }

        writer.println(prefix + "Misc:");
        writer.print(prefix + "\tmWorkspaceLoading=" + mWorkspaceLoading);

        mModel.dumpState(prefix, fd, writer, args);
    }

    /**
     * Prints out out state for debugging.
     */
    public void dumpState() {
        mModel.dumpState();
    }


    public static void addDumpLog(String tag, String log, boolean debugLog) {
        addDumpLog(tag, log, null, debugLog);
    }

    public static void addDumpLog(String tag, String log, Exception e, boolean debugLog) {
        if (debugLog) {
            if (e != null) {
                Log.d(tag, log, e);
            } else {
                Log.d(tag, log);
            }
        }
        if (DEBUG_DUMP_LOG) {
            sDateStamp.setTime(System.currentTimeMillis());
            synchronized (sDumpLogs) {
                sDumpLogs.add(sDateFormat.format(sDateStamp) + ": " + tag + ", " + log
                        + (e == null ? "" : (", Exception: " + e)));
            }
        }
    }

    public static HashMap<String, IOSAppWidget> getIOSAppWidgets() {
        return sIOSAppWidgets;
    }

    public void dumpLogsToLocalData() {
        if (DEBUG_DUMP_LOG) {
            new AsyncTask<Void, Void, Void>() {
                public Void doInBackground(Void... args) {
                    boolean success = false;
                    sDateStamp.setTime(sRunStart);
                    String FILENAME = sDateStamp.getMonth() + "-"
                            + sDateStamp.getDay() + "_"
                            + sDateStamp.getHours() + "-"
                            + sDateStamp.getMinutes() + "_"
                            + sDateStamp.getSeconds() + ".txt";

                    FileOutputStream fos = null;
                    File outFile = null;
                    try {
                        outFile = new File(getFilesDir(), FILENAME);
                        outFile.createNewFile();
                        fos = new FileOutputStream(outFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (fos != null) {
                        PrintWriter writer = new PrintWriter(fos);

                        writer.println(" ");
                        writer.println("Debug logs: ");
                        synchronized (sDumpLogs) {
                            for (int i = 0; i < sDumpLogs.size(); i++) {
                                writer.println("  " + sDumpLogs.get(i));
                            }
                        }
                        writer.close();
                    }
                    try {
                        if (fos != null) {
                            fos.close();
                            success = true;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
        }
    }

    private void enterAppAnimation() {
        final int animStyle = mTempAppAnimationStyle > 0 ? mTempAppAnimationStyle : mAppAnimationStyle;
        switch (animStyle) {
            case APP_ANIM_ZOOM:
                overridePendingTransition(R.anim.in_pending_zoom, R.anim.in_pending_zoom_o);
                break;
            case APP_ANIM_ROTATE:
                overridePendingTransition(R.anim.in_pending_rotate, R.anim.in_pending_rotate_o);
                break;
            case APP_ANIM_FADE:
                overridePendingTransition(R.anim.fade_in_short_time, R.anim.fade_out_short_time); //changed by Hong

            default:
                break;
        }

    }

    public void setTempAppAnimationStyle(int style) {
        mTempAppAnimationStyle = style;
    }


    public void changeWorkspaceIconTextColor(int color) {
        /*mDeviceProfile.workspaceIconTextColor = color;
        mWorkspace.changeIconTextColor(color);
        mHotseat.changeIconTextColor(color);*/
        /*文件夹里的应用不会改变*/
        reloadLauncher(false/*resetDesktop*/);
    }

    public void scaleWorkspaceIconTextFont(int font) {
        /*mDeviceProfile.scaleIconTextSize(this, scale);
        mWorkspace.setIconTextSizePx(mDeviceProfile.iconTextSizePx);*/
        /*文件夹里的应用不会改变*/
        reloadLauncher(false/*resetDesktop*/);
    }

    public void scaleWorkspaceIconTextSize(float scale) {
        /*mDeviceProfile.scaleIconTextSize(this, scale);
        mWorkspace.setIconTextSizePx(mDeviceProfile.iconTextSizePx);*/
        /*文件夹里的应用不会改变*/
        reloadLauncher(false/*resetDesktop*/);
    }

    public void scaleWorkspaceIcon() {
        reloadLauncher(false/*resetDesktop*/);
    }

    public void setDeleteDropTargetAnim(int style) {
        //mSearchDropTargetBar.setDeletDropTargetAnim(style);
    }

    private UnreadLoaderCompact mUnreadLoader = null;
    private boolean mUnreadLoadCompleted = false;
    private boolean mBindingWorkspaceFinished = false;
    private boolean mBindingAppsFinished = false;

    @Override
    public void bindComponentUnreadChanged(final ComponentName component, final int unreadNum) {
        DebugUtil.debugUnread(TAG, "bindComponentUnreadChanged: component = " + component
                + ", unreadNum = " + unreadNum + ", this = " + this);
        // Post to message queue to avoid possible ANR.
        mHandler.post(new Runnable() {
            public void run() {
                final long start = System.currentTimeMillis();
                if (mWorkspace != null) {
                    mWorkspace.updateComponentUnreadChanged(component, unreadNum);
                }
            }
        });


    }

    @Override
    public void bindUnreadInfoIfNeeded() {
        DebugUtil.debugUnread(TAG, "bindUnreadInfoIfNeeded: mBindingWorkspaceFinished = "
                + mBindingWorkspaceFinished + ", thread = " + Thread.currentThread());
        if (mBindingWorkspaceFinished) {
            bindWorkspaceUnreadInfo();
        }

        if (mBindingAppsFinished) {
            bindAppsUnreadInfo();
        }

        mUnreadLoadCompleted = true;
    }


    private void bindWorkspaceUnreadInfo() {
        mHandler.post(new Runnable() {
            public void run() {
                if (mWorkspace != null) {
                    mWorkspace.updateShortcutsAndFoldersUnread();
                }
            }
        });
    }

    private void bindAppsUnreadInfo() {
    }

    @Override
    public void onRecentChange(ArrayList<ComponentName> sortComps) {
    }

    public void restartSelf() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public boolean enableGestureEvent() {
        return mWorkspace.isInNormalMode() && !isOnCustomContent() && !isFolderOpen();
    }

    private void checkAndRequestPermission() {

        if (Utilities.ATLEAST_MARSHMALLOW) {
            PermissionUtil.checkSelfPermissions(this, Manifest.permission.READ_CALL_LOG);
        }
        /*
        if(this.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED){
              this.requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG},1);
              Log.d("5258=====>xiaopeng","Manifest.permission.READ_CALL_LOG.......");
        }
        */
    }

    private void startNewspageApp() {
        Intent intent = new Intent("android.intent.action.INSTALLED_COMPLETE");
        intent.setPackage("com.ios.widget.newspage");
        sendBroadcast(intent);
    }

    public LauncherCallbacks mLauncherCallbacks;

    /**
     * Call this after onCreate to set or clear overlay.
     */
    public void setLauncherOverlay(LauncherOverlay overlay) {
        if (overlay != null) {
            overlay.setOverlayCallbacks(new LauncherOverlayCallbacksImpl());
        }
        mWorkspace.setLauncherOverlay(overlay);
    }

    public boolean setLauncherCallbacks(LauncherCallbacks callbacks) {
        mLauncherCallbacks = callbacks;
        return true;
    }

    public interface LauncherOverlay {

        /**
         * Touch interaction leading to overscroll has begun
         */
        void onScrollInteractionBegin();

        /**
         * Touch interaction related to overscroll has ended
         */
        void onScrollInteractionEnd();

        /**
         * Scroll progress, between 0 and 100, when the user scrolls beyond the leftmost
         * screen (or in the case of RTL, the rightmost screen).
         */
        void onScrollChange(float progress, boolean rtl);

        /**
         * Called when the launcher is ready to use the overlay
         *
         * @param callbacks A set of callbacks provided by Launcher in relation to the overlay
         */
        void setOverlayCallbacks(LauncherOverlayCallbacks callbacks);
    }

    public interface LauncherOverlayCallbacks {
        void onScrollChanged(float progress);
    }

    class LauncherOverlayCallbacksImpl implements LauncherOverlayCallbacks {

        public void onScrollChanged(float progress) {
            if (mWorkspace != null) {
                mWorkspace.onOverlayScrollChanged(progress);
            }
        }
    }

    public boolean hasSettings() {
        if (mLauncherCallbacks != null) {
            return mLauncherCallbacks.hasSettings();
        } else {
            // On O and above we there is always some setting present settings (add icon to
            // home screen or icon badging). On earlier APIs we will have the allow rotation
            // setting, on devices with a locked orientation,
            return Utilities.ATLEAST_OREO || !getResources().getBoolean(R.bool.allow_rotation);
        }
    }

    private final ArrayList<DeviceProfile.OnDeviceProfileChangeListener> mDPChangeListeners = new ArrayList<>();


    public void addOnDeviceProfileChangeListener(DeviceProfile.OnDeviceProfileChangeListener listener) {
        mDPChangeListeners.add(listener);
    }

    public void removeOnDeviceProfileChangeListener(DeviceProfile.OnDeviceProfileChangeListener listener) {
        mDPChangeListeners.remove(listener);
    }

    protected void dispatchDeviceProfileChanged() {
        for (int i = mDPChangeListeners.size() - 1; i >= 0; i--) {
            mDPChangeListeners.get(i).onDeviceProfileChanged(mDeviceProfile);
        }
    }

    public boolean isDragging() {
        return this.mDragController != null && this.mDragController.isDragging();
    }

    public boolean isInWorkspace() {
        return this.mState == State.WORKSPACE;
    }

    public boolean isWorkspaceNormal() {
        if (this.mWorkspace != null) {
            return this.mWorkspace.isInNormalMode();
        }
        return true;
    }

    public boolean isInPreviewView() {
        return false;//this.mPreviewView != null && (this.mPreviewViewContainer.getVisibility() == 0 || hasShowPreviewMsg() || this.mPreviewView.isAnimating());
    }

    public int getWorkspaceWidth() {
        return this.mWorkspaceWidth;
    }

    public int getWorkspaceHeight() {
        return this.mWorkspaceHeight;
    }

    public void setWorkspaceWidth(int width) {
        this.mWorkspaceWidth = width;
    }

    public void setWorkspaceHeight(int height) {
        this.mWorkspaceHeight = height;
    }

    public void startTidyUp() {
        mWorkspace.startTidyUp();
    }

    public boolean isTidyUping() {
        return DragLayer.sTidyUping;
    }

    public void endTidyUp() {
        Folder openFolder;
        if (isDragging()) {
            this.mDragController.cancelDrag();
        }

        if (isTidyUping()) {
            this.mWorkspace.endTidyUp(true);
        }

//        if (this.mWorkspace.isSpringLoaded()) {
//            this.mWorkspace.clearAppWidgetAnimation();
//            this.mWorkspace.changeStateToSmall(Workspace.State.NORMAL);
//        }
//        closeToggle(false);
//        hidePageListView();
    }

    private static int sCellHeight = -1;
    private static int sCellWidth = -1;

    public static int getCellWidth() {
        return sCellWidth;
    }

    public static int getCellHeight() {
        return sCellHeight;
    }

    public static void setCellWidthAndHeight(int width, int height) {
        sCellWidth = width;
        sCellHeight = height;
    }

    public static String getRealSystemFolderTitle(Launcher launcher, String str) {
        if (!str.startsWith(" ")) {
            return str;
        }
        String[] sysFolder = launcher.getSysFolder();
        String[] sysFolderName = launcher.getSysFolderName();
        if (sysFolder.length != sysFolderName.length) {
            return str;
        }
        for (int i = 0; i < sysFolder.length; i++) {
            if (str.equals(sysFolder[i])) {
                return sysFolderName[i];
            }
        }
        return null;
    }

    public String[] getSysFolder() {
        if (this.mSysFolder != null) {
            this.mSysFolder = getResources().getStringArray(R.array.system_folder_title);
        }
        return this.mSysFolder;
    }

    public String[] getSysFolderName() {
        if (this.mSysFolderName == null) {
            this.mSysFolderName = getResources().getStringArray(R.array.system_foldertitle_res);
        }
        return this.mSysFolderName;
    }

    public boolean isResuming() {
        return !this.mPaused;
    }

    public void removeFolder(FolderInfo folderInfo) {
//        if (folderInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
////            DockBar.sScreenData.removeItem(folderInfo);
//        }

        String title = String.format("Remove %s?", folderInfo.title);
        List<IOSDialogButton> iosDialogButtons = new ArrayList<>();
        iosDialogButtons.add(new IOSDialogButton(1, "Remove From Home Screen", false, IOSDialogButton.TYPE_POSITIVE));
        iosDialogButtons.add(new IOSDialogButton(2, "Cancel", false, IOSDialogButton.TYPE_POSITIVE));

        new IOSDialog.Builder(this)
                .title(title)
                .message("Removing from Home Screen will keep all apps from the last page. Bookmarks will be moved out of the folder.")
                .multiOptions(true)
                .multiOptionsListeners(new IOSDialogMultiOptionsListeners() {
                    @Override
                    public void onClick(IOSDialog iosDialog, IOSDialogButton iosDialogButton) {
                        iosDialog.dismiss();
                        switch (iosDialogButton.getId()) {
                            case 1:
                                break;
                            case 2:
                                break;
                        }
                    }
                })
                .iosDialogButtonList(iosDialogButtons)
                .build()
                .show();
    }

    public int getPrivatePageCount() {
        return this.mWorkspace.getPrivatePageCount();
    }

    public int getNormalPageCount() {
        return getWorkspaceScreenSize() - getPrivatePageCount();
    }

    public int getWorkspaceScreenSize() {
        return this.mWorkspace.getChildCount();
    }

    public void hideBlurBg() {
        this.m_imgFolderBlurBg.setImageDrawable(null);
        this.m_imgFolderBlurBg.setVisibility(View.GONE);
        if (this.m_bmpBlurResult != null) {
            this.m_bmpBlurResult.recycle();
            this.m_bmpBlurResult = null;
        }
    }

    public void uninstallApplication(ShortcutInfo shortcutInfo) {
        if (shortcutInfo == null) {
            return;
        }
        if (shortcutInfo.itemType != LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
            if (shortcutInfo.screenId == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                this.getHotseat().removeShortcut(shortcutInfo);
            } else {
                this.mWorkspace.removeShortcutInfoInPage(shortcutInfo);
            }
        } else if (shortcutInfo != null && shortcutInfo.uninstallable && shortcutInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
            if (shortcutInfo instanceof AppInfo) {
                showUninstallDialog((AppInfo) shortcutInfo);
            } else if (shortcutInfo instanceof ShortcutInfo) {
                if (shortcutInfo.screenId == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                    this.getHotseat().removeShortcut(shortcutInfo);
                } else {
                    this.mWorkspace.removeShortcutInfoInPage(shortcutInfo);
                }
            }
        }
    }

    private void showUninstallDialog(final AppInfo applicationInfo) {
        Launcher.this.startUninstallApp(applicationInfo);
        Log.e(TAG, "onPrepareDialog --->DIALOG_UNINSTALL_APP ---> appInfo null. ");
    }

    private String cutString(int maxLength, String str) {
        if (this.mTextMeasurePaint == null) {
            this.mTextMeasurePaint = new Paint();
            this.mTextMeasurePaint.setTextSize(getResources().getDimension(R.dimen.dialog_title_tsize));
        }
        if (Math.ceil((double) this.mTextMeasurePaint.measureText(str)) <= ((double) maxLength)) {
            return str;
        }
        int stripText = Utilities.stripText(str, ((float) maxLength) - this.mTextMeasurePaint.measureText("..."), this.mTextMeasurePaint);
        return stripText > 0 ? str.substring(0, stripText) + "..." : str.length() > 4 ? str.substring(0, 4) : str;
    }

    private String cutString(String str) {
        return str.length() > 10 ? str.substring(0, 10) + "......" : str;
    }

    private void startUninstallApp(AppInfo applicationInfo) {
        Log.d(TAG, "UninstallApp -- uninstallApp -- itemInfo = " + applicationInfo);
        if (applicationInfo != null) {
            String packageName = applicationInfo.componentName.getPackageName();
            if (!this.mModel.checkApplicationEnabled(this, packageName)) {
                return;
            }
            startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + packageName)));
        }
    }

    public int getIndexByScreenId(long screenId) {
        if (this.mWorkspace != null) {
            int childCount = this.mWorkspace.getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (((CellLayout) this.mWorkspace.getChildAt(i)).getScreenId() == screenId) {
                    return i;
                }
            }
        }
        return -1;
    }

    public boolean isInBatchMode() {
        return this.mIsBatchMode;
    }

//    public View createShortcut(int layoutId, ViewGroup viewGroup, ShortcutInfo shortcutInfo) {
//        BubbleTextView bubbleTextView = (BubbleTextView) this.mInflater.inflate(layoutId, viewGroup, false);
//        if (shortcutInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_VIRTUAL_APP) {
//            bubbleTextView.applyFromShortcutInfo(shortcutInfo);
//        } else {
//            bubbleTextView.applyFromShortcutInfo(shortcutInfo, this.mIconCache);
//        }
//
//        bubbleTextView.setCompoundDrawablePadding(mDeviceProfile.iconDrawablePaddingPx);
//        bubbleTextView.setClickable(true);
//        bubbleTextView.setOnClickListener(this);
//        bubbleTextView.setOnFocusChangeListener(mFocusHandler);
//
////        bubbleTextView.setLauncher(this);
////        bubbleTextView.setDarkEffectListener(this.mDarkEffectAgent);
//        return bubbleTextView;
//    }

    public void showPageListView() {
//        if (!this.mIsPageListViewVisible) {
//            this.mDockbar.changeStateToSmall();
//            this.mPageListView.show();
//            this.mIsPageListViewVisible = true;
//        }
    }

    public static int getRealStatusBarHeight(Context context) {
        try {
            Class<?> cls = Class.forName("com.android.internal.R$dimen");
            return context.getResources().getDimensionPixelSize(Integer.parseInt(cls.getField("status_bar_height").get(cls.newInstance()).toString()));
        } catch (Exception e) {
            if (context instanceof Launcher) {
                return (int) (25.0f * context.getResources().getDisplayMetrics().density);
            }
            return 25;
        }
    }

    public void commandCloseFolder() {
//        if (!this.mHandler.hasMessages(2) &&
//                !isDragging() && this.mWorkspace.isNormal() &&
//                this.mPreviewView != null &&
//                this.mPreviewView.isZoomIn() &&
//                !this.mPreviewView.isAnimating()) {
//            this.mWorkspace.sendCommandToCurrScreen(18, null);
//        }
    }

    private int mWorkspaceWidth;
    private int mWorkspaceHeight;
    protected String[] mSysFolder;
    protected String[] mSysFolderName;
    private Bitmap m_bmpBlurResult;
    private ImageView m_imgFolderBlurBg;
    private Thread m_threadBlur;
    private ValueAnimator mFolderImgBlurBgAnim = null;
    private static final int SC_MULTIPLE_CHOICES = 2;
    private CommonDialog.Builder mBuilder = null;
    private CommonDialog mCustomDialog = null;
    Paint mTextMeasurePaint;
    private boolean mIsBatchMode;
    private InputMethodManager mInputMethodManager;

    private void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = this.mInputMethodManager;
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onAppsLibraryClosed() {
        // TODO: 2023.11.08 Hide Blur Effect
        //mAppsLibraryLayout.mSearchBlurView.setBitmapBackground(appsLibraryLayout.e.getAppsLibraryBlurBackgroundView().getBlurBitmap());
    }

    //todo app library
    @Override
    public void onAppsLibraryOpened() {
        this.mAppsLibraryLayout.clearFocus();
        hideKeyboard(mAppsLibraryLayout);
        BouncyRecyclerView bouncyRecyclerView = mAppsLibraryLayout.mSearchResultRV;

        if (bouncyRecyclerView != null && bouncyRecyclerView.getVisibility() == View.VISIBLE) {
            this.mAppsLibraryLayout.transitionToStart();
        }
        // Đảm bảo mỗi lần mở library đều có app: dựng lại category nếu trống + khôi phục
        // hiển thị danh sách (phòng bind bị bỏ lỡ hoặc transition search để lại alpha=0).
        //
        // allApp chỉ được gán khi callback bind (bindAllApplications/bindAppsAdded) chạy tới
        // đúng activity đang sống. Do vòng đời loader của Launcher3, callback có thể post vào
        // activity cũ đã chết -> allApp còn null dù model ĐÃ nạp xong app. Khi đó đọc thẳng từ
        // nguồn sự thật của model (mBgAllAppsList.data qua getAllAppInfo) để không bao giờ trống.
        ArrayList<AppInfo> appsForLibrary = allApp;
        if ((appsForLibrary == null || appsForLibrary.isEmpty()) && mModel != null) {
            java.util.List<AppInfo> fromModel = mModel.getAllAppInfo();
            if (fromModel != null && !fromModel.isEmpty()) {
                appsForLibrary = new ArrayList<>(fromModel);
                allApp = appsForLibrary;
            }
        }
        this.mAppsLibraryLayout.ensureReady(appsForLibrary);
    }

    @Override
    public void onAppsLibrarySlide(float f) {
        float f2 = 1.0f - f;
        float interpolation = initInterpolator(0.0f, 0.0f, 0.58f, 1.0f).getInterpolation(f2);
        float interpolation2 = initInterpolator(0.35f, 0.19f, 0.84f, 0.56f).getInterpolation(f2);
        mSliderBlurBg.changeBlur(interpolation);
        float f3 = 1.0f - (interpolation2 * 0.1f);
        getDragLayer().setScaleX(f3);
        getDragLayer().setScaleY(f3);
    }

    private BlurScreenLayout getAppsLibraryBlurBackgroundView() {
        return mSliderBlurBg;
    }

    // todo left view
    @Override
    public void onLeftPageClosed() {
        hideKeyboard(this.mCustomContentView);
        mCustomContentView.onClosePage();
        //((CustomContentView.a) getWorkspace().getCustomContentCallbacks()).b(false);
    }

    @Override
    public void onLeftPageOpened() {
        mCustomContentView.clearFocus();
        mCustomContentView.onOpenPage();
        hideKeyboard(mCustomContentView);
    }

    @Override
    public void onLeftPageSlide(float f) {
        float f2 = 1.0f - f;
        float interpolation = Launcher.initInterpolator(0.0f, 0.0f, 0.58f, 1.0f).getInterpolation(f2);
        float interpolation2 = Launcher.initInterpolator(0.35f, 0.19f, 0.84f, 0.56f).getInterpolation(f2);
        getAppsLibraryBlurBackgroundView().changeBlur(interpolation);
        float f3 = 1.0f - (interpolation2 * 0.1f);
        getDragLayer().setScaleX(f3);
        getDragLayer().setScaleY(f3);
    }

    public static Interpolator initInterpolator(float control1, float control2, float control3, float control4) {
        return Build.VERSION.SDK_INT >= 21 ? new PathInterpolator(control1, control2, control3, control4) : new CubicInterpolate(control1, control2, control3, control4);
    }

    public boolean isOpeningFolder() {
        Workspace workspace = this.mWorkspace;
        return (workspace != null ? workspace.getOpenFolder() : null) != null;
    }

    public boolean isOpeningFloatingMenu() {
        return showingFloatingMenu;
    }

    public IconCache getIconCache() {
        return mIconCache;
    }

    public SearchViewLayout getSearchViewLayout() {
        return mSearchViewLayout;
    }

    public BlurScreenLayout getBlurBackground() {
        return this.mBlurBackgroundView;
    }

    public boolean isOpeningAppsLibrary() {
        DragAppsLibraryLayout dragAppsLibraryLayout = mDragAppsLibraryLayout;
        return dragAppsLibraryLayout != null && dragAppsLibraryLayout.isAppsLibraryOpening(this.mDeviceProfile.widthPx);
    }

    public boolean isOpeningLeftPage() {
        DragAppsLibraryLayout dragAppsLibraryLayout = this.mDragAppsLibraryLayout;
        return dragAppsLibraryLayout != null && dragAppsLibraryLayout.isLeftPageOpening(this.mDeviceProfile.widthPx);
    }

    @Override
    public void onSearchViewAlphaChanged(float f) {
        Workspace workspace = this.mWorkspace;
        if (workspace != null) {
            workspace.setTranslationY(f * mDeviceProfile.getCurrentHeight() * 0.1f);
        }
    }

    @Override
    public void onSearchViewClosed() {
        this.mBlurBackgroundView.clear(false);
    }

    @Override
    public void onSearchViewOpened() {

    }

    public void showFolderBlurBackground(float amount) {
        BlurScreenLayout blurScreenLayout = this.mBlurBackgroundView;
        if (blurScreenLayout != null) {
            blurScreenLayout.changeBlur(amount);
        }
    }

    public boolean isOpeningSearchView() {
        SearchViewLayout searchViewLayout = this.mSearchViewLayout;
        return searchViewLayout != null && (searchViewLayout.isOpening() || this.mSearchViewLayout.isOpened());
    }

    public void openFloatingMenu(View view) {
        DragLayer.LayoutParams layoutParams = new DragLayer.LayoutParams(-1, -1);
        // TODO: 2023.11.26 KDH Add Condition to floatingMenu is not null

        if (mSearchViewLayout.isOpened()) return;
        this.showingFloatingMenu = true;

        if (view instanceof BubbleTextView) {

        } else if (view instanceof LauncherAppWidgetHostView) {

        } else {
            return;
        }

        view.setVisibility(View.INVISIBLE);
        // Vẫn thêm overlay trong suốt để chạm ra ngoài thì đóng menu, nhưng KHÔNG vẽ
        // nền mờ (blur) xung quanh khi menu hiện lên theo yêu cầu.
        getDragLayer().addView(mFloatingMenuBlurBg, layoutParams);
        // showFloatingBlurBackground(view);
    }

    private void showFloatingBlurBackground(View view) {
        BlurScreenLayout blurScreenLayout = this.mFloatingMenuBlurBg;
        if (blurScreenLayout != null) {
            try {
                blurScreenLayout.mHandler1.obtainMessage(2, view).sendToTarget();
            } catch (Throwable th) {
                Log.e(TAG, "showFloatingBlurBackground: " + th.getMessage());
                th.getMessage();
            }
        }
    }

    public void closeFloatingMenu() {
        Log.d(TAG, "closeFloatingMenu: ");
        if (!this.showingFloatingMenu) {
            return;
        }
        this.showingFloatingMenu = false;
        this.mOpenAppWidgetHostView = null;
        AbstractFloatingView.closeAllOpenViews(this, true);

        this.mFloatingMenuBlurBg.clear(true);
    }

    public LauncherRootView getLauncherView() {
        return mLauncherView;
    }

    public void hideAppsLibrary() {
        if (this.mAppsLibraryLayout != null) {
            ObjectAnimator objectAnimator = LauncherAnimUtils.ofPropertyValuesHolder(this.mAppsLibraryLayout, PropertyValuesHolder.ofFloat("alpha", 0.0f), PropertyValuesHolder.ofFloat("scaleX", 0.9f), PropertyValuesHolder.ofFloat("scaleY", 0.9f));
            objectAnimator.setInterpolator(Launcher.initInterpolator(0.0f, 0.0f, 0.58f, 1.0f));
            objectAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mAppsLibraryLayout.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            });
            objectAnimator.setDuration(268L);
            mAppsLibraryLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            objectAnimator.start();
        }
    }

    public void showAppsLibrary() {
        if (mAppsLibraryLayout != null) {
            ObjectAnimator animator = LauncherAnimUtils.ofPropertyValuesHolder(mAppsLibraryLayout, PropertyValuesHolder.ofFloat("alpha", 1.0f), PropertyValuesHolder.ofFloat("scaleX", 1.0f), PropertyValuesHolder.ofFloat("scaleY", 1.0f));
            animator.setDuration(268L);
            animator.setInterpolator(Launcher.initInterpolator(0.0f, 0.0f, 0.58f, 1.0f));
            animator.start();
        }
    }

    public boolean isShaking() {
        return mIsShaking;
    }

    public boolean isWidgetsViewVisible() {
        return mState == mOnResumeState || this.mOnResumeState == State.WIDGETS || this.mWidgetsView.getVisibility() == View.VISIBLE;
    }

    public void onClickAddWidgetButton(View view) {
        openWidgetView(true, true);
    }

    private View mEditMenuView;

    /**
     * Dropdown hiển thị khi bấm nút "Edit" (góc trái-trên) trong chế độ chỉnh sửa home screen.
     * Gồm 3 chức năng: thêm tiện ích, chỉnh sửa page, background.
     *
     * Menu được thêm trực tiếp vào DragLayer (cùng cửa sổ với launcher) thay vì dùng
     * PopupWindow. PopupWindow tạo một cửa sổ riêng, khi đóng lại đúng lúc launcher đang
     * dựng ảnh blur/drawing-cache để mở panel widget sẽ đụng vào bitmap đã bị recycle ->
     * crash "trying to use a recycled bitmap" và chỉ hiện lớp phủ mờ.
     */
    public void showEditMenu(final View anchor) {
        dismissEditMenu();

        final DragLayer dragLayer = getDragLayer();
        final View content = LayoutInflater.from(this).inflate(R.layout.edit_home_menu, dragLayer, false);

        // Lớp phủ trong suốt phủ kín màn hình: chạm ra ngoài menu thì đóng menu.
        final FrameLayout overlay = new FrameLayout(this);
        overlay.setLayoutParams(new DragLayer.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissEditMenu();
            }
        });

        FrameLayout.LayoutParams contentLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        Rect r = new Rect();
        dragLayer.getViewRectRelativeToSelf(anchor, r);
        contentLp.leftMargin = r.left;
        contentLp.topMargin = r.bottom + (int) (getResources().getDisplayMetrics().density * 6);
        overlay.addView(content, contentLp);

        content.findViewById(R.id.menu_add_widget).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissEditMenu();
                onClickAddWidgetButton(anchor);
            }
        });
        content.findViewById(R.id.menu_edit_page).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissEditMenu();
                onClickEditPage();
            }
        });
        content.findViewById(R.id.menu_background).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissEditMenu();
                onClickWallpaperPicker(anchor);
            }
        });

        dragLayer.addView(overlay);
        mEditMenuView = overlay;
    }

    public boolean dismissEditMenu() {
        if (mEditMenuView != null) {
            getDragLayer().removeView(mEditMenuView);
            mEditMenuView = null;
            return true;
        }
        return false;
    }

    private View mEditPagesOverlay;

    /**
     * Mở màn "Edit Pages" (kiểu iOS): thumbnail tất cả page, kéo-thả đổi thứ tự, tích ẩn/hiện.
     */
    public void onClickEditPage() {
        showEditPages();
    }

    public void showEditPages() {
        if (mEditPagesOverlay != null) {
            return;
        }
        // Tắt rung icon để xem thumbnail rõ.
        cancelShakingAnimation();

        com.amz.ios.launcher.editpage.EditPagesOverlay overlay =
                new com.amz.ios.launcher.editpage.EditPagesOverlay(this);
        overlay.setLayoutParams(new DragLayer.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        getDragLayer().addView(overlay);
        mEditPagesOverlay = overlay;
    }

    public void onEditPagesClosed() {
        mEditPagesOverlay = null;
    }

    /**
     * Áp thứ tự + trạng thái ẩn từ màn Edit Pages về Workspace và lưu bền vững.
     * Page ẩn giữ nguyên app (chỉ tháo khỏi view tree); trạng thái ẩn lưu ở SharedPreferences.
     */
    public void applyPageChanges(ArrayList<Long> order, java.util.HashSet<Long> hidden) {
        mEditPagesOverlay = null;
        if (mWorkspace == null || order == null) {
            return;
        }
        if (hidden == null) {
            hidden = new java.util.HashSet<>();
        }
        com.amz.ios.launcher.editpage.HiddenPagesPrefs.setHidden(this, hidden);
        mWorkspace.applyEditPages(order, hidden);
    }

    /** Khôi phục trạng thái ẩn page (tháo khỏi view tree) sau khi bind xong workspace. */
    private void restoreHiddenPages() {
        if (mWorkspace == null) {
            return;
        }
        java.util.Set<Long> hidden =
                com.amz.ios.launcher.editpage.HiddenPagesPrefs.getHidden(this);
        if (!hidden.isEmpty()) {
            mWorkspace.detachHiddenPages(hidden);
        }
    }

    public void openWidgetView(boolean z, boolean z2) {
        if (z2) {
            this.mWidgetsView.scrollToTop();
        }
        if (mState == State.NONE || mState == State.APPS || mState == State.WORKSPACE) {
            getWorkspace().setState(Workspace.State.SHOW_WIDGETS);
            if (this.mWidgetsView.mWidgetListAdapter.getItemCount() <= 0) {
                //todo notify wallpaperchange
                notifyWidgetProvidersChanged();
            }
            this.mWidgetsView.postOnAnimation(mOpenWidgetViewRunnable);
            this.mState = State.WIDGETS;
            this.mUserPresent = false;
            updateAutoAdvanceState();
            closeFolder();
            getWindow().getDecorView().setAccessibilityDelegate(
                    LauncherAppState.getInstance().getAccessibilityDelegate()
            );
            getWindow().getDecorView().sendAccessibilityEvent(32);
        }
    }

    @Override
    public void notifyWidgetProvidersChanged() {
        mWidgetsView = findViewById(R.id.widgets_view);
        if (mWidgetsView != null && mWidgetsView.mWidgetListAdapter != null) {
            mModel.updateWidgetsModel(mWidgetsView.mWidgetListAdapter.getItemCount() == 0);
            WidgetsModel widgetsModel = mModel.mBgWidgetsModel;
            if (widgetsModel == null) return;
            widgetsModel.setFilterNull();
            mWidgetsView.setWidgetModel(widgetsModel);
            mWidgetsView.mWidgetListAdapter.setWidgetModel(widgetsModel);
            mWidgetsView.mWidgetListAdapter.notifyDataSetChanged();
        }
    }

    public void closeWidgetView(final boolean z) {
        WidgetsContainerView widgetsContainerView;
        if (isWidgetsViewVisible() && (widgetsContainerView = this.mWidgetsView) != null) {
            widgetsContainerView.postOnAnimation(() -> {
                        mWidgetsView.clearFocus();
                        mWidgetsView.collapseAppStyle();
                        mWidgetsView.setVisibility(View.GONE);
                        mWidgetsView.setPanelStateInternal(SlidingUpPanelLayout.PanelState.COLLAPSED);
                        mWidgetsView.close();
                        if (z) {
                            onShakingAllApps();
                        }
                    }
            );
        }
        mState = State.WORKSPACE;
        getWorkspace().setState(Workspace.State.NORMAL);
    }

    public void closeWidgetViewWithAnimation() {
        this.mWidgetsView.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }

    public void setOrientation() {
        if (this.mRotationEnabled) {
            unlockScreenOrientation(true);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }
    }

    public void unlockScreenOrientation(boolean z) {
        if (this.mRotationEnabled) {
            if (z) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            } else {
                this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    }
                }, 500L);
            }
        }
    }

    @TargetApi(18)
    public void lockScreenOrientation() {
        if (this.mRotationEnabled) {
            setRequestedOrientation(
                    Utilities.ATLEAST_JB_MR2 ?
                            ActivityInfo.SCREEN_ORIENTATION_LOCKED :
                            mapConfigurationOriActivityInfoOri(getResources().getConfiguration().orientation)
            );
        }
    }

    private int mapConfigurationOriActivityInfoOri(int i) {
        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        int rotation = defaultDisplay.getRotation();
        boolean flag = true;
        if (rotation == 1 || rotation == 3) {
            if (i == 2) {
                i = 1;
                flag = false;
            }
        }
        if (flag) i = 2;
        int add = 1;
        int[] values = {1, 0, 9, 8};
        if (i != 2)
            add = 0;
        return values[(rotation + add) % 4];
    }

}

interface DebugIntents {
    String DELETE_DATABASE = "com.amz.ios.launcher.action.DELETE_DATABASE";
    String MIGRATE_DATABASE = "com.amz.ios.launcher.action.MIGRATE_DATABASE";
}