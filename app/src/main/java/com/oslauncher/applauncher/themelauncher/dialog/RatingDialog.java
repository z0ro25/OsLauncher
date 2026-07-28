package com.oslauncher.applauncher.themelauncher.dialog;


import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.oslauncher.applauncher.themelauncher.R;
import com.oslauncher.applauncher.themelauncher.tool.languageTool.LanguageUtil;

public class RatingDialog extends Dialog {
    private OnPress onPress;
    private final RatingBar rtb;
    private final ImageView imgIcon;
    private final Context context;
    private final TextView btnRate;
    private final TextView btnNotNow;

    public RatingDialog(Context context2) {
        super(context2);
        this.context = context2;
        LanguageUtil.setLocale(context2);
        setContentView(R.layout.dialog_rating_app);

        getWindow().setGravity(Gravity.CENTER);
        getWindow().setLayout(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        rtb = findViewById(R.id.rtb);
        imgIcon = findViewById(R.id.imgIcon);
        btnRate = findViewById(R.id.btnRateUs);
        btnNotNow = findViewById(R.id.btnNotNow);
        onclick();
        changeRating();

    }

    public interface OnPress {
        void send(float star);

        void rating(float star);

        void cancel();

        void later();
    }

    public void init(OnPress onPress) {
        this.onPress = onPress;
    }

    public void changeRating() {
        rtb.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                String getRating = String.valueOf(rtb.getRating());
                switch (getRating) {
                    case "1.0":
                        btnRate.setText(context.getResources().getString(R.string.Thankyou));
                        btnNotNow.setVisibility(View.GONE);
                        imgIcon.setImageResource(R.drawable.rating_1);
                        break;
                    case "2.0":
                        btnNotNow.setVisibility(View.GONE);
                        btnRate.setText(context.getResources().getString(R.string.Thankyou));
                        imgIcon.setImageResource(R.drawable.rating_2);
                        break;
                    case "3.0":
                        btnNotNow.setVisibility(View.GONE);
                        btnRate.setText(context.getResources().getString(R.string.Thankyou));
                        imgIcon.setImageResource(R.drawable.rating_3);
                        break;
                    case "4.0":
                        btnNotNow.setVisibility(View.GONE);
                        btnRate.setText(context.getResources().getString(R.string.Thankyou));
                        imgIcon.setImageResource(R.drawable.rating_4);
                        break;
                    case "5.0":
                        btnNotNow.setVisibility(View.GONE);
                        btnRate.setText(context.getResources().getString(R.string.Thankyou));
                        imgIcon.setImageResource(R.drawable.rating_5);
                        break;
                    default:
                        btnRate.setText(context.getResources().getString(R.string.rate_us));
                        btnNotNow.setVisibility(View.VISIBLE);
                        imgIcon.setImageResource(R.drawable.rating_0);
                        break;
                }
            }
        });


    }

    public String getRating() {
        return String.valueOf(this.rtb.getRating());
    }

    public void onclick() {
        btnRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (rtb.getRating() == 0) {
                    Toast.makeText(context,context.getString(R.string.please_reate_use),Toast.LENGTH_LONG).show();
                    return;
                }
                if (rtb.getRating() <= 3.0) {
                    imgIcon.setVisibility(View.GONE);
                    onPress.send(rtb.getRating());
                } else {
                    imgIcon.setVisibility(View.VISIBLE);
                    onPress.rating(rtb.getRating());
                }
            }
        });

        btnNotNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPress.later();
            }
        });

    }

}
