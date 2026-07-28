package com.oslauncher.applauncher.themelauncher.Features.lang;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.oslauncher.applauncher.themelauncher.Features.languageStart.IClickItemLanguage;
import com.oslauncher.applauncher.themelauncher.R;
import com.oslauncher.applauncher.themelauncher.model.LanguageModel;

import java.util.List;


public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.LangugeViewHolder> {
    private Context context;
    private List<LanguageModel> languageModelList;
    private IClickItemLanguage iClickItemLanguage;

    public LanguageAdapter(List<LanguageModel> languageModelList, IClickItemLanguage listener, Context context) {
        this.languageModelList = languageModelList;
        this.iClickItemLanguage = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public LangugeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_language, parent, false);
        return new LangugeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LangugeViewHolder holder, int position) {
        LanguageModel languageModel = languageModelList.get(position);
        if (languageModel == null) {
            return;
        }
        holder.txtName.setText(languageModel.getLanguageName());
        if (languageModel.getActive()) {
            holder.layoutItem.setBackground(context.getDrawable(R.drawable.bg_circle_ff8c21_str_ff8c21));
            holder.imgCheck.setImageResource(R.drawable.ic_dot_lang_select);
        } else {
            holder.layoutItem.setBackground(context.getDrawable(R.drawable.bg_circle_f5f5f5));
            holder.imgCheck.setImageResource(R.drawable.ic_dot_lang_unselect);
        }
        if (languageModel.getCode().equals("en")) {
            Glide.with(context)
                    .load(R.drawable.ic_en)
                    .into(holder.icLang);
        } else if (languageModel.getCode().equals("de")) {
            Glide.with(context)
                    .load(R.drawable.ic_ger)
                    .into(holder.icLang);
        } else if (languageModel.getCode().equals("es")) {
            Glide.with(context)
                    .load(R.drawable.ic_span)
                    .into(holder.icLang);
        } else if (languageModel.getCode().equals("fr")) {
            Glide.with(context)
                    .load(R.drawable.ic_fr)
                    .into(holder.icLang);
        } else if (languageModel.getCode().equals("pt")) {
            Glide.with(context)
                    .load(R.drawable.ic_port)
                    .into(holder.icLang);
        } else if (languageModel.getCode().equals("zh")) {
            Glide.with(context)
                    .load(R.drawable.ic_china)
                    .into(holder.icLang);
        } else if (languageModel.getCode().equals("hi")) {
            Glide.with(context)
                    .load(R.drawable.ic_hi)
                    .into(holder.icLang);
        } else if (languageModel.getCode().equals("in")) {
            Glide.with(context)
                    .load(R.drawable.ic_indo)
                    .into(holder.icLang);
        }

        holder.layoutItem.setOnClickListener(v -> {
            iClickItemLanguage.onClickItemLanguage(languageModel.getCode());
            for (LanguageModel i : languageModelList) {
                if (i == languageModel) {
                    languageModel.setActive(true);
                } else i.setActive(false);
            }
            notifyDataSetChanged();
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
        private ImageView imgCheck;
        private TextView txtName;
        private ConstraintLayout layoutItem;
        private ImageView icLang;
        private View divider;

        public LangugeViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCheck = itemView.findViewById(R.id.imgCheck);
            txtName = itemView.findViewById(R.id.txtName);
            layoutItem = itemView.findViewById(R.id.layoutItem);
            icLang = itemView.findViewById(R.id.iv_lang);
            divider = itemView.findViewById(R.id.divider);
        }
    }

    public void setCheck(String code) {
        boolean check = false;
        for (LanguageModel item : languageModelList) {
            if (item.getCode().equals(code)) {
                item.setActive(true);
                check = true;
            } else {
                item.setActive(false);
            }
        }
        if (!check) {
            for (LanguageModel item : languageModelList) {
                if (item.getCode().equals("en")) {
                    item.setActive(true);
                }
            }
            notifyDataSetChanged();
        }

    }
}

