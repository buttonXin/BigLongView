package com.laogao.biglongview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.Nullable;

/**
 * 描述：加载大长图片的view
 * <p>
 * 1、先将图片加载到内存中
 * 2、开启复用，并创建区域解码器
 * 3、在获取到view的宽高，即onMeasure中进行创建加载图片的rect的区域
 * 4、在onDraw中复用bitmap，并将加载到的区域进行缩放，绘制到canvas中
 * <p>
 * 5、监听触摸的点击事件，交给GestureDetector ；
 * 6、在每次点击的时候就停止上一次的滑动事件
 * 7、处理触摸的滑动事件；
 * 8、添加滑动的惯性处理；
 * 9、最后进行计算
 *
 * @author : 老高
 * @date : 2020/5/1
 **/
public class BigLongView extends View implements GestureDetector.OnGestureListener {

    private BitmapFactory.Options mOptions;
    private int mImageWidth;
    private int mImageHeight;
    private int mMeasuredWidth;
    private int mMeasuredHeight;
    private Bitmap mBitmap;
    // 将加载的区域进行缩放
    private Matrix mMatrix;
    // 区域解码器
    private BitmapRegionDecoder mDecoder;
    // 缩放比
    private float mScale;
    // 图片的区域
    private Rect mRect;
    // 手势与滑动事件
    private GestureDetector mGestureDetector;
    private Scroller mScroller;
    // 最初加载图片的区域，也就是分块加载的第一块（每次都是取图片的当前大小的区域进行加载！！！）
    private int mInitRect;

    public BigLongView(Context context) {
        this(context, null);
    }

    public BigLongView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BigLongView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    private void init(Context context) {
        mOptions = new BitmapFactory.Options();
        mRect = new Rect();
        // 手势类
        mGestureDetector = new GestureDetector(context, this);
        // 滚动类
        mScroller = new Scroller(context);
        mMatrix = new Matrix();
    }

    /**
     * 这里的方法可以进行重载，方便后续扩展
     *
     * @param is
     */
    public void setImage(InputStream is) {
        mOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, mOptions);
        mImageWidth = mOptions.outWidth;
        mImageHeight = mOptions.outHeight;

        Log.e("laogao", "setImage: 图片的宽 " + mImageWidth + " ,高=" + mImageHeight);
        // 内存复用
        mOptions.inMutable = true;

        // 使用565的颜色通道
        mOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        mOptions.inJustDecodeBounds = false;

        // 区域解码器
        try {
            mDecoder = BitmapRegionDecoder.newInstance(is, false);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        requestLayout();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mMeasuredWidth = getMeasuredWidth();
        mMeasuredHeight = getMeasuredHeight();

        // 开始计算最初的一块区域
        mRect.left = 0;
        mRect.top = 0;
        // 加载的是图片的宽度
        mRect.right = mImageWidth;
        // 计算缩放比例
        mScale = mMeasuredWidth / (float) mImageWidth;
        // 因为是长图，不可能加载全部的高度；所以加载的是view的高度，但是要除以缩放比才是真正的要加载的bottom
        mInitRect = (int) (mMeasuredHeight / mScale);
        mRect.bottom = mInitRect;

        // 上面的right  和 bottom 加载的含义 是将原图的某一块区域全部放到rect中，后续操作会进行缩放
        // 比如view是全屏加载 ；手机分辨率是 1080 * 1920 ； 图片是 2160 * 9600 ；
        // 那加载的区域就是 rect = （0 ，0 ， 2160 ， （1920/(1/2)）3840）
        // 在onDraw的时候就需要将当前的rect进行缩放到与手机宽高相同的比例上
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 复用bitmap的内存
        mOptions.inBitmap = mBitmap;
        mBitmap = mDecoder.decodeRegion(mRect, mOptions);
        // 在onMeasure中的解释， 进行缩放宽高
        mMatrix.setScale(mScale, mScale);
        canvas.drawBitmap(mBitmap, mMatrix, null);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 将监听的点击事件放到 GestureDetector中进行处理
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        // 当每次触摸到的时候就停止上一次的滚动事件
        if (!mScroller.isFinished()) {
            mScroller.forceFinished(true);
        }
        return true;
    }

    /**
     * @param motionEvent  开始的事件
     * @param motionEvent1 及时事件
     * @param scrollX      x周移动的距离
     * @param scrollY      y周移动的距离
     * @return
     */
    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1,
                            float scrollX, float scrollY) {

        mRect.offset(0, (int) scrollY);

        Log.e("laogao", "onScroll: top=" + mRect.top + " ,bottom= " + mRect.bottom);

        // 滑动到图片顶部的时候，保持刚开始加载的那一块区域 ；top 与bottom都要进行设置
        if (mRect.top <= 0) {
            mRect.top = 0;
            mRect.bottom = mInitRect;
            return false;
        }

        // 当前加载到图片最下部，则保持加载最后的那一块；top值等于图片高度- 加载的那一块
        if (mRect.bottom >= mImageHeight) {
            mRect.top = mImageHeight - mInitRect;
            mRect.bottom = mImageHeight;
            return false;
        }

        postInvalidate();

        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

        // 最后2个参数：最小的Y值是0， 最大的Y值：因为刚开始的时候已经加载了一块区域，所以最大的Y值是减去这块区域后的数值
        mScroller.fling(0, mRect.top, 0, -(int) velocityY, 0, 0,
                0, mImageHeight - mInitRect);
        return false;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        if (mScroller.isFinished()) {
            return;
        }
        // 还在滑动的状态，获取当前的Y值， 因为Y值肯定是在第一块区域开始的，上面的mScroller.fling已经设定的
        // 所以，获取Y值给到top，然后计算bottom的值，开始进行重绘
        if (mScroller.computeScrollOffset()) {
            mRect.top = mScroller.getCurrY();
            mRect.bottom = mRect.top + mInitRect;
            postInvalidate();
        }
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }


    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

}
