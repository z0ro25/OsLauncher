package com.amz.ios.launcher.util;

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.view.animation.Interpolator;


public final class CubicInterpolate implements Interpolator {
    public final float[] prePos;
    public final float[] curPos;

    public CubicInterpolate(float control1, float control2, float control3, float control4) {
        Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        path.cubicTo(control1, control2, control3, control4, 1.0f, 1.0f);

        PathMeasure pathMeasure = new PathMeasure(path, false);

        float length = pathMeasure.getLength();
        int count = ((int) (length / 0.002f)) + 1;
        this.prePos = new float[count];
        this.curPos = new float[count];

        float[] pos = new float[2];
        for (int i = 0; i < count; i++) {
            pathMeasure.getPosTan((i * length) / (count - 1), pos, null);
            this.prePos[i] = pos[0];
            this.curPos[i] = pos[1];
        }
    }

    @Override
    public final float getInterpolation(float input) {
        if (input <= 0.0f) {
            return 0.0f;
        }
        if (input >= 1.0f) {
            return 1.0f;
        }
        int smallMid = 0;
        int bigMid = this.prePos.length - 1;
        while (bigMid - smallMid > 1) {
            int mid = (smallMid + bigMid) / 2;
            if (input < this.prePos[mid]) {
                bigMid = mid;
            } else {
                smallMid = mid;
            }
        }

        float diff = prePos[bigMid] - prePos[smallMid];
        if (diff == 0.0f) {
            return this.curPos[smallMid];
        }

        float cur = curPos[smallMid];
        return calcInterpolate(this.curPos[bigMid], cur, (input - this.prePos[smallMid]) / diff, cur);
    }

    public static float calcInterpolate(float end, float start, float percent, float initValue) {
        return ((end - start) * percent) + initValue;
    }
}
