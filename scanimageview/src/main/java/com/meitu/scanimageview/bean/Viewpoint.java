package com.meitu.scanimageview.bean;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zmc on 2017/8/4.
 */

public class Viewpoint {

    private static final String TAG =Viewpoint.class.getSimpleName();
    private final Rect mWindowInOriginalBitmap;//在原图中窗口大小
    private final int mRealWidth;//真正的宽度
    private final int mRealHeight;//真正的高度
    private float mScaleLevel = 1;//放大倍数
    private List<BlockBitmap> mBlockBitmapList = new ArrayList<>();//图片
    private float mblockSize;
    private Bitmap mThumbnail;
    private BlockBitmap mThumbnailBlock;

    /**
     * 图片块
     */
    public Viewpoint(int mRealWidth, int mRealHeight) {
        this.mRealHeight = mRealHeight;
        this.mRealWidth = mRealWidth;
        mWindowInOriginalBitmap = new Rect(0,0,(int)(mRealWidth * mScaleLevel),(int)(mRealHeight * mScaleLevel));
        mblockSize = mRealWidth / 2 + (mRealWidth % 2) == 0 ? 1 : 0;
    }



    public int getBlockSizeInOriginalBitmap(){
        return (int) (mblockSize* mScaleLevel);
    }

    public Rect getWindowInOriginalBitmap() {
        return mWindowInOriginalBitmap;
    }

    public int getRealWidth() {
        return mRealWidth;
    }

    public int getRealHeight() {
        return mRealHeight;
    }


    public List<BlockBitmap> getBlockBitmapList() {
        return mBlockBitmapList;
    }

    public void setScaleLevel(float mScale) {
        this.mScaleLevel = mScale;
    }

    public float getScaleLevel() {
        return mScaleLevel;
    }

    public void setThumbnail(Bitmap mThumbnail) {
        if (mThumbnail != null) {
            this.mThumbnail = mThumbnail;
            mThumbnailBlock = new BlockBitmap(mThumbnail);
            mThumbnailBlock.setDstRect(0, 0, mRealWidth, mRealHeight);
        }
    }

    //拿到缩略图模块
    public BlockBitmap getmThumbnailBlock() {
        return mThumbnailBlock;
    }

    public void addBitmapBlock(BlockBitmap blockBitmap){
        if(blockBitmap!=null) {
            mBlockBitmapList.add(blockBitmap);
        }
    }

    public void moveWindow(float distanceX, float distanceY) {
        int left = (int) (mWindowInOriginalBitmap.left + distanceX);
        int right = (int) (mWindowInOriginalBitmap.right +distanceX);
        int top = (int) (mWindowInOriginalBitmap.top + distanceY);
        int bottom = (int) (mWindowInOriginalBitmap.bottom + distanceY);

        mWindowInOriginalBitmap.set(left,top,right,bottom);
    }

    public void postScaleWindow(float scaleFactor) {
        Log.d(TAG,"scaleFactor"+scaleFactor);
        mScaleLevel *= scaleFactor;
        float dScale = 1-scaleFactor;
        int centerX = mWindowInOriginalBitmap.centerX();
        int centerY = mWindowInOriginalBitmap.centerY();
        //以中心点放大
        int dx = (int) (mWindowInOriginalBitmap.width() *dScale);
        int dy = (int) (mWindowInOriginalBitmap.height() * dScale);
        Log.d(TAG,"dx"+dx);
        Log.d(TAG,"dy"+dy);
        int left = mWindowInOriginalBitmap.left - dx/2;
        int right = mWindowInOriginalBitmap.right + dx/2;

        int top = mWindowInOriginalBitmap.top - dy/2;
        int bottom = mWindowInOriginalBitmap.bottom +dy/2;
        mWindowInOriginalBitmap.set(left,top,right,bottom);
    }
}
