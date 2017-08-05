package com.meitu.scanimageview.bean;

import android.graphics.Bitmap;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zmc on 2017/8/4.
 */

public class Viewpoint {

    private final Rect window ;
    private final int mRealWidth;//真正的宽度
    private final int mRealHeight;//真正的高度
    private float mScale;//放大倍数
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
        window = new Rect(0,0,mRealWidth,mRealHeight);
        mblockSize = mRealWidth / 2 + (mRealWidth % 2) == 0 ? 1 : 0;
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

    public void setThumbnail(Bitmap mThumbnail) {
        this.mThumbnail = mThumbnail;
        mThumbnailBlock = new BlockBitmap(mThumbnail);
        mThumbnailBlock.setDstRect(0,0,mRealWidth,mRealHeight);
    }


    public void addBitmapBlock(BlockBitmap blockBitmap){
        if(mThumbnail!=null) {
            mBlockBitmapList.add(mThumbnailBlock);
        }
    }
}
