package com.amz.ios.ioslite.common.analytics;

/**
 * Created by gujianfei on 2016/12/22.
 */
public class UMEventConstants {

    //>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>Launcher
    public static final String LAUNCHER_EVENT = "LauncherEvent";

    /**
     * searchbox
     */
    public static final String SEARCHBOX_EVENT = "SearchboxEvent";
    //搜索框点击
    public static final String SEARCHBOX_CLICK = "SearchboxClick";
    //桌面搜素框内搜索按钮点击
    public static final String DESKTOP_SEARCH_BUTTON_CLICK = "DesktopSearchButtonClick";
    //搜素界面搜素框内搜索按钮点击
    public static final String SEARCH_ACTIVITY_SEARCH_BUTTON_CLICK = "SearchActivitySearchButtonClick";
    //联系人条目点击次数
    public static final String CONTACT_ITEM_CLICK = "ContactItemClick";
    //音乐条目点击次数
    public static final String MUSIC_ITEM_CLICK = "MusicItemClick";
    //应用列表条目点击次数
    public static final String APPS_ITEM_CLICK = "AppsItemClick";
    //应用推荐点击
    public static final String SEARCHBOX_APPRECOMMEND_CLICK = "AppRecommendClick";
    //新闻标题点击
    public static final String SEARCHBOX_NEWSTITLE_CLICK = "NewstitleClick";
    //新闻内容点击
    public static final String SEARCHBOX_NEWSCONTENT_CLICK = "NewscontentClick";
    //阅读更多点击
    public static final String SEARCHBOX_READMORE_CLICK = "ReadMoreClick";

    /**
     * iosKnow
     */
    public static final String IOS_DROPICON = "IOSKnowEvent";
    //下拉图标点击
    public static final String DROPDOWNICON_CLICK = "IOSKnowClick";

    /**
     * cleanwidget
     */
    public static final String IOS_CLEANWIDGET = "CleanEvent";
    //一键加速点击
    public static final String CLEANWIDGET_CLICK  = "CleanIconClick";

    /**
     * theme
     */
    public static final String IOS_THEME = "ThemeEvent";
    //启动主题
    public static final String THEME_ENTER = "ThemeEnter";
    //退出主题
    public static final String THEME_EXIT = "ThemeExit";
    //各个主题的点击
    public static final String THEME_PERTHEME_CLICK = "PerthemeClick";
    //主题分享点击
    public static final String THEME_SHARE = "ThemeShare";
    //访问最新主题页面
    public static final String THEME_ACCESS_NEWESTTHEME = "NewestTheme";
    //访问极简主题页面
    public static final String THEME_ACCESS_MINIMALISTTHEME = "MinimalistTheme";
    //访问精选主题页面
    public static final String THEME_ACCESS_SELECTIONTHEME  = "SelectionTheme";
    //访问主题详情页面
    public static final String THEME_ACCESS_THEMEDETAIL = "ThemeDetail";

    /**
     * wallpaper
     */
    public static final String IOS_WALLPAPER = "WallpaperEvent";
    //启动壁纸
    public static final String WALLPAPER_ENTER = "wallpaperEnter";
    //退出壁纸
    public static final String WALLPAPER_EXIT = "wallpaperExit";
    //各个壁纸的点击
    public static final String WALLPAPER_PERWALLPAPER_CLICK = "PerWallpaperclick";
    //壁纸分享点击
    public static final String WALLPAPER_SHARE = "WallpaperShare";
    //访问最新壁纸页面
    public static final String WALLPAPER_ACCESS_NEWESTWALLPAPER = "NewestWallpaper";
    //访问精选壁纸页面
    public static final String WALLPAPER_ACCESS_SELECTIONWALLPAPER = "SelectionWallpaper";

    /**
     * all apps
     */
    public static final String IOS_ALLAPPS = "AllAppsEvent";
    //所有应用图标的点击
    public static final String ALLAPPS_ICON_CLICK = "AppsIconClick";
    //所有应用内搜索框的点击
    public static final String ALLAPPS_SEARCHBOX_CLICK = "AppsSearchboxClick";
    //搜索框输入有效内容的次数
    public static final String ENTER_VALID_CONTENT = "EnterValidContent";
    //搜索框输入有效内容并搜索到有效数据的次数
    public static final String SEARCH_FOR_ACTIVE_CONTENT = "SerachForActiveContent";
    //所有应用内各应用的点击
    public static final String ALLAPPS_PERAPP_CLICK = "AppsPerappClick";

