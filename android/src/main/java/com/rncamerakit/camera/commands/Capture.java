package com.rncamerakit.camera.commands;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.Promise;
import com.google.common.util.concurrent.ListenableFuture;
import com.rncamerakit.SaveImageTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Capture implements Command {

    private final Context context;
    private final boolean saveToCameraRoll;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    public Capture(Context context, boolean saveToCameraRoll) {
        this.context = context;
        this.saveToCameraRoll = saveToCameraRoll;
        this.cameraExecutor = Executors.newSingleThreadExecutor();
        initializeCamera();
    }

    private void initializeCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                imageCapture = new ImageCapture.Builder().build();
                // Camera setup code should already be in your CameraViewManager or similar class
                // You would bind the lifecycle and preview there
            } catch (Exception e) {
                Log.e("Capture", "Error initializing camera: ", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    @Override
    public void execute(final Promise promise) {
        try {
            takePicture(promise);
        } catch (Exception e) {
            Log.e("Capture", "Error executing capture: ", e);
            promise.reject("CAPTURE_ERROR", "Failed to execute capture", e);
        }
    }

    private void takePicture(final Promise promise) {
        if (imageCapture == null) {
            promise.reject("CAPTURE_ERROR", "ImageCapture is not initialized");
            return;
        }

        File photoFile = new File(
                context.getExternalFilesDir(null),
                new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                        .format(new Date()) + ".jpg"
        );

        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputFileOptions,
                cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = Uri.fromFile(photoFile);
                        if (saveToCameraRoll) {
                            new SaveImageTask(context, promise, saveToCameraRoll).execute(photoFile.getAbsolutePath().getBytes());
                        } else {
                            promise.resolve(savedUri.toString());
                        }
                    }

                    @Override
                    public void onError(ImageCaptureException exception) {
                        Log.e("Capture", "Photo capture failed: ", exception);
                        promise.reject("CAPTURE_ERROR", "Photo capture failed", exception);
                    }
                });
    }
}
