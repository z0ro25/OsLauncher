package com.amz.ios.launcher.bounce;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.widget.EdgeEffect;

import androidx.dynamicanimation.animation.SpringAnimation;

import static androidx.recyclerview.widget.RecyclerView.EdgeEffectFactory.DIRECTION_BOTTOM;

public class BounceEdgeEffect extends EdgeEffect {

    SpringAnimation spring;
    View view;
    int direction;
    float flingSize;
    float overScrollSize;

    public BounceEdgeEffect(Context context, SpringAnimation spring, View view, int direction, float flingSize, float overScrollSize){
        super(context);
        this.spring = spring;
        this.view = view;
        this.direction = direction;
        this.flingSize = flingSize;
        this.overScrollSize = overScrollSize;
    }

    @Override
    public void onPull(float deltaDistance) {
        super.onPull(deltaDistance);
        onPullAnimation(deltaDistance);
    }

    @Override
    public void onPull(float deltaDistance, float displacement) {
        super.onPull(deltaDistance, displacement);
        onPullAnimation(deltaDistance);
    }

    private void onPullAnimation(float deltaDistance){
        float delta = 0.0f;
        if (direction == DIRECTION_BOTTOM)
            delta = -1 * view.getWidth() * deltaDistance * overScrollSize;
        else delta = 1 * view.getWidth() * deltaDistance * overScrollSize;
        spring.cancel();
        float translationY = view.getTranslationY();
        view.setTranslationY(
                translationY + delta
        );
    }

    @Override
    public void onRelease() {
        super.onRelease();
        spring.start();
    }

    @Override
    public void onAbsorb(int velocity) {
        super.onAbsorb(velocity);
        float v = direction == DIRECTION_BOTTOM ? -1 * velocity * flingSize : 1 * velocity * flingSize;
        spring.setStartVelocity(v).start();
    }

    @Override
    public boolean draw(Canvas canvas) {
        setSize(0,0);
        return super.draw(canvas);
    }
}