    /**
     * weather
     */
    public static final String IOS_WEATHER_EVENT    = "WeatherEvent";
    //天气图标的点击
    public static final String WEATHER_WIDGET_CLICK    = "WeatherIconClick";
    //添加城市按钮的点击
    public static final String WEATHER_CITYADDED_CLICK = "CityAddedClick";
    //天气分享的点击
    public static final String WEATHER_SHARE = "WeatherShare";

    /**
     * settings
     */
    public static final String IOS_SETTINGS_EVENT = "SettingsEvent";
    //启动设置
    public static final String SETTINGS_ENTER = "SettingsEnter";
    //退出设置
    public static final String SETTINGS_EXIT = "SettingsExit";
    //应用管理点击次数
    public static final String SETTINGS_APPMANAGE_CLICK = "AppManageClick";
    //系统设置点击次数
    public static final String SETTINGS_SYSTEMSETTING_CLICK   = "SystemSettingClick";
    //桌面设置点击次数
    public static final String SETTINGS_LAUNCHERSETTING_CLICK = "DeskSettingClick";
    //设置页面访问次数
    public static final String SETTINGS_ACCESS = "SettingAccess";

    /**
     * 长按桌面空白(longclick)
     */
    public static final String IOS_LONGCLICK_EVENT = "LongClickEvent";
    //启动(用户长按空白处视为一次启动)
    public static final String LONGCLICK_ENTER = "LongClickEnter";
    //退出
    public static final String LONGCLICK_EXIT = "LongClickExit";

    public static final String LONGCLICK_MENU_MENU_CLICK  = "MenuMenuClick";
    public static final String LONGCLICK_MENU_SEARCH_CLICK  = "MenuSearchClick";
    //主题壁纸点击
    public static final String LONGCLICK_THEME_WALLPAPER_CLICK  = "ThemeWallpClick";
    //添加小工具点击
    public static final String LONGCLICK_WIDGET_CLICK = "AddToolsClick";
    //桌面设置点击
    public static final String LONGCLICK_LAUNCHERSETTINGS_CLICK = "DeskSettingsClick";
    //滑屏效果点击
    public static final String LONGCLICK_SLIDE_SCREEN_EFFECT_CLICK = "SlideScreenEffectClick";
    //设置主界面的图标点击
    public static final String LONGCLICK_SET_HOME_PAGE_ICON_CLICK = "SetHomePageIconClick";
    //系统设置点击
    public static final String LONGCLICK_SYSTEMSETTINGS_CLICK   = "SystemSettingsClick";

    /**
     * launchersettings
     */
    public static final String IOS_LAUNCHERSETTINGS_EVENT = "DeskSettingEvent";
    //启动(用户点击桌面设置按钮视为一次启动)
    public static final String LAUNCHERSETTINGS_ENTER = "DeskSettingEnter";
    //退出
    public static final String LAUNCHERSETTINGS_EXIT = "DeskSettingExit";
    //设置默认桌面点击
    public static final String LAUNCHERSETTINGS_DEFAULTLAUNCHER_CLICK = "DefLauncherClick";
    //联系我们点击
    public static final String LAUNCHERSETTINGS_CONTACTUS_CLICK = "ContactUsClick";
    //外观点击
    public static final String LAUNCHERSETTINGS_OUTLOOK_CLICK = "OutlookClick";
    //头条新闻点击
    public static final String LAUNCHERSETTINGS_HEADNEWS_CLICK = "HeadnewsClick";
    //显示搜索框点击
    public static final String LAUNCHERSETTINGS_SHOWSEARCHBOX_CLICK   = "ShowSearchbox";
    //显示首屏iosknow点击
    public static final String LAUNCHERSETTINGS_SHOWIOSKNOW_CLICK  = "ShowIOSknow";
    //检查更新点击
    public static final String LAUNCHERSETTINGS_CHECKANDUPDATE_CLICK  = "CheckAndUpdate";
    //分享点击
    public static final String LAUNCHERSETTINGS_IOSSHARE = "IOSShare";
    //关于IOS点击
    public static final String LAUNCHERSETTINGS_ABOUTIOS_CLICK = "AboutIOS";
    //IOS探索發現
    public static final String LAUNCHERSETTINGS_APPRECOMMEND_CLICK = "AppRecommendClick";

