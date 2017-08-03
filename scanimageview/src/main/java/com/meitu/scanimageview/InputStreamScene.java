package com.meitu.scanimageview;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;

import com.meitu.scanimageview.util.ImageLoadUtil;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zmc on 2017/8/2.
 */

public class InputStreamScene extends AbsScene {

    private BitmapRegionDecoder mBitmapRegionDecoder;
    private Bitmap mSampleBitmap;

    private final int BITMAP_INSAMPLESIZE;//样例图压缩比例


    // TODO: 2017/8/2 复用会重用输入流
    public InputStreamScene(ContentResolver contentResolver, Uri uri) throws IOException {
        InputStream inputStream = contentResolver.openInputStream(uri);
        mBitmapRegionDecoder = BitmapRegionDecoder.newInstance(inputStream, false);
        mOriginBitmapWindow = ImageLoadUtil.calculateBitmapSize(contentResolver, uri);//计算出原图范围
        BitmapFactory.Options options = ImageLoadUtil.calculateInSampleSize(contentResolver, uri, 1500, 1500);//压缩到1500以下
        //要将缓存和最开始视图都初始化好
        BITMAP_INSAMPLESIZE = options.inSampleSize;
        // TODO: 2017/8/2 流关闭 的问题
        inputStream.close();
        inputStream = contentResolver.openInputStream(uri);
        mSampleBitmap = BitmapFactory.decodeStream(inputStream, null, options);//加载样例图片
        //开启一个缓存线程去做缓存的工作
    }


    @Override
    protected void loadSampleBitmapToViewpoint(Bitmap bitmap, Viewpoint viewpoint) {
        Canvas canvas = new Canvas(bitmap);
        int left =  viewpoint.getWindow().left / BITMAP_INSAMPLESIZE;
        int top = viewpoint.getWindow().top / BITMAP_INSAMPLESIZE;
        int right = viewpoint.getWindow().right / BITMAP_INSAMPLESIZE;
        int bottom =viewpoint.getWindow().bottom / BITMAP_INSAMPLESIZE;

        //以下是设置放大中心点
        int centerX = (right+left)/2;
        int centerY = (bottom+top)/2;

        Rect src = new Rect(left, top, right, bottom);
        int scaleX = (int) (src.width()/2/viewpoint.getScale());
        int scaleY = (int) (src.height()/2/viewpoint.getScale());

        src.set(centerX-scaleX,centerY-scaleY,centerX+scaleX,centerY+scaleY);


        Rect dst = new Rect(0, 0, viewpoint.getWindow().width(), viewpoint.getWindow().height());
        canvas.drawBitmap(mSampleBitmap, src, dst, new Paint());//绘制上去
    }


}