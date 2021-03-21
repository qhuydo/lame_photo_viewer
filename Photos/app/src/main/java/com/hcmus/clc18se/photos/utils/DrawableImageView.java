package com.hcmus.clc18se.photos.utils;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatImageView;

// link https://pristalovpavel.wordpress.com/2015/05/16/drawing-on-image-in-android/
public class DrawableImageView extends AppCompatImageView implements View.OnTouchListener
{

    float downx = 0;
    float downy = 0;
    float upx = 0;
    float upy = 0;
    Canvas canvas;
    Paint paint;
    Matrix matrix;

    public DrawableImageView(Context context)
    {
        super(context);
        setOnTouchListener(this);
    }

    public DrawableImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        setOnTouchListener(this);
    }

    public DrawableImageView(Context context, AttributeSet attrs,
                             int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        setOnTouchListener(this);
    }

    public void setNewImage(Bitmap alteredBitmap, Bitmap bmp)
    {
        canvas = new Canvas(alteredBitmap );
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(5);
        matrix = new Matrix();
        canvas.drawBitmap(bmp, matrix, paint);

        setImageBitmap(alteredBitmap);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        int action = event.getAction();

        switch (action)
        {
            case MotionEvent.ACTION_DOWN:
                downx = getPointerCoords(event)[0];//event.getX();
                downy = getPointerCoords(event)[1];//event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                upx = getPointerCoords(event)[0];//event.getX();
                upy = getPointerCoords(event)[1];//event.getY();
                canvas.drawLine(downx, downy, upx, upy, paint);
                invalidate();
                downx = upx;
                downy = upy;
                break;
            case MotionEvent.ACTION_UP:
                upx = getPointerCoords(event)[0];//event.getX();
                upy = getPointerCoords(event)[1];//event.getY();
                canvas.drawLine(downx, downy, upx, upy, paint);
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            default:
                break;
        }
        return true;
    }

    final float[] getPointerCoords(MotionEvent e)
    {
        final int index = e.getActionIndex();
        final float[] coords = new float[] { e.getX(index), e.getY(index) };
        Matrix matrix = new Matrix();
        getImageMatrix().invert(matrix);
        matrix.postTranslate(getScrollX(), getScrollY());
        matrix.mapPoints(coords);
        return coords;
    }
}

