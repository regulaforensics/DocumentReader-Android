package com.regula.documentreader.demo;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.regula.documentreader.api.results.DocumentReaderGraphicField;
import com.regula.documentreader.api.translation.TranslationUtil;

public class GraphicResultsFragment extends Fragment {
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

    View scrollView;
    TextView noDataTv;
    LinearLayout scrollViewContents;

    int orientation;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.graphic_result_fragment,container, false);

        orientation = getResources().getConfiguration().orientation;
        scrollView = fragmentView.findViewById(R.id.graphicResultScroll);
        scrollViewContents = (LinearLayout) fragmentView.findViewById(R.id.graphicResultScrollContent);
        noDataTv = (TextView) fragmentView.findViewById(R.id.noDataTV);

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        int count =0;

        if (ResultsActivityTabbed.documentReaderResults != null && ResultsActivityTabbed.documentReaderResults.graphicResult != null) {
            if(scrollViewContents.getChildCount()==0) {
                LayoutInflater inflater = getActivity().getLayoutInflater();
                for (final DocumentReaderGraphicField field : ResultsActivityTabbed.documentReaderResults.graphicResult.fields) {
                    LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.single_graphic_result, null);
                    ImageView imageView = (ImageView) layout.findViewById(R.id.graphicResultIv);
                    TextView textView = (TextView) layout.findViewById(R.id.graphicResultTv);

                    LinearLayout.LayoutParams layoutParams;
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                        layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    } else {
                        layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    }
                    layout.setLayoutParams(layoutParams);

                    imageView.setImageBitmap(field.value);
                    imageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Dialog dialog = createEnlargedImageDialog(getContext(), field.value);
                            dialog.show();
                        }
                    });
                    textView.setText(TranslationUtil.getGraphicFieldTranslation(getContext(), field.fieldType));

                    if (count % 2 > 0) {
                        layout.setBackgroundColor(Color.LTGRAY);
                    } else {
                        layout.setBackgroundColor(Color.WHITE);
                    }

                    scrollViewContents.addView(layout);
                }
            }
        } else {
            if (scrollView != null) {
                scrollView.setVisibility(View.GONE);
            }
            noDataTv.setVisibility(View.VISIBLE);
        }
    }

    private Dialog createEnlargedImageDialog(Context context, final Bitmap bitmap) {

        final Dialog dialog = new Dialog(context, android.R.style.Theme_DeviceDefault_Dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View layout = LayoutInflater.from(context).inflate(R.layout.enlarged_image_layout, null);
        dialog.setContentView(layout);
        ImageView imageView = (ImageView) layout.findViewById(R.id.enlargedImgIv);
        imageView.setImageBitmap(bitmap);

        //noinspection ConstantConditions setting not null inside
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
