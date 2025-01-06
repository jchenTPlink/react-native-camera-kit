package com.rncamerakit.camera;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

public class Orientation {

    private static final String TAG = "Orientation";

    /**
     * Calculates the rotation for the use case based on the device's current rotation.
     *
     * @param context the context to access display metrics.
     * @return the rotation constant for CameraX use cases.
     */
    public static int getRotation(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) {
            Log.e(TAG, "Unable to retrieve WindowManager");
            return Surface.ROTATION_0; // Default rotation
        }

        int rotation = windowManager.getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return Surface.ROTATION_0;
            case Surface.ROTATION_90:
                return Surface.ROTATION_90;
            case Surface.ROTATION_180:
                return Surface.ROTATION_180;
            case Surface.ROTATION_270:
                return Surface.ROTATION_270;
            default:
                return Surface.ROTATION_0;
        }
    }

    /**
     * Determines the CameraSelector lens facing direction.
     *
     * @param isFrontFacing boolean indicating if the front-facing camera is desired.
     * @return CameraSelector.LENS_FACING_FRONT or CameraSelector.LENS_FACING_BACK
     */
    public static int getLensFacing(boolean isFrontFacing) {
        return isFrontFacing ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
    }

    /**
     * Retrieves the optimal target resolution for the camera use case.
     *
     * @param context the context to access display metrics.
     * @return a DisplayMetrics object containing width and height.
     */
    public static DisplayMetrics getTargetResolution(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getMetrics(metrics);
        } else {
            Log.e(TAG, "Unable to retrieve WindowManager");
        }
        return metrics;
    }
}
