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

public class SL_LockAdapter extends BaseAdapter {
    private LayoutInflater inflater ;
    private ArrayList<SL_LockSoftInfo> list  ;
    //public static Map<Integer, Boolean> isSelected = new HashMap<Integer,Boolean>();
    public SL_LockAdapter(Context context){
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        list  = new ArrayList<SL_LockSoftInfo>();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    public void setList(ArrayList<SL_LockSoftInfo> list){
        this.list = list;
        notifyDataSetChanged();
    }
    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

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

        holder.appIcon.setImageBitmap(list.get(position).getIcon());
        holder.appName.setText(list.get(position).getName());
        holder.rb_select.setImageResource(R.drawable.sl_un_lock);
        /*final ImageView cbox = holder.rb_select;
        holder.appLay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub


                AnimationSet animationSet = new AnimationSet(true);
                TranslateAnimation translateAnimation = new TranslateAnimation(
                        Animation.RELATIVE_TO_SELF, 0f,
                        Animation.RELATIVE_TO_SELF, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0f,
                        Animation.RELATIVE_TO_SELF, 0f);
                translateAnimation.setFillAfter(true);
                translateAnimation.setDuration(300);
                animationSet.addAnimation(translateAnimation);
                ((View) cbox.getParent()).startAnimation(animationSet);

                 Handler handler = new Handler();

                    handler.postDelayed(new Runnable() {

                        public void run() {

                            Message m = new Message();
                            m.what=2;
                            m.arg1 = position;
                            SL_LockAppListActivity.mHandler.sendMessage(m);
                            try{

                                database.delete(LocksoftDatabaseHelp.Locksoft_table,LocksoftDatabaseHelp.PACKAGE_NAME+"=?",new String[]{arrayList.get(position).getPackageName()});
                                for(int ii =0;ii<KedouSecurityService.list.size();ii++){
                                    if(KedouSecurityService.list.get(ii).getPackageName().equals(arrayList.get(position).getPackageName())){
                                        KedouSecurityService.list.remove(ii);
                                        break;
                                    }
                                }
                                arrayList.remove(position);
                                adapter.notifyDataSetChanged();
                                LockAppListActivity.mHandler.sendEmptyMessage(1);
                            }catch(Exception e){
                            }

                        }

                    }, 300);

            }
        });*/

        return view;

    }

}
