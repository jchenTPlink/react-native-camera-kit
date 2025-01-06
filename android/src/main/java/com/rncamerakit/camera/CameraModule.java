package com.rncamerakit.camera;

import android.content.Context;
import android.util.Log;
import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.google.common.util.concurrent.ListenableFuture;
import com.rncamerakit.camera.permission.CameraPermission;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraModule extends ReactContextBaseJavaModule {

    private final CameraPermission cameraPermission;
    private Promise checkPermissionStatusPromise;
    private ExecutorService cameraExecutor;

    public CameraModule(ReactApplicationContext reactContext) {
        super(reactContext);
        cameraPermission = new CameraPermission();
        checkPermissionWhenActivityIsAvailable();
        cameraExecutor = Executors.newSingleThreadExecutor();
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
                // No action needed
            }

            @Override
            public void onHostDestroy() {
                cameraExecutor.shutdown();
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
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(getReactApplicationContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                boolean hasFrontCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA);
                promise.resolve(hasFrontCamera);
            } catch (Exception e) {
                Log.e("CameraModule", "Failed to check front camera availability", e);
                promise.reject(e);
            }
        }, ContextCompat.getMainExecutor(getReactApplicationContext()));
    }

    @ReactMethod
    public void hasFlashForCurrentCamera(Promise promise) {
        // CameraX handles flash via CameraControl, but this requires a bound camera instance.
        // This method implementation will depend on the camera setup in your app.
        promise.reject("NOT_IMPLEMENTED", "This method needs a bound CameraX instance to check flash support.");
    }

    @ReactMethod
    public void capture(boolean saveToCameraRoll, final Promise promise) {
        // Implement capture logic using CameraX.
        promise.reject("NOT_IMPLEMENTED", "CameraX capture not implemented yet.");
    }
}
