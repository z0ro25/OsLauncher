package com.zhuoyi.security.batterysave;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zhuoyi.security.batterysave.util.BS_LOG;
import com.zhuoyi.security.batterysave.views.BS_TitleBar;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

public class BS_RankingActivity extends Activity implements BS_TitleBar.CallBack{

    private ListView listView;
    private Context mContext;
    private double mMinPercentOfTotal = 0.1;
    private static final String TAG = "BS_RankingActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.bs_activity_rank);
        mContext = getApplicationContext();

        BS_TitleBar titleBar = (BS_TitleBar) findViewById(R.id.rank_bar);
        titleBar.setOnCallBack(this);

        listView = (ListView) findViewById(R.id.list);
        /*listView.addFooterView(new ViewStub(this));*/


        List<AppInfo> apps = getAppListCpuTime();
        listView.setAdapter(new BatteryAdapter(mContext, apps));
        Log.d(TAG, "=============apps=" + apps);

    }

    private long getAppProcessTime(int pid) {
        FileInputStream in = null;
        String ret = null;
        try {
            in = new FileInputStream("/proc/" + pid + "/stat");
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int len = 0;
            while ((len = in.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            ret = os.toString();
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (ret == null) {
            return 0;
        }

        String[] s = ret.split(" ");
        if (s == null || s.length < 17) {
            return 0;
        }

        final long utime = string2Long(s[13]);
        final long stime = string2Long(s[14]);
        final long cutime = string2Long(s[15]);
        final long cstime = string2Long(s[16]);

        return utime + stime + cutime + cstime;
    }

    private long string2Long(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
        }
        return 0;
    }

    private List<AppInfo> getAppListCpuTime() {
        final List<AppInfo> list = new ArrayList<AppInfo>();

        long totalTime = 0;
        List<ProcessData> runningProcesses = getRunningApk();

        HashMap<String, AppInfo> templist = new HashMap<String, AppInfo>();

        for (ProcessData info : runningProcesses) {
            final long time = getAppProcessTime(info.pid);
            String[] pkgNames = info.getPns();
            if (pkgNames == null) {
                if (templist.containsKey(info.processName)) {
                    AppInfo sipper = templist.get(info.processName);
                    sipper.setValue(sipper.getValue() + time);
                } else {
                    AppInfo appInfo = new AppInfo(mContext, info.processName, time);
                    Drawable icon = getPackageManager().getDefaultActivityIcon();
                    appInfo.setIcon(icon);
                    appInfo.setAppName(info.processName);
                    templist.put(info.processName, appInfo);
                }
                totalTime += time;
            } else {
                for (String pkgName : pkgNames) {
                    if (templist.containsKey(pkgName)) {
                        AppInfo sipper = templist.get(pkgName);
                        sipper.setValue(sipper.getValue() + time);
                    } else {
                        AppInfo appInfo = new AppInfo(mContext, pkgName, time);
                        try {
                            PackageManager pm = getPackageManager();
                            ApplicationInfo ai = pm.getApplicationInfo(pkgName, 0);
                            Drawable icon = ai.loadIcon(getPackageManager());
                            String appName = ai.loadLabel(pm).toString();
                            if (TextUtils.isEmpty(appName)) {
                                appName = pkgName;
                            }
                            appInfo .setAppName(appName);
                            if (icon == null) {
                                icon = getPackageManager().getDefaultActivityIcon();
                            }
                            appInfo.setIcon(icon);
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }

                        templist.put(pkgName, appInfo);
                    }
                    totalTime += time;
                }
            }
        }

        if (totalTime == 0) totalTime = 1;
        Log.d(TAG, "totalTime=" + totalTime);
        list.addAll(templist.values());
        for (int i = list.size() - 1; i >= 0; i--) {
            AppInfo sipper = list.get(i);
            double percentOfTotal = 0;

            try {
                percentOfTotal = sipper.getValue() * 100.00 / totalTime;
                Locale locale = Locale.ENGLISH;
                DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(locale);
                DecimalFormat df = new DecimalFormat("0.00",decimalFormatSymbols);
                String percen = df.format(percentOfTotal);
                percentOfTotal = Double.valueOf(percen);
            }catch (Exception e) {
                BS_LOG.logE("format is error::"+e.toString());
            }

            if (percentOfTotal < mMinPercentOfTotal) {
                list.remove(i);
            } else {
                sipper.setPercent(percentOfTotal);
            }
        }

        Collections.sort(list, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo object1, AppInfo object2) {
                double d1 = object1.getPercent();
                double d2 = object2.getPercent();
                if (d1 - d2 < 0) {
                    return 1;
                } else if (d1 - d2 > 0) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });

        return list;
    }

    @Override
    public void onLeftClick() {
        finish();
    }

    @Override
    public void onCenterClick() {

    }

    @Override
    public void onRightClick() {

    }

    class AppInfo {
        Context context;
        String pkgname;
        String appName;
        Drawable icon;
        long value;
        double percent;

        public AppInfo(Context mContext, String pkgname, long time) {
            this.context = mContext;
            this.pkgname = pkgname;
            this.value = time;

        }

        public String getPkgname() {
            return pkgname;
        }

        public void setPkgname(String pkgname) {
            this.pkgname = pkgname;
        }
        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }

        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }

        public double getPercent() {
            return percent;
        }

        public void setPercent(double percent) {
            this.percent = percent;
        }

        public Drawable getIcon() {
            return icon;
        }

        public void setIcon(Drawable icon) {
            this.icon = icon;
        }

        @Override
        public String toString() {
            return "AppInfo{" +
                    "pkgname='" + pkgname + '\'' +
                    ", appName='" + appName + '\'' +
                    ", icon=" + icon +
                    ", value=" + value +
                    ", percent=" + percent +
                    '}';
        }
    }

    class ProcessData {
        Context context;
        int uid;
        int pid;
        String[] pns;
        String processName;


        public ProcessData() {

        }


        public int getUid() {
            return uid;
        }

        public void setUid(int uid) {
            this.uid = uid;
        }

        public int getPid() {
            return pid;
        }

        public void setPid(int pid) {
            this.pid = pid;
        }

        public String[] getPns() {
            return pns;
        }

        public void setPns(String[] pns) {
            this.pns = pns;
        }

        public String getProcessName() {
            return processName;
        }

        public void setProcessName(String processName) {
            this.processName = processName;
        }


        @Override
        public String toString() {
            return "ProcessData{" +
                    "pid=" + pid +
                    ", pns=" + Arrays.toString(pns) +
                    ", processName='" + processName + '\'' +
                    ", uid=" + uid +
                    '}';
        }
    }

    private List<ProcessData> getRunningApk() {
        List<ProcessData> datas = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        String pgkProcessAppMap = "";
        String cmd = "ps";
        try {
            java.lang.Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            int index = 0;
            while ((line = in.readLine()) != null) {
                if (index > 0) {
                    StringTokenizer st = new StringTokenizer(line);
                    String uid = st.nextToken();//0 index USER
                    Log.d(TAG, "uid====" + uid);
                    if (uid.startsWith("u0_")) {
                        int s0 = getUidForStr(uid);
                        String s = st.nextToken();
                        String s1 = st.nextToken();
                        String s2 = st.nextToken();
                        String s3 = st.nextToken();
                        String s4 = st.nextToken();
                        String s5 = st.nextToken();
                        String s6 = st.nextToken();
                        String s7 = "";
                        if (st.hasMoreElements() || st.hasMoreTokens()) {
                            s7 = st.nextToken();
                        }
                        pgkProcessAppMap += s + ":" + s1 + ":" + s7 + ",";

                        try {
                            ProcessData pd = new ProcessData();
                            pd.setPid(Integer.valueOf(s));
                            pd.setUid(s0);
                            String[] pns = getPackageManager().getPackagesForUid(s0);
                            pd.setPns(pns);
                            pd.setProcessName(s7);
                            datas.add(pd);
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }

                }
                index++;
            }
        } catch (IOException e) {
            Log.e(TAG, "getRunningApk err=" + e.toString());
        }
        if (pgkProcessAppMap.contains(",") && pgkProcessAppMap.length() > 0) {
            pgkProcessAppMap = pgkProcessAppMap.substring(0, pgkProcessAppMap.length() - 1);
        }
        long endTime = System.currentTimeMillis();
        Log.d(TAG, "do_exec datas = " + datas + "\t time = " + (endTime - startTime));
        return datas;
    }

    // 查询所有正在运行的应用程序信息： 包括他们所在的进程id和进程名
    // 这儿我直接获取了系统里安装的所有应用程序，然后根据报名pkgname过滤获取所有真正运行的应用程序
    private List<ProcessData> queryAllRunningAppInfo() {
        PackageManager pm = this.getPackageManager();
        // 查询所有已经安装的应用程序
        List<ApplicationInfo> listAppcations = pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
        Collections.sort(listAppcations, new ApplicationInfo.DisplayNameComparator(pm));// 排序

        // 保存所有正在运行的包名 以及它所在的进程信息
        Map<String, ActivityManager.RunningAppProcessInfo> pgkProcessAppMap = new HashMap<String, ActivityManager.RunningAppProcessInfo>();

        ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        // 通过调用ActivityManager的getRunningAppProcesses()方法获得系统里所有正在运行的进程
        List<ActivityManager.RunningAppProcessInfo> appProcessList = mActivityManager
                .getRunningAppProcesses();

        for (ActivityManager.RunningAppProcessInfo appProcess : appProcessList) {
            int pid = appProcess.pid; // pid
            String processName = appProcess.processName; // 进程名
            String[] pkgNameList = appProcess.pkgList; // 获得运行在该进程里的所有应用程序包
            // 输出所有应用程序的包名
            for (int i = 0; i < pkgNameList.length; i++) {
                String pkgName = pkgNameList[i];
                // 加入至map对象里
                pgkProcessAppMap.put(pkgName, appProcess);
            }
        }
        // 保存所有正在运行的应用程序信息
        List<ProcessData> runningAppInfos = new ArrayList<ProcessData>(); // 保存过滤查到的AppInfo

        for (ApplicationInfo app : listAppcations) {
            // 如果该包名存在 则构造一个RunningAppInfo对象
            if (pgkProcessAppMap.containsKey(app.packageName)) {
                // 获得该packageName的 pid 和 processName
                int pid = pgkProcessAppMap.get(app.packageName).pid;
                String processName = pgkProcessAppMap.get(app.packageName).processName;
                runningAppInfos.add(getAppInfo(app, pid, processName));
            }
        }

        return runningAppInfos;

    }

    private ProcessData getAppInfo(ApplicationInfo app, int pid, String processName) {
        ProcessData pd = new ProcessData();
        pd.setPid(pid);
        pd.setUid(app.uid);
        String[] pns = getPackageManager().getPackagesForUid(app.uid);
        pd.setPns(pns);
        pd.setProcessName(processName);
        return pd;
    }

    public int getUidForStr(String str) {
        if (!TextUtils.isEmpty(str) && str.startsWith("u0_a")) {
            String data = str.substring(4);//remove u0_a starts str.
            Log.e("Test2", "data -- > " + data);
            int uid = Integer.valueOf(data) + 10000;
            return uid;
        }
        return -1;
    }


    private class BatteryAdapter extends BaseAdapter {
        private final LayoutInflater inflater;
        private Context context;
        private List<AppInfo> apps;

        public BatteryAdapter(Context mContext, List<AppInfo> apps) {
            this.context = mContext;
            this.apps = apps;
            inflater = LayoutInflater.from(BS_RankingActivity.this);
        }

        @Override
        public int getCount() {
            return apps.size();
        }

        @Override
        public AppInfo getItem(int i) {
            return apps.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder = null;
            if (convertView == null) {
                holder = new Holder();
                convertView = inflater.inflate(R.layout.bs_listview_item, null);
                holder.appIcon = (ImageView) convertView.findViewById(R.id.appIcon);
                holder.appName = (TextView) convertView.findViewById(R.id.appName);
                holder.txtProgress = (TextView) convertView.findViewById(R.id.txtProgress);
                holder.progress = (ProgressBar) convertView.findViewById(R.id.progress);
                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }

            AppInfo sipper = getItem(position);
            holder.appName.setText(sipper.getAppName());
            holder.appIcon.setImageDrawable(sipper.getIcon());

            double percentOfTotal = sipper.getPercent();
            holder.txtProgress.setText(String.valueOf(percentOfTotal));
            holder.progress.setProgress((int) percentOfTotal);
            return convertView;

        }
    }

    class Holder {
        ImageView appIcon;
        TextView appName;
        TextView txtProgress;
        ProgressBar progress;
    }
}
