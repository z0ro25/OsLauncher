package com.amz.ios.launcher.leftpage.widgets;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.launcher.IconCache;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.AppInfo;
import com.amz.ios.launcher.awareness.AppUsagesModel;
import com.amz.ios.launcher.compat.LauncherActivityInfoCompat;
import com.amz.ios.launcher.compat.LauncherAppsCompat;
import com.amz.ios.launcher.compat.UserHandleCompat;
import com.amz.ios.launcher.leftpage.model.AppSuggestionInfo;
import com.amz.ios.launcher.leftpage.adapter.AppSuggestionAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.Callable;

public class AppSuggestionWidget extends BlurConstraintLayoutWidget {

    RecyclerView mAppSuggestionRV;
    Launcher mLauncher;
    Handler mHandler;
    int mMargin;
    AppSuggestionAdapter mAppSuggestionAdapter;
    Runnable mAppSuggestionReloadRunnable;

    public AppSuggestionWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppSuggestionWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (context instanceof Launcher) {
            mLauncher = (Launcher) context;
            mMargin = mLauncher.getDeviceProfile().edgeMarginPx;
        }

        this.mHandler = new Handler();
        this.mAppSuggestionReloadRunnable = new Runnable() {
            @Override
            public void run() {
                reloadSuggestionApps(AppSuggestionWidget.this);
            }
        };

