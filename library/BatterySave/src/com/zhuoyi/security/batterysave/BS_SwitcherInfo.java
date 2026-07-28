package com.zhuoyi.security.batterysave;

/**
 * Created by tangxiaohui on 2016/8/17.
 */

public class BS_SwitcherInfo {
    private int icon;
    private String switcher;
    private String explain;
    private boolean state;
    private String mark;
    private int tvState;

    public BS_SwitcherInfo(int i, String sw, String ex, boolean st, String m, int tvs) {
        icon = i;
        switcher = sw;
        explain = ex;
        state = st;
        mark = m;
        tvState = tvs;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getSwitcher() {
        return switcher;
    }

    public void setSwitcher(String switcher) {
        this.switcher = switcher;
    }

    public String getExplain() {
        return explain;
    }

    public void setExplain(String explain) {
        this.explain = explain;
    }

    public boolean getStarte() {
        return state;
    }

    public void setStarte(boolean state) {
        this.state = state;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public int getTvState() {
        return tvState;
    }

    public void setTvState(int tvState) {
        this.tvState = tvState;
    }
    
    
}