    /*
    * IOSStackWidget
    * */
    public static final String STACK_WIDGET_EVENT = "StackWidgetEvent";
    //添加广告卡片到桌面上
    public static final String ADD_STACK_WIDGET_TO_DESKTOP = "AddStackWidgetToDesktop";
    //从桌面上移除广告卡片
    public static final String REMOVE_STACK_WIDGET_FROM_DESKTOP = "RemoveStackWidgetFromDesktop";
    //翻动下一张卡片
    public static final String SHOW_NEXT_CARD = "ShowNextCard";
    //主题卡片点击
    public static final String THEME_CLUB_CARD_CLICK = "ThemeClubCardClick";
    //游戏中心卡片点击
    public static final String GAME_CENTER_CARD_CLICK = "GameCenterCardClick";
    //探索卡片点击
    public static final String DISCOVERY_CARD_CLICK = "DiscoveryCardClick";
    //广告卡片点击
    public static final String ADS_CARD_CLICK = "AdsCardClick";

    /*
    * IOSDiscovery
    * */
    public static final String DISCOVERY_EVENT   = "DiscoveryEvent";
    //探索启动次数
    public static final String ENTER_DISCOVERY   = "EnterDiscovery";
    //应用(国内资源)点击次数
    public static final String CN_APPS_CLICK     = "CnAppsClick";
    //应用(海外资源)点击次数
    public static final String ABROAD_APPS_CLICK = "AbroadAppsClick";
    //应用展示量
    public static final String CN_APPS_SHOW = "CnAppsShow";
    //应用下载成功量
    public static final String CN_APPS_DOWNLOAD = "CnAppsDownload";
    //应用安装成功量
    public static final String CN_APPS_INSTALL = "CnAppsInstall";

    /*
    * (SmartSort)智能整理
    * */
    public static final String SMART_SORT_EVENT = "SmartSortEvent";
    //各个被整理文件夹点击次数
    public static final String DESKTOP_FOLDER_CLICK = "FolderClick";
    //各个被整理文件夹内应用移出个数
    public static final String DESKTOP_FOLDER_APPS_REMOVE = "AppsRemove";
    //各个被整理文件夹应用添加个数
    public static final String DESKTOP_FOLDER_APPS_ADD    = "AppsAdd";

    /*
    * ads event
    * */
    public static final String ADS_EVNET = "AdsEvent";
    /*
    * TW ads (台湾广告统计:缺少广告打开的统计)
    * */
    public static final String TW_ADS_Event = "TwAdsEvent";
    //广告请求
    public static final String TW_AD_REQUEST = "TwAdRequest";
    //广告请求成功
    public static final String TW_AD_RESPONSE_SUCCESSFUL   = "TwAdResponseSuccessful";
    //广告请求失败
    public static final String TW_AD_RESPONSE_FAIL = "TwAdResponseFail";
    /*
    * Droi ads(Droi广告统计)
    * */
    public static final String DROI_ADS_Event = "DroiAdsEvent";
    //广告请求
    public static final String DROI_AD_REQUEST = "DroiAdRequest";
    //广告请求成功
    public static final String DROI_AD_RESPONSE_SUCCESSFUL = "DroiAdResponseSuccessful";
    //广告请求失败
    public static final String DROI_AD_RESPONSE_FAIL = "DroiAdResponseFail";
    //广告点击
    public static final String DROI_AD_CLICK = "DroiAdClick";

