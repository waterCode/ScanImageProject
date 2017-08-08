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
import android.util.LruCache;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

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

    private String TAG = ScanPhotoView.class.getSimpleName();
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private BitmapRegionDecoder mBitmapRegionDecoder;
    private Viewpoint mViewPoint;
    private InputStreamBitmapDecoderFactory mBitmapDecoderFactory;
    private float mMinScale;
    private float mCurrentScaled;
    private float mMaxScale = 3;
    public static final int DEFAULT_ANIMATION_TIME = 400;


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
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        mScaleGestureDetector.onTouchEvent(event);
        return true;
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
        }
        post(new Runnable() {
            @Override
            public void run() {
                mViewPoint = new Viewpoint(getWidth(), getHeight(), mBitmapDecoderFactory.getImageWidthAndHeight());//创建一个视图窗口
                loadThumbnailTask.execute();//执行加载缩略图任务
            }
        });
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
            getAllDetailBitmapBlock(mViewPoint);//拿到所有缓存中有的块
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
     * 获取所有的清晰模块
     *
     * @param mViewPoint 窗口类
     */
    private List<BlockBitmap> getAllDetailBitmapBlock(Viewpoint mViewPoint) {
        Point[] startAndEnd = getStartAndEndPosition(mViewPoint, mBitmapRegionDecoder);//开始和结束的列
        getAllAvailableBlock(startAndEnd, mViewPoint.getSampleScale());
        return null;
    }


    private void getAllAvailableBlock(Point[] startAndEnd, int sampleScale) {
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

        List<BlockBitmap> blockBitmapList = mViewPoint.getBlockBitmapList();
        blockBitmapList.clear();//使用前先清空

        for (; i < endRow; i++) {
            for (j = startColumn; j < endColumn; j++) {
                //遍历每个位置，从缓存里面取，有就直接添加，没有就去开始一个任务去加载
                BlockBitmap blockBitmap = getBlockBitmapFromLru(i, j, sampleScale);
                if (blockBitmap == null) {//没有就开启一个任务去加载，异步的
                    startTask(i, j, sampleScale);
                } else {
                    //有的话添加入图片块集合
                    blockBitmapList.add(blockBitmap);//设置模块
                }
            }
        }
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

    private Point[] getStartAndEndPosition(Viewpoint mViewPoint, BitmapRegionDecoder mBitmapRegionDecoder) {
        int blockLength = mViewPoint.getBlockSizeInOriginalBitmap();//获取宽度
        //int[] widthAndheight = mBitmapDecoderFactory.getImageWidthAndHeight();
        Rect viewpointWindow = mViewPoint.getWindowInOriginalBitmap();

        /*int maxRow = widthAndheight[0] / blockLength + 1;
        int maxColumn = widthAndheight[1] / blockLength + 1;*/
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
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            int dx = (int) (e2.getX() - e1.getX());
            int dy = (int) (e2.getY() - e1.getY());
            Log.d(TAG, "onFling" + ",dx::" + dx + ",dy::" + dy);

            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            //动画放大到最大，再点击，回到最小
            Log.d(TAG, "doubleTab");
            float goalScale;
            if ((mMaxScale - mCurrentScaled) < 0.2) {
                //返回到最小
                goalScale = mMinScale / mCurrentScaled;
            } else {
                //放大到最大
                goalScale = mMaxScale / mCurrentScaled;
            }
            SmoothScale(goalScale, e.getX(), e.getY(), DEFAULT_ANIMATION_TIME);
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
        float focusX, focusY;//在屏幕的放大中心

        focusX = 1f / mCurrentScaled * sx + viewPointWindow.left;//在原图中的放大中心点
        focusY = 1f / mCurrentScaled * sy + viewPointWindow.top;//在原图中的放大中心点

        Log.d(TAG, "focusXInOriginalBitmap：" + focusX);
        Log.d(TAG, "focusYInOriginalBitmap：" + focusY);

        mCurrentScaled *= scaleFactor;//实时更新当前放大倍数
        Log.d(TAG, "currentScale:" + mCurrentScaled);
        mViewPoint.setScaleLevel(1f / mCurrentScaled);//同时设置viewPoint的window放大水平1
        if (mViewPoint != null) {
            mDisplayMatrix.postScale(scaleFactor, scaleFactor, sx, sy);

            mViewPoint.postScaleWindow(1f / scaleFactor, focusX, focusY);
            invalidate();
        }
    }

    private void moveTo(int distanceX, int distanceY) {
        if (mViewPoint != null) {
            float[] realMove = getRealMove(distanceX, distanceY);//越界检查
            mDisplayMatrix.postTranslate(-realMove[0], -realMove[1]);
            mViewPoint.moveWindow((int) (realMove[0] * 1f / mCurrentScaled), (int) (realMove[1] * 1f / mCurrentScaled));
            invalidate();
        }
    }

    private float[] getRealMove(float distanceX, float distanceY) {
        float[] move = new float[2];
        Rect window = mViewPoint.getWindowInOriginalBitmap();
        int[] widthAndHeight = mBitmapDecoderFactory.getImageWidthAndHeight();
        Log.d(TAG, "当前left" + window.left);
        Log.d(TAG, "可能移动distanceX" + distanceX);
        if ((window.left + distanceX) < 0) {
            distanceX = 0 - window.left;
        }
        if ((window.right + distanceX) > widthAndHeight[0]) {
            distanceX = widthAndHeight[0] - window.right;
        }
        if ((window.top + distanceY) < 0) {
            distanceY = 0 - window.top;
        }
        if ((window.bottom + distanceY > widthAndHeight[1])) {
            distanceY = widthAndHeight[1] - window.bottom;
        }
        Log.d(TAG, "实际移动distanceX" + distanceX);
        move[0] = distanceX * mCurrentScaled;
        move[1] = distanceY * mCurrentScaled;
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
                postInvalidate();
            }
            return null;
        }

        private void initViewPointWindow() {
            //初始化放大倍数
            mViewPoint.postScaleWindow(1f / mMinScale);

        }

        private void initDisplayMatrixSetMinScale(Bitmap thumbnailBitmap, Viewpoint mViewPoint, int mThumbnailInSampleSize) {
            // TODO: 2017/8/7 暂时从左上角开始
            float widthScale = 1f * mViewPoint.getRealWidth() / thumbnailBitmap.getWidth();
            float heightScale = 1f * mViewPoint.getRealHeight() / thumbnailBitmap.getHeight();
            float scale = Math.max(widthScale, heightScale);//取最小scale
            mDisplayMatrix.postScale(scale, scale);
            mMinScale = 1f / mThumbnailInSampleSize * scale;
            mCurrentScaled = mMinScale;
            mViewPoint.setScaleLevel(1f / mCurrentScaled);

        }

    };


}
