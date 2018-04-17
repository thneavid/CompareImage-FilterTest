package a.androidtestcv;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int LEFT = 0, RIGHT = 1;
    private Bitmap leftImage, rightImage;
    private ImageView ivL, ivR;
    private Manager manager;

    private Matrix matrix = new Matrix(), savedMatrix = new Matrix();

    private static final int NONE = 0, DRAG = 1, ZOOM = 2;
    private int mode = NONE;

    private PointF startPoint = new PointF(), rightStart = new PointF();
    private PointF mid = new PointF();
    private float oldDist = 1f;


    private int width, height;
    private String leftPath, rightPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        ivL = (ImageView) findViewById(R.id.left);
        ivR = (ImageView) findViewById(R.id.right);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        width = metrics.widthPixels;
        height = metrics.heightPixels;

        ivL.setMaxWidth(width / 2);
        ivL.setMaxHeight(height);
        ivR.setMaxWidth(width / 2);
        ivR.setMaxHeight(height);

//        width = getWindowManager().getDefaultDisplay().getWidth();
//        height = getWindowManager().getDefaultDisplay().getHeight();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        manager = Manager.getInstance();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        return (event.getX() > ivL.getRight() ||
//                event.getX() < ivL.getLeft() ||
//                event.getY() > ivL.getTop() ||
//                event.getY() < ivL.getBottom() ||
//                event.getX() > ivR.getRight() ||
//                event.getX() < ivR.getLeft() ||
//                event.getY() > ivR.getTop() ||
//                event.getY() < ivR.getBottom())
//                &&
        return super.onTouchEvent(event);
    }

    public void loadLeftImage(View view) {
        showFileChooser(LEFT);
    }

    public void loadRightImage(View view) {
        showFileChooser(RIGHT);
    }

    void showFileChooser(int im) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        ArrayList<String> extensions = new ArrayList<>();
        extensions.add(".jpg");
        extensions.add(".jpeg");
        extensions.add(".png");
        extensions.add(".bmp");
        intent.putStringArrayListExtra("filter", extensions);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    im);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            // Get the Uri of the selected file
            Uri uri = data.getData();
            Log.d(TAG, "File Uri: " + uri.toString());
            // Get the path
            String path = null;
            try {
                path = getPath(this, uri);
                File file = new File(path);
                ExifInterface exif = new ExifInterface(file.getCanonicalPath());
                String model = exif.getAttribute(ExifInterface.TAG_MODEL);
                String aperture = exif.getAttribute(ExifInterface.TAG_APERTURE);
                String focal_length = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
                Log.e(TAG, "Model: " + model);
                Log.e(TAG, "aperture: " + aperture);
                Log.e(TAG, "Focal Length: " + focal_length);

            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.e(TAG, "File Path: " + path);
            // Get the file instance
            // File file = new File(path);
            // Initiate the upload

            switch (requestCode) {
                case LEFT:
                    leftPath = path;
                    leftImage = BitmapFactory.decodeFile(path);
                    ivL.setImageBitmap(leftImage);
                    ivL.setOnTouchListener(touchAction);
                    break;
                case RIGHT:
                    rightPath = path;
                    rightImage = BitmapFactory.decodeFile(path);
                    ivR.setImageBitmap(rightImage);
                    ivR.setOnTouchListener(touchAction);
                    break;
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public String getPath(Context context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {"_data"};
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = 0;
                if (cursor != null) {
                    column_index = cursor.getColumnIndexOrThrow("_data");
                }
                if (cursor != null && cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.navigation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            reset();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void reset() {
        leftImage = BitmapFactory.decodeFile(leftPath);
        rightImage = BitmapFactory.decodeFile(rightPath);
        ivL.setImageBitmap(leftImage);
        ivR.setImageBitmap(rightImage);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_sobel) {
            if (leftImage != null && rightImage != null) {
                leftImage = manager.showGradient(leftImage);
                rightImage = manager.showGradient(rightImage);
                ivL.setImageBitmap(leftImage);
                ivR.setImageBitmap(rightImage);
            }
        } else if (id == R.id.nav_features) {
            if (leftImage != null && rightImage != null) {
                ivL.setImageBitmap(manager.showFeaturePoint(leftImage));
                ivR.setImageBitmap(manager.showFeaturePoint(rightImage));
            }
        } else if (id == R.id.nav_canny) {
            if (leftImage != null && rightImage != null) {
                leftImage = manager.canny(leftImage);
                rightImage = manager.canny(rightImage);
                ivL.setImageBitmap(leftImage);
                ivR.setImageBitmap(rightImage);
            }
        } else if (id == R.id.nav_adjust) {
            if (leftImage != null && rightImage != null) {
                leftImage = manager.imAdjust(leftImage);
                rightImage = manager.imAdjust(rightImage);
                ivL.setImageBitmap(leftImage);
                ivR.setImageBitmap(rightImage);
            }
        } else if (id == R.id.nav_thin) {
            if (leftImage != null && rightImage != null) {
                leftImage = manager.thinning(leftImage);
                rightImage = manager.thinning(rightImage);
                ivL.setImageBitmap(leftImage);
                ivR.setImageBitmap(rightImage);
            }
        } else if (id == R.id.nav_send) {
            if (leftImage != null && rightImage != null) {
                Bitmap[] bmArray = PointsAssociater.associateImages(leftImage, rightImage);
                ivL.setImageBitmap(bmArray[0]);
                ivR.setImageBitmap(bmArray[1]);
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    View.OnTouchListener touchAction = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            dragAndZoom(event);
            return true;
        }
    };

    private void dragAndZoom(MotionEvent event) {

        ivL.setScaleType(ImageView.ScaleType.MATRIX);
        ivR.setScaleType(ImageView.ScaleType.MATRIX);

        float scale;
        float x = event.getX(), y = event.getY();
        Log.e("||||", "[" + width + "; " + height + "]");
        Log.e("||||", "event : [" + x + "; " + y + "]");

        // Handle touch events here...

        switch (event.getAction() & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN:
                // first finger down only
                savedMatrix.set(matrix);
                startPoint.set(x, y);
                Log.e("||||", "startPoint : [" + startPoint.x + "; " + startPoint.y + "]");

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
                    Log.e("||||", "mid : [" + mid.x + "; " + mid.y + "]");
                    mode = ZOOM;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    matrix.set(savedMatrix);
                    matrix.postTranslate(x - startPoint.x, y - startPoint.y);
                    Log.e("||||", "translate : [" + (x - startPoint.x) + "; " + (y - startPoint.y) + "]");

                } else if (mode == ZOOM) {
                    // pinch zooming
                    float newDist = spacing(event);
                    if (newDist > 5f) {
                        matrix.set(savedMatrix);
                        scale = newDist / oldDist;

                        matrix.postScale(scale, scale, mid.x, mid.y);

                        Log.e("||||", "scale : [" + (mid.x) + "; " + (mid.y) + "]");
                        Log.e("||||", "scale = " + scale);
                    }
                }
                break;
        }
//        limitDrag(matrix, view);
        ivL.setImageMatrix(matrix); // display the transformation on screen
        ivR.setImageMatrix(matrix); // display the transformation on screen
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
        int _y_up = view.getTop();

        Rect bounds = view.getDrawable().getBounds();
//        int viewWidth = getResources().getDisplayMetrics().widthPixels;
//        int viewHeight = getResources().getDisplayMetrics().heightPixels;
        int viewWidth = view.getWidth();
        int viewHeight = view.getHeight();


        if (viewHeight <= height) {
            _y_up = 0;
        }
        if (viewHeight > height / 2 && viewHeight < height) {
            _y_up = 140;
        }

        int width = bounds.right - bounds.left;
        int height = bounds.bottom - bounds.top;
        width = viewWidth / 2;
        height = viewHeight / 2;

        float minX = (-width) * scaleX;  //* scaleX;
        float minY = (-height) * scaleY; //* scaleY;

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
            transX = -minX; // -(minX + 30);
        }

        if ((transY) > (viewHeight)) {
            //  _y_up
            transY = (viewHeight);
        } else if (transY < minY) {
            transY = minY; //(minY + _y_up);
        }

        if ((-transY) > (viewHeight)) {
            //  _y_down
            transY = -(viewHeight);
        } else if (-transY < minY) {
            transY = -minY; //-(minY + 170);
        }

        values[Matrix.MTRANS_X] = transX;
        values[Matrix.MTRANS_Y] = transY;
        m.setValues(values);
    }

    public void rotateLeftImage(View view) {
        if (leftImage != null) {
            leftImage = manager.rotateBitmap(leftImage, 90);
            ivL.setImageBitmap(leftImage);
        }
    }

    public void rotateRightImage(View view) {
        if (rightImage != null) {
            rightImage = manager.rotateBitmap(rightImage, 90);
            ivR.setImageBitmap(rightImage);
        }
    }
}
