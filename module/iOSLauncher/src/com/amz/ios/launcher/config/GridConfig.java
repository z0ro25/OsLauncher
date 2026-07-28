package com.amz.ios.launcher.config;

import android.content.Context;

import com.amz.ios.launcher.R;

import java.util.HashMap;


public class GridConfig {
    public static final HashMap<Integer, GridInfo> PRE_DESKTOP_GRID = new HashMap<>();
    private static final String WORKSPACE_XML = "default_workspace";

    static {
        PRE_DESKTOP_GRID.put(0, new GridInfo(4, 4));
        PRE_DESKTOP_GRID.put(1, new GridInfo(5, 4));
        PRE_DESKTOP_GRID.put(2, new GridInfo(4, 5));
        PRE_DESKTOP_GRID.put(3, new GridInfo(5, 5));
    }


    public static String getGridWorkspaceName(Context context) {
        int style = Settings.getDesktopGrid(context);
        if (style < 0) {
            style = 0;
        }
        return PRE_DESKTOP_GRID.get(style).getXmlName();
    }

    public static String getGridWorkspaceXmlName(Context context) {
        return getGridWorkspaceName(context) + ".xml";
    }

    public static String getDefaultWorkspaceName(Context context) {
        return WORKSPACE_XML;
    }

    public static class GridInfo {
        public int rows;
        public int columns;
        public int layoutId;

        public GridInfo(int rows, int columns) {
            this.rows = rows;
            this.columns = columns;
            this.layoutId = getLayoutId(rows, columns);
        }

        private int getLayoutId(int rows, int columns) {
            int layoutId = -1;
            if (rows == 4 && columns == 4) {
                layoutId = R.xml.default_workspace_4x4;
            } else if (rows == 5 && columns == 4) {
                layoutId = R.xml.default_workspace_5x4;
            } else if (rows == 4 && columns == 5) {
                layoutId = R.xml.default_workspace_4x5;
            } else if (rows == 5 && columns == 5) {
                layoutId = R.xml.default_workspace_5x5;
            }
            return layoutId;
        }


        private String getXmlName() {
            return WORKSPACE_XML + "_" + rows + "x" + columns;
        }

        @Override
        public boolean equals(Object obj) {
            try {
                if (obj != null) {
                    GridInfo other = (GridInfo) obj;
                    // Note: no null checks, because mPackage and mClass can
                    // never be null.
                    return rows == other.rows && columns == other.columns;
                }
            } catch (ClassCastException e) {
            }
            return false;
        }

        @Override
        public int hashCode() {
            return rows * 10 + columns;
        }
    }
}
