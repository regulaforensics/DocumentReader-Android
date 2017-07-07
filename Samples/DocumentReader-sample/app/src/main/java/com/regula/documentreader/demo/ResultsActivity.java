package com.regula.documentreader.demo;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.regula.documentreader.api.DocumentReader;
import com.regula.documentreader.api.enums.eGraphicFieldType;
import com.regula.documentreader.api.results.DocumentReaderResults;
import com.regula.documentreader.api.results.DocumentReaderTextField;
import com.regula.documentreader.api.results.DocumentReaderTextResult;

import java.util.List;

public class ResultsActivity extends AppCompatActivity {
    // The 3 states (events) which the user is trying to perform
    static final int NONE = 0, DRAG = 1, ZOOM = 2;
    int mode = NONE;
    // These matrices will be used to scale points of the image
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();
    // these PointF objects are used to record the point(s) the user is touching
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;

    private ListView listView;
    private TextView noData, imgTitle;
    private ImageView croppedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        listView = (ListView) findViewById(R.id.resultsLv);
        noData = (TextView) findViewById(R.id.noDataTV);
        imgTitle = (TextView) findViewById(R.id.imgTitle);
        croppedImage = (ImageView) findViewById(R.id.croppedImg);
    }

    @Override
    protected void onResume() {
        super.onResume();

        DocumentReaderResults result = DocumentReader.Instance().getResults();
        DocumentReaderTextResult textResult = result.textResult;
        List<DocumentReaderTextField> textResults = null;
        if(textResult!=null){
            textResults = textResult.fields;
        }
        if(textResults!=null && textResults.size()>0){
            listView.setVisibility(View.VISIBLE);
            View listViewHeader = LayoutInflater.from(ResultsActivity.this).inflate(R.layout.text_fields_header_layout, null);
            if(listView.getHeaderViewsCount()==0) {
                listView.addHeaderView(listViewHeader);
            }
            listView.setAdapter(new TextDataAdapter(ResultsActivity.this, 0, textResults, listViewHeader));
        }

        final Bitmap frontImg = result.getGraphicFieldImageByType(eGraphicFieldType.gt_Document_Front);
        if(frontImg!=null){
            imgTitle.setVisibility(View.VISIBLE);
            croppedImage.setVisibility(View.VISIBLE);
            croppedImage.setImageBitmap(frontImg);
            croppedImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Dialog dialog = createEnlargedImageDialog(ResultsActivity.this,frontImg);
                    dialog.show();
                }
            });
        }

        if(textResults==null || textResults.size()==0 && frontImg==null){
            noData.setVisibility(View.VISIBLE);
        }
    }

    private Dialog createEnlargedImageDialog(Context context, final Bitmap bitmap) {

        final Dialog dialog = new Dialog(context, android.R.style.Theme_DeviceDefault_Dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View layout = LayoutInflater.from(context).inflate(R.layout.enlarged_image_layout, null);
        dialog.setContentView(layout);
        ImageView imageView = (ImageView) layout.findViewById(R.id.enlargedImgIv);
        imageView.setImageBitmap(bitmap);

        dialog.getWindow().setAttributes(new WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG, WindowManager.LayoutParams.FLAG_FULLSCREEN));

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ImageView view = (ImageView) v;
                view.setScaleType(ImageView.ScaleType.MATRIX);

                float minScaleX = (float) v.getWidth() / (float) bitmap.getWidth();
                float minScaleY = (float) v.getHeight() / (float) bitmap.getHeight();
                float minScale = Math.min(minScaleX, minScaleY);
                float maxScale = (float) 2.5 * minScale;

                float scale;

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:   // first finger down only
                        matrix.set(view.getImageMatrix());
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
                            matrix.postTranslate(event.getX() - start.x, event.getY() - start.y); // create the transformation in the matrix  of points
                        } else if (mode == ZOOM) { // pinch zooming
                            float newDist = spacing(event);
                            if (newDist > 5f) {
                                matrix.set(savedMatrix);
                                scale = newDist / oldDist; // setting the scaling of the
                                // matrix...if scale > 1 means
                                // zoom in...if scale < 1 means
                                // zoom out
                                matrix.postScale(scale, scale, mid.x, mid.y);
                            }

                            float[] f = new float[9];
                            matrix.getValues(f);
                            float scaleX = f[Matrix.MSCALE_X];
                            float scaleY = f[Matrix.MSCALE_Y];

                            if (scaleX <= minScale) {
                                matrix.postScale((minScale) / scaleX, (minScale) / scaleY, mid.x, mid.y);
                            } else if (scaleX >= maxScale) {
                                matrix.postScale((maxScale) / scaleX, (maxScale) / scaleY, mid.x, mid.y);
                            }
                        }
                        break;
                }

                view.setImageMatrix(matrix); // display the transformation on screen

                return true; // indicate event was handled
            }
        });

        return dialog;
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }
}
