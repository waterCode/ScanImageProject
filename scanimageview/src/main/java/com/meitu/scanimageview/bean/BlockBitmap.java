package com.meitu.scanimageview.bean;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;

/**
 * Created by zmc on 2017/8/4.
 */

public class BlockBitmap {

    private Bitmap mBitmap;
    private Position mPosition = new Position();

    private final Rect src = new Rect();//在原图的位置
    private final Rect dst = new Rect();//需要画在原图的区域

    public BlockBitmap(int width, int height) {
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

    }


    public BlockBitmap(Bitmap bmp) {
        if (bmp != null) {
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

    public void setDstRect(int left, int top, int right, int bottom) {
        dst.set(left, top, right, bottom);
    }

    public void setSrcRect(int left, int top, int right, int bottom) {
        src.set(left, top, right, bottom);
    }

    public void setPosition(int row, int column, int level) {

    }




    public static class Position {
        int row;
        int column;
        float level;


        public Position() {
        }

        public Position(int row, int column, float level) {
            this.row = row;
            this.column = column;
            this.level = level;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Position) {
                Position target = (Position) obj;
                if (this.row == target.getRow() && this.column == target.getColumn() && this.level == target.getLevel()) {
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
            int iTotal = 17;
            int iConstant = 37;
            iTotal = iTotal * iConstant + row;
            iTotal = iTotal * iConstant + column;
            iTotal = (int) (iTotal * iConstant + level*10);
            return iTotal;
        }

        public int getRow() {
            return row;
        }

        public int getColumn() {
            return column;
        }

        public float getLevel() {
            return level;
        }
    }
}
