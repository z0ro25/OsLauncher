# Rule R8 module Themes tự mang sang :app khi build release (consumerProguardFiles).

# Retrofit 2.9 + suspend + R8 full mode: thiếu thì generic của Continuation bị xoá ->
# "Class cannot be cast to ParameterizedType". Retrofit chỉ tự mang rule này từ 2.10+.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Model Gson của API Themes: giữ nguyên field + generic để parse không ra null/rỗng.
-keep class com.ezla.oslauncher.themes.Models.** { *; }
