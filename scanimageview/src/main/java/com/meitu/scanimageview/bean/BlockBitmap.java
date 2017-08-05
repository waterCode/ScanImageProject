package com.meitu.scanimageview.bean;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;

/**
 * Created by zmc on 2017/8/4.
 */

public class BlockBitmap {

    private Bitmap mBitmap;
    private Point position = new Point();

    private final Rect src = new Rect();//在原图的位置
    private final Rect dst = new Rect();//需要画在原图的区域

    public BlockBitmap(int width, int height) {
        mBitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);

    }

    public BlockBitmap(Bitmap bmp){
        if(bmp != null) {
            mBitmap = bmp;
            src.set(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        }
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public Rect getSrc() {
        return src;
    }

    public Rect getDst() {
        return dst;
    }

    public void setDstRect(int left, int top, int right , int bottom){
        dst.set(left,top,right,bottom);
    }

    public void setSrcRect(int left, int top, int right , int bottom){
        src.set(left,top,right,bottom);
    }
}
