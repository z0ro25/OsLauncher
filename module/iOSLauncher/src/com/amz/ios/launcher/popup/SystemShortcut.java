package com.amz.ios.launcher.popup;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.amz.ios.launcher.DeleteDropTarget;
import com.amz.ios.launcher.InfoDropTarget;
import com.amz.ios.launcher.ItemInfo;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAppWidgetInfo;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.UninstallDropTarget;
import com.amz.ios.launcher.util.Themes;

/**
 * Represents a system shortcut for a given app. The shortcut should have a static label and
 * icon, and an onClickListener that depends on the item that the shortcut services.
 * <p>
 * Example system shortcuts, defined as inner classes, include Widgets and AppInfo.
 */
public abstract class SystemShortcut {
    private final int mIconResId;
    private final int mLabelResId;

    public SystemShortcut(int iconResId, int labelResId) {
        mIconResId = iconResId;
        mLabelResId = labelResId;
    }

    public Drawable getIcon(Context context, int colorAttr) {
        Drawable icon = context.getResources().getDrawable(mIconResId, context.getTheme()).mutate();
        icon.setTint(Themes.getAttrColor(context, colorAttr));
        return icon;
    }

    public String getLabel(Context context) {
        return context.getString(mLabelResId);
    }

    public abstract View.OnClickListener getOnClickListener(final Launcher launcher,
                                                            final ItemInfo itemInfo);

    public static class Widgets extends SystemShortcut {

        public Widgets() {
            super(R.drawable.ic_widget, R.string.widget_button_text);
        }

        @Override
        public View.OnClickListener getOnClickListener(final Launcher launcher,
                                                       final ItemInfo itemInfo) {
//            final List<WidgetItem> widgets = launcher.getWidgetsForPackageUser(new PackageUserKey(
//                    itemInfo.getTargetComponent().getPackageName(), itemInfo.user));
//            if (widgets == null) {
            return null;
//            }
//            return new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    AbstractFloatingView.closeAllOpenViews(launcher);
//                    WidgetsBottomSheet widgetsBottomSheet =
//                            (WidgetsBottomSheet) launcher.getLayoutInflater().inflate(
//                                    R.layout.widgets_bottom_sheet, launcher.getDragLayer(), false);
//                    widgetsBottomSheet.populateAndShow(itemInfo);
//                }
//            };
        }
    }

    public static class AppInfo extends SystemShortcut {
        public AppInfo() {
            super(R.drawable.ic_info_no_shadow, R.string.info_target_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(final Launcher launcher,
                                                       final ItemInfo itemInfo) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    launcher.closeFloatingMenu();
                    InfoDropTarget.startDetailsActivityForInfo(itemInfo, launcher);
                }
            };
        }
    }

    public static class HiddenApp extends SystemShortcut {

        public HiddenApp() {
            super(R.drawable.ic_hidden_popup_menu, R.string.hidden_app);
        }

        @Override
        public View.OnClickListener getOnClickListener(Launcher launcher, ItemInfo itemInfo) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launcher.closeFloatingMenu();
                    launcher.addHiddenApp(itemInfo);
                }
            };
        }
    }

    public static class EditHomeScreen extends SystemShortcut {
        public EditHomeScreen() {
            super(R.drawable.ic_edit_home, R.string.edit_home_screen);
        }

        @Override
        public View.OnClickListener getOnClickListener(final Launcher launcher,
                                                       final ItemInfo itemInfo) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    launcher.onEditHomeScreen();
                    if (itemInfo instanceof LauncherAppWidgetInfo) {

                    }
                }
            };
        }
    }

    public static class RemoveApp extends SystemShortcut {
        public RemoveApp() {
            super(R.drawable.ic_remove, R.string.remove_app);
        }

        @Override
        public View.OnClickListener getOnClickListener(final Launcher launcher,
                                                       final ItemInfo itemInfo) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (itemInfo instanceof LauncherAppWidgetInfo) {
                        LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) itemInfo;
                        DeleteDropTarget.removeWorkspaceOrFolderItem(
                                launcher,
                                info,
                                launcher.mOpenAppWidgetHostView
                        );
                        launcher.closeFloatingMenu();
                    } else {
                        launcher.closeFloatingMenu();
                        UninstallDropTarget.startUninstallActivity(
                                launcher,
                                itemInfo
                        );
                    }

                }
            };
        }
    }
}
