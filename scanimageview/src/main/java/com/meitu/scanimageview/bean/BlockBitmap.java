package com.meitu.scanimageview.bean;

import android.graphics.Bitmap;
import android.graphics.Rect;

/**
 * Created by zmc on 2017/8/4.
 */

public class BlockBitmap {

    //最新ban
    private Bitmap mBitmap;
    private final Position mPosition = new Position();

    private final Rect src = new Rect();//在原图的位置
    private final Rect dst = new Rect();//需要画在原图的区域

    private final Rect mPositionInOriginBitmap = new Rect();

    public BlockBitmap(int width, int height) {
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

    }


    public BlockBitmap(Bitmap bmp) {
        if (bmp != null) {
            mBitmap = bmp;
            src.set(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        }
    }

    public void setBitmap(Bitmap mBitmap) {
        this.mBitmap = mBitmap;
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

    public void setDstRect(int left, int top, int right, int bottom) {
        dst.set(left, top, right, bottom);
    }

    public void setSrcRect(int left, int top, int right, int bottom) {
        src.set(left, top, right, bottom);
    }

    public void setPosition(int row, int column, int sampleScale) {
        mPosition.row = row;
        mPosition.column = column;
        mPosition.sampleScale = sampleScale;
    }

    public Position getPosition() {
        return mPosition;
    }


    public Rect getPositionInOriginBitmap(int blockSize) {
        int left = blockSize * mPosition.column * mPosition.sampleScale;
        int top = blockSize * mPosition.row * mPosition.sampleScale;
        int right = left + blockSize * mPosition.sampleScale;
        int bottom = blockSize * mPosition.sampleScale + top;
        //通过position，获取
        mPositionInOriginBitmap.set(left, top, right, bottom);
        return mPositionInOriginBitmap;
    }

    public static class Position {
        int row;
        int column;
        int sampleScale;


        public Position() {
        }

        public Position(int row, int column, int sampleScale) {
            this.row = row;
            this.column = column;
            this.sampleScale = sampleScale;
        }

        @Override
        public String toString() {
            return "Position{" +
                    "row=" + row +
                    ", column=" + column +
                    ", sampleScale=" + sampleScale +
                    '}';
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Position) {
                Position target = (Position) obj;
                if (this.row == target.getRow() && this.column == target.getColumn() && this.sampleScale == target.getSampleScale()) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int iTotal = 7;
            int iConstant = 17;
            iTotal = iTotal * iConstant + row;
            iTotal = iTotal * iConstant + column;
            iTotal = iTotal * iConstant + sampleScale * 100;
            //Log.d("Position","hashCode"+iTotal);
            return iTotal;
        }

        public int getRow() {
            return row;
        }

        public int getColumn() {
            return column;
        }

        public int getSampleScale() {
            return sampleScale;
        }
    }
}
