package com.rncamerakit.camera;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("MagicNumber")
public class CameraViewManager extends SimpleViewManager<CameraView> {

    private static final String TAG = "CameraViewManager";
    private static CameraDevice cameraDevice;
    private static CameraCaptureSession captureSession;
    private static CaptureRequest.Builder previewRequestBuilder;
    private static ThemedReactContext reactContext;
    private static CameraView currentCameraView;

    @Override
    public String getName() {
        return "CameraView";
    }

    @Override
    protected CameraView createViewInstance(ThemedReactContext reactContext) {
        CameraViewManager.reactContext = reactContext;
        return new CameraView(reactContext);
    }

    public static void setCameraView(CameraView cameraView) {
        currentCameraView = cameraView;
    }

    public static CameraView getCurrentCameraView() {
        return currentCameraView;
    }

    public static void removeCameraView() {
        currentCameraView = null;
    }

    public static CameraDevice getCurrentCameraDevice() {
        return cameraDevice;
    }

    public static CameraCaptureSession getCurrentCaptureSession() {
        return captureSession;
    }

    public static Rect getFramingRectInPreview(int width, int height) {
        CameraView cameraView = getCurrentCameraView();
        if (cameraView != null) {
            return cameraView.getFramingRectInPreview(width, height);
        }
        return null;
    }

    private static void openCamera(CameraView view) {
        CameraManager cameraManager = (CameraManager) reactContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            String cameraId = cameraIdList[0]; // Use the first camera by default

            for (String id : cameraIdList) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id;
                    break;
                }
            }

            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;
                    startPreview(view);
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private static void startPreview(CameraView view) {
        try {
            SurfaceHolder holder = view.getHolder();
            Surface surface = holder.getSurface();

            if (cameraDevice == null || surface == null) {
                return;
            }

            CameraManager cameraManager = (CameraManager) reactContext.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraDevice.getId());
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            Size[] supportedSizes = map.getOutputSizes(SurfaceHolder.class);
            Size optimalSize = chooseOptimalSize(supportedSizes, view.getWidth(), view.getHeight());

            holder.setFixedSize(optimalSize.getWidth(), optimalSize.getHeight());

            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    captureSession = session;
                    try {
                        captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    // Handle failure
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        List<Size> bigEnough = new ArrayList<>();
        for (Size option : choices) {
            if (option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        return !bigEnough.isEmpty() ? Collections.min(bigEnough, (s1, s2) -> Long.signum((long) s1.getWidth() * s1.getHeight() - (long) s2.getWidth() * s2.getHeight())) : choices[0];
    }

    @ReactProp(name = "scanBarcode")
    public void setShouldScan(CameraView view, boolean scanBarcode) {
        // Implement barcode scanning logic with Camera2 if necessary
    }

    @ReactProp(name = "showFrame", defaultBoolean = false)
    public void setFrame(CameraView view, boolean show) {
        view.setShowFrame(show);
    }

    @ReactProp(name = "frameColor", defaultInt = Color.GREEN)
    public void setFrameColor(CameraView view, @ColorInt int color) {
        view.setFrameColor(color);
    }

    @ReactProp(name = "laserColor", defaultInt = Color.RED)
    public void setLaserColor(CameraView view, @ColorInt int color) {
        view.setLaserColor(color);
    }

    @ReactProp(name = "surfaceColor")
    public void setSurfaceBackground(CameraView view, @ColorInt int color) {
        view.setSurfaceBgColor(color);
    }

    @Nullable
    @Override
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.<String, Object>builder()
                .put("onReadCode",
                        MapBuilder.of("registrationName", "onReadCode"))
                .build();
    }
}
