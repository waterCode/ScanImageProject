package com.meitu.scanimageview;

import android.content.Context;
import android.graphics.Canvas;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.io.IOException;

/**
 * Created by zmc on 2017/8/3.
 */

public class ScanPhotoView extends ImageView {

    private InputStreamScene imageSecene;

    public ScanPhotoView(Context context) {
        this(context, null);
    }

    public ScanPhotoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScanPhotoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
}
