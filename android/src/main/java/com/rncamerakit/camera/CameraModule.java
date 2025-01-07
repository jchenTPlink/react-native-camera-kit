package com.rncamerakit.camera;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import com.rncamerakit.camera.commands.Capture;
import com.rncamerakit.camera.permission.CameraPermission;
import com.rncamerakit.camerax.CameraXViewManager;

import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;

import java.util.concurrent.ExecutionException;

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
                    getCurrentActivity().runOnUiThread(() ->
                        checkPermissionStatusPromise.resolve(cameraPermission.checkAuthorizationStatus(getCurrentActivity()))
                    );
                    checkPermissionStatusPromise = null;
                }
            }

            @Override
            public void onHostPause() { }

            @Override
            public void onHostDestroy() { }
        });
    }

    @NonNull
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
        // CameraX approach: Check if front camera is available
        ProcessCameraProvider.getInstance(getReactApplicationContext())
                .addListener(() -> {
                    try {
                        ProcessCameraProvider provider = ProcessCameraProvider.getInstance(getReactApplicationContext()).get();
                        boolean hasFront = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA);
                        promise.resolve(hasFront);
                    } catch (ExecutionException | InterruptedException e) {
                        promise.resolve(false);
                    }
                }, Utils.getMainExecutor());
    }

    @ReactMethod
    public void hasFlashForCurrentCamera(Promise promise) {
        // Instead of direct reference to camera parameters,
        // we can check if current camera supports a flash by querying the camera's CameraInfo.
        // This is tricky if you have multiple camera views or a single static reference.
        boolean hasFlash = CameraXViewManager.hasFlashForCurrentCamera();
        promise.resolve(hasFlash);
    }

    @ReactMethod
    public void changeCamera(Promise promise) {
        // Tells our CameraX-based manager to switch from front to back or vice versa
        boolean changed = CameraXViewManager.changeCamera();
        promise.resolve(changed);
    }

    @ReactMethod
    public void setFlashMode(String mode, Promise promise) {
        boolean success = CameraXViewManager.setFlashMode(mode);
        promise.resolve(success);
    }

    @ReactMethod
    public void getFlashMode(Promise promise) {
        // We can maintain a static variable in CameraXViewManager
        // that tracks the current flash mode.
        String flash = CameraXViewManager.getFlashMode();
        promise.resolve(flash);
    }

    @ReactMethod
    public void capture(boolean saveToCameraRoll, final Promise promise) {
        // With CameraX, we typically call an ImageCapture use case.
        // We'll adapt your old "Capture" command to call the new approach.
        new Capture(getReactApplicationContext(), saveToCameraRoll).execute(promise);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        cameraPermission.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
