package com.example.zhangchen.miyu_compass.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by zhangchen on 2016/3/8.
 */
public class CompassView extends ImageView {
    private float direction;//表示旋转的点数
    private Drawable drawable;

    public CompassView(Context context) {
        super(context);
        direction = 0.0f;
        drawable = null;
    }

    public CompassView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        direction = 0.0f;
        drawable = null;
    }

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        direction = 0.0f;
        drawable = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (drawable == null) {
            drawable = getDrawable();
            drawable.setBounds(0, 0, getWidth(), getHeight());
        }

        canvas.save();
        canvas.rotate(direction, getWidth() / 2, getHeight() / 2);
        drawable.draw(canvas);
        canvas.restore();
    }

    /*
    自定义更新方向的方法
     */
    public void updateDirection(float direction) {
        this.direction = direction;
        invalidate();
    }
}



























