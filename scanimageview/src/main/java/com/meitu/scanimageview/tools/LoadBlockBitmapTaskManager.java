package com.meitu.scanimageview.tools;

import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.util.LruCache;

import com.meitu.scanimageview.bean.BlockBitmap;
import com.meitu.scanimageview.bean.Viewpoint;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by zmc on 2017/8/6.
 */
public class LoadBlockBitmapTaskManager {
    private Viewpoint mViewPoint;
    private LruCache<BlockBitmap.Position, BlockBitmap> mBlockBitmapLru;

    private Executor mTaskPool;
    private BitmapRegionDecoder mDecoder;

    public LoadBlockBitmapTaskManager(Viewpoint mViewPoint, LruCache<BlockBitmap.Position, BlockBitmap> mBlockBitmapLru, BitmapRegionDecoder decoder) {
        this.mViewPoint = mViewPoint;
        this.mBlockBitmapLru =mBlockBitmapLru;
        mTaskPool = Executors.newFixedThreadPool(5);
        mDecoder = decoder;
    }

    public void summitTask(LoadBitmapTask task){
        mTaskPool.execute(task);
    }







    public class LoadBitmapTask implements Runnable{

        private  int row;
        private int column;
        private  float scaleLevel;
        public LoadBitmapTask(int row, int column, float scaleLevel) {
            super();
            this.row = row;
            this.column = column;
            this.scaleLevel = scaleLevel;
        }

        @Override
        public void run() {
            if(mViewPoint.checkIsVisiable(row,column,scaleLevel)){
                //加载图片
                Rect rect = mViewPoint.getRect(row,column,scaleLevel);
                BitmapFactory.Options options = new BitmapFactory.Options();
                //options.inSampleSize =scaleLevel;
                mDecoder.decodeRegion(rect,options);
                //放入Lru缓存
            }
        }
    }
}
