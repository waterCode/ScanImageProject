package com.meitu.scanimageview.tools;

import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamBitmapDecoderFactory implements BitmapDecoderFactory {
    private InputStream mInputStream;
    private final int[] mWidthAndheight = new int[2];

    public InputStreamBitmapDecoderFactory(InputStream inputStream) {
        this.mInputStream = inputStream;
    }

    @Override
    public BitmapRegionDecoder made() throws IOException {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(mInputStream, new Rect(), options);

        mWidthAndheight[0] = options.outWidth;
        mWidthAndheight[1] = options.outHeight;
        BitmapRegionDecoder bitmapRegionDecoder = BitmapRegionDecoder.newInstance(mInputStream, false);
        return bitmapRegionDecoder;
    }

    /**
     * 只能用一次
     *
     * @return dd
     */
    @Override
    public int[] getImageWidthAndHeight() {

        return mWidthAndheight;
    }
}