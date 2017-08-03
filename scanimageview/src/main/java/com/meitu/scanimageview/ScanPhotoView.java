package com.meitu.scanimageview;

import android.content.Context;
import android.graphics.Canvas;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import java.io.IOException;

/**
 * Created by zmc on 2017/8/3.
 */

public class ScanPhotoView extends android.support.v7.widget.AppCompatImageView {

    private InputStreamScene imageSecene;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;


    public ScanPhotoView(Context context) {
        this(context, null);
    }

    public ScanPhotoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScanPhotoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mGestureDetector = new GestureDetector(getContext(),new MoveGestureListener());
        mScaleGestureDetector = new ScaleGestureDetector(getContext(),new ScaleGestureListener());
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public void setImageURI (@Nullable Uri uri)
    {
        try {
            imageSecene = new InputStreamScene(getContext().getContentResolver(),uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        post(new Runnable() {
            @Override
            public void run() {
                imageSecene.initViewPoint(getWidth(),getHeight());//初始化视图窗口
                postInvalidate();
            }
        });
    }



    @Override
    protected void onDraw(Canvas canvas) {
        imageSecene.draw(canvas);
    }

    private class MoveGestureListener extends GestureDetector.SimpleOnGestureListener{

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            moveTo(distanceX,distanceY);
            return true;
        }
    }

    private class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener{
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            imageSecene.postScaleViewPointWindow(detector.getScaleFactor());
            return true;
        }
    }
    private void moveTo(float distanceX, float distanceY) {
        // TODO: 2017/8/3 为什么是减
        if(imageSecene != null){
            imageSecene.moveViewPointWindow((int)distanceX,(int)distanceY);
        }
        postInvalidate();
    }


}