    /**
     * newspage
     */
    public static final String IOS_NEWSPAGE = "NewspageEvent";
    //启动新闻
    public static final String NEWSPAGE_ENTER = "NewspageEnter";
    //退出新闻
    public static final String NEWSPAGE_EXIT = "NewspageExit";
    //跳出新闻（用户打开页面后未进行任何交互动作便退出/用户使用时长小于等于5秒即视为跳出）
    public static final String NEWSPAGE_JUMPOUT = "NewspageJumpout";
    //点击新闻
    public static final String NEWSPAGE_ITEM_CLICK = "NewspageItemClick";
    //访问for you页面
    public static final String NEWSPAGE_ACCESS_FORYOU = "NewsForyou";
    //访问health页面
    public static final String NEWSPAGE_ACCESS_HEALTH = "NewsHealth";
    //访问fashion页面
    public static final String NEWSPAGE_ACCESS_FASHION = "NewsFashion";
    //访问auto页面
    public static final String NEWSPAGE_ACCESS_AUTO = "NewsAuto";
    //访问business页面
    public static final String NEWSPAGE_ACCESS_BUSINESS = "NewsBusiness";
    //访问technology页面
    public static final String NEWSPAGE_ACCESS_TECHNOLOGY = "NewsTechnology";
    //访问sports页面
    public static final String NEWSPAGE_ACCESS_SPORTS = "NewsSports";
    //访问entertain页面
    public static final String NEWSPAGE_ACCESS_ENTERTAIN = "NewsEntertain";
    //访问hot页面
    public static final String NEWSPAGE_ACCESS_HOT = "NewsHot";
    //访问新闻详情页面
    public static final String NEWSPAGE_ACCESS_NEWSDETAIL = "NewsDetail";


    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>IOS_CENTER

    public static final String C2C_EVENT = "C2CEvent";
    /**
     * ios俱乐部
     */
    public static final String IOS_CLUB = "IOSClubEvent";
    //启动
    public static final String IOSCLUB_ENTER = "IOSClubEnter";
    //退出
    public static final String IOSCLUB_EXIT = "IOSClubExit";
    //桌面图标按钮点击
    public static final String IOSCLUB_ICON_CLICK = "ClubIconClick";
    //玩机攻略页面
    public static final String IOSCLUB_ACCESS_STRATAGEPAGE = "StratagePage";
    //任务提示页面
    public static final String IOSCLUB_ACCESS_PROJECTTIPSPAGE = "ProjectTipsPage";


    /**
     * ios登录
     */
    public static final String IOS_LOGIN = "IOSLoginEvent";
    //登录界面启动
    public static final String IOSLOGIN_ENTER = "IOSLoginEnter";
    //登录按钮点击
    public static final String IOSLOGIN_LOGINBUTTOM_CLICK = "LoginBtnClick";
    //facebook登录失败
    public static final String IOSLOGIN_FACEBOOK_FAILURE  = "LoginFBFail";
    //facebook登录成功
    public static final String IOSLOGIN_FACEBOOK_SUCCESS  = "LoginFBSuccess";
    //google登录失败
    public static final String IOSLOGIN_GOOGLE_FAILURE = "LoginGoogleFail";
    //google登录成功
    public static final String IOSLOGIN_GOOGLE_SUCCESS = "LoginGoogleSuccess";
    //登陆页面展示
    public static final String IOSLOGIN_ACCESS_LOGINPAGE  = "AccessLoginPage";

    /**
     * 我的界面
     */
    public static final String IOS_MY = "IOSMyEvent";
    //界面启动
    public static final String IOSMY_ENTER = "IOSMyEnter";
    //签到按钮点击
    public static final String IOSMY_SIGNBUTTOM_CLICK  = "SignBtnClick";
    //排行榜点击
    public static final String IOSMY_LEADERBOARD_CLICK = "LeaderBoarClick";
    //兑换按钮点击
    public static final String IOSMY_EXCHANGE_CLICK = "ExchangeBtnClick";
    //分享按钮点击
    public static final String IOSMY_SHARE_CLICK = "IOSMyShareClick";
    //联系我们按钮点击
    public static final String IOSMY_CONTACTUS_CLICK = "IOSMyContactUs";
    //刷新按钮点击
    public static final String IOSMY_REFRESH_CLICK = "RefreshBtnClick";
    //我的界面展示
    public static final String IOSMY_ACCESS_MYPAGE = "AccessMyPage";

