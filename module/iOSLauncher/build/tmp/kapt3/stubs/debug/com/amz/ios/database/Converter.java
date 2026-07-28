package com.amz.ios.database;

import java.lang.System;

@kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\r\n\u0002\b\u0002\n\u0002\u0010\u0015\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0003\u001a\u0004\u0018\u00010\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\u0012\u0010\u0007\u001a\u0004\u0018\u00010\u00042\u0006\u0010\b\u001a\u00020\tH\u0007J\u0012\u0010\n\u001a\u0004\u0018\u00010\u00042\u0006\u0010\u000b\u001a\u00020\fH\u0007J\u0012\u0010\r\u001a\u0004\u0018\u00010\u00062\u0006\u0010\u000e\u001a\u00020\u0004H\u0007J\u0012\u0010\u000f\u001a\u0004\u0018\u00010\t2\u0006\u0010\u000e\u001a\u00020\u0004H\u0007J\u0012\u0010\u0010\u001a\u0004\u0018\u00010\f2\u0006\u0010\u000e\u001a\u00020\u0004H\u0007\u00a8\u0006\u0011"}, d2 = {"Lcom/amz/ios/database/Converter;", "", "()V", "fromChar", "", "char", "", "fromIntArray", "array", "", "fromUser", "data", "Lcom/amz/ios/launcher/compat/UserHandleCompat;", "toChar", "string", "toIntArray", "toUser", "iOSLauncher_debug"})
public final class Converter {
    
    public Converter() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    @androidx.room.TypeConverter()
    public final java.lang.String fromUser(@org.jetbrains.annotations.NotNull()
    com.amz.ios.launcher.compat.UserHandleCompat data) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    @androidx.room.TypeConverter()
    public final com.amz.ios.launcher.compat.UserHandleCompat toUser(@org.jetbrains.annotations.NotNull()
    java.lang.String string) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    @androidx.room.TypeConverter()
    public final java.lang.String fromChar(@org.jetbrains.annotations.NotNull()
    java.lang.CharSequence p0_1526187) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    @androidx.room.TypeConverter()
    public final java.lang.CharSequence toChar(@org.jetbrains.annotations.NotNull()
    java.lang.String string) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    @androidx.room.TypeConverter()
    public final java.lang.String fromIntArray(@org.jetbrains.annotations.NotNull()
    int[] array) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    @androidx.room.TypeConverter()
    public final int[] toIntArray(@org.jetbrains.annotations.NotNull()
    java.lang.String string) {
        return null;
    }
}