package com.test.testwheelviewlib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Scroller;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("unused")
public class TimeSpannedWheelView<T> extends View implements Runnable {

    private static final String TAG = "WheelView";

    private static final int DEFAULT_SCROLL_DURATION = 250;
    private static final long DEFAULT_CLICK_CONFIRM = 120;

    //默认折射比值，通过字体大小来实现折射视觉差
    private static final float DEFAULT_REFRACT_RATIO = 1f;

    //文字对齐方式
    public static final int TEXT_ALIGN_LEFT = 0;
    public static final int TEXT_ALIGN_CENTER = 1;
    public static final int TEXT_ALIGN_RIGHT = 2;

    //滚动状态
    public static final int SCROLL_STATE_IDLE = 0;
    public static final int SCROLL_STATE_DRAGGING = 1;
    public static final int SCROLL_STATE_SCROLLING = 2;

    //弯曲效果对齐方式
    public static final int CURVED_ARC_DIRECTION_LEFT = 0;
    public static final int CURVED_ARC_DIRECTION_CENTER = 1;
    public static final int CURVED_ARC_DIRECTION_RIGHT = 2;

    public static final float DEFAULT_CURVED_FACTOR = 0.75f;

    //分割线填充类型
    public static final int DIVIDER_TYPE_FILL = 0;
    public static final int DIVIDER_TYPE_WRAP = 1;

    private TextPaint textPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
    //字体大小
    private float mTextSize;
    //是否自动调整字体大小以显示完全
    private boolean isAutoFitTextSize;
    private Paint.FontMetrics mFontMetrics;
    //每个item的高度
    private int mItemHeight;
    //文字的最大宽度
    private int mMaxTextWidth;
    //文字中心距离baseline的距离
    private int mCenterToBaselineY;
    //可见的item条数
    private int mVisibleItems;
    //每个item之间的空间，行间距
    private float mLineSpacing;
    //是否循环滚动
    private boolean isCyclic;
    //文字对齐方式
    @TextAlign
    private int mTextAlign;
    //文字颜色
    private int mTextColor;
    //选中item文字颜色
    private int mSelectedItemTextColor;

    //是否显示分割线
    private boolean isShowDivider;
    //分割线的颜色
    private int mDividerColor;
    //分割线高度
    private float mDividerSize;
    //分割线填充类型
    @DividerType
    private int mDividerType;
    //分割线类型为DIVIDER_TYPE_WRAP时 分割线左右两端距离文字的间距
    private float mDividerPaddingForWrap;
    //分割线两端形状，默认圆头
    private Paint.Cap mDividerCap = Paint.Cap.ROUND;
    //分割线和选中区域偏移，实现扩大选中区域
    private float mDividerOffset;

    //是否绘制选中区域
    private boolean isDrawSelectedRect;
    //选中区域颜色
    private int mSelectedRectColor;

    //文字起始X
    private int mStartX;
    //X轴中心点
    private int mCenterX;
    //Y轴中心点
    private int mCenterY;
    //选中边界的上下限制
    private int mSelectedItemTopLimit;
    private int mSelectedItemBottomLimit;
    //裁剪边界
    private int mClipLeft;
    private int mClipTop;
    private int mClipRight;
    private int mClipBottom;
    //绘制区域
    private Rect mDrawRect;
    //字体外边距，目的是留有边距
    private float mTextBoundaryMargin;
    //数据为Integer类型时，是否需要格式转换
    private boolean isIntegerNeedFormat;
    //数据为Integer类型时，转换格式，默认转换为两位数
    private String mIntegerFormat;

    //3D效果
    private Camera mCamera;
    private Matrix mMatrix;
    //是否是弯曲（3D）效果
    private boolean isCurved;
    //弯曲（3D）效果左右圆弧偏移效果方向 center 不偏移
    @CurvedArcDirection
    private int mCurvedArcDirection;
    //弯曲（3D）效果左右圆弧偏移效果系数 0-1之间 越大越明显
    private float mCurvedArcDirectionFactor;
    //选中后折射的偏移 与字体大小的比值，1为不偏移 越小偏移越明显
    //(普通效果和3d效果都适用)
    private float mRefractRatio;

    //数据列表
    @NonNull
    private List<T> mDataList = new ArrayList<>(1);
    //数据变化时，是否重置选中下标到第一个位置
    private boolean isResetSelectedPosition = false;

    private VelocityTracker mVelocityTracker;
    private int mMaxFlingVelocity;
    private int mMinFlingVelocity;
    private Scroller mScroller;

    //最小滚动距离，上边界
    private int mMinScrollY;
    //最大滚动距离，下边界
    private int mMaxScrollY;

    //Y轴滚动偏移
    private int mScrollOffsetY;
    //Y轴已滚动偏移，控制重绘次数
    private int mScrolledY = 0;
    //手指最后触摸的位置
    private float mLastTouchY;
    //手指按下时间，根据按下抬起时间差处理点击滚动
    private long mDownStartTime;
    //是否强制停止滚动
    private boolean isForceFinishScroll = false;
    //是否是快速滚动，快速滚动结束后跳转位置
    private boolean isFlingScroll;
    //当前选中的下标
    private int mSelectedItemPosition;
    //当前滚动经过的下标
    private int mCurrentScrollPosition;

    //字体
    private boolean mIsBoldForSelectedItem = false;
    //如果 mIsBoldForSelectedItem==true 则这个字体为未选中条目的字体
    private Typeface mNormalTypeface = null;
    //如果 mIsBoldForSelectedItem==true 则这个字体为选中条目的字体
    private Typeface mBoldTypeface = null;

    //监听器
    private OnItemSelectedListener<T> mOnItemSelectedListener;
    private OnWheelChangedListener mOnWheelChangedListener;

    //是否开启音频效果
    private boolean isSoundEffect = false;

    public TimeSpannedWheelView(Context context) {
        this(context, null);
    }

