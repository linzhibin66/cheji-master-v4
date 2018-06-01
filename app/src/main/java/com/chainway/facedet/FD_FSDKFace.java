package com.chainway.facedet;

import android.graphics.Rect;

/**
 * Created by Administrator on 2018/1/9.
 */

public class FD_FSDKFace {
    Rect mRect;
    public FD_FSDKFace() {
        mRect = new Rect();
    }
    public Rect getRect() {
        return mRect;
    }
    public String toString() {
        return mRect.toString();
    }
}
