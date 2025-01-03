package com.rncamerakit.camera;

import android.graphics.Color;
import android.graphics.Rect;
import androidx.annotation.ColorInt;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import com.facebook.react.uimanager.ThemedReactContext;
import com.rncamerakit.Utils;
import com.rncamerakit.camera.barcode.BarcodeFrame;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class CameraView extends FrameLayout implements SurfaceHolder.Callback {
    private SurfaceView surface;

    private boolean showFrame;
    private Rect frameRect;
    private BarcodeFrame barcodeFrame;
    @ColorInt private int frameColor = Color.GREEN;
    @ColorInt private int laserColor = Color.RED;

    public CameraView(ThemedReactContext context) {
        super(context);
        surface = new SurfaceView(context);
        setBackgroundColor(Color.BLACK);
        addView(surface, MATCH_PARENT, MATCH_PARENT);
        surface.getHolder().addCallback(this);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int actualPreviewWidth = getWidth();
        int actualPreviewHeight = getHeight();

        // Get the camera's aspect ratio (replace this with the actual camera aspect ratio)
        float cameraAspectRatio = 16f / 9f; // Replace with your camera's actual aspect ratio
        int previewHeight;
        int previewWidth;

        // Calculate dimensions to maintain aspect ratio
        if (actualPreviewWidth / (float) actualPreviewHeight > cameraAspectRatio) {
            // Screen is wider than the camera aspect ratio
            previewWidth = actualPreviewWidth;
            previewHeight = (int) (actualPreviewWidth / cameraAspectRatio);
        } else {
            // Screen is taller than the camera aspect ratio
            previewHeight = actualPreviewHeight;
            previewWidth = (int) (actualPreviewHeight * cameraAspectRatio);
        }

        // Center the preview if it doesn't fill the entire screen
        int horizontalOffset = (actualPreviewWidth - previewWidth) / 2;
        int verticalOffset = (actualPreviewHeight - previewHeight) / 2;

        // Layout the surface view to match the calculated dimensions
        surface.layout(
            horizontalOffset,
            verticalOffset,
            horizontalOffset + previewWidth,
            verticalOffset + previewHeight
        );

        if (barcodeFrame != null) {
            ((View) barcodeFrame).layout(
                horizontalOffset,
                verticalOffset,
                horizontalOffset + previewWidth,
                verticalOffset + previewHeight
            );
        }
    }



    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        CameraViewManager.setCameraView(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        CameraViewManager.setCameraView(this);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        CameraViewManager.removeCameraView();
    }


    public SurfaceHolder getHolder() {
        return surface.getHolder();
    }

    private final Runnable measureAndLayout = new Runnable() {
        @Override
        public void run() {
            measure(
                    MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
            layout(getLeft(), getTop(), getRight(), getBottom());
        }
    };

    @Override
    public void requestLayout() {
        super.requestLayout();
        post(measureAndLayout);
    }

    public void setShowFrame(boolean showFrame) {
        this.showFrame = showFrame;
    }

    public void showFrame() {
        if (showFrame) {
            barcodeFrame = new BarcodeFrame(getContext());
            barcodeFrame.setFrameColor(frameColor);
            barcodeFrame.setLaserColor(laserColor);
            addView(barcodeFrame);
            requestLayout();
        }
    }

    public Rect getFramingRectInPreview(int previewWidth, int previewHeight) {
        if (frameRect == null) {
            if (barcodeFrame != null) {
                Rect framingRect = new Rect(barcodeFrame.getFrameRect());
                int frameWidth = barcodeFrame.getWidth();
                int frameHeight = barcodeFrame.getHeight();

                if (previewWidth < frameWidth) {
                    framingRect.left = framingRect.left * previewWidth / frameWidth;
                    framingRect.right = framingRect.right * previewWidth / frameWidth;
                }
                if (previewHeight < frameHeight) {
                    framingRect.top = framingRect.top * previewHeight / frameHeight;
                    framingRect.bottom = framingRect.bottom * previewHeight / frameHeight;
                }

                frameRect = framingRect;
            } else {
                frameRect = new Rect(0, 0, previewWidth, previewHeight);
            }
        }
        return frameRect;
    }

    public void setFrameColor(@ColorInt int color) {
        this.frameColor = color;
        if (barcodeFrame != null) {
            barcodeFrame.setFrameColor(color);
        }
    }

    public void setLaserColor(@ColorInt int color) {
        this.laserColor = color;
        if (barcodeFrame != null) {
            barcodeFrame.setLaserColor(laserColor);
        }
    }

    /**
     * Set background color for Surface view on the period, while camera is not loaded yet.
     * Provides opportunity for user to hide period while camera is loading
     * @param color - color of the surfaceview
     */
    public void setSurfaceBgColor(@ColorInt int color) {
        surface.setBackgroundColor(color);
    }
}
