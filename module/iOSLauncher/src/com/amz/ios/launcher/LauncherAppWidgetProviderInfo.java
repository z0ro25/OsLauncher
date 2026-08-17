package com.amz.ios.launcher;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.amz.ios.launcher.widget.configure.PictureAppWidgetProviderConfigureActivity;

import org.json.JSONException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.lang.reflect.Field;
import java.security.SecureClassLoader;

/**
 * This class is a thin wrapper around the framework AppWidgetProviderInfo class. This class affords
 * a common object for describing both framework provided AppWidgets as well as custom widgets
 * (who's implementation is owned by the launcher). This object represents a widget type / class,
 * as opposed to a widget instance, and so should not be confused with {@link LauncherAppWidgetInfo}
 */
public class LauncherAppWidgetProviderInfo extends AppWidgetProviderInfo {
    private static final String TAG = "LAWPInfo";

    public boolean isIOSWidget = false;
    public boolean isIOSIconWidget = false;

    public int spanX;
    public int spanY;
    public int minSpanX;
    public int minSpanY;

    public static LauncherAppWidgetProviderInfo fromProviderInfo(Context context,
                                                                 AppWidgetProviderInfo info) {

        // In lieu of a public super copy constructor, we first write the AppWidgetProviderInfo
        // into a parcel, and then construct a new LauncherAppWidgetProvider info from the
        // associated super parcel constructor. This allows us to copy non-public members without
        // using reflection.
        Parcel p = Parcel.obtain();
        info.writeToParcel(p, 0);
        p.setDataPosition(0);
        LauncherAppWidgetProviderInfo lawpi = new LauncherAppWidgetProviderInfo(p);
        lawpi.configure = info.configure;
        lawpi.provider = info.provider;
        p.recycle();
        return lawpi;
    }

    public static LauncherAppWidgetProviderInfo fromProviderInfo(AppWidgetProviderInfo info) {
        try {
            LauncherAppWidgetProviderInfo result = fromProviderInfo(null, info);
            return result;
        } catch (Exception ex) {
            return null;
        }
    }


