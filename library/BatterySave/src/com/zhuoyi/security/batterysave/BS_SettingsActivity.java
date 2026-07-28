package com.zhuoyi.security.batterysave;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.zhuoyi.security.batterysave.util.BS_SettingsUtil;
import com.zhuoyi.security.batterysave.util.BS_Utils;
import com.zhuoyi.security.batterysave.views.BS_TitleBar;

public class BS_SettingsActivity extends Activity implements BS_TitleBar.CallBack{

    private String TAG = "BatterySwitcherSettingsActivity";
    private Context mContext = null;
    private ListView mBList;
    private BatteryListAdapter mBAdapter;
    private List<BS_SwitcherInfo> mData = null;

    int[] icons = {//R.drawable.bs_ic_clean,
            R.drawable.bs_ic_gps,
            R.drawable.bs_ic_synchronization,
            R.drawable.bs_ic_light,
            R.drawable.bs_ic_rotate,
            R.drawable.bs_ic_overtime,
            R.drawable.bs_ic_shock,
            R.drawable.bs_ic_data,
            R.drawable.bs_ic_wifi_ap,
            R.drawable.bs_ic_wlan,
            R.drawable.bs_ic_blue,
            R.drawable.bs_ic_bell,
            R.drawable.bs_ic_feedback
    };
    
    String[] min = {//"18",
            "31",
            "6",
            "12",
            "1",
            "2",
            "1",
            "59",
            "37",
            "31",
            "12",
            "1",
            "1"
    };
    
