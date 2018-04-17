package a.androidtestcv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import boofcv.struct.image.Color3_I32;

public class MyImageView extends android.support.v7.widget.AppCompatImageView implements View.OnTouchListener{

    private Bitmap bm;

    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    private PointF start = new PointF();
    private PointF mid = new PointF();
    private float oldDist = 1f;


    public MyImageView(Context context) {
        super(context);
    }

    public MyImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setImage(Bitmap bitmap) {
        setImageBitmap(bitmap);
        boofcv.struct.image.Color3_I32 im = new Color3_I32();
//        image = bitmap;
//        bitmap.
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        ImageView i = (ImageView) v;

        dragAndZoom(i, event);
        return true;

    }
    private void dragAndZoom(View v, MotionEvent event) {
        ImageView view = (ImageView) v;
        view.setScaleType(ImageView.ScaleType.MATRIX);
        float scale;

        // Handle touch events here...

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:

                // first finger down only
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());


                mode = DRAG;
                break;

            case MotionEvent.ACTION_UP: // first finger lifted

            case MotionEvent.ACTION_POINTER_UP: // second finger lifted

                mode = NONE;

                break;

            case MotionEvent.ACTION_POINTER_DOWN: // first and second finger down

                oldDist = spacing(event);

                if (oldDist > 5f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;

                }
                break;

            case MotionEvent.ACTION_MOVE:

                if (mode == DRAG) {
                    matrix.set(savedMatrix);

                    matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);

                } else if (mode == ZOOM) {
                    // pinch zooming
                    float newDist = spacing(event);

                    if (newDist > 5f) {
                        matrix.set(savedMatrix);
                        scale = newDist / oldDist;
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                }
                break;
        }

        view.setImageMatrix(matrix); // display the transformation on screen


    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt((double) (x * x + y * y));
    }


    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private void limitDrag(Matrix m, ImageView view) {

        float[] values = new float[9];
        m.getValues(values);
        float transX = values[Matrix.MTRANS_X];
        float transY = values[Matrix.MTRANS_Y];
        float scaleX = values[Matrix.MSCALE_X];
        float scaleY = values[Matrix.MSCALE_Y];

        Rect bounds = view.getDrawable().getBounds();
        int viewWidth = getResources().getDisplayMetrics().widthPixels;
        int viewHeight = getResources().getDisplayMetrics().heightPixels;

        int _y_up = view.getTop();


        if (viewHeight <= 480) {
            _y_up = 0;
        }
        if (viewHeight > 480 && viewHeight < 980) {
            _y_up = 140;
        }


        int width = bounds.right - bounds.left;
        int height = bounds.bottom - bounds.top;
        width = viewWidth / 2;
        height = viewHeight / 2;


        //height = 200 ;
        float minX = (-width);//* scaleX;
        float minY = (-height);//* scaleY;


        if ((transX) > (viewWidth)) {
            //_x_left
            transX = viewWidth;
        } else if (transX < minX) {
            transX = minX;
        }

        if ((-transX) > (viewWidth)) {
            // _x_right
            transX = -(viewWidth);
        } else if (-transX < minX) {
            transX = -(minX + 30);
        }

        if ((transY) > (viewHeight)) {
            //  _y_up
            transY = (viewHeight);
        } else if (transY < minY) {
            transY = (minY + _y_up);
        }

        if ((-transY) > (viewHeight)) {
            //  _y_down
            transY = -(viewHeight);
        } else if (-transY < minY) {
            transY = -(minY + 170);
        }

        values[Matrix.MTRANS_X] = transX;
        values[Matrix.MTRANS_Y] = transY;
        m.setValues(values);
    }
}