    public static LauncherAppWidgetProviderInfo fromIOSWidgetComponent(Context context, ComponentName component) throws Exception {
        Log.d(TAG, "fromIOSWidgetComponent ComponentName" + component);

        LauncherAppWidgetProviderInfo lawpi = new LauncherAppWidgetProviderInfo();
        lawpi.isIOSWidget = true;
        lawpi.provider = component;

        PackageManager packageManager = context.getPackageManager();
        ActivityInfo activityInfo = packageManager.getReceiverInfo(component, PackageManager.GET_META_DATA);
        lawpi.label = activityInfo.loadLabel(context.getPackageManager()) + "";
        lawpi.icon = activityInfo.getIconResource();
        final Context packgeContext = context.createPackageContext(component.getPackageName(), Context.CONTEXT_IGNORE_SECURITY
                | Context.CONTEXT_INCLUDE_CODE);


        if (Utilities.ATLEAST_NOUGAT) {
            try {
                Field pInfo = AppWidgetProviderInfo.class.getDeclaredField("providerInfo");
                pInfo.setAccessible(true);
                pInfo.set(lawpi, activityInfo);
            } catch (Exception e) {
            }
        }

        XmlResourceParser parser = null;

        parser = activityInfo.loadXmlMetaData(packageManager, "android.appwidget.provider");
        if (parser == null) {
            throw new XmlPullParserException("null");
        } else {
            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT && type != XmlPullParser.START_TAG) {
            }

            String namespace = parser.getAttributeNamespace(0);
            int minWidthId = parser.getAttributeResourceValue(namespace, "minWidth", -1);
            int minHeightId = parser.getAttributeResourceValue(namespace, "minHeight", -1);
            int minResizeWidthId = parser.getAttributeResourceValue(namespace, "minResizeWidth", -1);
            int minResizeHeightId = parser.getAttributeResourceValue(namespace, "minResizeHeight", -1);
            String configure = parser.getAttributeValue(namespace,"configure");
            if (!TextUtils.isEmpty(configure)){
                lawpi.configure = new ComponentName(context,configure);
            }
            float minWidth = packgeContext.getResources().getDimension(minWidthId);
            float minHeight = packgeContext.getResources().getDimension(minHeightId);

            lawpi.minWidth = (int) minWidth;
            lawpi.minHeight = (int) minHeight;
            if (lawpi.minHeight == 0 && lawpi.minWidth == 0) {
                lawpi.isIOSIconWidget = true;
            }

            if (minResizeWidthId > 0 && minResizeHeightId > 0) {
                lawpi.minResizeWidth = (int) packgeContext.getResources().getDimension(minResizeWidthId);
                lawpi.minResizeHeight = (int) packgeContext.getResources().getDimension(minResizeHeightId);
            }

            lawpi.updatePeriodMillis = parser.getAttributeResourceValue(namespace, "updatePeriodMillis", 86400000);
            lawpi.initialLayout = parser.getAttributeResourceValue(namespace, "initialLayout", -1);
            lawpi.previewImage = parser.getAttributeResourceValue(namespace, "previewImage", -1);
        }
        lawpi.initSpans();
        return lawpi;
    }

    public LauncherAppWidgetProviderInfo() {
    }

    public LauncherAppWidgetProviderInfo(Parcel in) {
        super(in);
        initSpans();
    }

    public LauncherAppWidgetProviderInfo(Context context, IOSAppWidget widget) {
        isIOSWidget = true;

        provider = new ComponentName(context, widget.getClass().getName());
        icon = widget.getIcon();
        label = widget.getLabel();
        previewImage = widget.getPreviewImage();
        initialLayout = widget.getWidgetLayout();
        resizeMode = widget.getResizeMode();
        configure = widget.getConfigure();
        initSpans();
        // Widget iOS khai báo kích thước cố định qua interface (getSpanX/Y, getMinSpanX/Y).
        // initSpans() phía trên lại tính span từ minWidth/minHeight — vốn LUÔN = 0 cho widget iOS
        // (constructor này không set) — nên ép mọi widget iOS về 1x1, thêm vào màn hình chỉ bé
        // bằng 1 icon. Ghi đè lại bằng span do provider khai báo, kẹp trong lưới hiện tại để
        // không vượt số cột/hàng. Chỉ tác động đường widget iOS, không đụng initSpans() dùng chung
        // cho widget hệ thống (đi qua fromProviderInfo với minWidth/minHeight thật).
        InvariantDeviceProfile idp = LauncherAppState.getInstance().getInvariantDeviceProfile();
        spanX = Math.max(1, Math.min(widget.getSpanX(), idp.numColumns));
        spanY = Math.max(1, Math.min(widget.getSpanY(), idp.numRows));
        minSpanX = Math.max(1, Math.min(widget.getMinSpanX(), idp.numColumns));
        minSpanY = Math.max(1, Math.min(widget.getMinSpanY(), idp.numRows));
    }

    private void initSpans() {
        if (isIOSIconWidget) {
            minSpanX = minSpanY = spanX = spanY = 1;
        }

        LauncherAppState app = LauncherAppState.getInstance();
        InvariantDeviceProfile idp = app.getInvariantDeviceProfile();
        int numRows = idp.numRows;
        int numColumns = idp.numColumns;

        // We only care out the cell size, which is independent of the the layout direction.
        Rect paddingPort = idp.portraitProfile.getWorkspacePadding(false /* isLayoutRtl */);
        int statusNavigationHeightPx = idp.portraitProfile.statusBarHeightPx + idp.portraitProfile.navigationBarHeightPx;

        // Always assume we're working with the smallest span to make sure we
        // reserve enough space in both orientations.
        float smallestCellWidth = DeviceProfile.calculateCellWidth(
                idp.portraitProfile.widthPx - paddingPort.left - paddingPort.right,
                idp.numColumns);
        float smallestCellHeight = DeviceProfile.calculateCellWidth(
                idp.portraitProfile.heightPx - paddingPort.top - paddingPort.bottom - statusNavigationHeightPx,
                idp.numRows);

        // We want to account for the extra amount of padding that we are adding to the widget
        // to ensure that it gets the full amount of space that it has requested.
        Rect widgetPadding = AppWidgetHostView.getDefaultPaddingForWidget(
                app.getContext(), provider, null);
        spanX = Math.max(1, (int) Math.ceil(
                (minWidth + widgetPadding.left + widgetPadding.right) / smallestCellWidth));
        spanY = Math.max(1, (int) Math.ceil(
                (minHeight + widgetPadding.top + widgetPadding.bottom) / smallestCellHeight));

        minSpanX = Math.max(1, (int) Math.ceil(
                (minResizeWidth + widgetPadding.left + widgetPadding.right) / smallestCellWidth));
        minSpanY = Math.max(1, (int) Math.ceil(
                (minResizeHeight + widgetPadding.top + widgetPadding.bottom) / smallestCellHeight));

        spanX = Math.min(spanX, numColumns);
        spanY = Math.min(spanY, numRows);

        minSpanX = Math.min(minSpanX, numColumns);
        minSpanY = Math.min(minSpanY, numRows);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public String getLabel(PackageManager packageManager) {
        if (isIOSWidget) {
            return Utilities.trim(label);
        }
        return super.loadLabel(packageManager);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Drawable getIcon(Context context, IconCache cache) {
        if (isIOSWidget) {
            return cache.getFullResIcon(provider.getPackageName(), icon);
        }
        return super.loadIcon(context,
                LauncherAppState.getInstance().getInvariantDeviceProfile().fillResIconDpi);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Drawable getPreview(Context context) {
        if (isIOSWidget) {
            return context.getPackageManager().getDrawable(
                    provider.getPackageName(), previewImage, null);
        }

        return super.loadPreviewImage(context,
                LauncherAppState.getInstance().getInvariantDeviceProfile().fillResIconDpi);
    }

    public String toString(PackageManager pm) {
        if (isIOSWidget) {
            return "WidgetProviderInfo(" + provider + ")";
        }
        return String.format("WidgetProviderInfo provider:%s package:%s short:%s label:%s",
                provider.toString(), provider.getPackageName(), provider.getShortClassName(), getLabel(pm));
    }

    public Point getMinSpans(InvariantDeviceProfile idp, Context context) {
        return new Point(
                (resizeMode & RESIZE_HORIZONTAL) != 0 ? minSpanX : -1,
                (resizeMode & RESIZE_VERTICAL) != 0 ? minSpanY : -1);
    }
}
