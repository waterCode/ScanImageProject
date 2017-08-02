package com.meitu.scanimageview;

import android.content.Context;
import android.graphics.Canvas;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by zmc on 2017/7/31.
 */

public class ScanImageView extends SurfaceView implements SurfaceHolder.Callback {


    private InputStreamScene imageSecene;
    private DrawThread mDrawThread;

    public ScanImageView(Context context) {
        this(context, null);
    }

    public ScanImageView(Context context, AttributeSet attrs) {
        this(context, null, 0);
    }

    public ScanImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
    }


    public void setImageUri(@NonNull Uri uri) throws IOException {
        // TODO: 2017/8/2 要不要声明notnull
        if (uri == null) return;
        imageSecene = new InputStreamScene(getContext().getContentResolver(),uri);
        post(new Runnable() {
            @Override
            public void run() {
                imageSecene.initViewPoint(getWidth(),getHeight());//初始化视图窗口
            }
        });
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mDrawThread = new DrawThread(holder);
        mDrawThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    private class DrawThread extends Thread {

        boolean running = true;
        // TODO: 2017/8/2 这个应不应该判空
        private final SurfaceHolder mSurfaceHolder;

        public DrawThread(SurfaceHolder hoder) {
            mSurfaceHolder = hoder;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        @Override
        public void run() {
            Canvas canvas;
            while (running) {
                if (mSurfaceHolder != null) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    canvas = mSurfaceHolder.lockCanvas();
                    synchronized (mSurfaceHolder) {
                        // TODO: 2017/8/2 这里又是否需要判空
                        imageSecene.draw(canvas);
                    }
                    mSurfaceHolder.unlockCanvasAndPost(canvas);
                } else {
                    break;
                }
            }
        }
    }
}
