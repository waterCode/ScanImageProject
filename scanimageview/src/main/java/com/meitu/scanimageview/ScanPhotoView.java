package com.meitu.scanimageview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.LruCache;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.meitu.scanimageview.bean.BlockBitmap;
import com.meitu.scanimageview.bean.Viewpoint;
import com.meitu.scanimageview.tools.InputStreamBitmapDecoderFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Created by zmc on 2017/8/3.
 */

public class ScanPhotoView extends android.support.v7.widget.AppCompatImageView {

    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private BitmapRegionDecoder mBitmapRegionDecoder;
    private Viewpoint mViewPoint;
    private InputStreamBitmapDecoderFactory mBitmapDecoderFactory;
    LruCache<BlockBitmap.Position,BlockBitmap> mBlockBitmapLru =new LruCache<BlockBitmap.Position,BlockBitmap>((int) (Runtime.getRuntime().freeMemory()/4)){
        @Override
        protected int sizeOf(BlockBitmap.Position key, BlockBitmap value) {
            return value.getBitmap().getByteCount();
        }
    };

    private int mCurrentScale = 1;

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
                mViewPoint = new Viewpoint(getWidth(), getHeight());//创建一个视图窗口
                loadThumbnailTask.execute();//执行加载缩略图任务
            }
        });
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (mViewPoint != null) {
            //设置viewpoint的放大倍数
            mViewPoint.setScaleLevel(mCurrentScale);

            updateAllBitmapBlock();
            if (mViewPoint.getBlockBitmapList() != null) {
                for (BlockBitmap blockBitmap : mViewPoint.getBlockBitmapList()) {
                    canvas.drawBitmap(blockBitmap.getBitmap(), blockBitmap.getSrc(), blockBitmap.getDst(), null);
                }
            }
        }

    }

    private void updateAllBitmapBlock() {
        List<BlockBitmap> blockBitmapList = mViewPoint.getBlockBitmapList();
        blockBitmapList.clear();//先清空
        //添加缩略图模块
        updateThumbnailBlock(mViewPoint, mBitmapRegionDecoder);//更新先
        if (mViewPoint.getmThumbnailBlock() != null) {//绘制缩略图
            blockBitmapList.add(mViewPoint.getmThumbnailBlock());
        }

        //加载清晰模块
        loadDetailBitmap(mViewPoint, mBitmapRegionDecoder);
    }

    private void loadDetailBitmap(Viewpoint mViewPoint, BitmapRegionDecoder mBitmapRegionDecoder) {
        Point[] startAndEnd = getStartAndEndPosition(mViewPoint, mBitmapRegionDecoder);
        getAllAvailableBlock(startAndEnd,mViewPoint.getScaleLevel());


    }

    private void getAllAvailableBlock(Point[] startAndEnd, float scaleLevel) {
        int startRow = startAndEnd[0].y;
        int startColumn = startAndEnd[0].x;
        int endRow = startAndEnd[1].y;
        int endCloumn = startAndEnd[1].x;


        int i= startRow;
        int j= startColumn;

        for(;i<endRow;i++){
            for(;j < endCloumn;j++){
                //遍历每个位置，从缓存里面取，有就直接添加，没有就去开始一个任务去加载
                BlockBitmap blockBitmap = getBlockBitmapFromLru(i,j,scaleLevel);
                if(blockBitmap  == null){//没有就开启一个任务去加载，异步的
                    // TODO: 2017/8/5 开始
                    startTask(blockBitmap);
                }else{
                    //有的话添加入图片块集合
                    mViewPoint.getBlockBitmapList().add(blockBitmap);
                }
            }
        }
    }

    private void startTask(BlockBitmap blockBitmap) {
    }

    private BlockBitmap getBlockBitmapFromLru(int row, int column, float scaleLevel) {
        BlockBitmap.Position key  = new BlockBitmap.Position(row,column,scaleLevel);

        BlockBitmap blockBitmap = mBlockBitmapLru.get(key);
        //设置绘制区域
        if(blockBitmap!=null){
            //计算绘制区域并返回

            return blockBitmap;
        }else {
            return null;
        }
    }

    private Point[] getStartAndEndPosition(Viewpoint mViewPoint, BitmapRegionDecoder mBitmapRegionDecoder) {
        int blockLength = mViewPoint.getBlockSizeInOriginalBitmap();//获取宽度
        int[] widthAndheight = mBitmapDecoderFactory.getImageWidthAndHeight();
        Rect widow = mViewPoint.getWindowInOriginalBitmap();

        int maxRow = widthAndheight[0] / blockLength + 1;
        int maxColuman = widthAndheight[1] / blockLength + 1;
        int startRow = widow.left / blockLength;
        int startColumn = widow.top / blockLength;
        int endRow = widow.right / blockLength + 1;
        int endColuman = widow.bottom / blockLength + 1;
        Point[] point = new Point[2];
        point[0].y = startRow;
        point[0].x = startColumn;
        point[1].y = endRow;
        point[2].x = endColuman;
        return  point;
    }

    private void updateThumbnailBlock(Viewpoint mViewPoint, BitmapRegionDecoder mBitmapRegionDecoder) {
        BlockBitmap thumbnailBlock = mViewPoint.getmThumbnailBlock();
        if (thumbnailBlock != null) {//也就是存在缩略图
            //计算出当前窗口在缩略图的位置
            Rect window = mViewPoint.getWindowInOriginalBitmap();
            int left = window.left / mThumbnailInSampleSize;
            int top = window.top / mThumbnailInSampleSize;
            int right = window.right / mThumbnailInSampleSize;
            int bottom = window.bottom / mThumbnailInSampleSize;
            thumbnailBlock.setSrcRect(left, top, right, bottom);
            //添加模块
        }
    }

    private class MoveGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            moveTo(distanceX, distanceY);
            return true;
        }
    }

    private class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (mViewPoint != null) {
                mViewPoint.postScaleWindow(detector.getScaleFactor());
                invalidate();
            }
            return true;
        }
    }

    private void moveTo(float distanceX, float distanceY) {
        // TODO: 2017/8/3 为什么是减
        if (mViewPoint != null) {
            int[] realMove = getRealMove(distanceX, distanceY);//越界检查
            mViewPoint.moveWindow(realMove[0], realMove[1]);
            invalidate();
        }
    }

    private int[] getRealMove(float distanceX, float distanceY) {
        int[] move = new int[2];
        Rect windowInOriginalBitmap = mViewPoint.getWindowInOriginalBitmap();
        int[] widthAndHeight = mBitmapDecoderFactory.getImageWidthAndHeight();
        if ((windowInOriginalBitmap.left + distanceX) < 0) {//左边界越界
            distanceX = 0 - windowInOriginalBitmap.left;
        }
        if ((windowInOriginalBitmap.right + distanceX) > widthAndHeight[0]) {//右边界越界
            distanceX = widthAndHeight[0] - windowInOriginalBitmap.right;
        }
        if ((windowInOriginalBitmap.top + distanceY) < 0) {
            //上边界越界
            distanceY = 0 - windowInOriginalBitmap.top;
        }
        if ((windowInOriginalBitmap.bottom + distanceY) > widthAndHeight[1]) {
            distanceY = widthAndHeight[1] - windowInOriginalBitmap.bottom;
        }
        move[0] = (int) distanceX;
        move[1] = (int) distanceY;
        return move;
    }


    private int mThumbnailInSampleSize;
    AsyncTask<String, String, String> loadThumbnailTask = new AsyncTask<String, String, String>() {
        @Override
        protected String doInBackground(String... params) {
            if (mViewPoint != null && mBitmapRegionDecoder != null) {
                int[] widthAndHeight = mBitmapDecoderFactory.getImageWidthAndHeight();
                int maxInSampleSize = Math.max(widthAndHeight[0] / mViewPoint.getRealWidth(), widthAndHeight[1] / mViewPoint.getRealHeight());
                int i = 1;
                while (maxInSampleSize > Math.pow(2, i)) {
                    i++;
                }
                mThumbnailInSampleSize = (int) Math.pow(2, i);
                BitmapFactory.Options option = new BitmapFactory.Options();
                option.inSampleSize = mThumbnailInSampleSize;
                Rect rect = new Rect(0, 0, widthAndHeight[0], widthAndHeight[1]);
                Bitmap bitmap = mBitmapRegionDecoder.decodeRegion(rect, option);//缩略图
                mViewPoint.setThumbnail(bitmap);//设置缩略图
                postInvalidate();
            }
            return null;
        }

    };



}
