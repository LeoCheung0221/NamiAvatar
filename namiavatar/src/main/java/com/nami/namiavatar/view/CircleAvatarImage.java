package com.nami.namiavatar.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.nami.namiavatar.R;

/**
 * author: leo on 2016/6/16 0016 11:33
 * email : leocheung4ever@gmail.com
 * description: custom circle image view
 * what & why is modified:
 */
public class CircleAvatarImage extends ImageView {

    //====================================== Constants =============================================
    private static final int DEFAULT_BORDER_WIDTH = 0; //默认边界宽度
    private static final int DEFAULT_BORDER_COLOR = Color.TRANSPARENT; //默认边界颜色-透明 即没有边界
    private static final boolean DEFAULT_BORDER_OVERLAY = false; //默认没有描边
    private static final int DEFAULT_FILL_COLOR = Color.TRANSPARENT; //默认填充色-透明 即没有填充色
    private static final ScaleType SCALE_TYPE = ScaleType.CENTER_CROP; //原比例方法图像


    //====================================== Variables =============================================
    private int mBorderWidth = DEFAULT_BORDER_WIDTH;
    private int mBorderColor = DEFAULT_BORDER_COLOR;
    private boolean mBorderOverlay = DEFAULT_BORDER_OVERLAY;
    private int mFillColor = DEFAULT_FILL_COLOR;

    private boolean mReady; //是否准备初始化
    private boolean mSetupPending;//是否即将开始进入设置
    private boolean mDisableCirculatTransformation; //

    private Bitmap mBitmap;
    private int mBitmapHeight; //位图高度
    private int mBitmapWeight; //位图宽度
    private BitmapShader mBitmapShader; //位图渲染器

    private final Paint mBitmapPaint = new Paint(); //位图画笔
    private final Paint mBorderPaint = new Paint(); //边界画笔
    private final Paint mFillPaint = new Paint(); //填充画笔

    private final RectF mBorderRect = new RectF();
    private final RectF mDrawableRect = new RectF();
    private float mDrawableRadius;
    private float mBorderRadius;


    //====================================== Constructor ===========================================
    public CircleAvatarImage(Context context) {
        this(context, null);
    }

    public CircleAvatarImage(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleAvatarImage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CircleAvatarImage, defStyleAttr, 0);
        mBorderWidth = ta.getDimensionPixelSize(R.styleable.CircleAvatarImage_civ_border_width, DEFAULT_BORDER_WIDTH);
        mBorderColor = ta.getColor(R.styleable.CircleAvatarImage_civ_border_color, DEFAULT_BORDER_COLOR);
        mBorderOverlay = ta.getBoolean(R.styleable.CircleAvatarImage_civ_border_overlay, DEFAULT_BORDER_OVERLAY);
        mFillColor = ta.getColor(R.styleable.CircleAvatarImage_civ_fill_color, DEFAULT_FILL_COLOR);
        ta.recycle(); //如果不回收 将会影响下一次设置

        init();
    }


    //====================================== Override Methods ======================================
    /*
     * 绘制控件
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (mDisableCirculatTransformation)
            super.onDraw(canvas);

        if (mBitmap == null)
            return;

        //如果填充色不为透明, 则填充画笔先进行一次圆绘制
        if (mFillColor != Color.TRANSPARENT)
            canvas.drawCircle(mDrawableRect.centerX(), mDrawableRect.centerY(), mDrawableRadius, mFillPaint);
        //以位图画笔进行圆绘制
        canvas.drawCircle(mDrawableRect.centerX(), mDrawableRect.centerY(), mDrawableRadius, mBitmapPaint);
        //如果边界大于0 ,则需要绘制
        if (mBorderWidth > 0)
            canvas.drawCircle(mBorderRect.centerX(), mBorderRect.centerY(), mBorderRadius, mBorderPaint);
    }

    /*
     * 控件大小在绘制完毕前 会不断重写该方法
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setup();
    }


    //====================================== Private Methods =======================================

    /**
     * 初始化控件
     */
    private void init() {
        super.setScaleType(SCALE_TYPE); //沿用父类scale type
        mReady = true;

        if (mSetupPending) {
            setup();
            mSetupPending = false;
        }
    }

    /**
     * 设置控件
     * 如果控件没有渲染好 则不进行设置
     */
    private void setup() {
        if (!mReady) {
            mSetupPending = true;
            return;
        }

        if (getWidth() == 0 && getHeight() == 0) {
            return;
        }

        if (mBitmap == null) { //如果bitmap对象为空 重新绘制控件
            invalidate();
            return;
        }

        mBitmapShader = new BitmapShader(mBitmap,
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP); //当超出范围绘制 会复制范围内边缘染色

        mBitmapPaint.setAntiAlias(true); //设置位图抗锯齿
        mBitmapPaint.setShader(mBitmapShader); //设置位图渲染器

        mBorderPaint.setStyle(Paint.Style.STROKE); //设置边界画笔类型 描边
        mBorderPaint.setAntiAlias(true); //设置边界抗锯齿
        mBorderPaint.setColor(mBorderColor); //设置边界颜色
        mBorderPaint.setStrokeWidth(mBorderWidth); //设置边界描边粗细度

        mFillPaint.setStyle(Paint.Style.FILL); //设置画笔类型  填充
        mFillPaint.setAntiAlias(true); //设置填充抗锯齿
        mFillPaint.setColor(mFillColor); //设置填充颜色

        mBitmapHeight = mBitmap.getHeight();
        mBitmapWeight = mBitmap.getWidth();

        mBorderRect.set(calculateBounds());
    }

    /**
     * 计算矩形边界 -> 包裹CircleImageView的边界
     *
     * @return
     */
    private RectF calculateBounds() {
        //计算实际可达的高度 宽度
        int actualWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int actualHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        int sideLength = Math.min(actualWidth, actualHeight);
        return null;
    }


    //====================================== Public Methods ========================================


    //====================================== Getter Setter =========================================
}