        LayoutInflater.from(context).inflate(R.layout.app_suggestions_widget,this,true);
    }

    public void getAppSuggestions(){
        try {
            mAppSuggestionAdapter.mAppSuggestionInfoArrayList = new AppSuggestionCallable().call();
            mAppSuggestionAdapter.notifyDataSetChanged();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void reloadSuggestionApps(AppSuggestionWidget widget){
        if (widget != null)
            widget.getAppSuggestions();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mAppSuggestionRV = findViewById(R.id.app_suggestions_widget_content);
        mAppSuggestionRV.setItemAnimator(
                new DefaultItemAnimator()
        );
        mAppSuggestionAdapter = new AppSuggestionAdapter(mLauncher);
        mAppSuggestionRV.setAdapter(mAppSuggestionAdapter);
        mAppSuggestionRV.setLayoutManager(new GridLayoutManager(mLauncher, 4));
        ((ConstraintLayout.LayoutParams) mAppSuggestionRV.getLayoutParams()).setMargins(
                mMargin,
                mMargin,
                mMargin,
                mMargin
        );

        mAppSuggestionRV.addItemDecoration(new AppSuggestionDecoration(mMargin));
        this.mHandler.removeCallbacksAndMessages(null);
        this.mHandler.postDelayed(this.mAppSuggestionReloadRunnable, 1000L);
        setTextAndBackgroundColor(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    /** Số ô của khung app gợi ý (lưới 4 cột x 2 hàng). */
    private static final int SUGGESTION_COUNT = 8;

    /**
     * Nguồn dữ liệu cho khung app ở trang trái: app MỞ GẦN NHẤT trước.
     *
     * [THAY ĐỔI] Trước đây hàm này query thẳng bảng "icons" của app_icons.db với
     *   {@code ORDER BY history DESC}. Nhưng app_icons.db là DB CACHE ICON của IconCache, và cột
     *   history chỉ được ghi một lần lúc cache icon (IconCache.newContentValues) — KHÔNG có chỗ nào
     *   cập nhật khi người dùng mở app. Nên danh sách thực chất là "8 app có icon cache gần nhất"
     *   (gần với app mới cài) và gần như đứng yên, không phải app dùng gần đây.
     *
     *   Nay lấy từ AppUsagesModel — thứ vốn đã ghi lại MỌI lần mở app
     *   (Launcher.onClickAppShortcut -> mAppUsagesModel.onLaunch) và sắp sẵn theo thời điểm mở gần
     *   nhất. Không phải dựng thêm hạ tầng theo dõi nào.
     *
     *   Vẫn giữ truy vấn cũ làm phần BÙ: máy mới cài chưa mở app nào thì danh sách recent rỗng,
     *   bù cho đủ 8 ô để khung không trống trơn.
     */
    public class AppSuggestionCallable implements Callable<ArrayList<AppSuggestionInfo>> {
        @Override
        public ArrayList<AppSuggestionInfo> call() throws Exception {
            ArrayList<AppSuggestionInfo> arrayList = new ArrayList<>();
            // Nhớ component đã thêm để phần bù bên dưới không lặp lại app đã có.
            HashSet<String> added = new HashSet<>();

            IconCache iconCache = LauncherAppState.getInstance().getIconCache();
            LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(mLauncher);
            UserHandleCompat user = UserHandleCompat.myUserHandle();

            for (ComponentName cn : AppUsagesModel.getRecentComponents()) {
                if (arrayList.size() >= SUGGESTION_COUNT) break;
                if (cn == null) continue;
                try {
                    // null = app đã gỡ cài hoặc không còn activity khởi chạy -> bỏ qua, tránh để
                    // lại ô trống hoặc ô bấm vào không mở được gì.
                    LauncherActivityInfoCompat activityInfo =
                            launcherApps.resolveActivity(Intent.makeMainActivity(cn), user);
                    if (activityInfo == null) continue;

                    // Lấy icon + tên qua IconCache thay vì tự đọc DB: constructor AppInfo gọi
                    // iconCache.getTitleAndIcon(), và cacheLocked() bên trong tự nạp từ DB hoặc
                    // PackageManager nếu chưa có -> không bao giờ thiếu icon.
                    AppInfo appInfo = new AppInfo(mLauncher, activityInfo, user, iconCache);
                    if (appInfo.iconBitmap == null) continue;

                    String flat = cn.flattenToString();
                    arrayList.add(new AppSuggestionInfo(
                            arrayList.size(),
                            appInfo.title != null ? appInfo.title.toString() : "",
                            flat,
                            appInfo.iconBitmap));
                    added.add(flat);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (arrayList.size() < SUGGESTION_COUNT) {
                fillFromIconCacheDb(arrayList, added);
            }
            return arrayList;
        }
    }

    /**
     * Bù cho đủ {@link #SUGGESTION_COUNT} ô bằng danh sách cũ (app_icons.db, sắp theo history).
     * Chỉ chạy khi dữ liệu "mở gần nhất" chưa đủ — máy mới cài, hoặc vừa xoá dữ liệu launcher.
     */
    private void fillFromIconCacheDb(ArrayList<AppSuggestionInfo> out, HashSet<String> added) {
        Cursor query = null;
        try {
            String path = mLauncher.getDatabasePath("app_icons.db").getPath();
            if (path.isEmpty()) return;
            query = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
                    .query(true, "icons", null, null, null, null, null, "history DESC",
                            String.valueOf(SUGGESTION_COUNT));
            if (query == null) return;
            while (query.moveToNext()) {
                if (out.size() >= SUGGESTION_COUNT) break;
                try {
                    String label = query.getString(query.getColumnIndexOrThrow("label"));
                    String componentName = query.getString(
                            query.getColumnIndexOrThrow("componentName"));
                    if (componentName == null || added.contains(componentName)) continue;
                    @SuppressLint("Range") byte[] blob = query.getBlob(
                            query.getColumnIndex("icon"));
                    if (blob == null) continue;
                    out.add(new AppSuggestionInfo(out.size(), label, componentName,
                            BitmapFactory.decodeByteArray(blob, 0, blob.length)));
                    added.add(componentName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (query != null) query.close();
        }
    }

    @Override
    public void r() {
        super.r();
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) mAppSuggestionRV.getLayoutParams();
        layoutParams.setMargins(
                mMargin,
                mMargin,
                mMargin,
                mMargin
        );
        mAppSuggestionRV.setLayoutParams(layoutParams);
    }

    @Override
    public void setAppSuggestionViewMargin() {
        super.setAppSuggestionViewMargin();
        ((ConstraintLayout.LayoutParams) mAppSuggestionRV.getLayoutParams()).setMargins(
                mMargin,
                mMargin,
                mMargin,
                mMargin
        );
        this.mHandler.removeCallbacksAndMessages(null);
        this.mHandler.postDelayed(this.mAppSuggestionReloadRunnable, 1000L);
    }

    public static class AppSuggestionDecoration extends RecyclerView.ItemDecoration {
        final int margin;

        public AppSuggestionDecoration(int m)    {
            this.margin = m / 2;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            parent.setPadding(margin, margin, margin, margin);
            parent.setClipToPadding(false);
            outRect.top = margin;
            outRect.bottom = margin;
            outRect.left = margin;
            outRect.right = margin;
        }
    }

}
