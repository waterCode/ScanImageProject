package com.meitu.scanimageview.tools;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.support.v4.util.Pools;
import android.util.Log;
import android.util.LruCache;

import com.meitu.scanimageview.bean.BlockBitmap;
import com.meitu.scanimageview.bean.Viewpoint;
import com.meitu.scanimageview.util.LoadBlockBitmapCallback;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by zmc on 2017/8/6.
 */
public class LoadBlockBitmapTaskManager {
    private static final String TAG = LoadBlockBitmapTaskManager.class.getSimpleName();
    private Viewpoint mViewPoint;
    private Executor mTaskPool;
    private BitmapRegionDecoder mDecoder;
    private final Pools.SimplePool<BlockBitmap> mBlockBitmapSimplePool = new Pools.SynchronizedPool<>(30);
    private final Pools.SimplePool<Bitmap> mBitmapSimplePool = new Pools.SynchronizedPool<>(30);
    private final LIFOBlockDeque<Runnable> mLIFOBlockDeque  = new LIFOBlockDeque<>();

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE_SECONDS = 30;


    private final LruCache<BlockBitmap.Position, BlockBitmap> mBlockBitmapLruCache = new LruCache<BlockBitmap.Position, BlockBitmap>((int) (Runtime.getRuntime().maxMemory() / 6)) {
        @Override
        protected int sizeOf(BlockBitmap.Position key, BlockBitmap value) {
            return value.getBitmap().getByteCount();
        }

        @Override
        protected void entryRemoved(boolean evicted, BlockBitmap.Position key, BlockBitmap oldValue, BlockBitmap newValue) {
            if (evicted) {
                mBlockBitmapSimplePool.release(oldValue);
            }
        }
    };

    public void clearAllTask(){
        mLIFOBlockDeque.clear();
    }

    public LoadBlockBitmapTaskManager(Viewpoint mViewPoint, BitmapRegionDecoder decoder) {
        this.mViewPoint = mViewPoint;
        mTaskPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS, mLIFOBlockDeque);
        mDecoder = decoder;
    }

    public void summitTask(LoadBitmapTask task) {
        task.initData(this, mViewPoint, mDecoder, mBlockBitmapLruCache);
        mTaskPool.execute(task);
    }

    public LruCache<BlockBitmap.Position, BlockBitmap> getBlockBitmapLruCache() {
        return mBlockBitmapLruCache;
    }

    public Pools.SimplePool<BlockBitmap> getBlockBitmapSimplePool() {
        return mBlockBitmapSimplePool;
    }



    /**
     * 后进先出的线程池队列
     *
     * @param <T> 队列参数类型
     */
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
        private LruCache<BlockBitmap.Position, BlockBitmap> mBlockBitmapLruCache;
        private LoadBlockBitmapCallback mLoadBlockBitmapCallback;//处理完回调
        private LoadBlockBitmapTaskManager mTaskManager;


        public LoadBitmapTask(int row, int column, int sampleScale) {
            super();
            this.row = row;
            this.column = column;
            this.sampleScale = sampleScale;

        }

        private void initData(LoadBlockBitmapTaskManager loadBlockBitmapTaskManager, Viewpoint viewpoint, BitmapRegionDecoder decoder, LruCache<BlockBitmap.Position, BlockBitmap> blockBitmapLruCache) {
            this.mViewpoint = viewpoint;
            this.mDecoder = decoder;
            mBlockBitmapLruCache = blockBitmapLruCache;
            mTaskManager = loadBlockBitmapTaskManager;
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
                mViewpoint.checkBitmapRegion(bitmapRegionRect);//检查越界问题,如果越界取图片会造成崩溃
                if (isRectRegionIllegal(bitmapRegionRect)) {//不合法直接结束该线程执行
                    return;
                }
                //尝试获得块图对象
                BlockBitmap reuseBlockBitmap = mTaskManager.getBlockBitmapSimplePool().acquire();
                if (reuseBlockBitmap == null) {//对象池里面木有就创建一个新的
                    reuseBlockBitmap = new BlockBitmap(mViewpoint.getBlockSize(),mViewpoint.getBlockSize());//新建一个块图
                }


                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = sampleScale;
                options.inBitmap = reuseBlockBitmap.getBitmap();//将块图bitmap对象复用
                options.inMutable = true;


                long time = System.currentTimeMillis();
                Bitmap bmp = mDecoder.decodeRegion(bitmapRegionRect, options);//如果宽高相等话会出现不合法的情况
                bmp.prepareToDraw();
                Log.d(TAG,"优化显示：加载图片时间"+(System.currentTimeMillis() - time));
                reuseBlockBitmap.setBitmap(bmp);
                reuseBlockBitmap.setPosition(row, column, sampleScale);

                //放入Lru缓存
                mBlockBitmapLruCache.put(reuseBlockBitmap.getPosition(), reuseBlockBitmap);
                if (mLoadBlockBitmapCallback != null) {
                    Log.d(TAG, reuseBlockBitmap.getPosition().toString() + "加载成功，开启回调");
                    mLoadBlockBitmapCallback.onLoadFinished();
                }
            } else {
                Log.d(TAG, "重复任务执行bug:" + "任务不可见位置为" + "level" + sampleScale + ",row" + row + "column" + column);
            }
        }

        private boolean isRectRegionIllegal(Rect rect) {
            return rect.right <= rect.left || rect.bottom <= rect.top;
        }


    }
}
