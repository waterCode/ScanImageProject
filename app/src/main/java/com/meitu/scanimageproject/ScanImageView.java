package com.meitu.scanimageproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zmc on 2017/7/31.
 */

public class ScanImageView extends android.support.v7.widget.AppCompatImageView {
    private static final String TAG = ScanImageView.class.getSimpleName();
    private final Rect mScanViewRect = new Rect();//view所在图片真实区域，一开始为原始比例1
    private final Rect mRealBitmapRect = new Rect();//真实图片所在区域，
    private final Rect mOriginalBitmapRect = new Rect();//原图所在区域

    private final PointF leftTopPoint = new PointF(0,0);//左上角顶点
    private final Rect mBitmapArea = new Rect();
    private Bitmap mCurrentBitmap;
    private BitmapRegionDecoder mBitmapRegionDecoder;
    private final BitmapFactory.Options mDecoderOptions = new BitmapFactory.Options();

    private GestureDetector mGestureDetector;

    public ScanImageView(Context context) {
        this(context, null, 0);

    }

    public ScanImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScanImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setScaleType(ScaleType.MATRIX);
        mGestureDetector = new GestureDetector(getContext(), new MoveListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mScanViewRect.set(0, 0, right - left, bottom - top);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mCurrentBitmap, mScanViewRect, mRealBitmapRect, new Paint());

    }

    @Override
    public void setImageURI(@Nullable Uri uri) {
        super.setImageURI(uri);
        try {
            if (uri != null) {
                InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                mBitmapRegionDecoder = BitmapRegionDecoder.newInstance(inputStream, false);
                if (inputStream != null) {
                    inputStream.close();
                }

                inputStream = getContext().getContentResolver().openInputStream(uri);
                mDecoderOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputStream, null, mDecoderOptions);
                mOriginalBitmapRect.set(0, 0, mDecoderOptions.outWidth, mDecoderOptions.outHeight);
                mDecoderOptions.inJustDecodeBounds = true;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        post(new Runnable() {
            @Override
            public void run() {
                if (mBitmapRegionDecoder != null) {
                    updateBitmapRect();
                    updateBitmapArea(0,0);
                    mCurrentBitmap = mBitmapRegionDecoder.decodeRegion(mBitmapArea, mDecoderOptions);
                }
                if (mCurrentBitmap != null) ;
                setImageBitmap(mCurrentBitmap);
                postInvalidate();
            }
        });
    }

    private void updateBitmapRect() {
        mRealBitmapRect.left = (mScanViewRect.left < mOriginalBitmapRect.left) ? mOriginalBitmapRect.left : mScanViewRect.left;
        mRealBitmapRect.top = (mScanViewRect.top < mOriginalBitmapRect.top) ? mOriginalBitmapRect.top : mScanViewRect.top;
        mRealBitmapRect.right = (mScanViewRect.right > mOriginalBitmapRect.right) ? mOriginalBitmapRect.right : mScanViewRect.right;
        mRealBitmapRect.bottom = (mScanViewRect.bottom > mOriginalBitmapRect.bottom) ? mOriginalBitmapRect.bottom : mScanViewRect.bottom;

    }

    private void updateBitmapArea(float distanceX, float distanceY) {
        leftTopPoint.x += distanceX;//移动左上角顶点
        leftTopPoint.y += distanceY;
        if (leftTopPoint.x < 0) {
            leftTopPoint.x = 0;
        }
        if (leftTopPoint.y < 0) {
            leftTopPoint.y = 0;
        }

        float right = mScanViewRect.right - leftTopPoint.x;
        if (right < 0)
            right = 0;
        if (right > mScanViewRect.width())
            right = mScanViewRect.width();

        float bottom = mScanViewRect.bottom - leftTopPoint.y;
        if (bottom < 0)
            bottom = 0;
        if (bottom > mScanViewRect.height()) {
            bottom = mScanViewRect.height();
        }

        //设置区域
        mBitmapArea.set((int)leftTopPoint.x, (int)leftTopPoint.y, (int)bottom, (int)right);//会丢失精度吧
    }

    private void moveScanViewRect(float distanceX, float distanceY) {
        //右移动是减，
        Log.d(TAG, "distanceX" + distanceX);
        mOriginalBitmapRect.left -= distanceX;
        mOriginalBitmapRect.right -= distanceX;
        mOriginalBitmapRect.top -= distanceY;
        mOriginalBitmapRect.bottom -= distanceY;

    }

    private class MoveListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            moveScanViewRect(distanceX, distanceY);
            updateBitmapArea(distanceX, distanceY);
            updateBitmapRect();
            invalidate();
            return super.onScroll(e1, e2, distanceX, distanceY);

        }


    }


}
