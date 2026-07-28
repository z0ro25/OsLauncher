Google Analytics

> 帮助文档：
> https://support.google.com/analytics#topic=3544906 > https://developers.google.com/analytics/devguides/collection/android/v4/

######1. 相关代码
封装工具类：
`com.ios.ioslite.common.analytics.AnalyticsDelegate.java`
初始化在
`com.ios.iosliteLauncherApplication`
中
`public void onCreate() {}`
代码如下：
`AnalyticsDelegate.init(this, R.xml.google_analytics_config);`

> R.xml.google_analytics_config:存放 Google Analytics 的配置信息。

######2.编译添加

参看文档：

> https://developers.google.com/analytics/devguides/collection/android/v4/

######3. 调试
3.1 打开调试开关
`adb shell setprop log.tag.GAv4 DEBUG`
3.2 Log 查看
`adb logcat -v time -s GAv4`

######4. 编译错误
4.1 FAILURE: Build failed with an exception.

- What went wrong:
  Execution failed for task ':library:IOSLiteCommon:processReleaseGoogleServices'.
  > No matching client found for package name 'com.ios.ioslite.common'

**google-services.json 要放置在主工程目录，不能放置在 library 中，因为编译时会检查所在工程的包名。**

4.2FAILURE: Build failed with an exception.

- What went wrong:
  Execution failed for task ':IOSLiteApp:processDebugGoogleServices'.

  > Please fix the version conflict either by updating the version of the google-services plugin (information about the latest version is available at https://bintray.com/android/android-tools/com.google.gms.google-services/) or updating the version of com.google.android.gms to 9.0.0.

- Try:
  Run with --stacktrace option to get the stack trace. Run with --info or --debug option to get more log output.

**`apply plugin: 'com.google.gms.google-services`要放在`dependencies{}`.**

######5. 其他
如果遇到问题，可以通过官方文档

> https://developers.google.com/analytics/devguides/collection/android/v4/

还可以通过 StackOverflow

> http://stackoverflow.com/questions/tagged/google-analytics
