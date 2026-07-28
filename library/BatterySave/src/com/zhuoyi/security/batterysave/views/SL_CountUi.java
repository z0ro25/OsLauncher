package com.zhuoyi.security.batterysave.views;

public class SL_CountUi {

    protected static boolean canRember = false;
    protected static int Open_Activity_Count = 1;
    protected static long Open_Activity_Start_Time = -1;
    /*protected static long Open_Activity_End_Time = -1;*/

    protected static void setStartRemeber(boolean state) {
        canRember = state;
    }

    protected static void pageCount() {
        if (canRember) {
            SL_CountUi.Open_Activity_Count++;
        }
    }

    protected static void pageClean() {
        SL_CountUi.Open_Activity_Start_Time = -1;
        SL_CountUi.Open_Activity_Count = 1;
        canRember = false;
    }

    protected static int getPageCount() {
        return SL_CountUi.Open_Activity_Count;
    }

    protected static void setStartTime(long startTime) {
        if (canRember) {
            SL_CountUi.Open_Activity_Start_Time = startTime;
        }
    }

    protected static long getStartTime() {
        return SL_CountUi.Open_Activity_Start_Time;
    }
}
