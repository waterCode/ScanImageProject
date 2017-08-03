package com.meitu.scanimageview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;


/**
 * Created by zmc on 2017/8/2.
 */

public abstract class AbsScene {

    protected Rect mOriginBitmapWindow;//在子类构造器初始化
    protected Cache mBitmapCache = new Cache();
    protected Viewpoint mViewpoint;//在InitViewPoint初始化


    public void initViewPoint(int width, int height) {
        // TODO: 2017/8/2 这种不合法参数应该怎么检测
        mViewpoint = new Viewpoint(width, height);//创建一个视图窗口
    }


    public void draw(Canvas canvas) {
        if (mViewpoint != null) {
            mViewpoint.draw(canvas);
        }
    }

    public void moveViewPointWindow(int dx, int dy) {//相对移动来说屏幕框框在运动，向上滑和想左滑为正

        //检查边界

        if (mViewpoint != null) {

        /*    if (dx < 0) {//只能右边界越界
                if ((mViewpoint.window.right += dx) > mOriginBitmapWindow.width()) {
                    dx = mOriginBitmapWindow.width() - mViewpoint.window.right;
                }
            } else {//只可能左边界越界
                if ((mViewpoint.window.left += dx) < 0) {//左边界越界
                    dx = 0 - mViewpoint.window.left;
                }
            }*/

//            if (dy > 0) {//只可能下边界越界
//                if ((mViewpoint.window.bottom += dy) > mOriginBitmapWindow.height()) {
//                    dy = mOriginBitmapWindow.height() - mViewpoint.window.bottom;
//                }
//            } else {//只可能上边界越界
//                //上边界越界
//                if ((mViewpoint.window.top += dy) < 0) {
//                    dy = 0 - mViewpoint.window.top;
//                }
//            }

            mViewpoint.window.left += dx;
            mViewpoint.window.right += dx;
            mViewpoint.window.top += dy;
            mViewpoint.window.bottom += dy;
        }
    }

    public void postScaleViewPointWindow(float scaleFactor) {
         mViewpoint.scale *=scaleFactor;//相乘
    }


    public class Cache {

        private Bitmap cacheBitmap;
        private Rect cacheWindow;


        public void check(Viewpoint viewpoint) {
            //如果缓存区域包含图片,
            //不包含，则开缓存线程去工作，加载缓存
            loadSampleBitmapToViewpoint(mViewpoint.bitmap, viewpoint);
        }

    }

    /**
     * 加载样例进入viewPointBitmap
     *
     * @param bitmap          viewPoint 的Bitmap
     * @param viewpointWindow
     */
    protected abstract void loadSampleBitmapToViewpoint(Bitmap bitmap, Viewpoint viewpoint);


    public class Viewpoint {
        // TODO: 2017/8/2 内部类需要加m嘛
        private final Rect window;
        private Bitmap bitmap;
        private float scale = 1;

        public Viewpoint(int width, int height) {
            window = new Rect(0, 0, width, height);
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }

        public Rect getWindow() {
            return window;
        }


        public float getScale() {
            return scale;
        }

        public void draw(Canvas canvas) {
            mBitmapCache.check(mViewpoint);//检查缓存
            canvas.drawBitmap(bitmap, 0, 0, null);
        }
    }


}
