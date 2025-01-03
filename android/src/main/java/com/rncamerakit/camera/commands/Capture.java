package com.rncamerakit.camera.commands;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.rncamerakit.camera.CameraViewManager;
import com.rncamerakit.SaveImageTask;

import java.nio.ByteBuffer;

public class Capture implements Command {

    private static final String TAG = "Capture";

    private final Context context;
    private boolean saveToCameraRoll;

    private Handler backgroundHandler;
    private ImageReader imageReader;

    public Capture(Context context, boolean saveToCameraRoll) {
        this.context = context;
        this.saveToCameraRoll = saveToCameraRoll;
        setupBackgroundHandler();
    }

    private void setupBackgroundHandler() {
        HandlerThread handlerThread = new HandlerThread("CameraBackground");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());
    }

    @Override
    public void execute(final Promise promise) {
        try {
            tryTakePicture(promise);
        } catch (Exception e) {
            Log.e(TAG, "Error executing capture", e);
            promise.reject("CaptureError", "Failed to capture image", e);
        }
    }

    private void tryTakePicture(final Promise promise) throws CameraAccessException {
        CameraDevice cameraDevice = CameraViewManager.getCurrentCameraDevice();
        if (cameraDevice == null) {
            promise.reject("NoCamera", "Camera device is not available");
            return;
        }

        CameraCaptureSession captureSession = CameraViewManager.getCurrentCaptureSession();
        if (captureSession == null) {
            promise.reject("NoCaptureSession", "Camera capture session is not available");
            return;
        }

        // Configure ImageReader
        imageReader = ImageReader.newInstance(1920, 1080, android.graphics.ImageFormat.JPEG, 1);
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                image.close();

                // Save the image data
                new SaveImageTask(context, promise, saveToCameraRoll).execute(data);
            }
        }, backgroundHandler);

        // Build CaptureRequest
        CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureBuilder.addTarget(imageReader.getSurface());

        // Trigger the capture
        captureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                Log.i(TAG, "Image capture completed");
            }

            @Override
            public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                super.onCaptureFailed(session, request, failure);
                Log.e(TAG, "Image capture failed: " + failure.getReason());
                promise.reject("CaptureFailed", "Failed to capture image");
            }
        }, backgroundHandler);
    }
}
