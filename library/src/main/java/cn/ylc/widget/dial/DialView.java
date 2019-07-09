package cn.ylc.widget.dial;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import androidx.annotation.Nullable;

/**
 * 2019/7/5 10:23
 *
 * @author zero.zhou
 */
public class DialView extends View {
    private static final String TAG = DialView.class.getSimpleName();

    private int frameColor;
    private int frameCoverColor;
    private float frameStrokeWidth;

    private int calibrationColor;
    private float calibrationScale;
    private float calibrationScale2;
    private float calibrationStrokeWidth;
    private float calibrationStrokeWidth2;

    private long runInterval;
    private long stopRunInterval;

    private float calibrationDegree;
    private int calibrationDegreeInterval;

    private Paint mPaint;
    private Paint mPaint_Calibration, mPaint_Calibration_Strong;

    private float frameRadius;
    private float frameCenterX;
    private float frameCenterY;

    private boolean isRunning = true;

    public DialView(Context context) {
        this(context, null);
    }

    public DialView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.dialViewStyle);
    }

    public DialView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttr(attrs, defStyleAttr, R.style.DialViewStyle);
        init();
    }

    private void initAttr(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        final TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.DialView, defStyleAttr, defStyleRes);
        for (int i = 0, n = a.getIndexCount(); i < n; i++) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.DialView_android_minWidth) {
                int size = a.getDimensionPixelSize(i, 0);
                setMinimumWidth(size);
            } else if (attr == R.styleable.DialView_android_minHeight) {
                int size = a.getDimensionPixelSize(i, 0);
                setMinimumHeight(size);
            } else if (attr == R.styleable.DialView_frameColor) {
                frameColor = a.getColor(i, Color.TRANSPARENT);
            } else if (attr == R.styleable.DialView_frameCoverColor) {
                frameCoverColor = a.getColor(i, Color.RED);
            } else if (attr == R.styleable.DialView_frameStrokeWidth) {
                frameStrokeWidth = a.getDimension(i, 15);
            } else if (attr == R.styleable.DialView_calibrationStrokeWidth) {
                calibrationStrokeWidth = a.getDimension(i, 2);
            } else if (attr == R.styleable.DialView_calibration2StrokeWidth) {
                calibrationStrokeWidth2 = a.getDimension(i, 3);
            } else if (attr == R.styleable.DialView_calibrationColor) {
                calibrationColor = a.getColor(i, Color.RED);
            } else if (attr == R.styleable.DialView_calibrationScale) {
                calibrationScale = a.getFloat(i, 0.3f);
            } else if (attr == R.styleable.DialView_calibration2Scale) {
                calibrationScale2 = a.getFloat(i, 0.5f);
            } else if (attr == R.styleable.DialView_calibrationDegree) {
                calibrationDegree = a.getFloat(i, 6f);
            } else if (attr == R.styleable.DialView_calibrationDegreeInterval) {
                calibrationDegreeInterval = a.getInteger(i, 5);
            } else if (attr == R.styleable.DialView_runInterval) {
                runInterval = a.getInteger(i, 1000);
            } else if (attr == R.styleable.DialView_stopRunInterval) {
                stopRunInterval = a.getInteger(i, 50);
            }
        }
        a.recycle();
    }

    private void init() {
        generatePaint();
        mPaint = new Paint();
    }

    private void generatePaint() {
        mPaint_Calibration = new Paint();
        mPaint_Calibration.setColor(calibrationColor);
        mPaint_Calibration.setStyle(Paint.Style.STROKE); // 设置描边
        mPaint_Calibration.setStrokeWidth(calibrationStrokeWidth); // 设置描边线的粗细
        mPaint_Calibration.setAntiAlias(true); // 设置抗锯齿，使圆形更加圆滑

        mPaint_Calibration_Strong = new Paint();
        mPaint_Calibration_Strong.setColor(calibrationColor);
        mPaint_Calibration_Strong.setStyle(Paint.Style.STROKE); // 设置描边
        mPaint_Calibration_Strong.setStrokeWidth(calibrationStrokeWidth2); // 设置描边线的粗细
        mPaint_Calibration_Strong.setAntiAlias(true); // 设置抗锯齿，使圆形更加圆滑
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measure(widthMeasureSpec), measure(heightMeasureSpec));
        int width1 = getMeasuredWidth() - (getPaddingLeft() + getPaddingRight());
        int height1 = getMeasuredHeight() - (getPaddingTop() + getPaddingBottom());
        frameRadius = Math.min(width1, height1) / 2f;
        frameCenterX = getPaddingLeft() + width1 / 2f;
        frameCenterY = getPaddingTop() + height1 / 2f;
    }

    private int measure(int origin) {
        int result = 0;
        int specMode = MeasureSpec.getMode(origin);
        int specSize = MeasureSpec.getSize(origin);
        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
            case MeasureSpec.AT_MOST:
                result = Math.min(getSuggestedMinimumWidth(), specSize);
                break;
            case MeasureSpec.EXACTLY:
                result = specSize;
                break;
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 绘制圆形部分
        drawFrameCircle(canvas);
        // 绘制刻度线
        drawCalibration(canvas);
        // 绘制运行时覆盖圆
        drawFrameCoverCircle(canvas);
    }

    /**
     * 绘制刻度线
     * 从正上方，即12点处开始绘制一条直线，后面的只是旋转一下画布角度即可
     */
    private void drawCalibration(Canvas canvas) {
        // 先保存一下当前画布的状态，因为后面画布会进行旋转操作，而在绘制完刻度后，需要恢复画布状态
        canvas.save();
        // 计算12点处刻度的开始坐标
        float startX = frameCenterX;
        float startY = frameCenterY - frameRadius;
        // 计算12点处的结束坐标
        float stopX = startX;
        float stopY1 = startY + frameStrokeWidth * calibrationScale; // 非整点处的线长度
        float stopY2 = startY + frameStrokeWidth * calibrationScale2; // 整点处的线长度
        // 绘制所有刻度
        for (int i = 0, total = (int) (360 / calibrationDegree); i < total; i++) {
            if (i % calibrationDegreeInterval == 0) {
                canvas.drawLine(startX, startY, stopX, stopY2, mPaint_Calibration_Strong); // 绘制整点长的刻度
            } else {
                canvas.drawLine(startX, startY, stopX, stopY1, mPaint_Calibration); // 绘制非整点处短的刻度
            }
            canvas.rotate(calibrationDegree, frameCenterX, frameCenterY); // 以圆中心进行旋转
        }
        canvas.restore();
    }

    /**
     * 绘制圆形部分
     */
    private void drawFrameCircle(Canvas canvas) {
        resetPaint();
        mPaint.setStrokeWidth(frameStrokeWidth);
        mPaint.setColor(frameColor);
        // 
        float width_half = frameStrokeWidth / 2f;
        canvas.drawCircle(frameCenterX, frameCenterY, frameRadius - width_half, mPaint);
    }

    private float runDegree = 0;

    /**
     * 绘制运行时覆盖圆
     */
    private void drawFrameCoverCircle(Canvas canvas) {
        resetPaint();
        mPaint.setStrokeWidth(frameStrokeWidth);
        mPaint.setColor(frameCoverColor);
        float width_half = frameStrokeWidth / 2f;
        RectF rectF = new RectF(frameCenterX - frameRadius, frameCenterY - frameRadius, frameCenterX + frameRadius, frameCenterY + frameRadius);
        rectF.inset(width_half, width_half);
        canvas.save();
        canvas.rotate(-90, frameCenterX, frameCenterY); // 以圆中心进行旋转
        if (runDegree <= 360) {
            canvas.drawArc(rectF, 0, runDegree, false, mPaint);
        } else {
            canvas.drawArc(rectF, runDegree % 360, 360 - runDegree % 360, false, mPaint);
        }
        canvas.restore();
    }

    /**
     * 计算下一个角度
     */
    private void generateNextDegree() {
        runDegree = (runDegree + calibrationDegree) % 720;
    }

    private void resetPaint() {
        mPaint.reset();
        mPaint.setStyle(Paint.Style.STROKE);// 设置描边
        mPaint.setAntiAlias(true);// 设置抗锯齿，使圆形更加圆滑
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d(TAG, "onAttachedToWindow");
        runDegree = 0;
        if (isRunning) {
            postDelayed(loopRun, 0);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(TAG, "onDetachedFromWindow");
        removeCallbacks(loopRun);
        removeCallbacks(stopRun);
    }

    private Runnable loopRun = new Runnable() {
        @Override
        public void run() {
            generateNextDegree();
            postInvalidate();
            if (isRunning) {
                postDelayed(loopRun, runInterval);
            }
        }
    };

    private Runnable stopRun = new Runnable() {
        @Override
        public void run() {
            generateNextDegree();
            postInvalidate();
            if (runDegree % 720 != 0) {
                postDelayed(stopRun, stopRunInterval);
            }
        }
    };

    protected void startTick() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        postDelayed(loopRun, 0);
    }

    protected void stopTick() {
        if (!isRunning) {
            return;
        }
        isRunning = false;
        removeCallbacks(loopRun);
        postDelayed(stopRun, 0);
    }
}
