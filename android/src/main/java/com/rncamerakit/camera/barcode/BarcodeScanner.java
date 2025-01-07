package com.rncamerakit.camera.barcode;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReactApplicationContext;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.rncamerakit.camerax.CameraXView;
import com.rncamerakit.Utils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class BarcodeScannerX implements ImageAnalysis.Analyzer {

    private MultiFormatReader mMultiFormatReader;
    private static final List<BarcodeFormat> ALL_FORMATS = new ArrayList<>();
    private CameraXView cameraXView;
    private ReactApplicationContext reactContext;

    static {
        ALL_FORMATS.add(BarcodeFormat.AZTEC);
        ALL_FORMATS.add(BarcodeFormat.CODABAR);
        ALL_FORMATS.add(BarcodeFormat.CODE_39);
        ALL_FORMATS.add(BarcodeFormat.CODE_93);
        ALL_FORMATS.add(BarcodeFormat.CODE_128);
        ALL_FORMATS.add(BarcodeFormat.DATA_MATRIX);
        ALL_FORMATS.add(BarcodeFormat.EAN_8);
        ALL_FORMATS.add(BarcodeFormat.EAN_13);
        ALL_FORMATS.add(BarcodeFormat.ITF);
        ALL_FORMATS.add(BarcodeFormat.MAXICODE);
        ALL_FORMATS.add(BarcodeFormat.PDF_417);
        ALL_FORMATS.add(BarcodeFormat.QR_CODE);
        ALL_FORMATS.add(BarcodeFormat.RSS_14);
        ALL_FORMATS.add(BarcodeFormat.RSS_EXPANDED);
        ALL_FORMATS.add(BarcodeFormat.UPC_A);
        ALL_FORMATS.add(BarcodeFormat.UPC_E);
        ALL_FORMATS.add(BarcodeFormat.UPC_EAN_EXTENSION);
    }

    public BarcodeScannerX(@NonNull CameraXView view, @NonNull ReactApplicationContext reactContext) {
        this.cameraXView = view;
        this.reactContext = reactContext;

        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, ALL_FORMATS);
        mMultiFormatReader = new MultiFormatReader();
        mMultiFormatReader.setHints(hints);
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        // Convert ImageProxy to a ZXing-friendly format. Usually this means YUV -> a LuminanceSource
        try {
            LuminanceSource source = buildLuminanceSourceFromImageProxy(image);
            if (source == null) {
                image.close();
                return;
            }
            Result rawResult = decodeResult(source);
            if (rawResult != null) {
                // Send event to JS side
                new Handler(Looper.getMainLooper()).post(() -> {
                    WritableMap event = Arguments.createMap();
                    event.putString("codeStringValue", rawResult.getText());
                    // Fire "onReadCode" event
                    reactContext
                      .getJSModule(com.facebook.react.uimanager.events.RCTEventEmitter.class)
                      .receiveEvent(cameraXView.getId(), "onReadCode", event);
                });
            }
        } catch (Exception e) {
            Log.e("CameraKit", "Analyze exception: " + e.getMessage());
        } finally {
            image.close();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private LuminanceSource buildLuminanceSourceFromImageProxy(ImageProxy image) {
        // Pseudo-code for extracting the Y-plane from image in YUV_420_888 format
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        int ySize = yBuffer.remaining();
        byte[] yBytes = new byte[ySize];
        yBuffer.get(yBytes, 0, ySize);

        int width = image.getWidth();
        int height = image.getHeight();

        // If you need rotation, you can handle it here or let ZXing rotate. 
        // We'll assume upright for simplicity:

        // Then build a LuminanceSource (similar to your rotate logic).
        // Possibly create a PlanarYUVLuminanceSource if you want to handle cropping/rotation:
        // return new PlanarYUVLuminanceSource(yBytes, width, height, 0, 0, width, height, false);
        // Just be mindful that ZXing typically wants data in row-major (left-to-right, top-to-bottom).
        return new PlanarYUVLuminanceSource(yBytes, width, height, 0, 0, width, height, false);
    }

    private Result decodeResult(LuminanceSource source) {
        Result rawResult = null;
        if (source != null) {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                rawResult = mMultiFormatReader.decodeWithState(bitmap);
            } catch (ReaderException ignored) {
            } finally {
                mMultiFormatReader.reset();
            }
        }
        return rawResult;
    }
}