    String[] mark = {//"clean",
            "gps",
            "syn",
            "light",
            "rotate",
            "overtime",
            "vibrate",
            "data",
            "wifiap",
            "wifi",
            "bluetooth",
            "ringer",
            "touch"
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = BS_SettingsActivity.this;
        setContentView(R.layout.bs_settings);
        BS_TitleBar titleBar = (BS_TitleBar) findViewById(R.id.bs_settings_bar);
        titleBar.setOnCallBack(this);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateData();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void initView() {
        mBList = (ListView) findViewById(R.id.bs_switcher_settings_list);
        mBList.setOnItemClickListener(new ListItemClick());
        mData = new ArrayList<BS_SwitcherInfo>();
        String[] items = mContext.getResources().getStringArray(R.array.bs_switch_setting);
        if(items.length > 0) {
            for (int i= 0; i<items.length;i++) {
                BS_SwitcherInfo info = new BS_SwitcherInfo(icons[i], items[i], min[i], true, mark[i], 0);
                mData.add(info);
            }
        }
        mBAdapter = new BatteryListAdapter(mData);
        mBList.setAdapter(mBAdapter);
    }

    private void updateData() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB  ){
            new AsyncTask_Data().executeOnExecutor(Executors.newCachedThreadPool(),getApplicationContext());
        }else{
            new AsyncTask_Data().execute(getApplicationContext());
        }
    }
    
    private class AsyncTask_Data extends AsyncTask<Context, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Context... params) {
            // TODO Auto-generated method stub
            for (BS_SwitcherInfo bsInfo : mData) {
                switch (bsInfo.getMark()) {
                    case "clean":
                        break;
                    case "gps":
                        bsInfo.setStarte(BS_SettingsUtil.gpsCheck(mContext));
                        break;
                    case "syn":
                        bsInfo.setStarte(BS_SettingsUtil.isSyncSwitchOn(mContext));
                        break;
                    case "light":
                        bsInfo.setStarte(BS_SettingsUtil.brightnessCheck(mContext));
                        break;
                    case "rotate":
                        bsInfo.setStarte(BS_SettingsUtil.rotationCheck(mContext));
                        break;
                    case "overtime":
                        bsInfo.setStarte(BS_SettingsUtil.timeCheck(mContext));
                        break;
                    case "vibrate":
                        getRingerMode(bsInfo);
                        break;
                    case "data":
                        bsInfo.setStarte(BS_SettingsUtil.getMobileDataState(mContext));
                        break;
                    case "wifiap":
                        bsInfo.setStarte(BS_SettingsUtil.isWifiApEnabled(mContext));
                        break;
                    case "wifi":
                        bsInfo.setStarte(BS_SettingsUtil.wifiCheck(mContext));
                        break;
                    case "bluetooth":
                        bsInfo.setStarte(BS_SettingsUtil.bluetoothCheck());
                        break;
                    case "ringer":
                        getRingerMode(bsInfo);
                        break;
                    case "touch":
                        bsInfo.setStarte(BS_SettingsUtil.vibrateCheck(mContext));
                        break;
                default:
                    break;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            Collections.sort(mData, new Comparator<BS_SwitcherInfo>(){  
                public int compare(BS_SwitcherInfo o1, BS_SwitcherInfo o2) {  
                    if(o1.getStarte() == false && o2.getStarte() == true){  
                        return 1;  
                    }  
                    if(o1.getStarte() == o2.getStarte()){  
                        return 0;  
                    }  
                    return -1;  
                }  
            });  
            mBAdapter.notifyDataSetChanged();
        }
    }
    
    void getRingerMode (BS_SwitcherInfo bsInfo) {
        int ringer = BS_SettingsUtil.getRingerMode(mContext);
        Log.e(TAG,"ringer="+ringer);
        switch (ringer) {
            case AudioManager.RINGER_MODE_SILENT:
                bsInfo.setStarte(false);
                BS_SettingsUtil.setSwitcherSet(mContext, "switcher_vibrate", 1);
                BS_SettingsUtil.setSwitcherSet(mContext, "switcher_ringer", 1);
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                if (("vibrate").equals(bsInfo.getMark())) {
                    bsInfo.setStarte(true);
                } else {
                    bsInfo.setStarte(false);
                }
                BS_SettingsUtil.setSwitcherSet(mContext, "switcher_vibrate", 0);
                BS_SettingsUtil.setSwitcherSet(mContext, "switcher_ringer", 1);
                break;
            case AudioManager.RINGER_MODE_NORMAL:
                bsInfo.setStarte(true);
                BS_SettingsUtil.setSwitcherSet(mContext, "switcher_vibrate", 0);
                BS_SettingsUtil.setSwitcherSet(mContext, "switcher_ringer", 0);
                break;
        }
    }

    class ListItemClick implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            BS_SwitcherInfo info = (BS_SwitcherInfo)mBAdapter.getItem(position);
            //long leftTime = BS_Utils.getUnchargeLeftTime(BS_SettingsActivity.this);
            Log.d(TAG,"position="+position+" info="+info.getMark());
            boolean statB = info.getStarte();
            boolean statA = switcherState(info);
            if (statB != statA) {
                info.setStarte(statA);
                if (statA) {
                    info.setTvState(2);
                    //leftTime -= Long.parseLong(info.getExplain())*60*1000;
                    //BS_FileUtils.initData("----"+info.getExplain()+"==="+leftTime);
                } else {
                    info.setTvState(1);
                   // leftTime += Long.parseLong(info.getExplain())*60*1000;
                    //BS_FileUtils.initData("+++++"+info.getExplain()+"==="+leftTime);
                }
                //BS_Utils.setUnchargeLeftTime(BS_SettingsActivity.this,leftTime);
                mBAdapter.notifyDataSetChanged();
            }
        }
    }
    
    boolean switcherState (BS_SwitcherInfo bsInfo) {
        switch (bsInfo.getMark()) {
            case "clean":
                break;
            case "gps":
                return BS_SettingsUtil.gpsClick(mContext);
            case "syn":
                return BS_SettingsUtil.syncSwitchUtils(mContext);
            case "light":
                return BS_SettingsUtil.brightnessClick(mContext);
            case "rotate":
                return BS_SettingsUtil.rotationClick(mContext);
            case "overtime":
                return BS_SettingsUtil.timeClick(mContext);
            case "vibrate":
                BS_SettingsUtil.ringerClick(mContext, 1);
                return BS_SettingsUtil.getSwitcherSet(mContext, "switcher_vibrate") == 0;
            case "data":
                return BS_SettingsUtil.dataClick(mContext);
            case "wifiap":
                return BS_SettingsUtil.setWifiApEnabled(mContext);
            case "wifi":
                return BS_SettingsUtil.wifiClick(mContext);
            case "bluetooth":
                return BS_SettingsUtil.bluetoothClick(mContext);
            case "ringer":
                BS_SettingsUtil.ringerClick(mContext, 2);
                return BS_SettingsUtil.getSwitcherSet(mContext, "switcher_ringer") == 0;
            case "touch":
                return BS_SettingsUtil.vibrateClick(mContext);
        }
        return false;
    }

    private class BatteryListAdapter extends BaseAdapter {
        List<BS_SwitcherInfo> mData;
        private BatteryListAdapter(List<BS_SwitcherInfo> mList) {
            mData = mList;
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewCache holder ;
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.bs_battery_listview_item, parent, false);
                holder = new ViewCache();
                holder.bIcon = (ImageView) convertView.findViewById(R.id.bs_icon);
                holder.bSwitcher = (TextView) convertView.findViewById(R.id.bs_switcher);
                holder.bSExplain = (TextView) convertView.findViewById(R.id.bs_switcher_explain);
                holder.bSExplains = (TextView) convertView.findViewById(R.id.bs_explain_state);
                holder.bState = (ImageView) convertView.findViewById(R.id.bs_state);
                convertView.setTag(holder);
            } else {
                holder = (ViewCache) convertView.getTag();
            }
            BS_SwitcherInfo info = mData.get(position);
            holder.bIcon.setImageResource(info.getIcon());
            holder.bSwitcher.setText(info.getSwitcher());
            holder.bSExplains.setText("+");
            holder.bSExplain.setText(info.getExplain()+getResources().getString(R.string.bs_min));
            
            if (!info.getStarte()) {
                if (info.getTvState() == 1) {
                    holder.bSExplains.setText("+");
                    holder.bSExplains.setTextColor(getResources().getColor(R.color.bs_on));
                    holder.bSExplain.setTextColor(getResources().getColor(R.color.bs_on));
                    holder.bSExplain.setVisibility(View.VISIBLE);
                    holder.bSExplains.setVisibility(View.VISIBLE);
                } else {
                    holder.bSExplain.setVisibility(View.GONE);
                    holder.bSExplains.setVisibility(View.GONE);
                }
                holder.bState.setVisibility(View.VISIBLE);
            } else {
                if (info.getTvState() == 2) {
                    holder.bSExplains.setText("-");
                    holder.bSExplains.setTextColor(getResources().getColor(R.color.bs_off));
                    holder.bSExplain.setTextColor(getResources().getColor(R.color.bs_off));
                } else {
                    holder.bSExplain.setTextColor(getResources().getColor(R.color.bs_line));
                    holder.bSExplains.setTextColor(getResources().getColor(R.color.bs_line));
                }
                holder.bSExplain.setVisibility(View.VISIBLE);
                holder.bSExplains.setVisibility(View.VISIBLE);
                holder.bState.setVisibility(View.INVISIBLE);
            }
            return convertView;
        }
        class ViewCache {
            ImageView bIcon;
            TextView bSwitcher;
            TextView bSExplain;
            TextView bSExplains;
            ImageView bState;
        }
    }

    @Override
    public void onLeftClick() {
        // TODO Auto-generated method stub
        finish();
    }

    @Override
    public void onCenterClick() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onRightClick() {
        // TODO Auto-generated method stub
        
    }

}
