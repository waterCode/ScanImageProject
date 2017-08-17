package com.meitu.scanimageview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Scroller;

import com.meitu.scanimageview.bean.BlockBitmap;
import com.meitu.scanimageview.bean.Viewpoint;
import com.meitu.scanimageview.tools.InputStreamBitmapDecoderFactory;
import com.meitu.scanimageview.tools.LoadBlockBitmapTaskManager;
import com.meitu.scanimageview.util.LoadBlockBitmapCallback;

import java.io.IOException;
import java.util.List;

/**
 * Created by zmc on 2017/8/3.
 */

public class ScanPhotoView extends android.support.v7.widget.AppCompatImageView implements LoadBlockBitmapCallback {

    private static final String TAG = ScanPhotoView.class.getSimpleName();
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private BitmapRegionDecoder mBitmapRegionDecoder;
    private Viewpoint mViewPoint;
    private InputStreamBitmapDecoderFactory mBitmapDecoderFactory;
    private float mMinScale;
    private float mCurrentScaled;
    private float mMaxScale = 3;
    public static final int DEFAULT_ANIMATION_TIME = 400;

    private FlingScroller mScroller;

    private LoadBlockBitmapTaskManager mLoadBitmapTaskManager;
    private final Matrix mDisplayMatrix = new Matrix();

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

    public void testScale() {
        float scale = (1 / mCurrentScaled);
        float scaleFactor = scale / 2;
        onScale(scaleFactor, 0, 0);
    }


    private void init() {
        mGestureDetector = new GestureDetector(getContext(), new MoveGestureListener());
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureListener());
        mScroller = new FlingScroller(getContext());
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScroller.forceFinished(true);

