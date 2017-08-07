package com.meitu.scanimageview.tools;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class InputStreamBitmapDecoderFactory implements BitmapDecoderFactory {

    private final int[] mWidthAndheight = new int[2];
    private Context mContext;
    private Uri mUri;


    public InputStreamBitmapDecoderFactory(Context context, Uri uri) {
        mContext = context;
        mUri = uri;
    }

    @Override
    public BitmapRegionDecoder made() throws IOException {
        InputStream inputStream = mContext.getContentResolver().openInputStream(mUri);
        if (inputStream != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, new Rect(), options);
            mWidthAndheight[0] = options.outWidth;
            mWidthAndheight[1] = options.outHeight;
            inputStream.close();
        }
        inputStream = mContext.getContentResolver().openInputStream(mUri);
        if (inputStream != null) {
            try {
                return BitmapRegionDecoder.newInstance(inputStream, false);
            } finally {
                inputStream.close();
            }
        } else {
            return null;
        }
    }

    /**
     * 只能用一次
     *
     * @return dd
     */
    @Override
    public int[] getImageWidthAndHeight() {

        return Arrays.copyOf(mWidthAndheight,mWidthAndheight.length);
    }
}