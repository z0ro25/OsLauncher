package com.amz.ios.launcher.theme;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.palette.graphics.Palette;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.config.ThemeConfig;
import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.ioslite.common.util.PackageUtil;
import com.amz.ios.launcher.FastBitmapDrawable;
import com.amz.ios.launcher.IOSShortcut;
import com.amz.ios.launcher.IconCache;
import com.amz.ios.launcher.InvariantDeviceProfile;
import com.amz.ios.launcher.LauncherModel;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.config.Settings;
import com.amz.ios.launcher.leftpage.drawables.CalendarDrawable;
import com.amz.ios.launcher.provider.AppTypeProvider;
import com.amz.ios.launcher.util.Thunk;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ThemeManager {
    private static final String TAG = "Launcher.ThemeManager";
    private static final boolean DEBUG = false;


    public static final int BASELINE_ICON_SIZE_DP = 60;

    private Context mContext;
    private IconCache mIconCache;
    private final int mIconDpi;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mIconBitmapSizePx;

    private static HashMap<String, Rect> mThemeMaskRectMap = new HashMap<>();
    private static final int BACKGROUND_SIZE = 6;

    @Thunk
    final Handler mWorkerHandler;


    private Context mLocalContext;     //Local context
    private Resources mLocalResource;
    private String mLocalPkgName;
    private Context mThemePkgContext;        //For theme apk
    private String mThemePkgName;        //For theme apk
    private Resources mThemePkgResource;        //For theme apk
    private ThemeFactory mThemeFactory;

    private class ThemeFactory {
        private Bitmap[] backgroundList = new Bitmap[6];
        private Bitmap mask, pattern, border;

        public ThemeFactory() {
            init();
        }


        private void init() {
            Drawable drawable;
            Bitmap origin;
            drawable = readThemeResDrawable("ic_app_pack_mask");
            if (drawable != null && drawable instanceof BitmapDrawable) {
                origin = ((BitmapDrawable) drawable).getBitmap();
                mask = Bitmap.createScaledBitmap(origin, mIconBitmapSizePx, mIconBitmapSizePx, true);
                mask.setDensity(mIconDpi);
            }

            drawable = readThemeResDrawable("ic_app_pack_pattern");
            if (drawable != null && drawable instanceof BitmapDrawable) {
                origin = ((BitmapDrawable) drawable).getBitmap();
                pattern = Bitmap.createScaledBitmap(origin, mIconBitmapSizePx, mIconBitmapSizePx, true);
                pattern.setDensity(mIconDpi);
            }

            drawable = readThemeResDrawable("ic_app_pack_border");
            if (drawable != null && drawable instanceof BitmapDrawable) {
                origin = ((BitmapDrawable) drawable).getBitmap();
                border = Bitmap.createScaledBitmap(origin, mIconBitmapSizePx, mIconBitmapSizePx, true);
                border.setDensity(mIconDpi);
            }
        }


        public Bitmap createIconWidthThemeBg(Drawable source) {
            if (source == null) {// || (!(source instanceof BitmapDrawable))) {
                return null;
            }

            if (mask == null) {
                return null;
            }

            Bitmap background;
            final int index = new Random().nextInt(BACKGROUND_SIZE);
            if (backgroundList[index] == null) {
                Drawable drawable = readThemeResDrawable("ic_app_background_0" + (index + 1));
                if (drawable != null && drawable instanceof BitmapDrawable) {
                    Bitmap origin = ((BitmapDrawable) drawable).getBitmap();
                    origin.setDensity(mIconDpi);
                    backgroundList[index] = Bitmap.createScaledBitmap(origin, mIconBitmapSizePx, mIconBitmapSizePx, true);
                }
            }
            background = backgroundList[index];
            if (pattern == null && background == null) {
                if (BuildUtil.isPublicBuild()) {
                    return null;
                } else {
                    Bitmap sourceIconBitmap = Bitmap.createBitmap(mIconBitmapSizePx, mIconBitmapSizePx, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(sourceIconBitmap);
                    source.setBounds(0, 0, mIconBitmapSizePx, mIconBitmapSizePx);
                    source.draw(canvas);
                    return makePatternBg(mask, sourceIconBitmap);
                }
            }

            Bitmap maskedAppIcon;
            maskedAppIcon = maskBitmap(source);
            //return packAppIcon(pattern, background, maskedAppIcon, border);
            return maskedAppIcon;
        }

        private Bitmap maskBitmap(Drawable source) {
            Bitmap bitmap = Bitmap.createBitmap(mIconBitmapSizePx, mIconBitmapSizePx, Bitmap.Config.ARGB_8888);
            RectF rectF = new RectF(
                    0, 0, mIconBitmapSizePx, mIconBitmapSizePx
            );
            Path path = new Path();
            path.addRoundRect(rectF, 5, 5, Path.Direction.CCW);

            Canvas canvas = new Canvas(bitmap);
            canvas.clipPath(path);

            source.setBounds(
                    0, 0, mIconBitmapSizePx, mIconBitmapSizePx
            );

            canvas.drawColor(Color.WHITE);

            source.draw(canvas);
            canvas.setBitmap(null);
            return bitmap;
        }

        private Bitmap makePatternBg(Bitmap mask, Bitmap appIcon) {
            final float destIconSize = mask.getWidth();
            final int srcIconWidth = appIcon.getWidth();
            final int srcIconHeight = appIcon.getHeight();
            if (srcIconWidth > destIconSize || srcIconHeight > destIconSize) {
                Matrix matrix = new Matrix();
                final float scale = Math.min(destIconSize / srcIconWidth, destIconSize / srcIconHeight);
                matrix.setScale(scale, scale);
                appIcon = Bitmap.createBitmap(appIcon, 0, 0, srcIconWidth, srcIconHeight, matrix, true);
            }

            Bitmap result = Bitmap.createBitmap(mask.getWidth(), mask.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            canvas.drawColor(colorFromBitmap(appIcon));
            canvas.drawBitmap(appIcon, (mask.getWidth() - appIcon.getWidth()) / 2,
                    (mask.getHeight() - appIcon.getHeight()) / 2, null);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            canvas.drawBitmap(mask, 0, 0, paint);
            paint.setXfermode(null);
            return result;
        }


        private boolean usePatternBg() {
            return true;
        }

        private int colorFromBitmap(Bitmap bitmap) {
            final int NUMBER_OF_PALETTE_COLORS = 24;
            final Palette palette = Palette.generate(bitmap, NUMBER_OF_PALETTE_COLORS);
            if (palette != null && palette.getVibrantSwatch() != null) {
                return palette.getVibrantSwatch().getRgb();
            }
            return -1;
        }
    }


    public ThemeManager(Context context, InvariantDeviceProfile inv, IconCache iconCache) {
        mContext = context;
        mIconDpi = inv.fillResIconDpi;
        mScreenWidth = inv.screenWidthPx;
        mScreenHeight = inv.screenHeightPx;
        mIconBitmapSizePx = inv.iconBitmapSize;
        mWorkerHandler = new Handler(LauncherModel.getWorkerLooper());

        mLocalContext = context;
        mLocalResource = mLocalContext.getResources();
        mLocalPkgName = mLocalContext.getPackageName();

        mIconCache = iconCache;
        init();
    }

    private void init() {
        mIconCache.setThemeManager(this);
        String prePkgName = Settings.getThemePackageName(mContext);
        if (TextUtils.isEmpty(prePkgName) || !PackageUtil.isAppInstalled(mContext, prePkgName)) {
            prePkgName = ThemeConfig.getDefaultThemePkg();
        }
        applyThemePkg(prePkgName, Settings.shouldResetDefaultWallpaper(mContext));
    }

    private void setThemeContext(Context context) {
        mThemePkgContext = context;
        mThemePkgName = mThemePkgContext.getPackageName();
        mThemePkgResource = mThemePkgContext.getResources();
        mThemeFactory = new ThemeFactory();
    }

    public boolean applyThemePkg(String packageName, boolean changeWallpaper) {
        if (packageName.equals(mThemePkgName)) {
            return false;
        }

        if (packageName.equals(mLocalPkgName)) {
            setThemeContext(mLocalContext);
        } else {
            try {
                Context context = mLocalContext.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
                setThemeContext(context);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "applyThemePkg fail, not found package : " + packageName);
                return false;
            }
        }

        Settings.saveThemePackageName(mContext, packageName);
        if (changeWallpaper) {
            Settings.setPreferResetDefaultWallpaper(mContext, false);
            boolean resetSystemWallpaper = packageName.equals(ThemeConfig.getDefaultThemePkg()) && Partner.getBoolean(mContext, Partner.DEF_USE_SYSTEM_DEFAULT_WALLPAPER);
            applyThemeWallpaper(resetSystemWallpaper);
        }

        return true;
    }

    private void applyThemeWallpaper(boolean resetSystemWallpaper) {
        Runnable setWallpaperTask = null;
        if (resetSystemWallpaper) {
            setWallpaperTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
                        if (BuildUtil.ATLEAST_NOUGAT) {
                            wallpaperManager.clear(WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM);
                        } else {
                            wallpaperManager.clear();
                        }
                    } catch (Exception e) {
                        Log.i(TAG, "resetSystemWallpaper fail", e);
                    }
                }
            };

        } else {
            setWallpaperTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        int identifier = mThemePkgResource.getIdentifier("default_wallpaper", "drawable", mThemePkgContext.getPackageName());
                        Bitmap bitmap = BitmapFactory.decodeResource(mThemePkgResource, identifier);
                        WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
                        if (bitmap != null) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                            InputStream inputimage = new ByteArrayInputStream(baos.toByteArray());
                            final BufferedInputStream inputImage = new BufferedInputStream(inputimage);
                            wallpaperManager.suggestDesiredDimensions(mScreenWidth, mScreenHeight);
                            if (BuildUtil.ATLEAST_NOUGAT) {
                                wallpaperManager.setStream(inputImage,
                                        null,
                                        true,
                                        WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM);
                            } else {
                                wallpaperManager.setStream(inputImage);
                            }
                            //wallpaperManager.setBitmap(Bitmap.createBitmap(bitmap, 0, 0, mScreenWidth, mScreenHeight));
                            baos.close();
                            inputImage.close();
                            Log.e(TAG, "Did set theme wallpaper ok");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "set theme wallpaper fail", e);
                    }
                }

            };
        }

        if (setWallpaperTask != null) {
            mWorkerHandler.post(setWallpaperTask);
        }
    }

    public Drawable readFolderBgDrawable() {
        return readThemeResDrawable("ic_app_launcher_folder");
    }

    public String readFolderPreviewValue() {
        Pattern mPattern = Pattern.compile("folder_preview_values\">(\\S+)<");
        AssetManager manager = mThemePkgContext.getAssets();
        InputStream is = null;
        try {
            is = manager.open("theme_values.xml");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            Matcher matcher = mPattern.matcher(new String(buffer, "utf-8"));
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (IOException e) {
            //Log.i(TAG, "IOException:" + e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }

    public boolean isDefaultTheme() {
        if (mThemePkgName == null)
            return false;
        return mThemePkgName.equals(mLocalPkgName);
    }

    /**
     * Loads the customized icon resource based on the following priority scheme:
     * 1) Parse the comp app  type;  if it has a app type , read if rom res/drawable;
     * 2) Read asset icon resource with packagename or classname as resource name;
     */
    public Drawable readCustomizedIconForAppComp(ComponentName cn) {
        Drawable iconDrawable = null;
        AppTypeProvider appTypeProvider = AppTypeProvider.getAppTypeProvider();
        String iconResName = appTypeProvider.getIconResNameForComp(cn);

        if (!TextUtils.isEmpty(iconResName)) {
            /* jmc remove old clock
            if (cn.equals(appTypeProvider.getClockComp())) {
                Drawable clockDrawable = generateClockIcon();
                if (clockDrawable != null) {
                    return clockDrawable;
                }
            }

            /* xiaopepng remove Caleandar
            if (cn.equals(appTypeProvider.getCalendarComp())) {
                Drawable calendarDrawable = generateCalendarIcon();
                if (calendarDrawable != null) {
                    return calendarDrawable;
                }
            }
            */

            iconDrawable = readDrawableByResName(mThemePkgResource, iconResName, mThemePkgName);

//            // jmc For click component, we need to generate a live icon baed on current data;
//            if (iconDrawable != null && iconDrawable instanceof BitmapDrawable && cn.equals(appTypeProvider.getClockComp())) {
//                Bitmap bitmap = ((BitmapDrawable) iconDrawable).getBitmap();
//                bitmap.setDensity(mIconDpi);
//                return new ClockDrawable(mThemePkgContext,bitmap.getWidth(),bitmap.getHeight(), -1);//generateClockIconForTheme(bitmap);
//            }


            // For calendar component, we need to generate a live icon baed on current data;
            if (iconDrawable != null && iconDrawable instanceof BitmapDrawable && cn.equals(appTypeProvider.getCalendarComp())) {
                Bitmap bitmap = ((BitmapDrawable) iconDrawable).getBitmap();
                bitmap.setDensity(mIconDpi);
                return new CalendarDrawable(mThemePkgContext, bitmap.getWidth(), bitmap.getHeight());// generateCalendarIconForTheme(bitmap);
//                return generateCalendarIcon(((BitmapDrawable) iconDrawable).getBitmap());
            }

        }

        if (iconDrawable == null && mThemePkgContext != null) {
            iconDrawable = readDrawableFromAssetForComp(cn, mThemePkgContext.getAssets(), mThemePkgResource);
        }

        Bitmap bitmap = mIconCache.drawableToBitmap(iconDrawable);

        if (bitmap == null) return null;

        Bitmap newBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(newBitmap);
        RectF rectF = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        float radius = mLocalContext.getResources().getDimension(R.dimen.icon_round_corner);
        Path path = new Path();
        path.addRoundRect(rectF, radius, radius, Path.Direction.CW);
        canvas.clipPath(path);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return new FastBitmapDrawable(newBitmap);
    }


    public Drawable readCustomizedIconForShortcutComp(ComponentName cn) {
        Bitmap bitmap = null;

        String resName = IOSShortcut.getIconResName(cn);

        if (!TextUtils.isEmpty(resName)) {
            bitmap = readThemeAssetBitmap(resName);
        }

        if (bitmap != null) {
            return new BitmapDrawable(mLocalResource, bitmap);
        }
        return null;
    }


    public Drawable applyWithThemeBg(Drawable source) {
        Bitmap newIcon = null;

        /*
        if (mThemeFactory != null) {
            newIcon = mThemeFactory.createIconWidthThemeBg(source);
        }

        if (newIcon != null) {
            Drawable icon = new BitmapDrawable(mLocalContext.getResources(), newIcon);
            return icon;
        }

         */
        return source;
    }


    private Bitmap packAppIcon(Bitmap pattern, Bitmap background,
                               Bitmap maskedAppIcon, Bitmap border) {
        Bitmap result = Bitmap.createBitmap(maskedAppIcon.getWidth(),
                maskedAppIcon.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG,
                Paint.FILTER_BITMAP_FLAG));
        if (pattern != null) {
            canvas.drawBitmap(pattern, 0, 0, null);
        }

        if (background != null) {
            canvas.drawBitmap(background, 0, 0, null);
        }

        if (maskedAppIcon != null) {
            canvas.drawBitmap(maskedAppIcon, 0, 0, null);
        }

        if (border != null) {
            canvas.drawBitmap(border, 0, 0, null);
        }
        return result;
    }

    private Bitmap makeMaskedIcon(Bitmap mask, Drawable source) {

        final int maskIconWidth = mask.getWidth();
        final int maskIconHeight = mask.getHeight();

//        final Rect sourceRect = new Rect(0, 0, appIcon.getWidth(), appIcon.getHeight());
        Rect desRect = getMaskRectForPackage(mThemePkgContext.getPackageName(), mask);
        int w = desRect.width();
        int h = desRect.height();
        int sizePx = w > h ? h : w;

        desRect = new Rect(
                0, 0, maskIconWidth, maskIconHeight
        );

        Bitmap sourceIconBitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas sCanvas = new Canvas(sourceIconBitmap);
        source.setBounds(0, 0, sizePx, sizePx);
        source.draw(sCanvas);

        Bitmap bitmap = Bitmap.createBitmap(maskIconWidth, maskIconHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

//        canvas.drawBitmap(appIcon, sourceRect,
//                desRect, null);

        canvas.drawBitmap(sourceIconBitmap, desRect.left, desRect.top, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(mask, 0, 0, paint);

        paint.setXfermode(null);
        canvas.setBitmap(null);
        return bitmap;
    }


    private Rect getMaskRectForPackage(String packgeName, Bitmap mask) {
        if (mThemeMaskRectMap.get(packgeName) != null) {
            return mThemeMaskRectMap.get(packgeName);
        }

        Rect rect = extractBoundOfBitmap(mask);
        mThemeMaskRectMap.put(packgeName, rect);
        return rect;
    }


    private Drawable readThemeResDrawable(String resName) {
        return readDrawableByResName(mThemePkgResource, resName, mThemePkgName);
    }

    private Bitmap readThemeAssetBitmap(String resName) {
        return readBitmapFromAsset(resName, mThemePkgContext.getAssets());
    }

    private Drawable readLocalResDrawable(String resName) {
        return readDrawableByResName(mLocalResource, resName, mLocalPkgName);
    }

    private Bitmap readLocalAssetBitmap(String resName) {
        return readBitmapFromAsset(resName, mLocalContext.getAssets());
    }

    private Drawable readDrawableByResName(Resources res, String resName, String packageName) {
        if (res == null)
            return null;

        int identifier = res.getIdentifier(resName, "drawable", packageName);
        if (identifier > 0) {
            return res.getDrawableForDensity(identifier, mIconDpi);
        }
        return null;
    }

    private Drawable readDrawableFromAssetForComp(ComponentName cn, AssetManager assetManager, Resources res) {
        Bitmap bitmap = readBitmapFromAsset(cn.getPackageName(), assetManager);
        if (bitmap == null) {
            bitmap = readBitmapFromAsset(cn.getClassName(), assetManager);
        }

        if (bitmap == null) {
            return null;
        }
        return new BitmapDrawable(res, bitmap);
    }

    private Bitmap readBitmapFromAsset(String assetName, AssetManager assetManager) {
        Bitmap source = null;
        InputStream is = null;

        if (mIconDpi <= DisplayMetrics.DENSITY_XHIGH) {
            try {
                is = assetManager.open("icon-xhdpi/" + assetName + ".png");
                source = BitmapFactory.decodeStream(is);
                return source;
            } catch (IOException e) {
                if (DEBUG) Log.d(TAG, "readAppIconFromAsset fail : " + assetName);
                source = null;
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        if (source == null) {
            try {
                is = assetManager.open("icon-xxhdpi/" + assetName + ".png");
                source = BitmapFactory.decodeStream(is);
                return source;
            } catch (IOException e) {
                if (DEBUG) Log.d(TAG, "readAppIconFromAsset fail : " + assetName);
                source = null;
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        return source;
    }

    private static String[] DEFAULT_CALENDAR_ICONS_OF_DAYS = new String[]{
            "ic_launcher_icon01",
            "ic_launcher_icon02",
            "ic_launcher_icon03",
            "ic_launcher_icon04",
            "ic_launcher_icon05",
            "ic_launcher_icon06",
            "ic_launcher_icon07",
            "ic_launcher_icon08",
            "ic_launcher_icon09",
            "ic_launcher_icon10",
            "ic_launcher_icon11",
            "ic_launcher_icon12",
            "ic_launcher_icon13",
            "ic_launcher_icon14",
            "ic_launcher_icon15",
            "ic_launcher_icon16",
            "ic_launcher_icon17",
            "ic_launcher_icon18",
            "ic_launcher_icon19",
            "ic_launcher_icon20",
            "ic_launcher_icon21",
            "ic_launcher_icon22",
            "ic_launcher_icon23",
            "ic_launcher_icon24",
            "ic_launcher_icon25",
            "ic_launcher_icon26",
            "ic_launcher_icon27",
            "ic_launcher_icon28",
            "ic_launcher_icon29",
            "ic_launcher_icon30",
            "ic_launcher_icon31"};

    private static String[] DEFAULT_DAY_ICON_OF_WEEK = new String[]{
            "ic_day_01",
            "ic_day_02",
            "ic_day_03",
            "ic_day_04",
            "ic_day_05",
            "ic_day_06",
            "ic_day_07",
    };

    private static String[] DEFAULT_DAYS_OF_WEEK = new String[]{
            "일요일",
            "월요일",
            "화요일",
            "수요일",
            "목요일",
            "금요일",
            "토요일",
    };


    private Drawable generateCalendarIconForTheme(Bitmap background) {
        final Canvas canvas = new Canvas();
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        final int width = background.getWidth();
        final int height = background.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        canvas.setBitmap(bitmap);
        canvas.drawBitmap(background, 0, 0, null);

        final Resources res = mLocalContext.getResources();
        int activeColor = res.getColor(R.color.live_calendar_icon_txt_color, null);
        if (dayOfWeek == 1) {
            activeColor = res.getColor(R.color.live_calendar_icon_txt_color_red, null);
        }

        int indexOfDaysIcon = dayOfMonth - 1;
        Drawable dateIconDrawable = readThemeResDrawable(DEFAULT_CALENDAR_ICONS_OF_DAYS[indexOfDaysIcon]);
        if (dateIconDrawable != null && dateIconDrawable instanceof BitmapDrawable) {
//            dateIconDrawable.setColorFilter(activeColor, PorterDuff.Mode.SRC_IN);
            Bitmap dayBitmap = ((BitmapDrawable) dateIconDrawable).getBitmap();
            Rect source = new Rect(0, 0, dayBitmap.getWidth(), dayBitmap.getHeight());
            Rect target = new Rect(0, 0, width, height);
            canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            canvas.drawBitmap(dayBitmap, source, target, null);
        } else {
            // if dateIconDrawable is null , draw text on background;
            String day = String.valueOf(dayOfMonth);
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
            Rect textRect = new Rect();

            textPaint.setColor(activeColor);
            textPaint.setTypeface(Typeface.DEFAULT);
            textPaint.setAntiAlias(true);

            float textFullHeight = height * 0.4f;
            int textSizePx = (int) (textFullHeight / 2);

            textFullHeight = height * 0.85f;
            textSizePx = (int) (textFullHeight / 2);
            textPaint.setTextSize(textSizePx);
            textPaint.getTextBounds(day, 0, day.length(), textRect);

            int txtWidth = textRect.width();
            int txtHeight = textRect.height();

            float x = (width - txtWidth) * 0.5f;
            float y = height - (textFullHeight - txtHeight) * 0.5f;
            canvas.drawText(day, x,
                    y, textPaint);
        }

        indexOfDaysIcon = dayOfWeek - 1;
        Drawable dayIconDrawable = readThemeResDrawable(DEFAULT_DAY_ICON_OF_WEEK[indexOfDaysIcon]);
        if (dayIconDrawable != null && dayIconDrawable instanceof BitmapDrawable) {
            Bitmap dayBitmap = ((BitmapDrawable) dayIconDrawable).getBitmap();
            Rect source = new Rect(0, 0, dayBitmap.getWidth(), dayBitmap.getHeight());
            Rect target = new Rect(0, 0, width, height);
            canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            canvas.drawBitmap(dayBitmap, source, target, null);
        } else {
            // if dayIconDrawable is null , draw text on background;
            String dayOfWeekName = DEFAULT_DAYS_OF_WEEK[indexOfDaysIcon];
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
            Rect textRect = new Rect();

            textPaint.setColor(activeColor);
            textPaint.setTypeface(Typeface.DEFAULT);
            textPaint.setAntiAlias(true);

            float textFullHeight = height * 0.4f;
            int textSizePx = (int) (textFullHeight / 2);
            textPaint.setTextSize(textSizePx);
            textPaint.getTextBounds(dayOfWeekName, 0, dayOfWeekName.length(), textRect);

            int txtWidth = textRect.width();
            int txtHeight = textRect.height();

            float x = (width - txtWidth) * 0.5f;
            float y = (textFullHeight - txtHeight) * 0.5f + txtHeight + height * 0.05f;
            canvas.drawText(dayOfWeekName, x,
                    y, textPaint);
        }
        canvas.setBitmap(null);
        return new BitmapDrawable(mLocalResource, bitmap);
    }

    private Drawable generateCalendarIcon(Bitmap background) {
        final Canvas canvas = new Canvas();
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        final int width = background.getWidth();
        final int height = background.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        canvas.setBitmap(bitmap);
        canvas.drawBitmap(background, 0, 0, null);

        int indexOfDaysIcon = dayOfMonth - 1;
        String path = "calendar/" + DEFAULT_CALENDAR_ICONS_OF_DAYS[indexOfDaysIcon] + ".png";
        Bitmap dayBitmap = getBitmapFromAsset(path, mLocalContext.getAssets());
        if (dayBitmap != null) {
            Rect source = new Rect(0, 0, dayBitmap.getWidth(), dayBitmap.getHeight());
            Rect target = new Rect(0, 0, width, height);
            canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            canvas.drawBitmap(dayBitmap, source, target, null);
        } else {
            // if dateIconDrawable is null , draw text on background;
            String day = String.valueOf(dayOfMonth);
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
            Rect textRect = new Rect();
            textPaint.setAntiAlias(true);

            final Resources res = mLocalContext.getResources();
            textPaint.setColor(res.getColor(R.color.live_calendar_icon_txt_color));
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);

            int textSizePx = height / 2;
            textPaint.setTextSize(textSizePx);
            textPaint.getTextBounds(day, 0, day.length(), textRect);

            int txtWidth = textRect.width();
            int txtHeight = textRect.height();

            final float x = (width - txtWidth) * 0.5f;
            final float y = (height - txtHeight) * 0.5f + txtHeight;
            canvas.drawText(day, x,
                    y, textPaint);
        }
        canvas.setBitmap(null);
        return new BitmapDrawable(mLocalResource, bitmap);
    }

    public static Bitmap getBitmapFromAsset(String name, AssetManager assetManager) {
        Bitmap bitmap = null;
        InputStream is = null;
        try {
            is = assetManager.open(name);
            bitmap = BitmapFactory.decodeStream(is);
            return bitmap;
        } catch (Exception e) {
            bitmap = null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
        }
        return bitmap;
    }

    private Drawable generateCalendarIcon() {
        Bitmap background = getBitmapFromAsset("calendar/bg.png", mLocalContext.getAssets());
        if (background == null) {
            return null;
        }
        final Canvas canvas = new Canvas();
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        final int width = background.getWidth();
        final int height = background.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        canvas.setBitmap(bitmap);
        canvas.drawBitmap(background, 0, 0, null);

        int indexOfDaysIcon = dayOfMonth - 1;
        String path = "calendar/" + DEFAULT_CALENDAR_ICONS_OF_DAYS[indexOfDaysIcon] + ".png";
        Bitmap dayBitmap = getBitmapFromAsset(path, mLocalContext.getAssets());
        if (dayBitmap != null) {
            Rect source = new Rect(0, 0, dayBitmap.getWidth(), dayBitmap.getHeight());
            Rect target = new Rect(0, 0, width, height);
            canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            canvas.drawBitmap(dayBitmap, source, target, null);
        } else {
            // if dateIconDrawable is null , draw text on background;
            String day = String.valueOf(dayOfMonth);
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
            Rect textRect = new Rect();
            textPaint.setAntiAlias(true);

            final Resources res = mLocalContext.getResources();
            textPaint.setColor(res.getColor(R.color.live_calendar_icon_txt_color));
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);

            int textSizePx = height / 2;
            textPaint.setTextSize(textSizePx);
            textPaint.getTextBounds(day, 0, day.length(), textRect);

            int txtWidth = textRect.width();
            int txtHeight = textRect.height();

            final float x = (width - txtWidth) * 0.5f;
            final float y = (height - txtHeight) * 0.5f + txtHeight;
            canvas.drawText(day, x,
                    y, textPaint);
        }
        canvas.setBitmap(null);
        return new BitmapDrawable(mLocalResource, bitmap);
    }

    private Drawable generateClockIcon() {
        Bitmap mClockBg = getBitmapFromAsset("clock/clock_bg.png", mLocalContext.getAssets());
        if (mClockBg == null) {
            return null;
        }
        Bitmap mClockHour = getBitmapFromAsset("clock/hct_clock_hour.png", mLocalContext.getAssets());
        if (mClockHour == null) {
            return null;
        }
        Bitmap mClockMinute = getBitmapFromAsset("clock/hct_clock_minute.png", mLocalContext.getAssets());
        if (mClockMinute == null) {
            return null;
        }
        Bitmap mClockCenter = getBitmapFromAsset("clock/hct_clock_center.png", mLocalContext.getAssets());
        if (mClockCenter == null) {
            return null;
        }
        Canvas canvas = new Canvas();
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Calendar instance = Calendar.getInstance();
        int hour = instance.get(Calendar.HOUR);
        int minute = instance.get(Calendar.MINUTE);
        float hourAngle = ((((float) hour) * 30.0f) + ((((float) minute) / 60.0f) * 30.0f)) + 180.0f;
        float minuteAngle = (((float) minute) * 6.0f) + 180.0f;
        Bitmap clockBitmap = Bitmap.createBitmap(mClockBg.getWidth(), mClockBg.getHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(clockBitmap);
        int centerX = mClockBg.getWidth() / 2;
        int centerY = mClockBg.getHeight() / 2;
        canvas.drawBitmap(mClockBg, 0.0f, 0.0f, paint);
        canvas.save();
        canvas.rotate(minuteAngle, centerX, centerY);
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        canvas.drawBitmap(mClockMinute, centerX - mClockMinute.getWidth() / 2.0f, centerY, paint);
        canvas.restore();
        canvas.save();
        canvas.rotate(hourAngle, centerX, centerY);
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        canvas.drawBitmap(mClockHour, centerX - mClockHour.getWidth() / 2.0f, centerY, paint);
        canvas.restore();
        canvas.save();
        canvas.drawBitmap(mClockCenter, centerX - mClockCenter.getWidth() / 2.0f, centerY - mClockCenter.getHeight() / 2.0f, paint);
        canvas.restore();
        return new BitmapDrawable(mLocalResource, clockBitmap);
    }

    private Drawable generateClockIcon(Bitmap mClockBg) {
        if (mClockBg == null) {
            return null;
        }
        Bitmap mClockHour = getBitmapFromAsset("clock/hct_clock_hour.png", mLocalContext.getAssets());
        if (mClockHour == null) {
            return null;
        }
        Bitmap mClockMinute = getBitmapFromAsset("clock/hct_clock_minute.png", mLocalContext.getAssets());
        if (mClockMinute == null) {
            return null;
        }
        Bitmap mClockCenter = getBitmapFromAsset("clock/hct_clock_center.png", mLocalContext.getAssets());
        if (mClockCenter == null) {
            return null;
        }
        Canvas canvas = new Canvas();
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Calendar instance = Calendar.getInstance();
        int hour = instance.get(Calendar.HOUR);
        int minute = instance.get(Calendar.MINUTE);
        float hourAngle = ((((float) hour) * 30.0f) + ((((float) minute) / 60.0f) * 30.0f)) + 180.0f;
        float minuteAngle = (((float) minute) * 6.0f) + 180.0f;
        Bitmap clockBitmap = Bitmap.createBitmap(mClockBg.getWidth(), mClockBg.getHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(clockBitmap);
        int centerX = mClockBg.getWidth() / 2;
        int centerY = mClockBg.getHeight() / 2;
        canvas.drawBitmap(mClockBg, 0.0f, 0.0f, paint);
        canvas.save();
        canvas.rotate(minuteAngle, centerX, centerY);
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        canvas.drawBitmap(mClockMinute, centerX - mClockMinute.getWidth() / 2.0f, centerY, paint);
        canvas.restore();
        canvas.save();
        canvas.rotate(hourAngle, centerX, centerY);
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        canvas.drawBitmap(mClockHour, centerX - mClockHour.getWidth() / 2.0f, centerY, paint);
        canvas.restore();
        canvas.save();
        canvas.drawBitmap(mClockCenter, centerX - mClockCenter.getWidth() / 2.0f, centerY - mClockCenter.getHeight() / 2.0f, paint);
        canvas.restore();
        return new BitmapDrawable(mLocalResource, clockBitmap);

    }

    private Drawable generateClockIconForTheme(Bitmap mClockBg) {
        if (mClockBg == null) {
            return null;
        }

        Bitmap mClockHour = getBitmapFromAsset("clock/hct_clock_hour.png", mLocalContext.getAssets());
        Drawable mClockHourDrawable = readThemeResDrawable("ic_clock_hour");
        if (mClockHourDrawable != null && mClockHourDrawable instanceof BitmapDrawable) {
            mClockHour = ((BitmapDrawable) mClockHourDrawable).getBitmap();
        }
        if (mClockHour == null) {
            return null;
        }
        Bitmap mClockMinute = getBitmapFromAsset("clock/hct_clock_minute.png", mLocalContext.getAssets());
        Drawable mClockMinuteDrawable = readThemeResDrawable("ic_clock_minute");
        if (mClockMinuteDrawable != null && mClockMinuteDrawable instanceof BitmapDrawable) {
            mClockMinute = ((BitmapDrawable) mClockMinuteDrawable).getBitmap();
        }
        if (mClockMinute == null) {
            return null;
        }

        Bitmap mClockCenter = getBitmapFromAsset("clock/hct_clock_center.png", mLocalContext.getAssets());
        Drawable mClockCenterDrawable = readThemeResDrawable("ic_clock_center");
        if (mClockCenterDrawable != null && mClockCenterDrawable instanceof BitmapDrawable) {
            mClockCenter = ((BitmapDrawable) mClockCenterDrawable).getBitmap();
        }
        if (mClockCenter == null) {
            return null;
        }
        Canvas canvas = new Canvas();
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Calendar instance = Calendar.getInstance();
        int hour = instance.get(Calendar.HOUR);
        int minute = instance.get(Calendar.MINUTE);
        float hourAngle = ((((float) hour) * 30.0f) + ((((float) minute) / 60.0f) * 30.0f)) + 180.0f;
        float minuteAngle = (((float) minute) * 6.0f) + 180.0f;
        Bitmap clockBitmap = Bitmap.createBitmap(mClockBg.getWidth(), mClockBg.getHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(clockBitmap);
        int centerX = mClockBg.getWidth() / 2;
        int centerY = mClockBg.getHeight() / 2;
        canvas.drawBitmap(mClockBg, 0.0f, 0.0f, paint);
        canvas.save();
        canvas.rotate(minuteAngle, centerX, centerY);
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        canvas.drawBitmap(mClockMinute, centerX - mClockMinute.getWidth() / 2.0f, centerY, paint);
        canvas.restore();
        canvas.save();
        canvas.rotate(hourAngle, centerX, centerY);
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        canvas.drawBitmap(mClockHour, centerX - mClockHour.getWidth() / 2.0f, centerY, paint);
        canvas.restore();
        canvas.save();
        canvas.drawBitmap(mClockCenter, centerX - mClockCenter.getWidth() / 2.0f, centerY - mClockCenter.getHeight() / 2.0f, paint);
        canvas.restore();
        return new BitmapDrawable(mLocalResource, clockBitmap);

    }

    // If alpha of pixel above this limit , it will be treat as the boundary of a bitmap
    private static final int DELETE_ALPHA_FRAME_LIMIT = 188;

    /**
     * @param bitmap the bit map to extract content boundary;
     * @return The rect of the bitmap content;
     */
    public static Rect extractBoundOfBitmap(Bitmap bitmap) {
        int[] imgThePixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(
                imgThePixels,
                0,
                bitmap.getWidth(),
                0,
                0,
                bitmap.getWidth(),
                bitmap.getHeight());

        int top = 0;
        int left = 0;
        int right = 0;
        int bottom = 0;
        for (int h = 0; h < bitmap.getHeight(); h++) {
            boolean holdContentPix = false;
            for (int w = 0; w < bitmap.getWidth(); w++) {
                final int alpha = bitmap.getPixel(w, h) >>> 24;
                if (alpha > DELETE_ALPHA_FRAME_LIMIT) {
                    holdContentPix = true;
                    break;
                }
            }

            if (holdContentPix) {
                break;
            }
            top++;
        }

        for (int w = 0; w < bitmap.getWidth(); w++) {
            boolean holdContentPix = false;
            for (int h = 0; h < bitmap.getHeight(); h++) {
                final int alpha = bitmap.getPixel(w, h) >>> 24;
                if (alpha > DELETE_ALPHA_FRAME_LIMIT) {
                    holdContentPix = true;
                    break;
                }
            }
            if (holdContentPix) {
                break;
            }
            left++;
        }

        for (int w = bitmap.getWidth() - 1; w >= 0; w--) {
            boolean holdContentPix = false;
            for (int h = 0; h < bitmap.getHeight(); h++) {
                final int alpha = bitmap.getPixel(w, h) >>> 24;
                if (alpha > DELETE_ALPHA_FRAME_LIMIT) {
                    holdContentPix = true;
                    break;
                }
            }
            if (holdContentPix) {
                break;
            }
            right++;
        }

        for (int h = bitmap.getHeight() - 1; h >= 0; h--) {
            boolean holdContentPix = false;
            for (int w = 0; w < bitmap.getWidth(); w++) {
                final int alpha = bitmap.getPixel(w, h) >>> 24;
                if (alpha > DELETE_ALPHA_FRAME_LIMIT) {
                    holdContentPix = true;
                    break;
                }
            }
            if (holdContentPix) {
                break;
            }
            bottom++;
        }
        return new Rect(left, top, bitmap.getWidth() - right, bitmap.getHeight() - bottom);
    }

    public IconCache getmIconCache() {
        return mIconCache;
    }
}
