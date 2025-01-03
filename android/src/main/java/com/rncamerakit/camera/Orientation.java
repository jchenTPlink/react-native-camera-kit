package com.rncamerakit.camera;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.view.Surface;

import com.rncamerakit.DeviceUtils;

@SuppressWarnings({"MagicNumber", "deprecation"})
class Orientation {
    private static final int PORTRAIT_ROTATION = 90;

    static int getDeviceOrientation(Activity activity) {
        if (activity == null) return PORTRAIT_ROTATION;

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        CameraCharacteristics characteristics = getCameraCharacteristics(activity);
        if (characteristics == null) return PORTRAIT_ROTATION;

        int orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        int facing = characteristics.get(CameraCharacteristics.LENS_FACING);

        int result;
        if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
            result = (orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (orientation - degrees + 360) % 360;
        }
        return result;
    }

    static int getSupportedRotation(int rotation, Context context) {
        int degrees = convertRotationToSupportedAxis(rotation);
        return isFrontFacingCamera(context) ? adaptFrontCamera(degrees, context) : adaptBackCamera(degrees, context);
    }

    private static int convertRotationToSupportedAxis(int rotation) {
        if (rotation < 45) {
            return 0;
        } else if (rotation < 135) {
            return 90;
        } else if (rotation < 225) {
            return 180;
        } else if (rotation < 315){
            return 270;
        }
        return 0;
    }

    private static boolean isFrontFacingCamera(Context context) {
        CameraCharacteristics characteristics = getCameraCharacteristics(context);
        return characteristics != null &&
               characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
    }

    private static int adaptBackCamera(int degrees, Context context) {
        CameraCharacteristics characteristics = getCameraCharacteristics(context);
        if (characteristics == null) return degrees;
        int orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        return (orientation - degrees + 360) % 360;
    }

    private static int adaptFrontCamera(int degrees, Context context) {
        CameraCharacteristics characteristics = getCameraCharacteristics(context);
        if (characteristics == null) return degrees;

        int orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        if (DeviceUtils.isGoogleDevice()) {
            return (orientation + degrees) % 360;
        } else {
            return (orientation + degrees + 180) % 360;
        }
    }

    private static CameraCharacteristics getCameraCharacteristics(Context context) {
        try {
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String[] cameraIdList = cameraManager.getCameraIdList();

            for (String cameraId : cameraIdList) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return characteristics;  // Return front camera characteristics if needed
                }
            }

            // Default to the first camera if no specific requirements
            return cameraManager.getCameraCharacteristics(cameraIdList[0]);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
