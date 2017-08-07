package com.meitu.scanimageview.tools;

import android.graphics.BitmapRegionDecoder;

import java.io.IOException;

public interface BitmapDecoderFactory {
    BitmapRegionDecoder made() throws IOException;

    int[] getImageWidthAndHeight();
}