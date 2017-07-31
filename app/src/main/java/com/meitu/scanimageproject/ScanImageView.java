package com.meitu.scanimageproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zmc on 2017/7/31.
 */

public class ScanImageView extends android.support.v7.widget.AppCompatImageView {

    private final Rect mScanViewRect = new Rect();//view所在图片真实区域，一开始为原始比例1
    private final Rect mRealBitmapRect = new Rect();
    private final Rect mOriginalBitmapRect = new Rect();
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
                    mCurrentBitmap = mBitmapRegionDecoder.decodeRegion(mRealBitmapRect, mDecoderOptions);
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


    private void moveScanViewRect(float distanceX, float distanceY) {
        mOriginalBitmapRect.left -= distanceX;
        mOriginalBitmapRect.right -= distanceX;
        mOriginalBitmapRect.top -= distanceY;
        mOriginalBitmapRect.bottom -= distanceY;

    }

    private class MoveListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            moveScanViewRect(distanceX, distanceY);
            updateBitmapRect();
            invalidate();
            return super.onScroll(e1, e2, distanceX, distanceY);

        }


    }


}
