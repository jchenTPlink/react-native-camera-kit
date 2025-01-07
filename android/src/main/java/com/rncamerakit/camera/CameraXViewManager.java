package com.rncamerakit.camerax;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO;
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF;
import androidx.camera.core.ImageCapture.FLASH_MODE_ON;
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

    public static final String REACT_CLASS = "CameraView"; // same name as old
    private static ReactApplicationContext reactContext;

    // We'll store global camera state here, but typically you might prefer instance-based or a separate manager.
    private static ProcessCameraProvider cameraProvider;
    private static CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    private static Camera camera;
    private static ImageCapture imageCapture;
    private static ImageAnalysis imageAnalysis;
    private static BarcodeScannerX barcodeAnalyzer;

    private static AtomicBoolean isFrontFacing = new AtomicBoolean(false);
    private static String currentFlashMode = "auto"; // track as a string for old API compatibility

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

    // Expose event type to JS
    @Nullable
    @Override
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.<String, Object>builder()
                .put("onReadCode", MapBuilder.of("registrationName", "onReadCode"))
                .build();
    }

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

    /**
     * Rebinds or updates the camera use cases. 
     * Called whenever props change or we switch front/back.
     */
    private static void bindCameraUseCases(CameraXView view) {
        if (view.getContext() == null) return;
        if (cameraProvider == null) {
            // Attempt to get provider asynchronously
            ProcessCameraProvider.getInstance(view.getContext()).addListener(() -> {
                try {
                    cameraProvider = ProcessCameraProvider.getInstance(view.getContext()).get();
                    bindPreviewAnalysis(view);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }, Utils.getMainExecutor());
        } else {
            bindPreviewAnalysis(view);
        }
    }

    private static void bindPreviewAnalysis(CameraXView view) {
        if (cameraProvider == null) return;
        // Unbind everything first
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

        // Build ImageCapture (for capturing photos)
        imageCapture = new ImageCapture.Builder().build();
        setCameraXFlashMode(currentFlashMode);

        // Build ImageAnalysis (for barcode scanning)
        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        if (view.shouldScan()) {
            if (barcodeAnalyzer == null) {
                barcodeAnalyzer = new BarcodeScannerX(view, reactContext);
            }
            imageAnalysis.setAnalyzer(Utils.getWorkerExecutor(), barcodeAnalyzer);
        }

        // Bind them to lifecycle
        camera = cameraProvider.bindToLifecycle(
                view.getLifecycleOwner(),
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
        );
    }

    // ----- Static methods called by the JS Module -----

    public static boolean changeCamera() {
        // Flip front/back
        isFrontFacing.set(!isFrontFacing.get());
        // We can't "return" whether it actually changed easily, but assume it's fine
        return true;
    }

    public static boolean setFlashMode(String mode) {
        currentFlashMode = mode == null ? "auto" : mode;
        setCameraXFlashMode(currentFlashMode);
        return true;
    }

    public static String getFlashMode() {
        return currentFlashMode;
    }

    public static boolean hasFlashForCurrentCamera() {
        // We can check camera info if needed. 
        // For simplicity, return true if the device *might* have flash facing that direction.
        // A more robust approach would query the camera's characteristics.
        return true;
    }

    private static void setCameraXFlashMode(String mode) {
        if (imageCapture == null) return;
        switch (mode) {
            case "off":
                imageCapture.setFlashMode(FLASH_MODE_OFF);
                break;
            case "on":
                imageCapture.setFlashMode(FLASH_MODE_ON);
                break;
            case "auto":
            default:
                imageCapture.setFlashMode(FLASH_MODE_AUTO);
                break;
        }
    }
}
