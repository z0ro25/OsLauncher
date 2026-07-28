package com.amz.ios.launcher.bounce;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.EdgeEffect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.launcher.R;

public class BouncyRecyclerView extends RecyclerView {

    private DragDropCallBack callBack;
    OnOverPullListener onOverPullListener = null;
    float overScrollAnimationSize = .5f;
    float flingAnimationSize = .5f;
    int orientation = 1;
    float dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY;
    float stiffness = SpringForce.STIFFNESS_LOW;
    SpringAnimation spring = new SpringAnimation(this,SpringAnimation.TRANSLATION_Y)
                            .setSpring(
                                    new SpringForce().setFinalPosition(0f).setDampingRatio(dampingRatio).setStiffness(stiffness)
                            );
    boolean longPressDragEnabled = false;

    @SuppressLint("ResourceType")
    public BouncyRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.BouncyRecyclerView,0,0);
        longPressDragEnabled = a.getBoolean(R.styleable.BouncyRecyclerView_allow_drag_reorder, false);
        itemSwipeEnabled = a.getBoolean(R.styleable.BouncyRecyclerView_allow_item_swipe, false);
        overScrollAnimationSize = a.getFloat(R.styleable.BouncyRecyclerView_recyclerview_overscroll_animation_size, 0.5f);
        flingAnimationSize = a.getFloat(R.styleable.BouncyRecyclerView_recyclerview_fling_animation_size, 0.5f);

        switch (a.getInt(R.styleable.BouncyRecyclerView_recyclerview_damping_ratio, 0))
        {
            case 0:dampingRatio = Bouncy.DAMPING_RATIO_NO_BOUNCY;break;
            case 1:dampingRatio = Bouncy.DAMPING_RATIO_LOW_BOUNCY;break;
            case 2:dampingRatio = Bouncy.DAMPING_RATIO_MEDIUM_BOUNCY;break;
            case 3:dampingRatio = Bouncy.DAMPING_RATIO_HIGH_BOUNCY;break;
        }
        switch (a.getInt(R.styleable.BouncyRecyclerView_recyclerview_stiffness, 1))
        {
            case 0: stiffness = Bouncy.STIFFNESS_VERY_LOW;break;
            case 1: stiffness = Bouncy.STIFFNESS_LOW;break;
            case 2: stiffness = Bouncy.STIFFNESS_MEDIUM;break;
            case 3: stiffness = Bouncy.STIFFNESS_HIGH;break;
        }
        a.recycle();

        setEdgeEffectFactory(
                new EdgeEffectFactory(){
                    @NonNull
                    @Override
                    protected EdgeEffect createEdgeEffect(@NonNull final RecyclerView view, final int direction) {
                        return new EdgeEffect(view.getContext()){
                            @Override
                            public void onPull(float deltaDistance) {
                                super.onPull(deltaDistance);
                            }

                            @Override
                            public void onPull(float deltaDistance, float displacement) {
                                super.onPull(deltaDistance, displacement);
                                onPullAnimation(deltaDistance);
                                if (direction == DIRECTION_BOTTOM)
                                    if (onOverPullListener != null)
                                        onOverPullListener.onOverPulledBottom(deltaDistance);
                                else if (direction == DIRECTION_TOP)
                                    if (onOverPullListener!= null)
                                        onOverPullListener.onOverPulledTop(deltaDistance);
                            }

                            @Override
                            public void onRelease() {
                                super.onRelease();
                                if (touched) return;
                                if (onOverPullListener != null)
                                    onOverPullListener.onRelease();
                                spring.start();
                                forEachVisibleHolder(
                                        view,
                                        new ViewHolderProcess() {
                                            @Override
                                            public void handleProcess(ViewHolder viewHolder) {
                                                if (viewHolder instanceof BouncyViewHolder)
                                                    ((BouncyViewHolder) viewHolder).onRelease();
                                            }
                                        }
                                );

                            }

                            private void onPullAnimation(float distance){
                                float delta = 0.0f;
                                if (orientation == VERTICAL)
                                {
                                    if (direction == DIRECTION_BOTTOM)
                                        delta = -1 * view.getWidth() * distance * overScrollAnimationSize;
                                    else
                                        delta = 1 * view.getWidth() * distance * overScrollAnimationSize;
                                }
                                else
                                {
                                    if (direction == DIRECTION_RIGHT)
                                        delta = -1 * view.getWidth() * distance * overScrollAnimationSize;
                                    else
                                        delta =1 * view.getWidth() * distance * overScrollAnimationSize;
                                }
                                BouncyRecyclerView.this.setTranslationY(
                                        BouncyRecyclerView.this.getTranslationY() + delta
                                );
                                spring.cancel();
                                final float deltaDistance = delta;
                                forEachVisibleHolder(
                                        view,
                                        new ViewHolderProcess() {
                                            @Override
                                            public void handleProcess(ViewHolder viewHolder) {
                                                if (viewHolder instanceof BouncyViewHolder)
                                                    ((BouncyViewHolder) viewHolder).onPulled(deltaDistance);
                                            }
                                        }
                                );
                            }

                            @Override
                            public void onAbsorb(int velocity) {
                                super.onAbsorb(velocity);
                                float v;
                                if (orientation == VERTICAL)
                                {
                                    v = (direction == DIRECTION_BOTTOM) ?
                                            -1 * velocity * flingAnimationSize :
                                            1 * velocity * flingAnimationSize;
                                }
                                else
                                {
                                    v = direction == DIRECTION_RIGHT ?
                                            -1 * velocity * flingAnimationSize :
                                            1 * velocity * flingAnimationSize;
                                }
                                spring.setStartVelocity(v).start();

                                final int deltaDistance = velocity;
                                forEachVisibleHolder(
                                        view,
                                        new ViewHolderProcess() {
                                            @Override
                                            public void handleProcess(ViewHolder viewHolder) {
                                                if (viewHolder instanceof BouncyViewHolder)
                                                    ((BouncyViewHolder) viewHolder).onAbsorb(deltaDistance);
                                            }
                                        }
                                );
                            }

                            @Override
                            public boolean draw(Canvas canvas) {
                                setSize(0, 0);
                                return super.draw(canvas);
                            }
                        };
                    }
                }
        );
    }

    public OnOverPullListener getOnOverPullListener() {
        return onOverPullListener;
    }

    public void setOnOverPullListener(OnOverPullListener onOverPullListener) {
        this.onOverPullListener = onOverPullListener;
    }

    public void setDirection(int direction){
        this.orientation = direction;
        setupDirection(direction);
    }

    public void setupDirection(int direction){
        if (stiffness > 0)
        {
            switch (orientation)
            {
                case HORIZONTAL:
                    spring = new SpringAnimation(this, SpringAnimation.TRANSLATION_X)
                        .setSpring(new SpringForce()
                                .setFinalPosition(0f)
                                .setDampingRatio(dampingRatio)
                                .setStiffness(stiffness));
                    break;
                case VERTICAL:
                    spring = new SpringAnimation(this, SpringAnimation.TRANSLATION_Y)
                        .setSpring(new SpringForce()
                                .setFinalPosition(0f)
                                .setDampingRatio(dampingRatio)
                                .setStiffness(stiffness));

            }
        }
    }

    public void setDampingRatio(float ratio){
        dampingRatio = ratio;
        this.spring.setSpring(
                new SpringForce()
                        .setFinalPosition(0f)
                        .setDampingRatio(ratio)
                        .setStiffness(stiffness)
        );
    }

    public void setStiffness(float stiffness){
        this.stiffness = stiffness;
        this.spring.setSpring(
                new SpringForce()
                        .setFinalPosition(0f)
                        .setDampingRatio(dampingRatio)
                        .setStiffness(stiffness)
        );
    }

    public void setLongPressDragEnabled(boolean flag){
        this.longPressDragEnabled = flag;
        if (getAdapter() instanceof DragDropAdapter) callBack.setDragEnabled(flag);
    }

    boolean itemSwipeEnabled = false;
    public void setItemSwipeEnabled(boolean flag){
        this.itemSwipeEnabled = flag;
        if (getAdapter() instanceof DragDropAdapter) callBack.setSwipeEnabled(flag);
    }

    boolean touched = false;

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e == null) return super.onTouchEvent(e);
        int actionMasked = e.getActionMasked();
        touched = actionMasked != MotionEvent.ACTION_UP && actionMasked != MotionEvent.ACTION_CANCEL;
        return super.onTouchEvent(e);
    }

    @Override
    public void setAdapter(@Nullable Adapter adapter) {
        super.setAdapter(adapter);

        if (adapter instanceof DragDropAdapter)
        {
            callBack = new DragDropCallBack(adapter, longPressDragEnabled, itemSwipeEnabled);
            ItemTouchHelper touchHelper = new ItemTouchHelper(callBack);
            touchHelper.attachToRecyclerView(this);
        }
    }

    public void forEachVisibleHolder(RecyclerView recyclerView, ViewHolderProcess runnable){
        if (recyclerView == null || runnable == null) return;
        int count = recyclerView.getChildCount();
        for (int i = 0 ; i < count ; i++){
            runnable.handleProcess(
                    recyclerView.getChildViewHolder(
                            getChildAt(i)
                    )
            );
        }
    }

    interface ViewHolderProcess {
        void handleProcess(RecyclerView.ViewHolder viewHolder);
    }

    public static abstract class BouncyAdapter<T extends ViewHolder> extends RecyclerView.Adapter<T> implements DragDropAdapter<T> {
    }
}