    public TimeSpannedWheelView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimeSpannedWheelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrsAndDefault(context, attrs);
        initValue(context);
    }

    /**
     * 初始化自定义属性及默认值
     *
     * @param context 上下文
     * @param attrs   attrs
     */
    private void initAttrsAndDefault(Context context, AttributeSet attrs) {
        mRefractRatio = isCurved ? DEFAULT_REFRACT_RATIO : mRefractRatio;
        if (mRefractRatio > 1f) {
            mRefractRatio = 1.0f;
        } else if (mRefractRatio < 0f) {
            mRefractRatio = DEFAULT_REFRACT_RATIO;
        }
    }

    /**
     * 初始化并设置默认值
     *
     * @param context 上下文
     */
    private void initValue(Context context) {
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mMaxFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        mMinFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        mScroller = new Scroller(context);
        mDrawRect = new Rect();
        mCamera = new Camera();
        mMatrix = new Matrix();

        calculateTextSize();
        updateTextAlign();
    }


    /**
     * Maximum space occupied by measurement text
     */
    private void calculateTextSize() {
        if (isCurved) {
            textPaint.setTextSize(mTextSize);
            for (int i = 0; i < mDataList.size(); i++) {
                int textWidth = (int) textPaint.measureText(getDataText(mDataList.get(i)));
                mMaxTextWidth = Math.max(textWidth, mMaxTextWidth);
            }

            mFontMetrics = textPaint.getFontMetrics();
            //itemHeight实际等于字体高度+一个行间距
            mItemHeight = (int) (mFontMetrics.bottom - mFontMetrics.top + mLineSpacing);
        } else {
            textPaint.setTextSize(mTextSize);
            for (int i = 0; i < mDataList.size(); i++) {
                CharSequence text = getDataText(mDataList.get(i));
                int textWidth = (int) textPaint.measureText(text, 0, text.length());
                mMaxTextWidth = Math.max(textWidth, mMaxTextWidth);
            }

            if (mDataList.size() != 0) {
                SpannableString spannableString = ((SpannableString) mDataList.get(0));
                StaticLayout layout = new StaticLayout(spannableString, textPaint, (int) StaticLayout.getDesiredWidth(spannableString, textPaint), Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
                mItemHeight = layout.getHeight();
            }
        }
    }

    /**
     * 更新textAlign
     */
    private void updateTextAlign() {
        switch (mTextAlign) {
            case TEXT_ALIGN_LEFT:
                textPaint.setTextAlign(Paint.Align.LEFT);
                break;
            case TEXT_ALIGN_RIGHT:
                textPaint.setTextAlign(Paint.Align.RIGHT);
                break;
            case TEXT_ALIGN_CENTER:
            default:
                textPaint.setTextAlign(Paint.Align.CENTER);
                break;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //Line Space算在了mItemHeight中
        int height;
        if (isCurved) {
            height = (int) ((mItemHeight * mVisibleItems * 2 / Math.PI) + getPaddingTop() + getPaddingBottom());
        } else {
            height = mItemHeight * mVisibleItems + getPaddingTop() + getPaddingBottom();
        }
        int width = (int) (mMaxTextWidth + getPaddingLeft() + getPaddingRight() + mTextBoundaryMargin * 2);
        if (isCurved) {
            int towardRange = (int) (Math.sin(Math.PI / 48) * height);
            width += towardRange;
        }
        setMeasuredDimension(resolveSizeAndState(width, widthMeasureSpec, 0),
                resolveSizeAndState(height, heightMeasureSpec, 0));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //设置内容可绘制区域
        mDrawRect.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        mCenterX = mDrawRect.centerX();
        mCenterY = mDrawRect.centerY();
        mSelectedItemTopLimit = (int) (mCenterY - mItemHeight / 2 - mDividerOffset);
        mSelectedItemBottomLimit = (int) (mCenterY + mItemHeight / 2 + mDividerOffset);
        mClipLeft = getPaddingLeft();
        mClipTop = getPaddingTop();
        mClipRight = getWidth() - getPaddingRight();
        mClipBottom = getHeight() - getPaddingBottom();

        calculateDrawStart();
        //计算滚动限制
        calculateLimitY();

        //如果初始化时有选中的下标，则计算选中位置的距离
        int itemDistance = calculateItemDistance(mSelectedItemPosition);
        if (itemDistance > 0) {
            doScroll(itemDistance);
        }
    }

    /**
     * 起算起始位置
     */
    private void calculateDrawStart() {
        switch (mTextAlign) {
            case TEXT_ALIGN_LEFT:
                mStartX = (int) (getPaddingLeft() + mTextBoundaryMargin);
                break;
            case TEXT_ALIGN_RIGHT:
                mStartX = (int) (getWidth() - getPaddingRight() - mTextBoundaryMargin);
                break;
            case TEXT_ALIGN_CENTER:
            default:
                mStartX = getWidth() / 2;
                break;
        }

        if (isCurved) {
            mCenterToBaselineY = (int) (mFontMetrics.ascent + (mFontMetrics.descent - mFontMetrics.ascent) / 2);
        } else {
            if (mDataList.size() != 0) {
                SpannableString spannableString = ((SpannableString) mDataList.get(0));
                StaticLayout layout = new StaticLayout(spannableString, textPaint, (int) StaticLayout.getDesiredWidth(spannableString, textPaint), Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
                mCenterToBaselineY = (int) (layout.getLineBaseline(0) / 2);
            }
        }
    }

    /**
     * 计算滚动限制
     */
    private void calculateLimitY() {
        mMinScrollY = isCyclic ? Integer.MIN_VALUE : 0;
        //下边界 (dataSize - 1 - mInitPosition) * mItemHeight
        mMaxScrollY = isCyclic ? Integer.MAX_VALUE : (mDataList.size() - 1) * mItemHeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //How many items are scrolled, the Y value height of scrolling minus the height of each item
        int scrolledItem = mScrollOffsetY / dividedItemHeight();
        //The offset value when an item is not scrolled, smoothly sliding
        int scrolledOffset = mScrollOffsetY % dividedItemHeight();
        //Improvement arrangement
        int halfItem = (mVisibleItems + 1) / 2;
        //Calculated minimum index
        int minIndex;
        //Calculated maximum index
        int maxIndex;
        if (scrolledOffset < 0) {
            //Less than 0
            minIndex = scrolledItem - halfItem - 1;
            maxIndex = scrolledItem + halfItem;
        } else if (scrolledOffset > 0) {
            minIndex = scrolledItem - halfItem;
            maxIndex = scrolledItem + halfItem + 1;
        } else {
            minIndex = scrolledItem - halfItem;
            maxIndex = scrolledItem + halfItem;
        }

        //Draw item
        for (int i = minIndex; i < maxIndex; i++) {
            if (isCurved) {
                draw3DItem(canvas, i, scrolledOffset);
            } else {
                drawItem(canvas, i, scrolledOffset);
            }
        }
    }

    /**
     * Draw 2D effect
     *
     * @param canvas         CANVAS
     * @param index          INDEX
     * @param scrolledOffset Scroll offset
     */
    private void drawItem(Canvas canvas, int index, int scrolledOffset) {
        SpannableString spannableString = getSpannableDataByIndex(index);

        if (spannableString == null) {
            return;
        }

        //The offset of the item of index from the middle item
        int item2CenterOffsetY = (index - mScrollOffsetY / dividedItemHeight()) * mItemHeight - scrolledOffset;
        //Record the font start X of the initial measurement
        int startX = mStartX;
        //Re-measure font width and baseline offset
        int centerToBaselineY = isAutoFitTextSize ? remeasureTextSize(spannableString) : mCenterToBaselineY;

        setGradient(spannableString);
        clipAndDraw2DText(canvas, spannableString, mSelectedItemTopLimit, mSelectedItemBottomLimit, item2CenterOffsetY, centerToBaselineY);
    }

    private void setGradient(SpannableString spannableString) {
        Log.d("currentPosition", String.valueOf(mCurrentScrollPosition));

        //Calculate bottom item data index
        int tempBottom = mCurrentScrollPosition + 1;
        int bottomPosition = 0;
        if (tempBottom < mDataList.size()) {
            bottomPosition = tempBottom;
        }
        Log.d("bottomPosition", String.valueOf(bottomPosition));

        //Calculate top item data index
        int tempTop = mCurrentScrollPosition - 1;
        int topPosition = mDataList.size() - 1;
        if (tempTop != -1) {
            topPosition = tempTop;
        }
        Log.d("topPosition", String.valueOf(topPosition));

        SpannableString selectedItem = (SpannableString) mDataList.get(mCurrentScrollPosition);
        SpannableString topItem = (SpannableString) mDataList.get(topPosition);
        SpannableString bottomItem = (SpannableString) mDataList.get(bottomPosition);

        LinearGradientTopToBottomSpan[] topToBottomSpans = selectedItem.getSpans(0, selectedItem.length(), LinearGradientTopToBottomSpan.class);
        LinearGradientBottomToTopSpan[] bottomToTopSpans = selectedItem.getSpans(0, selectedItem.length(), LinearGradientBottomToTopSpan.class);

        if (topToBottomSpans.length > 0) {
            for (LinearGradientTopToBottomSpan item : topToBottomSpans) selectedItem.removeSpan(item);
        }
        if (bottomToTopSpans.length > 0) {
            for (LinearGradientBottomToTopSpan item : bottomToTopSpans) selectedItem.removeSpan(item);
        }

        bottomItem.setSpan(new LinearGradientTopToBottomSpan(), 0, spannableString.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        topItem.setSpan(new LinearGradientBottomToTopSpan(), 0, spannableString.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
    }

    private void updateStartX(SpannableString selectedItem, SpannableString otherItem) {
        float selectedItemWidth = StaticLayout.getDesiredWidth(selectedItem, textPaint);
        float otherItemWidth = StaticLayout.getDesiredWidth(otherItem, textPaint);

        mStartX = (int) (selectedItemWidth - otherItemWidth);
    }

    /**
     * 裁剪并绘制2d text
     *
     * @param canvas             画布
     * @param text               绘制的文字
     * @param clipTop            裁剪的上边界
     * @param clipBottom         裁剪的下边界
     * @param item2CenterOffsetY 距离中间项的偏移
     * @param centerToBaselineY  文字中心距离baseline的距离
     */
    private void clipAndDraw2DText(Canvas canvas, SpannableString text, int clipTop, int clipBottom,
                                   int item2CenterOffsetY, int centerToBaselineY) {
        canvas.save();
        //canvas.clipRect(mClipLeft, clipTop, mClipRight, clipBottom);
        canvas.translate(mStartX, mCenterY + item2CenterOffsetY - centerToBaselineY);

        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        StaticLayout layout = createStaticLayout(text, getWidth(), textPaint);
        layout.draw(canvas);

        canvas.restore();
    }


    class LinearGradientTopToBottomSpan extends CharacterStyle implements UpdateAppearance {
        @Override
        public void updateDrawState(TextPaint textPaint) {
            if (textPaint == null) return;

            textPaint.setShader(
                    new LinearGradient(
                            0f,
                            0f,
                            0f,
                            mItemHeight,
                            ContextCompat.getColor(getContext(), R.color.test_color_with_alpha_50),
                            ContextCompat.getColor(getContext(), android.R.color.transparent),
                            Shader.TileMode.CLAMP
                    )
            );
        }
    }

    class LinearGradientBottomToTopSpan extends CharacterStyle implements UpdateAppearance {
        @Override
        public void updateDrawState(TextPaint textPaint) {
            if (textPaint == null) return;

            textPaint.setShader(
                    new LinearGradient(
                            0f,
                            mItemHeight,
                            0f,
                            0f,
                            ContextCompat.getColor(getContext(), R.color.test_color_with_alpha_50),
                            ContextCompat.getColor(getContext(), android.R.color.transparent),
                            Shader.TileMode.CLAMP
                    )
            );
        }
    }

    private StaticLayout createStaticLayout(SpannableString text, Integer textWidth, TextPaint paint) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return StaticLayout.Builder.obtain(
                    text,
                    0,
                    text.length(),
                    paint,
                    textWidth
            )
                    .setIncludePad(false)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setMaxLines(1)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build();
        } else {
            StaticLayout layout;
            int maxLength = Math.min(text.length(), 200);

            do {
                layout = new StaticLayout(
                        ellipsize(text, maxLength, '…'),
                        paint,
                        textWidth,
                        Layout.Alignment.ALIGN_NORMAL,
                        1, 0, false
                );
                maxLength -= 10;
            } while (layout.getLineCount() > 1);

            return layout;
        }
    }

    private CharSequence ellipsize(
            SpannableString text,
            Integer size,
            Character ending
    ) {
        if (text.length() == 0 || size <= 0) {
            return new SpannableString("");
        } else if (text.length() <= size) {
            return text;
        } else {
            CharSequence sbString = text.subSequence(0, Math.max(size - 1, 0));
            return TextUtils.concat(sbString, ending.toString());
        }
    }

    /**
     * 重新测量字体大小
     *
     * @param contentText 被测量文字内容
     * @return 文字中心距离baseline的距离
     */
    private int remeasureTextSize(CharSequence contentText) {
        float textWidth = StaticLayout.getDesiredWidth(contentText, textPaint);
        float drawWidth = getWidth();
        float textMargin = mTextBoundaryMargin * 2;
        //稍微增加了一点文字边距 最大为宽度的1/10
        if (textMargin > (drawWidth / 10f)) {
            drawWidth = drawWidth * 9f / 10f;
            textMargin = drawWidth / 10f;
        } else {
            drawWidth = drawWidth - textMargin;
        }
        if (drawWidth <= 0) {
            return mCenterToBaselineY;
        }
        float textSize = mTextSize;
        while (textWidth > drawWidth) {
            textSize--;
            if (textSize <= 0) {
                break;
            }
            textPaint.setTextSize(textSize);
            textWidth = textPaint.measureText(contentText, 0, contentText.length());
        }
        //重新计算文字起始X
        recalculateStartX(textMargin / 2.0f);
        //高度起点也变了
        return recalculateCenterToBaselineY();
    }

    /**
     * 重新计算字体起始X
     *
     * @param textMargin 文字外边距
     */
    private void recalculateStartX(float textMargin) {
        switch (mTextAlign) {
            case TEXT_ALIGN_LEFT:
                mStartX = (int) textMargin;
                break;
            case TEXT_ALIGN_RIGHT:
                mStartX = (int) (getWidth() - textMargin);
                break;
            case TEXT_ALIGN_CENTER:
            default:
                mStartX = getWidth() / 2;
                break;
        }
    }

    /**
     * 字体大小变化后重新计算距离基线的距离
     *
     * @return 文字中心距离baseline的距离
     */
    private int recalculateCenterToBaselineY() {
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        //高度起点也变了
        return (int) (fontMetrics.ascent + (fontMetrics.descent - fontMetrics.ascent) / 2);
    }

    /**
     * 绘制弯曲（3D）效果的item
     *
     * @param canvas         画布
     * @param index          下标
     * @param scrolledOffset 滚动偏移
     */
    private void draw3DItem(Canvas canvas, int index, int scrolledOffset) {
        CharSequence text = getDataByIndex(index);
        if (text == null) {
            return;
        }
        // 滚轮的半径
        final int radius = (getHeight() - getPaddingTop() - getPaddingBottom()) / 2;
        //index 的 item 距离中间项的偏移
        int item2CenterOffsetY = (index - mScrollOffsetY / dividedItemHeight()) * mItemHeight - scrolledOffset;

        // 当滑动的角度和y轴垂直时（此时文字已经显示为一条线），不绘制文字
        if (Math.abs(item2CenterOffsetY) > radius * Math.PI / 2) return;

        final double angle = (double) item2CenterOffsetY / radius;
        // 绕x轴滚动的角度
        float rotateX = (float) Math.toDegrees(-angle);
        // 滚动的距离映射到y轴的长度
        float translateY = (float) (Math.sin(angle) * radius);
        // 滚动的距离映射到z轴的长度
        float translateZ = (float) ((1 - Math.cos(angle)) * radius);
        // 透明度
        int alpha = (int) (Math.cos(angle) * 255);

        //记录初始测量的字体起始X
        int startX = mStartX;
        //重新测量字体宽度和基线偏移
        int centerToBaselineY = isAutoFitTextSize ? remeasureTextSize(text) : mCenterToBaselineY;
        if (Math.abs(item2CenterOffsetY) <= 0) {
            //绘制选中的条目
            textPaint.setColor(mSelectedItemTextColor);
            textPaint.setAlpha(255);
            clipAndDraw3DText(canvas, text, mSelectedItemTopLimit, mSelectedItemBottomLimit,
                    rotateX, translateY, translateZ, centerToBaselineY);
        } else if (item2CenterOffsetY > 0 && item2CenterOffsetY < mItemHeight) {
            //绘制与下边界交汇的条目
            textPaint.setColor(mSelectedItemTextColor);
            textPaint.setAlpha(255);
            clipAndDraw3DText(canvas, text, mSelectedItemTopLimit, mSelectedItemBottomLimit,
                    rotateX, translateY, translateZ, centerToBaselineY);

            textPaint.setColor(mTextColor);
            textPaint.setAlpha(alpha);
            //缩小字体，实现折射效果
            float textSize = textPaint.getTextSize();
            textPaint.setTextSize(textSize * mRefractRatio);
            //mIsBoldForSelectedItem==true 改变字体
            changeTypefaceIfBoldForSelectedItem();
            //字体变化，重新计算距离基线偏移
            int reCenterToBaselineY = recalculateCenterToBaselineY();
            clipAndDraw3DText(canvas, text, mSelectedItemBottomLimit, mClipBottom,
                    rotateX, translateY, translateZ, reCenterToBaselineY);
            textPaint.setTextSize(textSize);
            //mIsBoldForSelectedItem==true 恢复字体
            resetTypefaceIfBoldForSelectedItem();
        } else if (item2CenterOffsetY < 0 && item2CenterOffsetY > -mItemHeight) {
            //绘制与上边界交汇的条目
            textPaint.setColor(mSelectedItemTextColor);
            textPaint.setAlpha(255);
            clipAndDraw3DText(canvas, text, mSelectedItemTopLimit, mSelectedItemBottomLimit,
                    rotateX, translateY, translateZ, centerToBaselineY);

            textPaint.setColor(mTextColor);
            textPaint.setAlpha(alpha);

            //缩小字体，实现折射效果
            float textSize = textPaint.getTextSize();
            textPaint.setTextSize(textSize * mRefractRatio);
            //mIsBoldForSelectedItem==true 改变字体
            changeTypefaceIfBoldForSelectedItem();
            //字体变化，重新计算距离基线偏移
            int reCenterToBaselineY = recalculateCenterToBaselineY();
            clipAndDraw3DText(canvas, text, mClipTop, mSelectedItemTopLimit,
                    rotateX, translateY, translateZ, reCenterToBaselineY);
            textPaint.setTextSize(textSize);
            //mIsBoldForSelectedItem==true 恢复字体
            resetTypefaceIfBoldForSelectedItem();
        } else {
            //绘制其他条目
            textPaint.setColor(mTextColor);
            textPaint.setAlpha(alpha);

            //缩小字体，实现折射效果
            float textSize = textPaint.getTextSize();
            textPaint.setTextSize(textSize * mRefractRatio);
            //mIsBoldForSelectedItem==true 改变字体
            changeTypefaceIfBoldForSelectedItem();
            //字体变化，重新计算距离基线偏移
            int reCenterToBaselineY = recalculateCenterToBaselineY();
            clipAndDraw3DText(canvas, text, mClipTop, mClipBottom,
                    rotateX, translateY, translateZ, reCenterToBaselineY);
            textPaint.setTextSize(textSize);
            //mIsBoldForSelectedItem==true 恢复字体
            resetTypefaceIfBoldForSelectedItem();
        }

        if (isAutoFitTextSize) {
            //恢复重新测量之前的样式
            textPaint.setTextSize(mTextSize);
            mStartX = startX;
        }
    }

    /**
     * 裁剪并绘制弯曲（3D）效果
     *
     * @param canvas            画布
     * @param text              绘制的文字
     * @param clipTop           裁剪的上边界
     * @param clipBottom        裁剪的下边界
     * @param rotateX           绕X轴旋转角度
     * @param offsetY           Y轴偏移
     * @param offsetZ           Z轴偏移
     * @param centerToBaselineY 文字中心距离baseline的距离
     */
    private void clipAndDraw3DText(Canvas canvas, CharSequence text, int clipTop, int clipBottom,
                                   float rotateX, float offsetY, float offsetZ, int centerToBaselineY) {

        canvas.save();
        canvas.clipRect(mClipLeft, clipTop, mClipRight, clipBottom);
        draw3DText(canvas, text, rotateX, offsetY, offsetZ, centerToBaselineY);
        canvas.restore();
    }

    /**
     * 绘制弯曲（3D）的文字
     *
     * @param canvas            画布
     * @param text              绘制的文字
     * @param rotateX           绕X轴旋转角度
     * @param offsetY           Y轴偏移
     * @param offsetZ           Z轴偏移
     * @param centerToBaselineY 文字中心距离baseline的距离
     */
    private void draw3DText(Canvas canvas, CharSequence text, float rotateX, float offsetY,
                            float offsetZ, int centerToBaselineY) {
        mCamera.save();
        mCamera.translate(0, 0, offsetZ);
        mCamera.rotateX(rotateX);
        mCamera.getMatrix(mMatrix);
        mCamera.restore();

        // 调节中心点
        float centerX = mCenterX;
        //根据弯曲（3d）对齐方式设置系数
        if (mCurvedArcDirection == CURVED_ARC_DIRECTION_LEFT) {
            centerX = mCenterX * (1 + mCurvedArcDirectionFactor);
        } else if (mCurvedArcDirection == CURVED_ARC_DIRECTION_RIGHT) {
            centerX = mCenterX * (1 - mCurvedArcDirectionFactor);
        }

        float centerY = mCenterY + offsetY;
        mMatrix.preTranslate(-centerX, -centerY);
        mMatrix.postTranslate(centerX, centerY);

        canvas.concat(mMatrix);
        canvas.drawText(text, 0, text.length(), mStartX, centerY - centerToBaselineY, textPaint);
    }

    private void changeTypefaceIfBoldForSelectedItem() {
        if (mIsBoldForSelectedItem) {
            textPaint.setTypeface(mNormalTypeface);
        }
    }

    private void resetTypefaceIfBoldForSelectedItem() {
        if (mIsBoldForSelectedItem) {
            textPaint.setTypeface(mBoldTypeface);
        }
    }

    /**
     * 根据下标获取到内容
     *
     * @param index 下标
     * @return 绘制的文字内容
     */
    private CharSequence getDataByIndex(int index) {
        int dataSize = mDataList.size();
        if (dataSize == 0) {
            return null;
        }

        CharSequence itemText = null;
        if (isCyclic) {
            int i = index % dataSize;
            if (i < 0) {
                i += dataSize;
            }
            itemText = getDataText(mDataList.get(i));
        } else {
            if (index >= 0 && index < dataSize) {
                itemText = getDataText(mDataList.get(index));
            }
        }
        return itemText;
    }

    private SpannableString getSpannableDataByIndex(int index) {
        int dataSize = mDataList.size();
        if (dataSize == 0) {
            return null;
        }

        SpannableString itemText = null;
        if (isCyclic) {
            int i = index % dataSize;
            if (i < 0) {
                i += dataSize;
            }
            itemText = (SpannableString) mDataList.get(i);
        } else {
            if (index >= 0 && index < dataSize) {
                itemText = (SpannableString) mDataList.get(index);
            }
        }
        return itemText;
    }

    /**
     * 获取item text
     *
     * @param item item数据
     * @return 文本内容
     */
    protected String getDataText(T item) {
        if (item == null) {
            return "";
        } else if (item instanceof IWheelEntity) {
            return ((IWheelEntity) item).getWheelText();
        } else if (item instanceof Integer) {
            //如果为整形则最少保留两位数.
            return isIntegerNeedFormat ? String.format(Locale.getDefault(), mIntegerFormat, item)
                    : String.valueOf(item);
        } else if (item instanceof String) {
            return (String) item;
        }

        return item.toString();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //屏蔽如果未设置数据时，触摸导致运算数据不正确的崩溃 issue #20
        if (!isEnabled() || mDataList.isEmpty()) {
            return super.onTouchEvent(event);
        }
        initVelocityTracker();
        mVelocityTracker.addMovement(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                //手指按下
                //处理滑动事件嵌套 拦截事件序列
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                //如果未滚动完成，强制滚动完成
                if (!mScroller.isFinished()) {
                    //强制滚动完成
                    mScroller.forceFinished(true);
                    isForceFinishScroll = true;
                }
                mLastTouchY = event.getY();
                //按下时间
                mDownStartTime = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_MOVE:
                //手指移动
                float moveY = event.getY();
                float deltaY = moveY - mLastTouchY;

                if (mOnWheelChangedListener != null) {
                    mOnWheelChangedListener.onWheelScrollStateChanged(SCROLL_STATE_DRAGGING);
                }
                onWheelScrollStateChanged(SCROLL_STATE_DRAGGING);
                if (Math.abs(deltaY) < 1) {
                    break;
                }
                //deltaY 上滑为正，下滑为负
                doScroll((int) -deltaY);
                mLastTouchY = moveY;
                invalidateIfYChanged();

                break;
            case MotionEvent.ACTION_UP:
                //手指抬起
                isForceFinishScroll = false;
                mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                float velocityY = mVelocityTracker.getYVelocity();
                if (Math.abs(velocityY) > mMinFlingVelocity) {
                    //快速滑动
                    mScroller.forceFinished(true);
                    isFlingScroll = true;
                    mScroller.fling(0, mScrollOffsetY, 0, (int) -velocityY, 0, 0,
                            mMinScrollY, mMaxScrollY);
                } else {
                    int clickToCenterDistance = 0;
                    if (System.currentTimeMillis() - mDownStartTime <= DEFAULT_CLICK_CONFIRM) {
                        //处理点击滚动
                        //手指抬起的位置到中心的距离为滚动差值
                        clickToCenterDistance = (int) (event.getY() - mCenterY);
                    }
                    int scrollRange = clickToCenterDistance +
                            calculateDistanceToEndPoint((mScrollOffsetY + clickToCenterDistance) % dividedItemHeight());
                    //大于最小值滚动值
                    boolean isInMinRange = scrollRange < 0 && mScrollOffsetY + scrollRange >= mMinScrollY;
                    //小于最大滚动值
                    boolean isInMaxRange = scrollRange > 0 && mScrollOffsetY + scrollRange <= mMaxScrollY;
                    if (isInMinRange || isInMaxRange) {
                        //在滚动范围之内再修正位置
                        //平稳滑动
                        mScroller.startScroll(0, mScrollOffsetY, 0, scrollRange);
                    }
                }

                invalidateIfYChanged();
                ViewCompat.postOnAnimation(this, this);
                //回收 VelocityTracker
                recycleVelocityTracker();
                break;
            case MotionEvent.ACTION_CANCEL:
                //事件被终止
                //回收
                recycleVelocityTracker();
                break;
        }
        return true;
    }

    /**
     * 初始化 VelocityTracker
     */
    private void initVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    /**
     * 回收 VelocityTracker
     */
    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    /**
     * 计算滚动偏移
     *
     * @param distance 滚动距离
     */
    private void doScroll(int distance) {
        mScrollOffsetY += distance;
        if (!isCyclic) {
            //修正边界
            if (mScrollOffsetY < mMinScrollY) {
                mScrollOffsetY = mMinScrollY;
            } else if (mScrollOffsetY > mMaxScrollY) {
                mScrollOffsetY = mMaxScrollY;
            }
        }
    }

    /**
     * 当Y轴的偏移值改变时再重绘，减少重回次数
     */
    private void invalidateIfYChanged() {
        if (mScrollOffsetY != mScrolledY) {
            mScrolledY = mScrollOffsetY;
            //滚动偏移发生变化
            if (mOnWheelChangedListener != null) {
                mOnWheelChangedListener.onWheelScroll(mScrollOffsetY);
            }
            onWheelScroll(mScrollOffsetY);
            //观察item变化
            observeItemChanged();
            invalidate();
        }
    }

    /**
     * 观察item改变
     */
    private void observeItemChanged() {
        //item改变回调
        int oldPosition = mCurrentScrollPosition;
        int newPosition = getCurrentPosition();
        if (oldPosition != newPosition) {
            //改变了
            if (mOnWheelChangedListener != null) {
                mOnWheelChangedListener.onWheelItemChanged(oldPosition, newPosition);
            }
            onWheelItemChanged(oldPosition, newPosition);
            //更新下标
            mCurrentScrollPosition = newPosition;
        }
    }

    /**
     * 强制滚动完成，直接停止
     */
    public void forceFinishScroll() {
        if (!mScroller.isFinished()) {
            mScroller.forceFinished(true);
        }
    }

    /**
     * 强制滚动完成，并且直接滚动到最终位置
     */
    public void abortFinishScroll() {
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
    }

    /**
     * 计算距离终点的偏移，修正选中条目
     *
     * @param remainder 余数
     * @return 偏移量
     */
    private int calculateDistanceToEndPoint(int remainder) {
        if (Math.abs(remainder) > mItemHeight / 2) {
            if (mScrollOffsetY < 0) {
                return -mItemHeight - remainder;
            } else {
                return mItemHeight - remainder;
            }
        } else {
            return -remainder;
        }
    }

    /**
     * The run method is used instead of compute Scroll because invalidate will also execute compute Scroll, resulting in inaccurate callback execution
     */
    @Override
    public void run() {
        //Stop scrolling to update the current subscript
        if (mScroller.isFinished() && !isForceFinishScroll && !isFlingScroll) {
            if (mItemHeight == 0) return;
            //Scrolling stopped
            if (mOnWheelChangedListener != null) {
                mOnWheelChangedListener.onWheelScrollStateChanged(SCROLL_STATE_IDLE);
            }
            onWheelScrollStateChanged(SCROLL_STATE_IDLE);
            int currentItemPosition = getCurrentPosition();
            //No callback when the currently selected Position has not changed onItemSelected()
            if (currentItemPosition == mSelectedItemPosition) {
                return;
            }
            mSelectedItemPosition = currentItemPosition;
            //Re-assign after stopping
            mCurrentScrollPosition = mSelectedItemPosition;

            //Stop scrolling, select the item to call back
            if (mOnItemSelectedListener != null) {
                mOnItemSelectedListener.onItemSelected(this, mDataList.get(mSelectedItemPosition), mSelectedItemPosition);
            }
            onItemSelected(mDataList.get(mSelectedItemPosition), mSelectedItemPosition);
            //Scroll state callback
            if (mOnWheelChangedListener != null) {
                mOnWheelChangedListener.onWheelSelected(mSelectedItemPosition);
            }
            onWheelSelected(mSelectedItemPosition);
        }

        if (mScroller.computeScrollOffset()) {
            int oldY = mScrollOffsetY;
            mScrollOffsetY = mScroller.getCurrY();

            if (oldY != mScrollOffsetY) {
                if (mOnWheelChangedListener != null) {
                    mOnWheelChangedListener.onWheelScrollStateChanged(SCROLL_STATE_SCROLLING);
                }
                onWheelScrollStateChanged(SCROLL_STATE_SCROLLING);
            }
            invalidateIfYChanged();
            ViewCompat.postOnAnimation(this, this);
        } else if (isFlingScroll) {
            //After the scrolling is completed, the final position needs to be adjusted according to whether it is a fast scrolling process
            isFlingScroll = false;
            //After fast scrolling, you need to adjust the final position after scrolling, restart the scroll to slide to the center position
            mScroller.startScroll(0, mScrollOffsetY, 0, calculateDistanceToEndPoint(mScrollOffsetY % dividedItemHeight()));
            invalidateIfYChanged();
            ViewCompat.postOnAnimation(this, this);
        }
    }

    /**
     * Calculate the current position index based on the offset
     *
     * @return The current subscript corresponding to the offset if dataList is empty return -1
     */
    private int getCurrentPosition() {
        if (mDataList.isEmpty()) {
            return -1;
        }
        int itemPosition;
        if (mScrollOffsetY < 0) {
            itemPosition = (mScrollOffsetY - mItemHeight / 2) / dividedItemHeight();
        } else {
            itemPosition = (mScrollOffsetY + mItemHeight / 2) / dividedItemHeight();
        }
        int currentPosition = itemPosition % mDataList.size();
        if (currentPosition < 0) {
            currentPosition += mDataList.size();
        }

        return currentPosition;
    }

    /**
     * mItemHeight Avoid zero when it is the dividend
     *
     * @return Dividend is not 0
     */
    private int dividedItemHeight() {
        return mItemHeight > 0 ? mItemHeight : 1;
    }

    /**
     * 获取音效开关状态
     *
     * @return 是否开启滚动音效
     */
    public boolean isSoundEffect() {
        return isSoundEffect;
    }

    /**
     * 设置音效开关
     *
     * @param isSoundEffect 是否开启滚动音效
     */
    public void setSoundEffect(boolean isSoundEffect) {
        this.isSoundEffect = isSoundEffect;
    }

    /**
     * 获取指定 position 的数据
     *
     * @param position 下标
     * @return position 对应的数据 {@link Nullable}
     */
    @Nullable
    public T getItemData(int position) {
        if (isPositionInRange(position)) {
            return mDataList.get(position);
        } else if (mDataList.size() > 0 && position >= mDataList.size()) {
            return mDataList.get(mDataList.size() - 1);
        } else if (mDataList.size() > 0 && position < 0) {
            return mDataList.get(0);
        }
        return null;
    }

    /**
     * 获取当前选中的item数据
     *
     * @return 当前选中的item数据
     */
    public T getSelectedItemData() {
        return getItemData(mSelectedItemPosition);
    }

    /**
     * 获取数据列表
     *
     * @return 数据列表
     */
    public List<T> getData() {
        return mDataList;
    }

    /**
     * 设置数据
     *
     * @param dataList 数据列表
     */
    public void setData(List<T> dataList) {
        if (dataList == null) {
            return;
        }

        mDataList = dataList;

        setLayoutParams(new LinearLayout.LayoutParams((int)StaticLayout.getDesiredWidth((SpannableString) mDataList.get(0), textPaint), FrameLayout.LayoutParams.WRAP_CONTENT));

        if (!isResetSelectedPosition && mDataList.size() > 0) {
            //不重置选中下标
            if (mSelectedItemPosition >= mDataList.size()) {
                mSelectedItemPosition = mDataList.size() - 1;
                //重置滚动下标
                mCurrentScrollPosition = mSelectedItemPosition;
            }
        } else {
            //重置选中下标和滚动下标
            mCurrentScrollPosition = mSelectedItemPosition = 0;
        }
        //强制滚动完成
        forceFinishScroll();
        calculateTextSize();
        calculateLimitY();
        //重置滚动偏移
        mScrollOffsetY = mSelectedItemPosition * mItemHeight;
        requestLayout();
        invalidate();
    }

    /**
     * 当数据变化时，是否重置选中下标到第一个
     *
     * @return 是否重置选中下标到第一个
     */
    public boolean isResetSelectedPosition() {
        return isResetSelectedPosition;
    }

    /**
     * 设置当数据变化时，是否重置选中下标到第一个
     *
     * @param isResetSelectedPosition 当数据变化时,是否重置选中下标到第一个
     */
    public void setResetSelectedPosition(boolean isResetSelectedPosition) {
        this.isResetSelectedPosition = isResetSelectedPosition;
    }

    /**
     * 获取字体大小
     *
     * @return 字体大小
     */
    public float getTextSize() {
        return mTextSize;
    }

    /**
     * 设置字体大小
     *
     * @param textSize 字体大小
     */
    public void setTextSize(float textSize) {
        setTextSize(textSize, false);
    }

    /**
     * 设置字体大小
     *
     * @param textSize 字体大小
     * @param isSp     单位是否是 sp
     */
    public void setTextSize(float textSize, boolean isSp) {
        float tempTextSize = mTextSize;
        mTextSize = isSp ? sp2px(textSize) : textSize;
        if (tempTextSize == mTextSize) {
            return;
        }
        //强制滚动完成
        forceFinishScroll();
        calculateTextSize();
        calculateDrawStart();
        calculateLimitY();
        //字体大小变化，偏移距离也变化了
        mScrollOffsetY = mSelectedItemPosition * mItemHeight;
        requestLayout();
        invalidate();
    }

    /**
     * 获取是否自动调整字体大小，以显示完全
     *
     * @return 是否自动调整字体大小
     */
    public boolean isAutoFitTextSize() {
        return isAutoFitTextSize;
    }

    /**
     * 设置是否自动调整字体大小，以显示完全
     *
     * @param isAutoFitTextSize 是否自动调整字体大小
     */
    public void setAutoFitTextSize(boolean isAutoFitTextSize) {
        this.isAutoFitTextSize = isAutoFitTextSize;
        invalidate();
    }

    /**
     * 获取当前字体
     *
     * @return 字体
     */
    public Typeface getTypeface() {
        return textPaint.getTypeface();
    }

    /**
     * 设置当前字体
     *
     * @param typeface 字体
     */
    public void setTypeface(Typeface typeface) {
        setTypeface(typeface, false);
    }

    /**
     * 设置当前字体
     *
     * @param typeface              字体
     * @param isBoldForSelectedItem 是否设置选中条目字体加粗，其他条目不会加粗
     */
    public void setTypeface(Typeface typeface, boolean isBoldForSelectedItem) {
        if (typeface == null || textPaint.getTypeface() == typeface) {
            return;
        }
        //强制滚动完成
        forceFinishScroll();
        mIsBoldForSelectedItem = isBoldForSelectedItem;
        if (mIsBoldForSelectedItem) {
            //如果设置了选中条目字体加粗，其他条目不会加粗，则拆分两份字体
            if (typeface.isBold()) {
                mNormalTypeface = Typeface.create(typeface, Typeface.NORMAL);
                mBoldTypeface = typeface;
            } else {
                mNormalTypeface = typeface;
                mBoldTypeface = Typeface.create(typeface, Typeface.BOLD);
            }
            //测量时 使用加粗字体测量，因为加粗字体比普通字体宽，以大的为准进行测量
            textPaint.setTypeface(mBoldTypeface);
        } else {
            textPaint.setTypeface(typeface);
        }
        calculateTextSize();
        calculateDrawStart();
        //字体大小变化，偏移距离也变化了
        mScrollOffsetY = mSelectedItemPosition * mItemHeight;
        calculateLimitY();
        requestLayout();
        invalidate();
    }

    /**
     * 获取文字对齐方式
     *
     * @return 文字对齐
     * {@link #TEXT_ALIGN_LEFT}
     * {@link #TEXT_ALIGN_CENTER}
     * {@link #TEXT_ALIGN_RIGHT}
     */
    public int getTextAlign() {
        return mTextAlign;
    }

    /**
     * 设置文字对齐方式
     *
     * @param textAlign 文字对齐方式
     *                  {@link #TEXT_ALIGN_LEFT}
     *                  {@link #TEXT_ALIGN_CENTER}
     *                  {@link #TEXT_ALIGN_RIGHT}
     */
    public void setTextAlign(@TextAlign int textAlign) {
        if (mTextAlign == textAlign) {
            return;
        }
        mTextAlign = textAlign;
        updateTextAlign();
        calculateDrawStart();
        invalidate();
    }

    /**
     * 获取未选中条目颜色
     *
     * @return 未选中条目颜色 ColorInt
     */
    public int getNormalItemTextColor() {
        return mTextColor;
    }

    /**
     * 设置未选中条目颜色
     *
     * @param textColorRes 未选中条目颜色 {@link ColorRes}
     */
    public void setNormalItemTextColorRes(@ColorRes int textColorRes) {
        setNormalItemTextColor(ContextCompat.getColor(getContext(), textColorRes));
    }

    /**
     * 设置未选中条目颜色
     *
     * @param textColor 未选中条目颜色 {@link ColorInt}
     */
    public void setNormalItemTextColor(@ColorInt int textColor) {
        if (mTextColor == textColor) {
            return;
        }
        mTextColor = textColor;
        invalidate();
    }

    /**
     * 获取选中条目颜色
     *
     * @return 选中条目颜色 ColorInt
     */
    public int getSelectedItemTextColor() {
        return mSelectedItemTextColor;
    }

    /**
     * 设置选中条目颜色
     *
     * @param selectedItemColorRes 选中条目颜色 {@link ColorRes}
     */
    public void setSelectedItemTextColorRes(@ColorRes int selectedItemColorRes) {
        setSelectedItemTextColor(ContextCompat.getColor(getContext(), selectedItemColorRes));
    }

    /**
     * 设置选中条目颜色
     *
     * @param selectedItemTextColor 选中条目颜色 {@link ColorInt}
     */
    public void setSelectedItemTextColor(@ColorInt int selectedItemTextColor) {
        if (mSelectedItemTextColor == selectedItemTextColor) {
            return;
        }
        mSelectedItemTextColor = selectedItemTextColor;
        invalidate();
    }

    /**
     * 获取文字距离边界的外边距
     *
     * @return 外边距值
     */
    public float getTextBoundaryMargin() {
        return mTextBoundaryMargin;
    }

    /**
     * 设置文字距离边界的外边距
     *
     * @param textBoundaryMargin 外边距值
     */
    public void setTextBoundaryMargin(float textBoundaryMargin) {
        setTextBoundaryMargin(textBoundaryMargin, false);
    }

    /**
     * 设置文字距离边界的外边距
     *
     * @param textBoundaryMargin 外边距值
     * @param isDp               单位是否为 dp
     */
    public void setTextBoundaryMargin(float textBoundaryMargin, boolean isDp) {
        float tempTextBoundaryMargin = mTextBoundaryMargin;
        mTextBoundaryMargin = isDp ? dp2px(textBoundaryMargin) : textBoundaryMargin;
        if (tempTextBoundaryMargin == mTextBoundaryMargin) {
            return;
        }
        requestLayout();
        invalidate();
    }

    /**
     * 获取item间距
     *
     * @return 行间距值
     */
    public float getLineSpacing() {
        return mLineSpacing;
    }

    /**
     * 设置item间距
     *
     * @param lineSpacing 行间距值
     */
    public void setLineSpacing(float lineSpacing) {
        setLineSpacing(lineSpacing, false);
    }

    /**
     * 设置item间距
     *
     * @param lineSpacing 行间距值
     * @param isDp        lineSpacing 单位是否为 dp
     */
    public void setLineSpacing(float lineSpacing, boolean isDp) {
        float tempLineSpace = mLineSpacing;
        mLineSpacing = isDp ? dp2px(lineSpacing) : lineSpacing;
        if (tempLineSpace == mLineSpacing) {
            return;
        }
        mScrollOffsetY = 0;
        calculateTextSize();
        requestLayout();
        invalidate();
    }

    /**
     * 获取数据为Integer类型时是否需要转换
     *
     * @return isIntegerNeedFormat
     */
    public boolean isIntegerNeedFormat() {
        return isIntegerNeedFormat;
    }

    /**
     * 设置数据为Integer类型时是否需要转换
     *
     * @param isIntegerNeedFormat 数据为Integer类型时是否需要转换
     */
    public void setIntegerNeedFormat(boolean isIntegerNeedFormat) {
        if (this.isIntegerNeedFormat == isIntegerNeedFormat) {
            return;
        }
        this.isIntegerNeedFormat = isIntegerNeedFormat;
        calculateTextSize();
        requestLayout();
        invalidate();
    }

    /**
     * 同时设置 isIntegerNeedFormat=true 和 mIntegerFormat=integerFormat
     *
     * @param integerFormat 注意：integerFormat 中必须包含并且只能包含一个格式说明符（format specifier）
     *                      格式说明符请参照 http://java2s.com/Tutorials/Java/Data_Format/Java_Format_Specifier.htm
     *                      <p>
     *                      如果有多个格式说明符会抛出 java.util.MissingFormatArgumentException: Format specifier '%s'(多出来的说明符)
     */
    public void setIntegerNeedFormat(String integerFormat) {
        isIntegerNeedFormat = true;
        mIntegerFormat = integerFormat;
        calculateTextSize();
        requestLayout();
        invalidate();
    }

    /**
     * 获取Integer类型转换格式
     *
     * @return integerFormat
     */
    public String getIntegerFormat() {
        return mIntegerFormat;
    }

    /**
     * 设置Integer类型转换格式
     *
     * @param integerFormat 注意：integerFormat 中必须包含并且只能包含一个格式说明符（format specifier）
     *                      格式说明符请参照 http://java2s.com/Tutorials/Java/Data_Format/Java_Format_Specifier.htm
     *                      <p>
     *                      如果有多个格式说明符会抛出 java.util.MissingFormatArgumentException: Format specifier '%s'(多出来的说明符)
     */
    public void setIntegerFormat(String integerFormat) {
        if (TextUtils.isEmpty(integerFormat) || integerFormat.equals(mIntegerFormat)) {
            return;
        }
        mIntegerFormat = integerFormat;
        calculateTextSize();
        requestLayout();
        invalidate();
    }

    /**
     * 获取可见条目数
     *
     * @return 可见条目数
     */
    public int getVisibleItems() {
        return mVisibleItems;
    }

    /**
     * 设置可见的条目数
     *
     * @param visibleItems 可见条目数
     */
    public void setVisibleItems(int visibleItems) {
        if (mVisibleItems == visibleItems) {
            return;
        }
        mVisibleItems = adjustVisibleItems(visibleItems);
        mScrollOffsetY = 0;
        requestLayout();
        invalidate();
    }

    /**
     * 跳转可见条目数为奇数
     *
     * @param visibleItems 可见条目数
     * @return 调整后的可见条目数
     */
    private int adjustVisibleItems(int visibleItems) {
        return Math.abs(visibleItems / 2 * 2 + 1); // 当传入的值为偶数时,换算成奇数;
    }

    /**
     * 是否是循环滚动
     *
     * @return 是否是循环滚动
     */
    public boolean isCyclic() {
        return isCyclic;
    }

    /**
     * 设置是否循环滚动
     *
     * @param isCyclic 是否是循环滚动
     */
    public void setCyclic(boolean isCyclic) {
        if (this.isCyclic == isCyclic) {
            return;
        }
        this.isCyclic = isCyclic;

        forceFinishScroll();
        calculateLimitY();
        //设置当前选中的偏移值
        mScrollOffsetY = mSelectedItemPosition * mItemHeight;
        invalidate();
    }

    /**
     * 获取当前选中下标
     *
     * @return 当前选中的下标
     */
    public int getSelectedItemPosition() {
        return mSelectedItemPosition;
    }

    /**
     * 设置当前选中下标
     *
     * @param position 下标
     */
    public void setSelectedItemPosition(int position) {
        setSelectedItemPosition(position, false);
    }

    /**
     * 设置当前选中下标
     *
     * @param position       下标
     * @param isSmoothScroll 是否平滑滚动
     */
    public void setSelectedItemPosition(int position, boolean isSmoothScroll) {
        setSelectedItemPosition(position, isSmoothScroll, 0);
    }

    /**
     * 设置当前选中下标
     * <p>
     * bug 修复记录：调用这个方法时大多数情况在初始化时，如果没有执行 onSizeChanged() 方法时，调用这个方法会导致失效
     * 因为 onSizeChanged() 方法执行结束才确定边界等信息，
     * 所以在 onSizeChanged() 方法增加了兼容，如果 mSelectedItemPosition >0 的情况重新计算一下滚动值。
     *
     * @param position       下标
     * @param isSmoothScroll 是否平滑滚动
     * @param smoothDuration 平滑滚动时间
     */
    public void setSelectedItemPosition(int position, boolean isSmoothScroll, int smoothDuration) {
        if (!isPositionInRange(position)) {
            return;
        }

        //item之间差值
        int itemDistance = calculateItemDistance(position);
        if (itemDistance == 0) {
            //如果最开始设置了下标为0，则itemDistance==0
            if (position!=mSelectedItemPosition) {
                mSelectedItemPosition=position;
                //选中条目回调
                if (mOnItemSelectedListener != null) {
                    mOnItemSelectedListener.onItemSelected(this, mDataList.get(mSelectedItemPosition), mSelectedItemPosition);
                }
                onItemSelected(mDataList.get(mSelectedItemPosition), mSelectedItemPosition);
                if (mOnWheelChangedListener != null) {
                    mOnWheelChangedListener.onWheelSelected(mSelectedItemPosition);
                }
                onWheelSelected(mSelectedItemPosition);
            }
            return;
        }
        //如果Scroller滑动未停止，强制结束动画
        abortFinishScroll();

        if (isSmoothScroll) {
            //如果是平滑滚动并且之前的Scroll滚动完成
            mScroller.startScroll(0, mScrollOffsetY, 0, itemDistance,
                    smoothDuration > 0 ? smoothDuration : DEFAULT_SCROLL_DURATION);
            invalidateIfYChanged();
            ViewCompat.postOnAnimation(this, this);

        } else {
            doScroll(itemDistance);
            mSelectedItemPosition = position;
            //选中条目回调
            if (mOnItemSelectedListener != null) {
                mOnItemSelectedListener.onItemSelected(this, mDataList.get(mSelectedItemPosition), mSelectedItemPosition);
            }
            onItemSelected(mDataList.get(mSelectedItemPosition), mSelectedItemPosition);
            if (mOnWheelChangedListener != null) {
                mOnWheelChangedListener.onWheelSelected(mSelectedItemPosition);
            }
            onWheelSelected(mSelectedItemPosition);
            invalidateIfYChanged();
        }

    }

    private int calculateItemDistance(int position) {
        return position * mItemHeight - mScrollOffsetY;
    }

    /**
     * 判断下标是否在数据列表范围内
     *
     * @param position 下标
     * @return 是否在数据列表范围内
     */
    public boolean isPositionInRange(int position) {
        return position >= 0 && position < mDataList.size();
    }

    /**
     * 获取是否显示分割线
     *
     * @return 是否显示分割线
     */
    public boolean isShowDivider() {
        return isShowDivider;
    }

    /**
     * 设置是否显示分割线
     *
     * @param isShowDivider 是否显示分割线
     */
    public void setShowDivider(boolean isShowDivider) {
        if (this.isShowDivider == isShowDivider) {
            return;
        }
        this.isShowDivider = isShowDivider;
        invalidate();
    }

    /**
     * 获取分割线颜色
     *
     * @return 分割线颜色 ColorInt
     */
    public int getDividerColor() {
        return mDividerColor;
    }

    /**
     * 设置分割线颜色
     *
     * @param dividerColorRes 分割线颜色 {@link ColorRes}
     */
    public void setDividerColorRes(@ColorRes int dividerColorRes) {
        setDividerColor(ContextCompat.getColor(getContext(), dividerColorRes));
    }

    /**
     * 设置分割线颜色
     *
     * @param dividerColor 分割线颜色 {@link ColorInt}
     */
    public void setDividerColor(@ColorInt int dividerColor) {
        if (mDividerColor == dividerColor) {
            return;
        }
        mDividerColor = dividerColor;
        invalidate();
    }

    /**
     * 获取分割线高度
     *
     * @return 分割线高度
     */
    public float getDividerHeight() {
        return mDividerSize;
    }

    /**
     * 设置分割线高度
     *
     * @param dividerHeight 分割线高度
     */
    public void setDividerHeight(float dividerHeight) {
        setDividerHeight(dividerHeight, false);
    }

    /**
     * 设置分割线高度
     *
     * @param dividerHeight 分割线高度
     * @param isDp          单位是否是 dp
     */
    public void setDividerHeight(float dividerHeight, boolean isDp) {
        float tempDividerHeight = mDividerSize;
        mDividerSize = isDp ? dp2px(dividerHeight) : dividerHeight;
        if (tempDividerHeight == mDividerSize) {
            return;
        }
        invalidate();
    }

    /**
     * 获取分割线填充类型
     *
     * @return 分割线填充类型
     * {@link #DIVIDER_TYPE_FILL}
     * {@link #DIVIDER_TYPE_WRAP}
     */
    public int getDividerType() {
        return mDividerType;
    }

    /**
     * 设置分割线填充类型
     *
     * @param dividerType 分割线填充类型
     *                    {@link #DIVIDER_TYPE_FILL}
     *                    {@link #DIVIDER_TYPE_WRAP}
     */
    public void setDividerType(@DividerType int dividerType) {
        if (mDividerType == dividerType) {
            return;
        }
        mDividerType = dividerType;
        invalidate();
    }

    /**
     * 获取自适应分割线类型时的分割线内边距
     *
     * @return 分割线内边距
     */
    public float getDividerPaddingForWrap() {
        return mDividerPaddingForWrap;
    }

    /**
     * 设置自适应分割线类型时的分割线内边距
     *
     * @param dividerPaddingForWrap 分割线内边距
     */
    public void setDividerPaddingForWrap(float dividerPaddingForWrap) {
        setDividerPaddingForWrap(dividerPaddingForWrap, false);
    }

    /**
     * 设置自适应分割线类型时的分割线内边距
     *
     * @param dividerPaddingForWrap 分割线内边距
     * @param isDp                  单位是否是 dp
     */
    public void setDividerPaddingForWrap(float dividerPaddingForWrap, boolean isDp) {
        float tempDividerPadding = mDividerPaddingForWrap;
        mDividerPaddingForWrap = isDp ? dp2px(dividerPaddingForWrap) : dividerPaddingForWrap;
        if (tempDividerPadding == mDividerPaddingForWrap) {
            return;
        }
        invalidate();
    }

    /**
     * 获取分割线两端形状
     *
     * @return 分割线两端形状
     * {@link Paint.Cap#BUTT}
     * {@link Paint.Cap#ROUND}
     * {@link Paint.Cap#SQUARE}
     */
    public Paint.Cap getDividerCap() {
        return mDividerCap;
    }

    /**
     * 设置分割线两端形状
     *
     * @param dividerCap 分割线两端形状
     *                   {@link Paint.Cap#BUTT}
     *                   {@link Paint.Cap#ROUND}
     *                   {@link Paint.Cap#SQUARE}
     */
    public void setDividerCap(Paint.Cap dividerCap) {
        if (mDividerCap == dividerCap) {
            return;
        }
        mDividerCap = dividerCap;
        invalidate();
    }

    /**
     * 获取是否绘制选中区域
     *
     * @return 是否绘制选中区域
     */
    public boolean isDrawSelectedRect() {
        return isDrawSelectedRect;
    }

    /**
     * 设置是否绘制选中区域
     *
     * @param isDrawSelectedRect 是否绘制选中区域
     */
    public void setDrawSelectedRect(boolean isDrawSelectedRect) {
        this.isDrawSelectedRect = isDrawSelectedRect;
        invalidate();
    }

    /**
     * 获取选中区域颜色
     *
     * @return 选中区域颜色 ColorInt
     */
    public int getSelectedRectColor() {
        return mSelectedRectColor;
    }

    /**
     * 设置选中区域颜色
     *
     * @param selectedRectColorRes 选中区域颜色 {@link ColorRes}
     */
    public void setSelectedRectColorRes(@ColorRes int selectedRectColorRes) {
        setSelectedRectColor(ContextCompat.getColor(getContext(), selectedRectColorRes));
    }

    /**
     * 设置选中区域颜色
     *
     * @param selectedRectColor 选中区域颜色 {@link ColorInt}
     */
    public void setSelectedRectColor(@ColorInt int selectedRectColor) {
        mSelectedRectColor = selectedRectColor;
        invalidate();
    }

    /**
     * 获取是否是弯曲（3D）效果
     *
     * @return 是否是弯曲（3D）效果
     */
    public boolean isCurved() {
        return isCurved;
    }

    /**
     * 设置是否是弯曲（3D）效果
     *
     * @param isCurved 是否是弯曲（3D）效果
     */
    public void setCurved(boolean isCurved) {
        if (this.isCurved == isCurved) {
            return;
        }
        this.isCurved = isCurved;
        calculateTextSize();
        requestLayout();
        invalidate();
    }

    /**
     * 获取弯曲（3D）效果左右圆弧效果方向
     *
     * @return 左右圆弧效果方向
     * {@link #CURVED_ARC_DIRECTION_LEFT}
     * {@link #CURVED_ARC_DIRECTION_CENTER}
     * {@link #CURVED_ARC_DIRECTION_RIGHT}
     */
    public int getCurvedArcDirection() {
        return mCurvedArcDirection;
    }

    /**
     * 设置弯曲（3D）效果左右圆弧效果方向
     *
     * @param curvedArcDirection 左右圆弧效果方向
     *                           {@link #CURVED_ARC_DIRECTION_LEFT}
     *                           {@link #CURVED_ARC_DIRECTION_CENTER}
     *                           {@link #CURVED_ARC_DIRECTION_RIGHT}
     */
    public void setCurvedArcDirection(@CurvedArcDirection int curvedArcDirection) {
        if (mCurvedArcDirection == curvedArcDirection) {
            return;
        }
        mCurvedArcDirection = curvedArcDirection;
        invalidate();
    }

    /**
     * 获取弯曲（3D）效果左右圆弧偏移效果方向系数
     *
     * @return 左右圆弧偏移效果方向系数
     */
    public float getCurvedArcDirectionFactor() {
        return mCurvedArcDirectionFactor;
    }

    /**
     * 设置弯曲（3D）效果左右圆弧偏移效果方向系数
     *
     * @param curvedArcDirectionFactor 左右圆弧偏移效果方向系数
     *                                 range 0.0-1.0 越大越明显
     */
    public void setCurvedArcDirectionFactor(@FloatRange(from = 0.0f, to = 1.0f) float curvedArcDirectionFactor) {
        if (mCurvedArcDirectionFactor == curvedArcDirectionFactor) {
            return;
        }
        if (curvedArcDirectionFactor < 0) {
            curvedArcDirectionFactor = 0f;
        } else if (curvedArcDirectionFactor > 1) {
            curvedArcDirectionFactor = 1f;
        }
        mCurvedArcDirectionFactor = curvedArcDirectionFactor;
        invalidate();
    }

    /**
     * 获取折射偏移比例
     *
     * @return 折射偏移比例
     */
    public float getRefractRatio() {
        return mRefractRatio;
    }

    /**
     * 设置选中条目折射偏移比例
     *
     * @param refractRatio 折射偏移比例 range 0.0-1.0
     */
    public void setRefractRatio(@FloatRange(from = 0.0f, to = 1.0f) float refractRatio) {
        float tempRefractRatio = mRefractRatio;
        mRefractRatio = refractRatio;
        if (mRefractRatio > 1f) {
            mRefractRatio = 1.0f;
        } else if (mRefractRatio < 0f) {
            mRefractRatio = DEFAULT_REFRACT_RATIO;
        }
        if (tempRefractRatio == mRefractRatio) {
            return;
        }
        invalidate();
    }

    @Deprecated
    public float getCurvedRefractRatio() {
        return mRefractRatio;
    }

    @Deprecated
    public void setCurvedRefractRatio(@FloatRange(from = 0.0f, to = 1.0f) float refractRatio) {
        setRefractRatio(refractRatio);
    }

    /**
     * 获取选中监听
     *
     * @return 选中监听器
     */
    public OnItemSelectedListener<T> getOnItemSelectedListener() {
        return mOnItemSelectedListener;
    }

    /**
     * 设置选中监听
     *
     * @param onItemSelectedListener 选中监听器
     */
    public void setOnItemSelectedListener(OnItemSelectedListener<T> onItemSelectedListener) {
        mOnItemSelectedListener = onItemSelectedListener;
    }

    /**
     * 获取滚动变化监听
     *
     * @return 滚动变化监听器
     */
    public OnWheelChangedListener getOnWheelChangedListener() {
        return mOnWheelChangedListener;
    }

    /**
     * 设置滚动变化监听
     *
     * @param onWheelChangedListener 滚动变化监听器
     */
    public void setOnWheelChangedListener(OnWheelChangedListener onWheelChangedListener) {
        mOnWheelChangedListener = onWheelChangedListener;
    }

    /*
      --------- 滚动变化方法同监听器方法（适用于子类） ------
     */

    /**
     * WheelView 滚动
     *
     * @param scrollOffsetY 滚动偏移
     */
    protected void onWheelScroll(int scrollOffsetY) {

    }

    /**
     * WheelView 条目变化
     *
     * @param oldPosition 旧的下标
     * @param newPosition 新下标
     */
    protected void onWheelItemChanged(int oldPosition, int newPosition) {

    }

    /**
     * WheelView 选中
     *
     * @param position 选中的下标
     */
    protected void onWheelSelected(int position) {

    }

    /**
     * WheelView 滚动状态
     *
     * @param state 滚动状态
     *              {@link TimeSpannedWheelView#SCROLL_STATE_IDLE}
     *              {@link TimeSpannedWheelView#SCROLL_STATE_DRAGGING}
     *              {@link TimeSpannedWheelView#SCROLL_STATE_SCROLLING}
     */
    protected void onWheelScrollStateChanged(int state) {

    }

    /**
     * 条目选中回调
     *
     * @param data     选中的数据
     * @param position 选中的下标
     */
    protected void onItemSelected(T data, int position) {

    }

    /**
     * dp转换px
     *
     * @param dp dp值
     * @return 转换后的px值
     */
    protected static float dp2px(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

    /**
     * sp转换px
     *
     * @param sp sp值
     * @return 转换后的px值
     */
    protected static float sp2px(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, Resources.getSystem().getDisplayMetrics());
    }

    /**
     * 自定义文字对齐方式注解
     * <p>
     * {@link #mTextAlign}
     * {@link #setTextAlign(int)}
     */
    @IntDef({TEXT_ALIGN_LEFT, TEXT_ALIGN_CENTER, TEXT_ALIGN_RIGHT})
    @Retention(value = RetentionPolicy.SOURCE)
    public @interface TextAlign {
    }

    /**
     * 自定义左右圆弧效果方向注解
     * <p>
     * {@link #mCurvedArcDirection}
     * {@link #setCurvedArcDirection(int)}
     */
    @IntDef({CURVED_ARC_DIRECTION_LEFT, CURVED_ARC_DIRECTION_CENTER, CURVED_ARC_DIRECTION_RIGHT})
    @Retention(value = RetentionPolicy.SOURCE)
    public @interface CurvedArcDirection {
    }

    /**
     * 自定义分割线类型注解
     * <p>
     * {@link #mDividerType}
     * {@link #setDividerType(int)}
     */
    @IntDef({DIVIDER_TYPE_FILL, DIVIDER_TYPE_WRAP})
    @Retention(value = RetentionPolicy.SOURCE)
    public @interface DividerType {
    }

    /**
     * 条目选中监听器
     *
     * @param <T>
     */
    public interface OnItemSelectedListener<T> {

        /**
         * 条目选中回调
         *
         * @param wheelView wheelView
         * @param data      选中的数据
         * @param position  选中的下标
         */
        void onItemSelected(TimeSpannedWheelView<T> wheelView, T data, int position);
    }

    /**
     * WheelView滚动状态改变监听器
     */
    public interface OnWheelChangedListener {

        /**
         * WheelView 滚动
         *
         * @param scrollOffsetY 滚动偏移
         */
        void onWheelScroll(int scrollOffsetY);

        /**
         * WheelView 条目变化
         *
         * @param oldPosition 旧的下标
         * @param newPosition 新下标
         */
        void onWheelItemChanged(int oldPosition, int newPosition);

        /**
         * WheelView 选中
         *
         * @param position 选中的下标
         */
        void onWheelSelected(int position);

        /**
         * WheelView 滚动状态
         *
         * @param state 滚动状态
         *              {@link TimeSpannedWheelView#SCROLL_STATE_IDLE}
         *              {@link TimeSpannedWheelView#SCROLL_STATE_DRAGGING}
         *              {@link TimeSpannedWheelView#SCROLL_STATE_SCROLLING}
         */
        void onWheelScrollStateChanged(int state);
    }
}
