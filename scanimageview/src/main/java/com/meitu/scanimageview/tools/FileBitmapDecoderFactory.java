package com.meitu.scanimageview.tools;

import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;

import java.io.File;
import java.io.IOException;

public class FileBitmapDecoderFactory implements BitmapDecoderFactory {
    private String mPath;

    public FileBitmapDecoderFactory(String filePath) {
        super();
        this.mPath = filePath;
    }

    public FileBitmapDecoderFactory(File file) {
        super();
        this.mPath = file.getAbsolutePath();
    }

    @Override
    public BitmapRegionDecoder made() throws IOException {
        return BitmapRegionDecoder.newInstance(mPath, false);
    }

    @Override
    public int[] getImageWidthAndHeight() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mPath, options);
        return new int[]{options.outWidth, options.outHeight};
    }
}