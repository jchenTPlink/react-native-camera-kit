package com.rncamerakit.camera.commands;

import android.content.Context;

import com.facebook.react.bridge.Promise;
import com.rncamerakit.SaveImageTask;
import com.rncamerakit.camerax.CameraXViewManager;

import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCapture.OutputFileResults;
import androidx.camera.core.ImageCapture.OutputFileOptions;
import androidx.camera.core.ImageProxy;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Capture implements Command {

    private final Context context;
    private boolean saveToCameraRoll;
    private static final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();

    public Capture(Context context, boolean saveToCameraRoll) {
        this.context = context;
        this.saveToCameraRoll = saveToCameraRoll;
    }

    @Override
    public void execute(final Promise promise) {
        ImageCapture imageCapture = CameraXViewManager.getImageCapture();
        if (imageCapture == null) {
            promise.reject("CameraKit", "ImageCapture is not initialized");
            return;
        }
        tryTakePicture(imageCapture, promise);
    }

    private void tryTakePicture(ImageCapture imageCapture, final Promise promise) {
        try {
            File photoFile = new File(context.getCacheDir(), "cameraX_" + System.currentTimeMillis() + ".jpg");
            OutputFileOptions outputOptions = new OutputFileOptions.Builder(photoFile).build();
            imageCapture.takePicture(outputOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(OutputFileResults outputFileResults) {
                    // The picture is on disk at photoFile.
                    // We can read it back into memory if we want to pass raw bytes to SaveImageTask
                    // or just pass the path to SaveImageTask. Let’s do raw bytes for consistency:

                    byte[] data = Utils.readFileToByteArray(photoFile);
                    // Then run SaveImageTask
                    new SaveImageTask(context, promise, saveToCameraRoll).execute(data);
                }

                @Override
                public void onError(ImageCaptureException exception) {
                    promise.reject("CameraKit", "takePicture failed: " + exception.getMessage());
                }
            });
        } catch (Exception e) {
            promise.reject("CameraKit", e.getMessage());
        }
    }
}
