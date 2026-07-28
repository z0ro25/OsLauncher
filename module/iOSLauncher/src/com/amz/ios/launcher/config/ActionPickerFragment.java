package com.amz.ios.launcher.config;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.amz.ios.ioslite.common.widget.SmoothCheckBox;
import com.amz.ios.launcher.AppInfo;
import com.amz.ios.launcher.IconCache;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.views.CustomTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by server on 17-3-14.
 */
public class ActionPickerFragment extends Fragment {

    public static final int TYPE_IOS_SHORTCUT = 1;
    public static final int TYPE_APPLICATION = 2;
    private static final String ARGUMENT_ACTION = "action_type";
    private static String mActionDesc;

    private int mActionType;
    private RecyclerView mRecyclerView;


    public static ActionPickerFragment newInstance(int argument, String actionDesc) {
        Bundle bundle = new Bundle();
        bundle.putInt(ARGUMENT_ACTION, argument);
        ActionPickerFragment contentFragment = new ActionPickerFragment();
        contentFragment.setArguments(bundle);
        mActionDesc = actionDesc;
        return contentFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            mActionType = bundle.getInt(ARGUMENT_ACTION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRecyclerView =
                (RecyclerView) inflater.inflate(R.layout.gesture_action_picker_view, container, false);
        return mRecyclerView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mRecyclerView.getContext()));
        RecyclerViewAdapter adapter;
        ArrayList infos = new ArrayList();

        if (mActionType == TYPE_IOS_SHORTCUT) {
            Context context = getContext();
            infos.add(new GestureEventModel.BlankItemInfo(context));
            infos.add(new GestureEventModel.AlarmShowItemInfo(context));
            infos.add(new GestureEventModel.HiddenFolderShowItemInfo(context));
            infos.add(new GestureEventModel.SearchShowItemInfo(context));
            infos.addAll(LauncherAppState.getInstance().getModel().getIOSShortcut());
        } else if (mActionType == TYPE_APPLICATION) {
            infos.addAll(LauncherAppState.getInstance().getModel().getAllAppInfo());
        }
        adapter = new RecyclerViewAdapter(getContext(), infos);
        mRecyclerView.setAdapter(adapter);
    }


    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

        private Context mContext;
        private List<Object> mActionInfos;
        private IconCache mIconCache;

        public RecyclerViewAdapter(Context context, List<Object> infos) {
            mContext = context;
            mActionInfos = infos;
            mIconCache = LauncherAppState.getInstance().getIconCache();
        }

        @Override
        public RecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view =
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.gesture_action_picker_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final RecyclerViewAdapter.ViewHolder holder, int position) {
            final View view = holder.mContent;
            ImageView icon = (ImageView) view.findViewById(R.id.icon);
            CustomTextView label = (CustomTextView) view.findViewById(R.id.label);
            SmoothCheckBox checkBox = (SmoothCheckBox) view.findViewById(R.id.checkbox);
            final Object object = mActionInfos.get(position);
            String actionDesc = null;
            if (object instanceof AppInfo) {
                AppInfo info = (AppInfo) object;
                icon.setImageBitmap(info.iconBitmap);
                label.setText(info.title);
                actionDesc = info.title.toString();
            } else if (object instanceof ResolveInfo) {
                ResolveInfo info = (ResolveInfo) object;
                mIconCache.applyIOSShortcut(info, icon, label);
                actionDesc = label.getText().toString();
            } else if (object instanceof GestureEventModel.BlankItemInfo) {
                GestureEventModel.BlankItemInfo info = (GestureEventModel.BlankItemInfo) object;
                label.setText(info.getDescription());
                icon.setImageDrawable(info.getDrawable());
                actionDesc = info.getDescription();
            }
            if (TextUtils.equals(actionDesc, mActionDesc)) {
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(true);
            }else {
                checkBox.setVisibility(View.GONE);
            }
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().setResult(Activity.RESULT_OK, createActionIntent(mContext, object));
                    getActivity().finish();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mActionInfos.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public View mContent;

            public ViewHolder(View v) {
                super(v);
                mContent = v;
            }
        }
    }

    private Intent createActionIntent(Context context, Object object) {
        Intent resultIntent = new Intent();
        Bundle bundle = new Bundle();
        Intent action = new Intent();
        if (object instanceof AppInfo) {
            AppInfo info = (AppInfo) object;
            bundle.putString(GestureEventModel.GESTURE_ACTION_DES, info.title.toString());
            bundle.putString(GestureEventModel.GESTURE_ACTION_URI, info.intent.toUri(0));
        } else if (object instanceof ResolveInfo) {
            ResolveInfo info = (ResolveInfo) object;
            action.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
            action.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            bundle.putString(GestureEventModel.GESTURE_ACTION_URI, action.toUri(0));
            bundle.putString(GestureEventModel.GESTURE_ACTION_DES, info.loadLabel(context.getPackageManager()).toString());
        } else if (object instanceof GestureEventModel.BlankItemInfo) {
            GestureEventModel.BlankItemInfo info = (GestureEventModel.BlankItemInfo) object;
            bundle.putString(GestureEventModel.GESTURE_ACTION_DES, info.getDescription());
            bundle.putString(GestureEventModel.GESTURE_ACTION_URI, info.getURI());
        }
        resultIntent.putExtras(bundle);
        return resultIntent;
    }
}
