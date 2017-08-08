package com.meitu.scanimageview.tools;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;

import com.meitu.scanimageview.bean.BlockBitmap;
import com.meitu.scanimageview.bean.Viewpoint;
import com.meitu.scanimageview.util.LoadBlockBitmapCallback;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by zmc on 2017/8/6.
 */
public class LoadBlockBitmapTaskManager {
    private static final String TAG = LoadBlockBitmapTaskManager.class.getSimpleName();
    private Viewpoint mViewPoint;
    private LruCache<BlockBitmap.Position, BlockBitmap> mBlockBitmapLruCache;

    private Executor mTaskPool;
    private BitmapRegionDecoder mDecoder;

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE_SECONDS = 30;
    public LoadBlockBitmapTaskManager(Viewpoint mViewPoint, LruCache<BlockBitmap.Position, BlockBitmap> mBlockBitmapLruCache, BitmapRegionDecoder decoder) {
        this.mViewPoint = mViewPoint;
        this.mBlockBitmapLruCache = mBlockBitmapLruCache;
        mTaskPool = new ThreadPoolExecutor(CORE_POOL_SIZE,MAXIMUM_POOL_SIZE,KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,new LIFOBlockDeque<Runnable>());
        mDecoder = decoder;
    }

    public void summitTask(LoadBitmapTask task) {
        task.initData(mViewPoint, mDecoder, mBlockBitmapLruCache);
        mTaskPool.execute(task);
    }

    private class LIFOBlockDeque<T> extends LinkedBlockingDeque<T> {

        @Override
        public boolean offer(T t) {
            return super.offerFirst(t);
        }


        @Override
        public boolean add(T t) {
            return super.offerFirst(t);
        }


        @Override
        public void put(T t) throws InterruptedException {
            super.putFirst(t);
        }
    }


    public static class LoadBitmapTask implements Runnable {

        private int row;
        private int column;
        private int sampleScale;
        private Viewpoint mViewpoint;
        private BitmapRegionDecoder mDecoder;
        LruCache<BlockBitmap.Position, BlockBitmap> mBlockBitmapLruCache;
        private LoadBlockBitmapCallback mLoadBlockBitmapCallback;

        public LoadBitmapTask(int row, int column, int sampleScale) {
            super();
            this.row = row;
            this.column = column;
            this.sampleScale = sampleScale;

        }

        public void initData(Viewpoint viewpoint, BitmapRegionDecoder decoder, LruCache<BlockBitmap.Position, BlockBitmap> blockBitmapLruCache) {
            this.mViewpoint = viewpoint;
            this.mDecoder = decoder;
            mBlockBitmapLruCache = blockBitmapLruCache;
        }

        public void setLoadFinshedListener(LoadBlockBitmapCallback loadBlockBitmapCallback) {
            mLoadBlockBitmapCallback = loadBlockBitmapCallback;
        }

        @Override
        public void run() {
            if (mViewpoint.checkIsVisiable(row, column, sampleScale)) {
                //加载图片
                Rect bitmapRegionRect = mViewpoint.getRect(row, column, sampleScale);
                Log.d(TAG, "开始加载图片块" + "所在行为" + row + ",列：" + column + "sampleScale:" + sampleScale
                        + ",加载区域为：" + bitmapRegionRect.toString());
                Log.d(TAG, "当前样例图片放大水平" + mViewpoint.getScaleLevel());

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = sampleScale;

                mViewpoint.checkBitmapRegion(bitmapRegionRect);//检查越界问题,如果越界取图片会造成崩溃

                Bitmap bmp = mDecoder.decodeRegion(bitmapRegionRect, options);
                //放入Lru缓存

                BlockBitmap blockBitmap = new BlockBitmap(bmp);
                blockBitmap.setPosition(row, column, sampleScale);
                mBlockBitmapLruCache.put(blockBitmap.getPosition(), blockBitmap);
                if (mLoadBlockBitmapCallback != null) {
                    mLoadBlockBitmapCallback.onLoadFinished();
                }

            }
        }
    }
}
