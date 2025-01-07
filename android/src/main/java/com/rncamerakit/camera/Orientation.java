package com.rncamerakit.camera;

// Removed: import android.hardware.Camera;
import android.app.Activity;
import android.view.Surface;

import com.rncamerakit.DeviceUtils;

/**
 * With CameraX, orientation is automatically managed.
 * This class is now mostly a stub, or you can remove it entirely if unused.
 */
@SuppressWarnings({"deprecation"})
class Orientation {
    private static final int PORTRAIT_ROTATION = 90;

    static int getDeviceOrientation(Activity activity) {
        // You can still do this if you need the device's screen rotation,
        // but CameraX will automatically adjust preview orientation.
        if (activity == null) return PORTRAIT_ROTATION;
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:   return 0;
            case Surface.ROTATION_90:  return 90;
            case Surface.ROTATION_180: return 180;
            case Surface.ROTATION_270: return 270;
            default: return 0;
        }
    }

    // (Optional) Remove or keep these placeholders if needed.
    // For demonstration, we keep them commented out:

    /*
    static int getSupportedRotation(int rotation) {
        // CameraX no longer needs manual rotation logic
        return rotation;
    }
    */
}
