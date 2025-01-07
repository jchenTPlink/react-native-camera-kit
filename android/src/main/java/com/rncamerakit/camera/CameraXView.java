package com.rncamerakit.camerax;

import android.content.Context;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

public class CameraXView extends FrameLayout implements LifecycleOwner {

    private PreviewView previewView;
    private LifecycleRegistry lifecycleRegistry;
    private boolean shouldScan = false;
    private boolean showFrame = false;
    private int frameColor;
    private int laserColor;
    private int surfaceBgColor;

    public CameraXView(@NonNull Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        // Create a LifecycleRegistry if context is not a lifecycle owner
        lifecycleRegistry = new LifecycleRegistry(this);
        lifecycleRegistry.setCurrentState(Lifecycle.State.RESUMED);

        previewView = new PreviewView(context);
        previewView.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT, 
                LayoutParams.MATCH_PARENT
        ));
        addView(previewView);
    }

    public PreviewView getPreviewView() {
        return previewView;
    }

    // LifecycleOwner approach
    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    // Props
    public boolean shouldScan() {
        return shouldScan;
    }

    public void setShouldScan(boolean shouldScan) {
        this.shouldScan = shouldScan;
    }

    public void setShowFrame(boolean show) {
        this.showFrame = show;
        // If you draw a frame, you could do it in an overlay
    }

    public void setFrameColor(int color) {
        this.frameColor = color;
    }

    public void setLaserColor(int color) {
        this.laserColor = color;
    }

    public void setSurfaceBgColor(int color) {
        this.surfaceBgColor = color;
        if (previewView != null) {
            previewView.setBackgroundColor(color);
        }
    }
}