    /**
     * 兑换界面
     */
    public static final String IOS_EXCHANGE = "ExchangeEvent";
    //兑换界面启动
    public static final String IOSEXCHANGE_ENTER = "ExchangeEnter";
    //10元话费点击
    public static final String IOSEXCHANGE_EXPENSES10_CLICK = "Exp10Click";
    //5元话费点击
    public static final String IOSEXCHANGE_EXPENSES5_CLIC = "Exp5Click";
    //50元话费点击
    public static final String IOSEXCHANGE_EXPENSES50_CLICK = "Exp50Click";
    //100元话费点击
    public static final String IOSEXCHANGE_EXPENSES100_CLICK = "Exp100Click";
    //10元话费兑换成功
    public static final String IOSEXCHANGE_EXPENSES10_SUCCESS = "Exp10Success";
    //5元话费兑换成功
    public static final String IOSEXCHANGE_EXPENSES5_SUCCESS = "Exp5Success";
    //50元话费兑换成功
    public static final String IOSEXCHANGE_EXPENSES50_SUCCESS = "Exp50Success";
    //100元话费兑换成功
    public static final String IOSEXCHANGE_EXPENSES100_SUCCESS = "Exp100Success";
    //输入手机号码
    public static final String IOSEXCHANGE_PHONENUM_INPUT = "PhoneNum";
    //输入手机运营商
    public static final String IOSEXCHANGE_PHONEOPERATOR_INPUT = "PhoneOperator";
    //提交按钮点击
    public static final String IOSEXCHANGE_SUBMIT_CLICK = "SubmitBtnClick";
    //提交成功
    public static final String IOSEXCHANGE_SUBMIT_SUCCESS = "SubmitSuccess";
    //进入兑换页
    public static final String IOSEXCHANGE_ACCESS_EXCHANGEPAGE = "ExchangePage";

    /**
     * 不同来源的用户
     */

    //-------从谷歌下载的用户------
    public static final String IOS_GOOGLE_USER = "GoogleUserEvent";
    //点击分享按钮
    public static final String USER_FROMGOOGLE_SHARE_CLICK = "GoogleShare";
    //分享成功
    public static final String USER_FROMGOOGLE_SHARE_SUCCESS = "GoogleShareSuccess";
    //点击取消按钮
    public static final String USER_FROMGOOGLE_CANCEL_CLICK = "GoogleCancel";
    //点击兑换按钮
    public static final String USER_FROMGOOGLE_EXCHANGE_CLICK = "GoogleExchange";
    //兑换成功
    public static final String USER_FROMGOOGLE_EXCHANGE_SUCCESS = "GLExchangeSuccess";
    //登录账号
    public static final String USER_FROMGOOGLE_LOGIN_CLICK = "GoogleLogin";
    //登录成功
    public static final String USER_FROMGOOGLE_LOGIN_SUCCESS = "GoogleLoginSuccess";
    //登录失败
    public static final String USER_FROMGOOGLE_LOGIN_FAILURE = "GoogleLoginFail";

    //-------从分享链接安装的用户------
    public static final String IOS_LINK_USER = "LinkUserEvent";
    //填写邀请码
    public static final String USER_FROMLINK_WRITE_INVITATIONCODE = "InvitationCode";
    //点击分享按钮
    public static final String USER_FROMLINK_SHARE_CLICK = "LinkShareClick";
    //分享成功
    public static final String USER_FROMLINK_SHARE_SUCCESS = "LinkShareSuccess";
    //点击任务提示
    public static final String USER_FROMLINK_PROJECTTIPS_CLICK = "LinkTaskTips";
    //点击兑换按钮
    public static final String USER_FROMLINK_EXCHANGE_CLICK = "LinkExchange";
    //兑换成功
    public static final String USER_FROMLINK_EXCHANGE_SUCCESS = "LinkExchangeSuccess";
    //点击取消按钮
    public static final String USER_FROMLINK_CANCLE_CLICK = "LinkCancle";
    //登录账号
    public static final String USER_FROMLINK_LOGIN_CLICK = "LinkLogin";
    //登录成功
    public static final String USER_FROMLINK_LOGIN_SUCCESS = "LinkLoginSuccess";
    //登录失败
    public static final String USER_FROMLINK_LOGIN_FAILURE = "LinkLoginFail";

    /**
     * iosos更新
     */
    public static final String IOSOS_UPDATE = "UpdateEvent";
    //点击去试试
    public static final String IOSOS_UPDATE_CLICK = "IOSosUpdate";

    /**
     * 区分用户渠道
     */
    public static final String IOS_USER_CHANNEL = "UserChannelEvent";
    //点击是
    public static final String USERCHANNEL_DISTINGUISH_YES = "DistinguishYes";
    //点击否
    public static final String USERCHANNEL_DISTINGUISH_NO  = "DistinguishNo";
}


