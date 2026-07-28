package com.oslauncher.applauncher.themelauncher.Features.languageStart;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.oslauncher.applauncher.themelauncher.R;
import com.oslauncher.applauncher.themelauncher.model.LanguageModel;

import java.util.List;

public class LanguageStartAdapter extends RecyclerView.Adapter<LanguageStartAdapter.LangugeViewHolder> {
    private List<LanguageModel> languageModelList;
    private IClickItemLanguage iClickItemLanguage;
    private Context context;

    public LanguageStartAdapter(List<LanguageModel> languageModelList, IClickItemLanguage listener, Context context) {
        this.languageModelList = languageModelList;
        this.iClickItemLanguage = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public LangugeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_language_start, parent, false);
        return new LangugeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LangugeViewHolder holder, int position) {
        LanguageModel languageModel = languageModelList.get(position);
        if (languageModel == null) {
            return;
        }
        holder.tvLang.setText(languageModel.getLanguageName());
        if (languageModel.getActive()) {
            holder.layoutItem.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_circle_ff8c21_str_ff8c21));
            holder.rdbCheck.setImageDrawable(context.getDrawable(R.drawable.ic_dot_lang_select));
        } else {
            holder.layoutItem.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_circle_f5f5f5));
            holder.rdbCheck.setImageDrawable(context.getDrawable(R.drawable.ic_dot_lang_unselect));
        }

        if (languageModel.getCode().equals("en")) {
            Glide.with(context).load(R.drawable.ic_en).into(holder.icLang);
        } else if (languageModel.getCode().equals("de")) {
            Glide.with(context).load(R.drawable.ic_ger).into(holder.icLang);
        } else if (languageModel.getCode().equals("es")) {
            Glide.with(context).load(R.drawable.ic_span).into(holder.icLang);
        } else if (languageModel.getCode().equals("fr")) {
            Glide.with(context).load(R.drawable.ic_fr).into(holder.icLang);
        } else if (languageModel.getCode().equals("pt")) {
            Glide.with(context).load(R.drawable.ic_port).into(holder.icLang);
        } else if (languageModel.getCode().equals("zh")) {
            Glide.with(context).load(R.drawable.ic_china).into(holder.icLang);
        } else if (languageModel.getCode().equals("hi")) {
            Glide.with(context).load(R.drawable.ic_hi).into(holder.icLang);
        } else if (languageModel.getCode().equals("in")) {
            Glide.with(context).load(R.drawable.ic_indo).into(holder.icLang);
        }

        holder.layoutItem.setOnClickListener(v -> {
            iClickItemLanguage.onClickItemLanguage(languageModel.getCode());
            setCheck(languageModel.getCode());
        });

    }

    @Override
    public int getItemCount() {
        if (languageModelList != null) {
            return languageModelList.size();
        } else {
            return 0;
        }
    }

    public class LangugeViewHolder extends RecyclerView.ViewHolder {
        private ImageView rdbCheck;
        private TextView tvLang;
        private ConstraintLayout layoutItem;
        private ImageView icLang;
        private View divider;

        public LangugeViewHolder(@NonNull View itemView) {
            super(itemView);
            icLang = itemView.findViewById(R.id.icLang);
            tvLang = itemView.findViewById(R.id.txtName);
            layoutItem = itemView.findViewById(R.id.layoutItem);
            rdbCheck = itemView.findViewById(R.id.iv_select_lang);
        }
    }

    public void setCheck(String code) {
        for (LanguageModel item : languageModelList) {
            if (item.getCode().equals(code)) {
                item.setActive(true);
            } else {
                item.setActive(false);
            }
        }
        notifyDataSetChanged();
    }
}

