package com.amz.ios.launcher.leftpage.widgets;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.leftpage.model.FavouriteContactInfo;
import com.amz.ios.launcher.leftpage.adapter.FavouriteContactAdapter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.Callable;

public class FavouriteContactWidget extends BlurConstraintLayoutWidget implements View.OnClickListener {

    Launcher mLauncher;
    int mMargin;

    RecyclerView mFavouriteContactOneRV;
    FavouriteContactAdapter mFavouriteContactOneAdapter;
    RecyclerView mFavouriteContactAllRV;
    FavouriteContactAdapter mFavouriteContactAllAdapter;

    AppCompatImageView mMoreIconIV;
    TextView mContactErrorTV;
    LinearLayoutCompat mPermissionRequestView;
    View mPermissionRequestBtn;

    ArrayList<FavouriteContactInfo> mOneContactList;
    ArrayList<FavouriteContactInfo> mAllContactList;
    boolean canReload;

    Handler mHandler;
    Runnable mReloadRunnable;

    ContentObserver mContactContentObserver;
    boolean u;


    public FavouriteContactWidget(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public FavouriteContactWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = (Launcher) context;
        mMargin = mLauncher.getDeviceProfile().edgeMarginPx;
        mReloadRunnable = new Runnable() {
            @Override
            public void run() {
                getFavouriteContacts();
            }
        };
        mHandler = new Handler();
        mContactContentObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean flag) {
                super.onChange(flag);
                if (canReload) {
                    mHandler.removeCallbacksAndMessages(null);
                    mHandler.postDelayed(mReloadRunnable, 5000L);
                }
            }
        };
        LayoutInflater.from(context).inflate(
                R.layout.favourite_contact_widget,
                this,
                true
        );
    }

    @Override
    public void setAppSuggestionViewMargin() {
        super.setAppSuggestionViewMargin();
        reload();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mLauncher.getContentResolver().unregisterContentObserver(this.mContactContentObserver);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setUpView();
        setAdapter();
        setListeners();
    }

    void setUpView(){
        mFavouriteContactOneRV = findViewById(R.id.favourite_contracts_one);
        mFavouriteContactAllRV = findViewById(R.id.favourite_contracts_all);
        mPermissionRequestBtn = findViewById(R.id.button_request_contact_permission);
        mMoreIconIV = findViewById(R.id.more_icon);
        mContactErrorTV = findViewById(R.id.contact_error);
        mPermissionRequestView = findViewById(R.id.favorite_contact_widget_permission);
    }

    void setAdapter(){
        mFavouriteContactAllAdapter = new FavouriteContactAdapter(mLauncher);
        mFavouriteContactAllRV.setAdapter(
                mFavouriteContactAllAdapter
        );
        mFavouriteContactAllRV.setLayoutManager(
                new GridLayoutManager(mLauncher,4, RecyclerView.VERTICAL,false)
        );

        mFavouriteContactOneAdapter = new FavouriteContactAdapter(mLauncher);
        mFavouriteContactOneRV.setAdapter(
                mFavouriteContactOneAdapter
        );
        mFavouriteContactOneRV.setLayoutManager(
                new GridLayoutManager(mLauncher,4,RecyclerView.VERTICAL,false)
        );
    }

    void setListeners(){
        mPermissionRequestBtn.setOnClickListener(this);
        mMoreIconIV.setOnClickListener(this);
    }

    public void getFavouriteContacts(){

        // TODO: 2023.11.21 Check Contact Load Permission
        boolean isGrantedPermission = true;

        if (isGrantedPermission){
            try {
                canReload = false;
                Integer num = new GetFavouriteContactCountCallable().call();
                postDelayed(new ReloadFavouriteContactRunnable(num),1000L);
            }
            catch (Throwable throwable){
                throwable.getMessage();
            }
        }
        else {
            this.mContactErrorTV.setVisibility(View.VISIBLE);
            this.mFavouriteContactOneRV.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ConstraintLayout constraintLayout = (ConstraintLayout) findViewById(R.id.favorite_contact_widget_content);
//        ((ConstraintLayout.LayoutParams) constraintLayout.getLayoutParams()).setMargins(mMargin, mMargin, mMargin, mMargin);
        setTextAndBackgroundColor(constraintLayout);
        ((ConstraintLayout.LayoutParams) mPermissionRequestView.getLayoutParams()).setMargins(mMargin, mMargin, mMargin, mMargin);
        setTextAndBackgroundColor(mPermissionRequestView);
        reload();
    }

    public void reload(){
        // TODO: 2023.11.21 Check permission
        boolean isGrantedPermission = true;

        if (isGrantedPermission){
            this.mContactErrorTV.setVisibility(View.GONE);
            this.mPermissionRequestBtn.setVisibility(View.GONE);
            this.mLauncher.getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_STREQUENT_URI, true, mContactContentObserver);
            if (!this.canReload || (this.mOneContactList != null && mOneContactList.size() <= 0)) {
                this.mHandler.removeCallbacksAndMessages(null);
                this.mHandler.postDelayed(this.mReloadRunnable, 1000L);
            }
        }
        else {
            this.mContactErrorTV.setVisibility(View.VISIBLE);
            this.mPermissionRequestBtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mPermissionRequestBtn){

        }
        else if (v == mMoreIconIV){
            boolean z = true;
            boolean z2 = !this.u;
            this.u = z2;
            RecyclerView rv;
            if (z2) {
                this.mMoreIconIV.animate().rotation(90.0f).setDuration(268L).start();
                rv = this.mFavouriteContactAllRV;
            } else {
                this.mMoreIconIV.animate().rotation(0.0f).setDuration(268L).start();
                rv = this.mFavouriteContactAllRV;
                z = false;
            }
            w(rv, z);
        }
    }

    public class GetFavouriteContactCountCallable implements Callable<Integer> {

        @SuppressLint("Range")
        @Override
        public Integer call() throws Exception {
            ArrayList<FavouriteContactInfo> arrayList;
            if (mOneContactList == null) {
                mOneContactList = new ArrayList<>();
            } else {
                mOneContactList.clear();
            }
            if (mAllContactList == null) {
                mAllContactList = new ArrayList<>();
            } else {
                mAllContactList.clear();
            }
            int i = 0;
            Cursor query = mLauncher.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, new String[]{"_id", "display_name", "starred"}, "starred='1'", null, null);
            if (query != null) {
                while (query.moveToNext()) {
                    FavouriteContactInfo cVar = new FavouriteContactInfo();
                    ArrayList<String> arrayList4 = new ArrayList<>();
                    cVar.a = query.getString(query.getColumnIndex("_id"));
                    cVar.b = query.getString(query.getColumnIndex("display_name"));
                    String str = cVar.a;
                    Bitmap bitmap = null;
                    try {
                        InputStream openContactPhotoInputStream = ContactsContract.Contacts.openContactPhotoInputStream(mLauncher.getContentResolver(), ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(str)));
                        Bitmap decodeStream = openContactPhotoInputStream != null ? BitmapFactory.decodeStream(openContactPhotoInputStream) : null;
                        if (openContactPhotoInputStream != null) {
                            openContactPhotoInputStream.close();
                        }
                        bitmap = decodeStream;
                    } catch (Throwable unused) {
                    }
                    cVar.mBmp = bitmap;
                    ContentResolver contentResolver = mLauncher.getContentResolver();
                    Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
                    String b = "contact_id=" + cVar.a;
                    Cursor query2 = contentResolver.query(uri, null, b, null, null);
                    if (query2 != null) {
                        while (query2.moveToNext()) {
                            arrayList4.add(query2.getString(query2.getColumnIndex("data1")));
                        }
                        query2.close();
                    }
                    cVar.c = arrayList4;
                    i++;
                    if (i <= 4)
                        arrayList = mOneContactList;
                    else
                        arrayList = mAllContactList;
                    arrayList.add(cVar);
                }
                query.close();
            }
            return i;
        }
    }

    public class ReloadFavouriteContactRunnable implements Runnable {

        public Integer mCountOfFavouriteContacts;

        public ReloadFavouriteContactRunnable(Integer num) {
            mCountOfFavouriteContacts = num;
        }

        @Override
        public void run() {
            if (mCountOfFavouriteContacts > 4){
                mFavouriteContactAllAdapter.setContacts(mAllContactList);
                mFavouriteContactOneAdapter.setContacts(mOneContactList);
                mFavouriteContactOneRV.setVisibility(View.VISIBLE);
                mMoreIconIV.setVisibility(View.VISIBLE);
                mMoreIconIV.setVisibility(View.VISIBLE);
            }
            else {
                mFavouriteContactOneAdapter.setContacts(mOneContactList);
                mMoreIconIV.setVisibility(View.GONE);
            }

            canReload = true;

            BlurConstraintLayoutWidget.setAllTextColor(
                (LinearLayout) FavouriteContactWidget.this.findViewById(R.id.expandable_layout),
                    Color.WHITE
            );
        }
    }

}
