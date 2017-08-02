package com.meitu.scanimageview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;



/**
 * Created by zmc on 2017/8/2.
 */

public abstract class AbsScene {

    protected  Rect mOriginBitmapWindow;
    protected Cache mBitmapCache;
    protected Viewpoint mViewpoint;


    public void initViewPoint(int width, int height) {
        mViewpoint = new Viewpoint(width,height);//创建一个视图窗口
    }




    public  void draw(Canvas canvas){
        mViewpoint.draw(canvas);
    }


    public class Cache {

        private Bitmap cacheBitmap;
        private Rect cacheWindow;


        public void check(Rect viewpointWindow) {
            //如果缓存区域包含图片,
            //不包含，则开缓存线程去工作，加载缓存
            loadSampleBitmapToViewpoint(mViewpoint.bitmap,viewpointWindow);
        }

    }

    /**
     * 加载样例进入viewPointBitmap
     * @param bitmap viewPoint 的Bitmap
     * @param viewpointWindow
     */
    protected abstract void loadSampleBitmapToViewpoint(Bitmap bitmap, Rect viewpointWindow);


    public class Viewpoint {
        // TODO: 2017/8/2 内部类需要加m嘛
        private final Rect window;
        private Bitmap bitmap;

        public Viewpoint(int width, int height) {
            window = new Rect(0,0,width,height);
            bitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        }

        public Rect getWindow(){
            return window;
        }

        public void draw(Canvas canvas) {
            mBitmapCache.check(mViewpoint.getWindow());//检查缓存
            canvas.drawBitmap(bitmap,0,0,null);
        }
    }


}
