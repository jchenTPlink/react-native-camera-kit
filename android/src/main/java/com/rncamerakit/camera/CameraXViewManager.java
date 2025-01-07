package com.rncamerakit.camerax;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
// Import only the CameraX classes you actually need
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.rncamerakit.camera.barcode.BarcodeScannerX;
import com.rncamerakit.Utils;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraXViewManager extends SimpleViewManager<CameraXView> {

    public static final String REACT_CLASS = "CameraView";

    private static ReactApplicationContext reactContext;

    // Our global-ish references
    private static ProcessCameraProvider cameraProvider;
    private static CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    private static Camera camera;
    private static ImageCapture imageCapture;
    private static ImageAnalysis imageAnalysis;
    private static BarcodeScannerX barcodeAnalyzer;

    private static AtomicBoolean isFrontFacing = new AtomicBoolean(false);

    // We'll track just "on"/"off" for torch
    private static String currentFlashMode = "off";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected CameraXView createViewInstance(ThemedReactContext context) {
        reactContext = (ReactApplicationContext) context.getApplicationContext();
        CameraXView view = new CameraXView(context);
        bindCameraUseCases(view);
        return view;
    }

    @Nullable
    @Override
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.<String, Object>builder()
                .put("onReadCode", MapBuilder.of("registrationName", "onReadCode"))
                .build();
    }

    // -------------------------------------------------------------------------
    // Props from JS
    // -------------------------------------------------------------------------

    @ReactProp(name = "scanBarcode")
    public void setShouldScan(CameraXView view, boolean scanBarcode) {
        view.setShouldScan(scanBarcode);
        bindCameraUseCases(view);
    }

    @ReactProp(name = "showFrame", defaultBoolean = false)
    public void setFrame(CameraXView view, boolean show) {
        view.setShowFrame(show);
    }

    @ReactProp(name = "frameColor", defaultInt = Color.GREEN)
    public void setFrameColor(CameraXView view, @ColorInt int color) {
        view.setFrameColor(color);
    }

    @ReactProp(name = "laserColor", defaultInt = Color.RED)
    public void setLaserColor(CameraXView view, @ColorInt int color) {
        view.setLaserColor(color);
    }

    @ReactProp(name = "surfaceColor")
    public void setSurfaceBackground(CameraXView view, @ColorInt int color) {
        view.setSurfaceBgColor(color);
    }

    // -------------------------------------------------------------------------
    // Camera Binding Logic
    // -------------------------------------------------------------------------

    private static void bindCameraUseCases(CameraXView view) {
        if (view.getContext() == null) return;

        if (cameraProvider == null) {
            // Acquire cameraProvider asynchronously
            ProcessCameraProvider.getInstance(view.getContext()).addListener(() -> {
                try {
                    cameraProvider = ProcessCameraProvider.getInstance(view.getContext()).get();
                    bindPreviewAnalysis(view);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }, Utils.getMainExecutor(view.getContext()));
        } else {
            bindPreviewAnalysis(view);
        }
    }

    private static void bindPreviewAnalysis(CameraXView view) {
        if (cameraProvider == null) return;

        cameraProvider.unbindAll();

        // Determine lens facing
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(isFrontFacing.get()
                        ? CameraSelector.LENS_FACING_FRONT
                        : CameraSelector.LENS_FACING_BACK)
                .build();

        // Build Preview
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(view.getPreviewView().getSurfaceProvider());

        // Build ImageCapture (for capturing photos if you need it)
        imageCapture = new ImageCapture.Builder().build();

        // Build ImageAnalysis for barcode scanning
        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        if (view.shouldScan()) {
            if (barcodeAnalyzer == null) {
                barcodeAnalyzer = new BarcodeScannerX(view, reactContext);
            }
            imageAnalysis.setAnalyzer(Utils.getWorkerExecutor(), barcodeAnalyzer);
        }

        // Bind to lifecycle
        camera = cameraProvider.bindToLifecycle(
                view,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
        );

        // Now apply current torch setting
        setFlashTorchMode(currentFlashMode);
    }

    // -------------------------------------------------------------------------
    // Public static methods - called by the React Module (CameraModule)
    // -------------------------------------------------------------------------

    public static boolean changeCamera() {
        isFrontFacing.set(!isFrontFacing.get());
        // If the camera is currently bound, we need to rebind.
        // We'll rely on the next call to bindCameraUseCases(...) from the view,
        // or you can store a reference to the current view and call it directly.
        return true;
    }

    /**
     * Instead of using FLASH_MODE_OFF / AUTO / ON,
     * we just enable or disable the torch. 
     *
     * @param mode "on" or "off" (or "auto" if you want, but we treat that as "off" here).
     */
    public static boolean setFlashMode(String mode) {
        // Save the string (for getFlashMode).
        currentFlashMode = mode == null ? "off" : mode;
        return setFlashTorchMode(currentFlashMode);
    }

    /**
     * Actually enable / disable the torch on the current camera, if available.
     */
    private static boolean setFlashTorchMode(String mode) {
        if (camera == null) {
            return false;
        }
        switch (mode) {
            case "on":
                camera.getCameraControl().enableTorch(true);
                break;
            default:
                // treat everything else as "off", including "auto"
                camera.getCameraControl().enableTorch(false);
                break;
        }
        return true;
    }

    public static String getFlashMode() {
        return currentFlashMode;
    }

    public static boolean hasFlashForCurrentCamera() {
        // A more robust approach would read the actual camera characteristics.
        // Here we just assume the back camera likely has a flash.
        return true;
    }

    // If you have a separate "getImageCapture()" method used in your Capture command:
    public static ImageCapture getImageCapture() {
        return imageCapture;
    }
}
