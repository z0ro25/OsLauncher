package com.zhuoyi.security.soft.lock;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.zhuoyi.security.batterysave.R;

public class SL_UnlockAdapter extends BaseAdapter {
    private Context context;
    private LayoutInflater inflater ;
    private ArrayList<SL_LockSoftInfo> listArr = new ArrayList<SL_LockSoftInfo>();
    //public static Map<Integer, Boolean> isSelected = new HashMap<Integer,Boolean>();
    public SL_UnlockAdapter(Context context){
        this.context = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return listArr.size();
    }

    public void setList(ArrayList<SL_LockSoftInfo> list){
        listArr = list;
        notifyDataSetChanged();
    }
    @Override
    public Object getItem(int position) {
        return listArr.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    public static View viewsss;
    @Override
    public View getView(final int position, View view,ViewGroup parent) {

        SL_LockHookView holder = null;
        if(view==null){
            holder=new SL_LockHookView();
            view = inflater.inflate(R.layout.sl_app_list_unlock_item, null);
            holder.appIcon=(ImageView)view.findViewById(R.id.sl_unlock_icon);
            holder.appName = (TextView)view.findViewById(R.id.sl_unlock_appname);
            holder.rb_select = (ImageView)view.findViewById(R.id.sl_show_unlock_item_cbox);
            holder.appLay = (RelativeLayout)view.findViewById(R.id.sl_show_unlock_item);
            view.setTag(holder);
        }else{
            holder = (SL_LockHookView)view.getTag();
        }

        holder.appIcon.setImageBitmap(listArr.get(position).getIcon());
        holder.appName.setText(listArr.get(position).getName());
        holder.rb_select.setImageResource(R.drawable.sl_add_lock);
        /*final ImageView cbox = holder.rb_select;
        holder.appLay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                AnimationSet animationSet = new AnimationSet(true);
                TranslateAnimation translateAnimation = new TranslateAnimation(
                        Animation.RELATIVE_TO_SELF, 0f,
                        Animation.RELATIVE_TO_SELF,-1f,
                        Animation.RELATIVE_TO_SELF, 0f,
                        Animation.RELATIVE_TO_SELF, 0f);
                translateAnimation.setFillAfter(true);
                translateAnimation.setDuration(200);
                animationSet.addAnimation(translateAnimation);
                ((View) cbox.getParent()).startAnimation(animationSet);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {

                    public void run() {
                        Message m = new Message();
                        m.arg1 = position;
                        m.what = 4;
                        SL_UnLocksoftListActivity.mHandler.sendMessage(m);
                    }

                }, 200);
            }
        });*/
        return view;
    }

}
