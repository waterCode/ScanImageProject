package com.meitu.scanimageview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.meitu.scanimageview.bean.BlockBitmap;
import com.meitu.scanimageview.bean.Viewpoint;
import com.meitu.scanimageview.tools.InputStreamBitmapDecoderFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Created by zmc on 2017/8/3.
 */

public class ScanPhotoView extends android.support.v7.widget.AppCompatImageView {

    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private BitmapRegionDecoder mBitmapRegionDecoder;
    private Viewpoint mViewPoint;
    private InputStreamBitmapDecoderFactory mBitmapDecoderFactory;

    private int mCurrentScale = 1;

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
        mGestureDetector = new GestureDetector(getContext(), new MoveGestureListener());
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureListener());
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        mScaleGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public void setImageURI(Uri uri) {
        if (uri != null) {

            try {

                mBitmapDecoderFactory = new InputStreamBitmapDecoderFactory(getContext(), uri);//创建一个解码器
                mBitmapRegionDecoder = mBitmapDecoderFactory.made();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        post(new Runnable() {
            @Override
            public void run() {
                mViewPoint = new Viewpoint(getWidth(), getHeight());//创建一个视图窗口
                loadThumbnailTask.execute();//执行加载缩略图任务
            }
        });
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (mViewPoint != null) {
            //设置viewpoint的放大倍数
            mViewPoint.setScale(mCurrentScale);

            updateAllBitmapBlock();
            if (mViewPoint.getBlockBitmapList() != null) {
                for (BlockBitmap blockBitmap : mViewPoint.getBlockBitmapList()) {
                    canvas.drawBitmap(blockBitmap.getBitmap(), blockBitmap.getSrc(), blockBitmap.getDst(), null);
                }
            }
        }

    }

    private void updateAllBitmapBlock() {
        List<BlockBitmap> blockBitmapList = mViewPoint.getBlockBitmapList();
        blockBitmapList.clear();//先清空
        //添加缩略图模块
        updateThumbnailBlock(mViewPoint, mBitmapRegionDecoder);//更新先
        if(mViewPoint.getmThumbnailBlock() != null) {
            blockBitmapList.add(mViewPoint.getmThumbnailBlock());
        }
    }

    private void updateThumbnailBlock(Viewpoint mViewPoint, BitmapRegionDecoder mBitmapRegionDecoder) {
        BlockBitmap thumbnailBlock = mViewPoint.getmThumbnailBlock();
        if (thumbnailBlock != null) {//也就是存在缩略图
            //计算出当前窗口在缩略图的位置
            Rect window = mViewPoint.getWindowInOriginalBitmap();
            int left = window.left / mThumbnailInSampleSize;
            int top = window.top / mThumbnailInSampleSize;
            int right = window.right / mThumbnailInSampleSize;
            int bottom = window.bottom / mThumbnailInSampleSize;
            thumbnailBlock.setSrcRect(left, top, right, bottom);
            //添加模块
        }
    }

    private class MoveGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            moveTo(distanceX, distanceY);
            return true;
        }
    }

    private class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            invalidate();
            return true;
        }
    }

    private void moveTo(float distanceX, float distanceY) {
        // TODO: 2017/8/3 为什么是减
        invalidate();
    }


    private int mThumbnailInSampleSize;
    AsyncTask<String, String, String> loadThumbnailTask = new AsyncTask<String, String, String>() {
        @Override
        protected String doInBackground(String... params) {
            if (mViewPoint != null && mBitmapRegionDecoder != null) {
                int[] widthAndHeight = mBitmapDecoderFactory.getImageWidthAndHeight();
                int maxInSampleSize = Math.max(widthAndHeight[0] / mViewPoint.getRealWidth(), widthAndHeight[1] / mViewPoint.getRealHeight());
                int i = 1;
                while (maxInSampleSize > Math.pow(2, i)) {
                    i++;
                }
                mThumbnailInSampleSize = (int) Math.pow(2, i);
                BitmapFactory.Options option = new BitmapFactory.Options();
                option.inSampleSize = mThumbnailInSampleSize;
                Rect rect = new Rect(0, 0, widthAndHeight[0], widthAndHeight[1]);
                Bitmap bitmap = mBitmapRegionDecoder.decodeRegion(rect, option);//缩略图
                mViewPoint.setThumbnail(bitmap);//设置缩略图
                postInvalidate();
            }
            return null;
        }

    };
}
