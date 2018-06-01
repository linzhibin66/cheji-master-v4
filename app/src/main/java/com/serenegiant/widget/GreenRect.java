package com.serenegiant.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by Administrator on 2017-12-14.
 */

public class GreenRect extends View {
    public GreenRect(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10.0f);//设置线宽
        paint.setAlpha(85);
        // TODO Auto-generated constructor stub
    }

    Paint paint;


    ArrayList<Rect> rectList = null;

    //设置矩形区域
    public void setRect(ArrayList<Rect> rectList){
        this.rectList = rectList;
        invalidate();
    }


    int width;
    int height;
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
        height = getMeasuredHeight();
        width = getMeasuredWidth();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);
        if (rectList != null && rectList.size() > 0){
            for (int i = 0; i < rectList.size(); i++){
//                canvas.drawRect(rectList.get(i), paint);
                float widthMuti;
                float heightMuti;
                widthMuti = (float)width/640;
                heightMuti = (float)height/480;
                float left;
                float right;
                if (false){
                    left = (width-rectList.get(i).left*widthMuti);
                    right = (width-rectList.get(i).right*widthMuti);
                }else
                {
                    left = (rectList.get(i).left*widthMuti);
                    right= (rectList.get(i).right*widthMuti);
                }
                float top = Math.min((rectList.get(i).top*heightMuti) , (rectList.get(i).bottom*heightMuti));
                float bottom =  Math.max((rectList.get(i).top*heightMuti) , (rectList.get(i).bottom*heightMuti));;
                canvas.drawRect(left, top,right, bottom, paint);
                Log.i("GreenRect", "Left:"+left+"Top:"+top+"Right:"+right+"Bottom:"+bottom );
                /*Log.i("GreenRect", "Width:"+width+" height:"+height);
                Log.i("GreenRect", "top:"+rectList.get(i).top+" botttom:"+rectList.get(i).bottom
                        +" left:"+rectList.get(i).left+" right:"+rectList.get(i).right );*/
            }
        }
    }
}