package com.meitu.scanimageview.bean;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zmc on 2017/8/4.
 */

public class Viewpoint {

    private static final String TAG = Viewpoint.class.getSimpleName();
    private final RectF mWindowInOriginalBitmapRecF = new RectF();
    private final Rect mWindowInOriginalBitmap = new Rect();
    private final RectF mStartWindow;
    private final int mRealWidth;//真正的宽度
    private final int mRealHeight;//真正的高度
    private float mScaleLevel = 1;//放大倍数，详细的放大倍数
    private List<BlockBitmap> mBlockBitmapList = new ArrayList<>();//图片
    private int mBlockSize;
    private Bitmap mThumbnail;//可以删除
    private BlockBitmap mThumbnailBlock;
    private final Rect originalBitmap;//原图大小区域，不会变
    private Matrix mMatrix = new Matrix();

    /**
     * @param mRealWidth          窗口的实际宽度
     * @param mRealHeight         窗口的实际高度
     * @param imageWidthAndHeight 原图的实际宽和高
     */
    public Viewpoint(int mRealWidth, int mRealHeight, int[] imageWidthAndHeight) {
        this.mRealHeight = mRealHeight;
        this.mRealWidth = mRealWidth;
        mStartWindow = new RectF(0, 0, (mRealWidth), (mRealHeight));
        originalBitmap = new Rect(0, 0, imageWidthAndHeight[0], imageWidthAndHeight[1]);//原图的大小
        mBlockSize = mRealWidth / 2 + ((mRealWidth % 2) == 0 ? 1 : 0);

    }


    public int getBlockSizeInOriginalBitmap() {
        return mBlockSize * getSampleScale();
    }

    public int getBlockSize(){
        return mBlockSize;
    }

    public Rect getWindowInOriginalBitmap() {
        mMatrix.mapRect(mWindowInOriginalBitmapRecF, mStartWindow);
        transFormRectToRectF(mWindowInOriginalBitmap, mWindowInOriginalBitmapRecF);
        return mWindowInOriginalBitmap;
    }

    private void transFormRectToRectF(Rect rect, RectF rectF) {
        int left = (int) rectF.left;
        int right = (int) rectF.right;
        int top = (int) rectF.top;
        int bottom = (int) rectF.bottom;
        rect.set(left, top, right, bottom);
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


    // 2表示1/2,4表示1/4，只会是2的倍数
    public int getSampleScale() {
        int scale = Math.round(mScaleLevel);
        return getNearScale(scale);
    }

    private int getNearScale(int scale) {
        int result = 1;
        while (result < scale) {
            result *= 2;
        }
        return result;
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

    public void addBitmapBlock(BlockBitmap blockBitmap) {
        if (blockBitmap != null) {
            mBlockBitmapList.add(blockBitmap);
        }
    }

    /**
     * @param distanceX 手指移动的x距离
     * @param distanceY 手指移动的y距离
     */
    public void moveWindow(int distanceX, int distanceY) {
        mMatrix.postTranslate(distanceX, distanceY);
    }

    public void postScaleWindow(float scaleFactor) {
        mMatrix.postScale(scaleFactor, scaleFactor);
    }

    public void postScaleWindow(float scaleFactor, float focusX, float focuxY) {
        mMatrix.postScale(scaleFactor, scaleFactor, focusX, focuxY);
    }


    /**
     * 返回当前位置是否可见
     *
     * @param row        所在行数
     * @param column     所在列数
     * @param scaleLevel 放大几倍
     * @return true 可见
     */
    public boolean checkIsVisiable(int row, int column, float scaleLevel) {
        return true;
    }

    public Rect getRect(int row, int column, int sampleScale) {
        int left = mBlockSize*sampleScale*column;
        int top = mBlockSize * sampleScale*row;
        int right = left+sampleScale*mBlockSize;
        int bottom = top +sampleScale * mBlockSize;
        return new Rect(left,top,right,bottom);
    }
}