        mScaleGestureDetector.onTouchEvent(event);
        if (!mScaleGestureDetector.isInProgress()) {
            mGestureDetector.onTouchEvent(event);
        }
        return true;
    }


    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {//移动,左滑是负数
            //直接获取间隔，然后调用moveto
            Log.d(TAG, "flingBug:currentXXX" + mScroller.getCurrX() + "YYY" + mScroller.getCurrY());
            Log.d(TAG, "flingBug:速度" + mScroller.getCurrVelocity());
            int currentValueX = mScroller.getCurrX();
            float dx = (currentValueX - mScroller.getOldValueX()) * mCurrentScaled;
            mScroller.setOldValueX(currentValueX);

            int currentValueY = mScroller.getCurrY();
            Log.d(TAG, "flingBug:viewPointWindow left" + mViewPoint.getWindowInOriginalBitmap().left);
            float dy = (currentValueY - mScroller.getOldValueY()) * mCurrentScaled;
            mScroller.setOldValueY(currentValueY);
            Log.d(TAG, "flingBug:computeScroll dx" + dx + ",dy:" + dy);
            moveTo(-dx, -dy);
            invalidate();
        }
    }

    @Override
    public void setImageURI(Uri uri) {
        if (uri != null) {

            try {

                mBitmapDecoderFactory = new InputStreamBitmapDecoderFactory(getContext(), uri);//创建一个解码器
                mBitmapRegionDecoder = mBitmapDecoderFactory.made();

            } catch (IOException e) {
                e.printStackTrace();
            }
        post(new Runnable() {
            @Override
            public void run() {
                mViewPoint = new Viewpoint(getWidth(), getHeight(), mBitmapDecoderFactory.getImageWidthAndHeight());//创建一个视图窗口
                loadThumbnailTask.execute();//执行加载缩略图任务
            }
        });
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {

        if (mViewPoint != null) {
            //设置viewpoint的放大倍数
            //mViewPoint.setScaleLevel(mCurrentScale);
            Rect window = mViewPoint.getWindowInOriginalBitmap();
            Log.d(TAG, "当前放大倍数" + mCurrentScaled);
            Log.d(TAG, "当前viewPoint窗口位置" + "left:" + window.left + "top:" + window.top + "right:" + window.right + "bottom" + window.bottom
                    + "width:" + window.width() + "height" + window.height());
            if (mViewPoint.getmThumbnailBlock() != null) {
                canvas.drawBitmap(mViewPoint.getmThumbnailBlock().getBitmap(), mDisplayMatrix, null);
            }

            getAllDetailBitmapBlock(mViewPoint, false);//拿到所有缓存中有的块
            //为所有的块设置位置
            updateAllBitmapBlock();

            //遍历绘制上去
            canvas.save();
            float scaleCanvas = mViewPoint.getSampleScale() / mViewPoint.getScaleLevel();
            canvas.scale(scaleCanvas, scaleCanvas);
            List<BlockBitmap> shouldDrawBlockBitmapList = mViewPoint.getBlockBitmapList();
            if (shouldDrawBlockBitmapList != null) {
                for (BlockBitmap block : shouldDrawBlockBitmapList) {
                    Log.d(TAG, "开始绘制图片(" + block.getPosition().toString() + ")" + "src " + block.getSrc().toString() + " ,dst" + block.getDst().toString());
                    canvas.drawBitmap(block.getBitmap(), block.getSrc(), block.getDst(), null);
                }
            }
            canvas.restore();

        }

    }

    private void updateAllBitmapBlock() {
        List<BlockBitmap> blockBitmapList = mViewPoint.getBlockBitmapList();
        if (blockBitmapList != null) {
            //获取
            for (BlockBitmap blockBitmap : blockBitmapList) {
                updateBitmapBlockSrcAndDstRect(blockBitmap, mViewPoint);//更新所有模块的区域
            }
        }
    }


    public void updateBitmapBlockSrcAndDstRect(BlockBitmap blockBitmap, Viewpoint viewpoint) {
        if (blockBitmap.getPosition().getColumn() == 0 && blockBitmap.getPosition().getRow() == 1) {
            System.currentTimeMillis();
        }
        Rect bitmapPosition = blockBitmap.getPositionInOriginBitmap(viewpoint.getBlockSize());
        Rect viewpointPosition = viewpoint.getWindowInOriginalBitmap();
        //求出src和dst
        setBlockBitmapSrcAndDst(blockBitmap, bitmapPosition, viewpointPosition);
    }

    /**
     * 设置绘制模块的src和dst
     *
     * @param blockBitmap       绘制模块
     * @param bitmapPosition    图片相对原图区域
     * @param viewpointPosition viewpoint相对原图区域
     */
    private void setBlockBitmapSrcAndDst(BlockBitmap blockBitmap, Rect bitmapPosition, Rect viewpointPosition) {

        // TODO: 2017/8/7  建立在一定有相交区域情况下
        //求出相交区域，
        int left = (bitmapPosition.left > viewpointPosition.left) ? bitmapPosition.left : viewpointPosition.left;//取大的左边
        int right = (bitmapPosition.right < viewpointPosition.right) ? bitmapPosition.right : viewpointPosition.right;//小的右边
        int top = (bitmapPosition.top > viewpointPosition.top) ? bitmapPosition.top : viewpointPosition.top;
        int bottom = (bitmapPosition.bottom < viewpointPosition.bottom) ? bitmapPosition.bottom : viewpointPosition.bottom;

        int sampleScale = blockBitmap.getPosition().getSampleScale();
        blockBitmap.setSrcRect((left - bitmapPosition.left) / sampleScale, (top - bitmapPosition.top) / sampleScale, (right - bitmapPosition.left) / sampleScale, (bottom - bitmapPosition.top) / sampleScale);
        blockBitmap.setDstRect((left - viewpointPosition.left) / sampleScale, (top - viewpointPosition.top) / sampleScale, (right - viewpointPosition.left) / sampleScale, (bottom - viewpointPosition.top) / sampleScale);
    }

    /**
     * 拿到缓存区所有块
     *
     * @param mViewPoint  视图窗口
     * @param isStartTask 不存在的是否开启任务
     */
    private void getAllDetailBitmapBlock(Viewpoint mViewPoint, boolean isStartTask) {
        Point[] startAndEnd = getStartAndEndPosition(mViewPoint);//开始和结束的列
        getAllAvailableBlock(startAndEnd, mViewPoint.getSampleScale(), isStartTask);
    }


    private void getAllAvailableBlock(Point[] startAndEnd, int sampleScale, boolean isStartTask) {

        boolean isAllBitmapBlockInCache = true;//是否都缓存区，是则取消队列中中的所有人无
        // TODO: 2017/8/8 这个应该在什么地方new？
        if (mLoadBitmapTaskManager == null) {
            mLoadBitmapTaskManager = new LoadBlockBitmapTaskManager(mViewPoint, mBitmapRegionDecoder);
        }

        int startRow = startAndEnd[0].y;
        int startColumn = startAndEnd[0].x;
        int endRow = startAndEnd[1].y;
        int endColumn = startAndEnd[1].x;

        int i = startRow;
        int j;
        List<BlockBitmap> blockBitmapList = null;
        if (!isStartTask) {
            blockBitmapList = mViewPoint.getBlockBitmapList();
            blockBitmapList.clear();//使用前先清空
        }
        for (; i < endRow; i++) {
            for (j = startColumn; j < endColumn; j++) {
                //遍历每个位置，从缓存里面取，有就直接添加，没有就去开始一个任务去加载
                BlockBitmap blockBitmap = getBlockBitmapFromLru(i, j, sampleScale);
                if (blockBitmap == null) {//没有就开启一个任务去加载，异步的
                    isAllBitmapBlockInCache =false;
                    if (isStartTask) {
                        startTask(i, j, sampleScale);
                    }
                } else {
                    //有的话添加入图片块集合
                    if (!isStartTask) {
                        blockBitmapList.add(blockBitmap);//设置模块
                    }
                }
            }
        }
        if (isAllBitmapBlockInCache){
            //清空所有任务队列
            mLoadBitmapTaskManager.clearAllTask();
        }
    }

    public void findNeedLoadBitmapBlockAndSumitTask() {
        getAllDetailBitmapBlock(mViewPoint, true);
    }

    private void startTask(int row, int column, int sampleScale) {

        LoadBlockBitmapTaskManager.LoadBitmapTask loadBitmapTask;
        loadBitmapTask = new LoadBlockBitmapTaskManager.LoadBitmapTask(row, column, sampleScale);
        loadBitmapTask.setLoadFinshedListener(this);
        mLoadBitmapTaskManager.summitTask(loadBitmapTask);
    }


    private BlockBitmap getBlockBitmapFromLru(int row, int column, int sampleScale) {
        BlockBitmap.Position key = new BlockBitmap.Position(row, column, sampleScale);

        BlockBitmap blockBitmap = mLoadBitmapTaskManager.getBlockBitmapLruCache().get(key);
        //设置绘制区域
        if (blockBitmap != null) {
            //计算绘制区域并返回

            return blockBitmap;
        } else {
            return null;
        }
    }

    /**
     * 根据当前的viewPoint 获取开始的行和列
     * @param mViewPoint 视图窗口
     * @return 开始和结束的列的点坐标
     */
    private Point[] getStartAndEndPosition(Viewpoint mViewPoint) {
        int blockLength = mViewPoint.getBlockSizeInOriginalBitmap();//获取宽度

        Rect viewpointWindow = mViewPoint.getWindowInOriginalBitmap();

        int startRow = viewpointWindow.top / blockLength;
        int startColumn = viewpointWindow.left / blockLength;
        int endRow = viewpointWindow.bottom / blockLength + 1;
        int endColumn = viewpointWindow.right / blockLength + 1;
        Point[] point = new Point[2];
        Point pointStart = new Point();
        pointStart.y = startRow;
        pointStart.x = startColumn;
        Point pointEnd = new Point();
        pointEnd.y = endRow;
        pointEnd.x = endColumn;
        point[0] = pointStart;
        point[1] = pointEnd;
        return point;
    }

    @Override
    public void onLoadFinished() {
        Log.d(TAG, "load one bitmap block finished");
        postInvalidate();
    }


    private class MoveGestureListener extends GestureDetector.SimpleOnGestureListener {


        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            moveTo((int) distanceX, (int) distanceY);
            return true;
        }


        @Override
        public boolean onDoubleTap(MotionEvent e) {
            //动画放大到最大，再点击，回到最小
            Log.d(TAG, "doubleTab");
            float goalScale;
            Log.d(TAG,"double tap bug:currentScale" + mCurrentScaled);

            if ((mMaxScale - mCurrentScaled) < 0.2) {
                //返回到最小
                goalScale = mMinScale ;
            } else {
                //放大到最大
                goalScale = mMaxScale ;
            }
            Log.d(TAG,"double tap bug:goalScale" + goalScale);
            SmoothScale(goalScale, e.getX(), e.getY(), DEFAULT_ANIMATION_TIME);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Rect viewpointRect = mViewPoint.getWindowInOriginalBitmap();
            Rect originalBitmapRect = mViewPoint.getOriginalBitmapRect();
            //获取起始点，结束点，最大最小距离
            float startX = 0;//实际起始点
            float startY = 0;//
            float minX = viewpointRect.right - originalBitmapRect.right;
            float maxX = viewpointRect.left - originalBitmapRect.left;
            float minY = viewpointRect.bottom - originalBitmapRect.bottom;
            float maxY = viewpointRect.top - originalBitmapRect.top;
            Log.d(TAG, "flingBug::startX: " + startX + ",minX: " + startY + ",minX: " + minX + ",minY:" + minY + ",MaxX:" + maxX + ",MaxY:" + maxY + ",velocityX:" + velocityX + ",velocityY:" + velocityY);
            mScroller.forceFinished(true);
            mScroller.fling((int) startX, (int) startY, (int) velocityX, (int) velocityY, (int) minX, (int) maxX, (int) minY, (int) maxY);
            invalidate();
            return true;
        }
    }

    private void SmoothScale(float goalScale, final float focusX, final float focusY, int defaultAnimationTime) {
        final ValueAnimator valueAnimator = ValueAnimator.ofFloat(mCurrentScaled, goalScale);
        valueAnimator.setDuration(defaultAnimationTime);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float goalScale = (float) animation.getAnimatedValue();
                float postScale = goalScale / mCurrentScaled;
                onScale(postScale, focusX, focusY);

            }
        });
        valueAnimator.start();
    }

    private class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();//放大因子
            ScanPhotoView.this.onScale(scaleFactor, detector.getFocusX(), detector.getFocusY());
            return true;
        }
    }


    public void onScale(float scaleFactor, float sx, float sy) {

        if ((mCurrentScaled * scaleFactor) < mMinScale) {//防止缩小到过小限制缩小倍数
            scaleFactor = mMinScale / mCurrentScaled;
        }

        if ((mCurrentScaled * scaleFactor) > mMaxScale) {//大于最大倍数
            scaleFactor = mMaxScale / mCurrentScaled;
        }

        Log.d(TAG, "ScaleFactor:" + scaleFactor);
        Rect viewPointWindow = mViewPoint.getWindowInOriginalBitmap();
        Log.d(TAG, "focusX：" + sx);
        Log.d(TAG, "focusY：" + sy);
        float focusXInOriginBitmap, focusYInOriginBitmap;//在屏幕的放大中心

        focusXInOriginBitmap = 1f / mCurrentScaled * sx + viewPointWindow.left;//在原图中的放大中心点
        focusYInOriginBitmap = 1f / mCurrentScaled * sy + viewPointWindow.top;//在原图中的放大中心点

        Log.d(TAG, "focusXInOriginalBitmap：" + focusXInOriginBitmap);
        Log.d(TAG, "focusYInOriginalBitmap：" + focusYInOriginBitmap);
        Log.d(TAG, "currentScale:" + mCurrentScaled);
        if (mViewPoint != null) {
            mDisplayMatrix.postScale(scaleFactor, scaleFactor, sx, sy);//实际移动图片
            mViewPoint.postScaleWindow(1f / scaleFactor, focusXInOriginBitmap, focusYInOriginBitmap);
            mCurrentScaled *= scaleFactor;//实时更新当前放大倍数
            mViewPoint.setScaleLevel(1f / mCurrentScaled);//同时设置viewPoint的window放大水平1
            float[] moveDxAndDy = checkPosition();
            Log.d(TAG, "缩放后移动距离为dx: " + moveDxAndDy[0] + "需移动dy是：" + moveDxAndDy[1]);
            mDisplayMatrix.postTranslate(-moveDxAndDy[0], -moveDxAndDy[1]);
            mViewPoint.moveWindow(moveDxAndDy[0] * 1f / mCurrentScaled, moveDxAndDy[1] * 1f / mCurrentScaled);

            findNeedLoadBitmapBlockAndSumitTask();
            invalidate();

        }
    }


    private float[] checkPosition() {
        Rect window = mViewPoint.getWindowInOriginalBitmap();
        float[] dxAndDy = new float[2];
        int[] widthAndHeight = mBitmapDecoderFactory.getImageWidthAndHeight();
        if (window.left < 0) {
            dxAndDy[0] = 0 - window.left;
        }
        if (window.top < 0) {
            dxAndDy[1] = 0 - window.top;
        }
        if (window.right > widthAndHeight[0]) {
            dxAndDy[0] = widthAndHeight[0] - window.right;
        }
        if (window.bottom > widthAndHeight[1]) {
            dxAndDy[1] = widthAndHeight[1] - window.bottom;
        }
        dxAndDy[0] *= mCurrentScaled;
        dxAndDy[1] *= mCurrentScaled;
        return dxAndDy;
    }

    private void moveTo(float distanceX, float distanceY) {
        if (mViewPoint != null) {
            float[] realMove = getRealMove(distanceX, distanceY);//越界检查
            mDisplayMatrix.postTranslate(-realMove[0], -realMove[1]);
            mViewPoint.moveWindow(realMove[0] * 1f / mCurrentScaled, realMove[1] * 1f / mCurrentScaled);
            Log.d(TAG,"抖动bug："+"moveTo: "+"distanceX"+distanceX+"distanceY"+distanceY);
            findNeedLoadBitmapBlockAndSumitTask();
            invalidate();
        }
    }


    /**
     * 获取真正需要移动距离
     *
     * @param distanceX x轴距离
     * @param distanceY y轴距离
     * @return 一个素组move【0】表示宽，move【1】表示高
     */
    private float[] getRealMove(float distanceX, float distanceY) {
        float[] move = new float[2];
        Rect window = mViewPoint.getWindowInOriginalBitmap();
        int[] widthAndHeight = mBitmapDecoderFactory.getImageWidthAndHeight();
        Log.d(TAG, "当前left" + window.left);
        Log.d(TAG, "可能移动distanceX" + distanceX);
        if ((window.left + distanceX) < 0) {
            float bigDistanceX = 0 - window.left;
            distanceX = bigDistanceX * mCurrentScaled;
        }
        if ((window.right + distanceX) > widthAndHeight[0]) {
            float bigDistanceX = widthAndHeight[0] - window.right;
            distanceX = bigDistanceX * mCurrentScaled;
        }
        if ((window.top + distanceY) < 0) {
            float bigDistanceY = 0 - window.top;
            distanceY = bigDistanceY * mCurrentScaled;
        }
        if ((window.bottom + distanceY > widthAndHeight[1])) {
            float bigDistanceY = widthAndHeight[1] - window.bottom;
            distanceY = bigDistanceY * mCurrentScaled;
        }
        Log.d(TAG, "实际移动distanceX" + distanceX);
        move[0] = distanceX;
        move[1] = distanceY;
        return move;
    }


    AsyncTask<String, String, String> loadThumbnailTask = new AsyncTask<String, String, String>() {
        @Override
        protected String doInBackground(String... params) {
            if (mViewPoint != null && mBitmapRegionDecoder != null) {
                int[] widthAndHeight = mBitmapDecoderFactory.getImageWidthAndHeight();//原图宽高
                int maxInSampleSize = Math.max(widthAndHeight[0] / mViewPoint.getRealWidth(), widthAndHeight[1] / mViewPoint.getRealHeight());
                int i = 1;
                while (maxInSampleSize > Math.pow(2, i)) {
                    i++;
                }
                int mThumbnailInSampleSize = (int) Math.pow(2, i);
                BitmapFactory.Options option = new BitmapFactory.Options();
                option.inSampleSize = mThumbnailInSampleSize;
                Rect rect = new Rect(0, 0, widthAndHeight[0], widthAndHeight[1]);
                Bitmap thumbnailBitmap = mBitmapRegionDecoder.decodeRegion(rect, option);//缩略图


                initDisplayMatrixSetMinScale(thumbnailBitmap, mViewPoint, mThumbnailInSampleSize);//此时已经设置好最小缩放倍数
                mViewPoint.setThumbnail(thumbnailBitmap);//设置缩略图
                //设置初始位置
                initViewPointWindow();

            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            findNeedLoadBitmapBlockAndSumitTask();
            invalidate();
        }

        private void initViewPointWindow() {
            //初始化放大倍数
            mViewPoint.postScaleWindow(1f / mMinScale);

        }

        private void initDisplayMatrixSetMinScale(Bitmap thumbnailBitmap, Viewpoint mViewPoint, int mThumbnailInSampleSize) {
            float widthScale = 1f * mViewPoint.getRealWidth() / thumbnailBitmap.getWidth();
            float heightScale = 1f * mViewPoint.getRealHeight() / thumbnailBitmap.getHeight();
            float scale = Math.max(widthScale, heightScale);//取最小scale
            mDisplayMatrix.postScale(scale, scale);//展示图放大的倍数
            mMinScale = 1f / mThumbnailInSampleSize * scale;
            mCurrentScaled = mMinScale;
            if(mMinScale>mMaxScale){
                mMaxScale = mMinScale;//应付小图的情况
            }
            mViewPoint.setScaleLevel(1f / mCurrentScaled);

        }

    };


    private class FlingScroller extends Scroller {

        int mOldValueX = 0;
        int mOldValueY = 0;

        public FlingScroller(Context context) {
            super(context);
        }


        public void setOldValueX(int oldValue) {
            this.mOldValueX = oldValue;
        }

        public void setOldValueY(int oldValue) {
            this.mOldValueY = oldValue;
        }


        public int getOldValueX() {
            return mOldValueX;
        }

        public int getOldValueY() {
            return mOldValueY;
        }

        @Override
        public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY) {
            mOldValueX = 0;
            mOldValueY = 0;
            super.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);
        }
    }
}
