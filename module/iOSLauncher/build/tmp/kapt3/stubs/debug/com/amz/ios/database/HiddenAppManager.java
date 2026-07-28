package com.amz.ios.database;

import java.lang.System;

@kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0005\u001a\n\u0012\u0004\u0012\u00020\u0007\u0018\u00010\u0006J\u000e\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u0007J\u000e\u0010\u000b\u001a\u00020\t2\u0006\u0010\f\u001a\u00020\rJ\u000e\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0007J\u000e\u0010\u0011\u001a\u00020\t2\u0006\u0010\f\u001a\u00020\rJ\u000e\u0010\u0012\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u0007R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/amz/ios/database/HiddenAppManager;", "", "()V", "database", "Lcom/amz/ios/database/HiddenDatabase;", "getAllHiddenApp", "", "Lcom/amz/ios/launcher/ItemInfo;", "hideApp", "", "data", "initDataBase", "context", "Landroid/content/Context;", "isHidden", "", "value", "notiFyUnHiddenApp", "unHideApp", "iOSLauncher_debug"})
public final class HiddenAppManager {
    @org.jetbrains.annotations.NotNull()
    public static final com.amz.ios.database.HiddenAppManager INSTANCE = null;
    private static com.amz.ios.database.HiddenDatabase database;
    
    private HiddenAppManager() {
        super();
    }
    
    public final void initDataBase(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.util.List<com.amz.ios.launcher.ItemInfo> getAllHiddenApp() {
        return null;
    }
    
    public final void notiFyUnHiddenApp(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    public final void hideApp(@org.jetbrains.annotations.NotNull()
    com.amz.ios.launcher.ItemInfo data) {
    }
    
    public final void unHideApp(@org.jetbrains.annotations.NotNull()
    com.amz.ios.launcher.ItemInfo data) {
    }
    
    public final boolean isHidden(@org.jetbrains.annotations.NotNull()
    com.amz.ios.launcher.ItemInfo value) {
        return false;
    }
}