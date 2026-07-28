package com.amz.ios.ioslite.common.ad;

import androidx.annotation.NonNull;

public class NativeAdViewBinder {

    public final static class Builder {
        private final int layoutId;
        private int titleTextId;
        private int desTextId;
        private int callBtnId;
        private int bigImageId;
        private int iconImageId;
        private int starRatingId;

        /**
         * the layout of the AD View 布局文件资源id
         */
        public Builder(final int layoutId) {
            this.layoutId = layoutId;
        }

        /**
         * the title of the AD View 广告标题
         */
        @NonNull
        public final Builder titleTextId(final int titleId) {
            this.titleTextId = titleId;
            return this;
        }

        /**
         * ad describe 广告介绍
         */
        @NonNull
        public final Builder desTextId(final int textId) {
            this.desTextId = textId;
            return this;
        }

        /**
         * ad button 广告按钮
         */
        @NonNull
        public final Builder callBtnId(final int callToActionId) {
            this.callBtnId = callToActionId;
            return this;
        }

        /**
         * ad big image 广告大图
         */
        @NonNull
        public final Builder bigImageId(final int bigImageId) {
            this.bigImageId = bigImageId;
            return this;
        }

        /**
         * the icon of the AD View 广告的icon
         */
        @NonNull
        public final Builder iconImageId(final int iconImageId) {
            this.iconImageId = iconImageId;
            return this;
        }

        /**
         * the star rating of the AD View 广告星级
         */
        @NonNull
        public final Builder starRatingId(final int starRatingId) {
            this.starRatingId = starRatingId;
            return this;
        }


        @NonNull
        public final NativeAdViewBinder build() {
            return new NativeAdViewBinder(this);
        }
    }

    public final int layoutId;
    public final int titleTextId;
    public final int desTextId;
    public final int callBtnId;
    public final int bigImageId;
    public final int iconImageId;
    public final int starRatingId;

    private NativeAdViewBinder(@NonNull final Builder builder) {
        this.layoutId = builder.layoutId;
        this.titleTextId = builder.titleTextId;
        this.desTextId = builder.desTextId;
        this.callBtnId = builder.callBtnId;
        this.bigImageId = builder.bigImageId;
        this.iconImageId = builder.iconImageId;
        this.starRatingId = builder.starRatingId;
    }
}
