package com.rncamerakit.camera;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraViewManager extends SimpleViewManager<PreviewView> {

    private static final String REACT_CLASS = "CameraViewManager";
    private ExecutorService cameraExecutor;
    private Camera camera;

    @NonNull
    @Override
    public String getName() {
        return REACT_CLASS;
    }

    static void setCameraView(CameraView cameraView) {
        if (!cameraViews.isEmpty() && cameraViews.peek() == cameraView) return;
        CameraViewManager.cameraViews.push(cameraView);
        connectHolder();
        createOrientationListener();
    }

    static void removeCameraView() {
        if (!cameraViews.isEmpty()) {
            cameraViews.pop();
        }
        if (!cameraViews.isEmpty()) {
            connectHolder();
        } else if (camera != null) {
            releaseCamera();
            camera = null;
        }
        if (cameraViews.isEmpty()) {
            clearOrientationListener();
        }
    }

    @NonNull
    @Override
    protected PreviewView createViewInstance(@NonNull ThemedReactContext reactContext) {
        PreviewView previewView = new PreviewView(reactContext);
        setUpCamera(reactContext, previewView);
        return previewView;
    }

    private void setUpCamera(@NonNull Context context, @NonNull PreviewView previewView) {
        cameraExecutor = Executors.newSingleThreadExecutor();

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                androidx.camera.core.Preview preview = new androidx.camera.core.Preview.Builder()
                        .build();

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                camera = cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, preview);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(context));
    }

    @Override
    public void onDropViewInstance(@NonNull PreviewView view) {
        super.onDropViewInstance(view);
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
            cameraExecutor = null;
        }
    }
}
