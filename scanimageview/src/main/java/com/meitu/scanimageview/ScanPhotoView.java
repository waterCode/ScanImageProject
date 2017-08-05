package com.meitu.scanimageview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

import com.meitu.scanimageview.bean.BlockBitmap;
import com.meitu.scanimageview.bean.Viewpoint;
import com.meitu.scanimageview.tools.BitmapDecoderFactory;
import com.meitu.scanimageview.tools.InputStreamBitmapDecoderFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by zmc on 2017/8/3.
 */

public class ScanPhotoView extends android.support.v7.widget.AppCompatImageView{

    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private BitmapRegionDecoder mBitmapRegionDecoder;
    private Viewpoint mViewPoint;
    private InputStreamBitmapDecoderFactory mBitmapDecoderFactory;

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
        mScaleGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public void setImageURI ( Uri uri)
    {
        if(uri != null){

        try {

            mBitmapDecoderFactory = new InputStreamBitmapDecoderFactory(getContext(),uri);//创建一个解码器
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
                mViewPoint = new Viewpoint(getWidth(),getHeight());//创建一个视图窗口
                loadThumbnailTask.execute();//执行加载缩略图任务
            }
        });
    }



    @Override
    protected void onDraw(Canvas canvas) {
        if(mViewPoint != null){
            List<BlockBitmap> blockBitmapList = mViewPoint.getBlockBitmapList();

            for(BlockBitmap blockBitmap:blockBitmapList){
                canvas.drawBitmap(blockBitmap.getBitmap(),blockBitmap.getSrc(),blockBitmap.getDst(),null);
            }
        }

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
            invalidate();
            return true;
        }
    }
    private void moveTo(float distanceX, float distanceY) {
        // TODO: 2017/8/3 为什么是减
        invalidate();
    }



    AsyncTask<String,String,String > loadThumbnailTask = new AsyncTask<String, String, String>() {
        @Override
        protected String doInBackground(String... params) {
            if(mViewPoint!=null && mBitmapRegionDecoder!=null){
                int[] widthAndHeight = mBitmapDecoderFactory.getImageWidthAndHeight();
                int maxInSampleSize = Math.max(widthAndHeight[0]/mViewPoint.getRealWidth(),widthAndHeight[1]/mViewPoint.getRealHeight());
                int i =1;
                while(maxInSampleSize>Math.pow(2,i)){
                    i++;
                }
                int inSampleSize = (int) Math.pow(2,i);
                BitmapFactory.Options option = new BitmapFactory.Options();
                option.inSampleSize = inSampleSize;
                Rect rect = new Rect(0,0,widthAndHeight[0],widthAndHeight[1]);
                Bitmap bitmap = mBitmapRegionDecoder.decodeRegion(rect,option);//缩略图
                mViewPoint.setThumbnail(bitmap);//设置缩略图
            }
                return  null;
        }

    };
}
