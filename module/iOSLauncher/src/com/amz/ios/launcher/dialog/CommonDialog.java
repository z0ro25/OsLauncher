package com.amz.ios.launcher.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.amz.ios.launcher.R;

public class CommonDialog extends Dialog {
    public static final int TYPE_CONFIRM = 1;
    public static final int TYPE_NORMAL = 0;

    public static class Builder {
        CommonDialog dialog;
        private Button mBtnNegative;
        private Button mBtnPositive;
        private FrameLayout mContent;
        private Context mContext;
        LayoutInflater mInflater;
        private String mMessage;
        private TextView mMessageView;
        private String mTitle;
        private TextView mTitleView;
        private int mType = 0;

        public Builder(Context context) {
            this.mContext = context;
        }

        public CommonDialog create() {
            this.mInflater = (LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.dialog = new CommonDialog(this.mContext, R.style.Common_Dialog);
            View inflate = this.mInflater.inflate(R.layout.common_dialog, (ViewGroup) null);
            this.mTitleView = (TextView) inflate.findViewById(R.id.title);
            this.mBtnPositive = (Button) inflate.findViewById(R.id.positiveButton);
            this.mBtnNegative = (Button) inflate.findViewById(R.id.negativeButton);
            this.mMessageView = (TextView) inflate.findViewById(R.id.message);
            this.mContent = (FrameLayout) inflate.findViewById(R.id.content);
            this.dialog.setContentView(inflate);
            return this.dialog;
        }

        public Builder setContentView(View view) {
            this.mContent.addView(view);
            this.mContent.setVisibility(View.VISIBLE);
            return this;
        }

        public Builder setLayoutView(View view) {
            this.dialog.setContentView(view);
            return this;
        }

        public Builder setMessage(int i) {
            this.mMessage = (String) this.mContext.getText(i);
            this.mMessageView.setVisibility(View.VISIBLE);
            this.mMessageView.setText(this.mMessage);
            return this;
        }

        public Builder setMessage(String str) {
            this.mMessage = str;
            this.mMessageView.setVisibility(View.VISIBLE);
            this.mMessageView.setText(this.mMessage);
            return this;
        }

        public Builder setNegativeButton(int i, final DialogInterface.OnClickListener onClickListener) {
            this.mBtnNegative.setText(i);
            this.mBtnNegative.setOnClickListener(new View.OnClickListener() {
                /* class com.amz.ios.ioslite.common.CommonDialog.Builder.AnonymousClass2 */

                public void onClick(View view) {
                    onClickListener.onClick(Builder.this.dialog, -2);
                }
            });
            return this;
        }

        public Builder setNegativeButton(String str, final DialogInterface.OnClickListener onClickListener) {
            this.mBtnNegative.setText(str);
            this.mBtnNegative.setOnClickListener(new View.OnClickListener() {
                /* class com.amz.ios.ioslite.common.CommonDialog.Builder.AnonymousClass3 */

                public void onClick(View view) {
                    onClickListener.onClick(Builder.this.dialog, -2);
                }
            });
            return this;
        }

        public Builder setPositiveButton(int i, final DialogInterface.OnClickListener onClickListener) {
            this.mBtnPositive.setText(i);
            this.mBtnPositive.setOnClickListener(new View.OnClickListener() {
                /* class com.amz.ios.ioslite.common.CommonDialog.Builder.AnonymousClass4 */

                public void onClick(View view) {
                    onClickListener.onClick(Builder.this.dialog, -1);
                }
            });
            return this;
        }

        public Builder setPositiveButton(String str, final DialogInterface.OnClickListener onClickListener) {
            this.mBtnPositive.setText(str);
            this.mBtnPositive.setOnClickListener(new View.OnClickListener() {
                /* class com.amz.ios.ioslite.common.CommonDialog.Builder.AnonymousClass5 */

                public void onClick(View view) {
                    onClickListener.onClick(Builder.this.dialog, -1);
                }
            });
            return this;
        }

        public Builder setSingleChoiceItems(CharSequence[] charSequenceArr, int i, final DialogInterface.OnClickListener onClickListener) {
            ListView listView = (ListView) this.mInflater.inflate(R.layout.common_preference_listview, (ViewGroup) null);
            listView.setAdapter((ListAdapter) new SingleChoiceAdapter(this.mContext, R.layout.common_dialog_singlechoice, charSequenceArr, i));
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                /* class com.amz.ios.ioslite.common.CommonDialog.Builder.AnonymousClass1 */

                @Override // android.widget.AdapterView.OnItemClickListener
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
                    onClickListener.onClick(Builder.this.dialog, i);
                }
            });
            this.mContent.addView(listView);
            this.mContent.setVisibility(View.VISIBLE);
            return this;
        }

        public Builder setTitle(int i) {
            this.mTitle = (String) this.mContext.getText(i);
            this.mTitleView.setText(this.mTitle);
            return this;
        }

        public Builder setTitle(String str) {
            this.mTitle = str;
            this.mTitleView.setText(this.mTitle);
            return this;
        }

        public void setType(int i) {
            this.mType = i;
            if (i == 1 && this.mBtnNegative != null) {
                this.mBtnNegative.setVisibility(View.GONE);
            }
        }

        public Builder showSingleButton() {
            this.mBtnPositive.setVisibility(View.GONE);
            this.mBtnNegative.setBackgroundResource(R.drawable.alert_dialog_button);
            return this;
        }
    }

    public static class SingleChoiceAdapter extends ArrayAdapter<CharSequence> {
        private int mCheckedIndex;
        private LayoutInflater mInflater;
        private int mResource;

        public SingleChoiceAdapter(Context context, int resourceId, CharSequence[] charSequenceArr, int checkedIndex) {
            super(context, resourceId, charSequenceArr);
            this.mResource = resourceId;
            this.mCheckedIndex = checkedIndex;
            this.mInflater = LayoutInflater.from(context);
        }

        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = this.mInflater.inflate(this.mResource, viewGroup, false);
            }

//            CheckedTextView checkedTextView = (CheckedTextView) view.findViewById(R.id.text1);
//            checkedTextView.setText((CharSequence) getItem(i));
//            if (i == this.mCheckedIndex) {
//                checkedTextView.setChecked(true);
//            } else {
//                checkedTextView.setChecked(false);
//            }
            return view;
        }

        public void setCheckedIndex(int i) {
            if (this.mCheckedIndex != i) {
                this.mCheckedIndex = i;
            }
        }
    }

    public CommonDialog(Context context) {
        super(context);
    }

    public CommonDialog(Context context, int i) {
        super(context, i);
    }
}
