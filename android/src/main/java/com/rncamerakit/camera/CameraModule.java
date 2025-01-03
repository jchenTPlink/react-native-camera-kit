package com.rncamerakit.camera;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.content.Context;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.rncamerakit.camera.commands.Capture;
import com.rncamerakit.camera.permission.CameraPermission;

public class CameraModule extends ReactContextBaseJavaModule {

    private final CameraPermission cameraPermission;
    private Promise checkPermissionStatusPromise;

    public CameraModule(ReactApplicationContext reactContext) {
        super(reactContext);
        cameraPermission = new CameraPermission();
        checkPermissionWhenActivityIsAvailable();
    }

    private void checkPermissionWhenActivityIsAvailable() {
        getReactApplicationContext().addLifecycleEventListener(new LifecycleEventListener() {
            @Override
            public void onHostResume() {
                if (checkPermissionStatusPromise != null && getCurrentActivity() != null) {
                    getCurrentActivity().runOnUiThread(() -> {
                        checkPermissionStatusPromise.resolve(cameraPermission.checkAuthorizationStatus(getCurrentActivity()));
                        checkPermissionStatusPromise = null;
                    });
                }
            }

            @Override
            public void onHostPause() {

            }

            @Override
            public void onHostDestroy() {

            }
        });
    }

    @Override
    public String getName() {
        return "RNKitCameraModule";
    }

    @ReactMethod
    public void checkDeviceCameraAuthorizationStatus(Promise promise) {
        if (getCurrentActivity() == null) {
            checkPermissionStatusPromise = promise;
        } else {
            promise.resolve(cameraPermission.checkAuthorizationStatus(getCurrentActivity()));
        }
    }

    @ReactMethod
    public void requestDeviceCameraAuthorization(Promise promise) {
        cameraPermission.requestAccess(getCurrentActivity(), promise);
    }

    @ReactMethod
    public void hasFrontCamera(Promise promise) {
        CameraManager cameraManager = (CameraManager) getReactApplicationContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            for (String cameraId : cameraIdList) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    promise.resolve(true);
                    return;
                }
            }
            promise.resolve(false);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            promise.reject("CameraAccessError", "Unable to access camera manager");
        }
    }

    @ReactMethod
    public void hasFlashForCurrentCamera(Promise promise) {
        CameraManager cameraManager = (CameraManager) getReactApplicationContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            for (String cameraId : cameraIdList) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                promise.resolve(hasFlash != null && hasFlash);
                return;
            }
            promise.resolve(false);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            promise.reject("CameraAccessError", "Unable to access camera manager");
        }
    }

    @ReactMethod
    public void changeCamera(Promise promise) {
        promise.reject("NotImplemented", "Camera switching is not implemented in this refactor.");
    }

    @ReactMethod
    public void setFlashMode(String mode, Promise promise) {
        promise.reject("NotImplemented", "Flash mode setting is not implemented in this refactor.");
    }

    @ReactMethod
    public void getFlashMode(Promise promise) {
        promise.reject("NotImplemented", "Flash mode retrieval is not implemented in this refactor.");
    }

    @ReactMethod
    public void capture(boolean saveToCameraRoll, final Promise promise) {
        new Capture(getReactApplicationContext(), saveToCameraRoll).execute(promise);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        cameraPermission.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
