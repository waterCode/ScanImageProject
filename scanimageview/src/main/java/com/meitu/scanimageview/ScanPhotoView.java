package com.meitu.scanimageview;

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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Created by zmc on 2017/8/3.
 */

public class ScanPhotoView extends android.support.v7.widget.AppCompatImageView implements LoadBlockBitmapCallback{

    private String Tag = ScanPhotoView.class.getSimpleName();
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private BitmapRegionDecoder mBitmapRegionDecoder;
    private Viewpoint mViewPoint;
    private InputStreamBitmapDecoderFactory mBitmapDecoderFactory;
    private float mMinScale;
    private float mCurrentScaled;

    LruCache<BlockBitmap.Position, BlockBitmap> mBlockBitmapLru = new LruCache<BlockBitmap.Position, BlockBitmap>((int) (Runtime.getRuntime().maxMemory() / 4)) {
        @Override
        protected int sizeOf(BlockBitmap.Position key, BlockBitmap value) {
            return value.getBitmap().getByteCount();
        }
    };
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

            } catch (FileNotFoundException e) {
                e.printStackTrace();
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
            Log.d(Tag, "当前放大倍数" + mCurrentScaled);
            Log.d(Tag, "当前viewPoint窗口位置" + "left:" + window.left + "top:" + window.top + "right:" + window.right + "bottom" + window.bottom
                    + "width:" + window.width() + "height" + window.height());
            if (mViewPoint.getmThumbnailBlock() != null) {
                canvas.drawBitmap(mViewPoint.getmThumbnailBlock().getBitmap(), mDisplayMatrix, null);
            }
            getAllDetailBitmapBlock(mViewPoint);
            /*Drawable drawable = getResources().getDrawable(R.drawable.ic_launcher);
            drawable.setBounds(-30,-30,42,42);
            drawable.draw(canvas);*/

        }

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
        int startRow = startAndEnd[0].y;
        int startColumn = startAndEnd[0].x;
        int endRow = startAndEnd[1].y;
        int endColumn = startAndEnd[1].x;


        int i = startRow;
        int j;
        mViewPoint.getBlockBitmapList().clear();//使用前先清空
        for (; i < endRow; i++) {
            for (j=startColumn; j < endColumn; j++) {
                //遍历每个位置，从缓存里面取，有就直接添加，没有就去开始一个任务去加载
                BlockBitmap blockBitmap = getBlockBitmapFromLru(i, j, sampleScale);
                if (blockBitmap == null) {//没有就开启一个任务去加载，异步的
                    // TODO: 2017/8/5 开始
                    startTask(i, j, sampleScale);
                } else {
                    //有的话添加入图片块集合
                    mViewPoint.getBlockBitmapList().add(blockBitmap);//设置模块
                }
            }
        }
    }

    private void startTask(int row, int column, int sampleScale ) {
        if (mLoadBitmapTaskManager == null) {
            mLoadBitmapTaskManager = new LoadBlockBitmapTaskManager(mViewPoint, mBlockBitmapLru, mBitmapRegionDecoder);
        }
        LoadBlockBitmapTaskManager.LoadBitmapTask loadBitmapTask;
        loadBitmapTask = new LoadBlockBitmapTaskManager.LoadBitmapTask(row, column, sampleScale);
        mLoadBitmapTaskManager.summitTask(loadBitmapTask);
    }


    private BlockBitmap getBlockBitmapFromLru(int row, int column, float scaleLevel) {
        BlockBitmap.Position key = new BlockBitmap.Position(row, column, scaleLevel);

        BlockBitmap blockBitmap = mBlockBitmapLru.get(key);
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
        int[] widthAndheight = mBitmapDecoderFactory.getImageWidthAndHeight();
        Rect viewpointWindow = mViewPoint.getWindowInOriginalBitmap();

        int maxRow = widthAndheight[0] / blockLength + 1;
        int maxColumn = widthAndheight[1] / blockLength + 1;
        int startRow = viewpointWindow.left / blockLength;
        int startColumn = viewpointWindow.top / blockLength;
        int endRow = viewpointWindow.right / blockLength + 1;
        int endColumn = viewpointWindow.bottom / blockLength + 1;
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
        postInvalidate();
    }


    private class MoveGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            moveTo((int) distanceX, (int) distanceY);
            return true;
        }
    }

    private class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();//放大因子
            if ((mCurrentScaled * scaleFactor) < mMinScale) {//防止缩小到过小限制缩小倍数
                scaleFactor = mMinScale / mCurrentScaled;
            }
            Log.d(Tag, "ScaleFactor:" + scaleFactor);
            Rect viewPointWindow = mViewPoint.getWindowInOriginalBitmap();
            Log.d(Tag, "focusX：" + mCurrentScaled);
            Log.d(Tag, "focusY：" + mCurrentScaled);

            float focusX = 1f / mCurrentScaled * detector.getFocusX() + viewPointWindow.left;
            float focusY = 1f / mCurrentScaled * detector.getFocusY() + viewPointWindow.top;

            mCurrentScaled *= scaleFactor;//实时更新当前放大倍数
            mViewPoint.setScaleLevel(1f / mCurrentScaled);//同时设置viewPoint的window放大水平1
            if (mViewPoint != null) {
                mDisplayMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                mViewPoint.postScaleWindow(1f / scaleFactor, focusX, focusY);
                invalidate();
            }
            return true;
        }
    }

    private void moveTo(int distanceX, int distanceY) {
        if (mViewPoint != null) {
            // TODO: 2017/8/7 bug估计是精度转换出现的问题
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
        Log.d(Tag, "当前left" + window.left);
        Log.d(Tag, "可能移动distanceX" + distanceX);
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
        Log.d(Tag, "实际移动distanceX" + distanceX);
        move[0] = distanceX * mCurrentScaled;
        move[1] = distanceY * mCurrentScaled;
        return move;
    }


    private int mThumbnailInSampleSize;

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
                mThumbnailInSampleSize = (int) Math.pow(2, i);
                BitmapFactory.Options option = new BitmapFactory.Options();
                option.inSampleSize = mThumbnailInSampleSize;
                Rect rect = new Rect(0, 0, widthAndHeight[0], widthAndHeight[1]);
                Bitmap thumbnailBitmap = mBitmapRegionDecoder.decodeRegion(rect, option);//缩略图


                initDisplayMatrixSetMinScale(thumbnailBitmap, mViewPoint, mThumbnailInSampleSize);//此时已经设置好最小缩放倍数
                mViewPoint.setThumbnail(thumbnailBitmap);//设置缩略图
                //设置初始位置
                initViewPointWindow(widthAndHeight);
                postInvalidate();
            }
            return null;
        }

        private void initViewPointWindow(int[] widthAndHeight) {
            //初始化放大倍数
            mViewPoint.postScaleWindow(1f / mMinScale);
            /*Rect viewpointWindow = mViewPoint.getWindowInOriginalBitmap();
            int dx = (viewpointWindow.width() - widthAndHeight[0]) / 2;
            int dy = (viewpointWindow.height() - widthAndHeight[1]) / 2;
            mViewPoint.moveWindow(dx, -dy);*/
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
            /*float startCenterX = thumbnailBitmap.getWidth() * scale / 2;//拿到中心位置
            float startCenterY = thumbnailBitmap.getHeight() * scale / 2;
            float endCenterX = mViewPoint.getRealWidth() / 2f;
            float endCenterY = mViewPoint.getRealHeight() / 2f;
            float dx = endCenterX - startCenterX;
            float dy = endCenterY - startCenterY;
            mDisplayMatrix.postTranslate(dx, dy);*/
        }

    };


}
