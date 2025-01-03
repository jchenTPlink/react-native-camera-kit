package com.rncamerakit.camera.barcode;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.rncamerakit.camera.CameraViewManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class BarcodeScanner {

    public interface ResultHandler {
        void handleResult(Result result);
    }

    private MultiFormatReader mMultiFormatReader;
    private static final List<BarcodeFormat> ALL_FORMATS = new ArrayList<>();
    private ResultHandler resultHandler;

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

    public BarcodeScanner(@NonNull ResultHandler resultHandler) {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, ALL_FORMATS);
        mMultiFormatReader = new MultiFormatReader();
        mMultiFormatReader.setHints(hints);

        this.resultHandler = resultHandler;
    }

    public void onImageAvailable(ImageReader imageReader) {
        Image image = imageReader.acquireLatestImage();
        if (image == null) {
            return;
        }

        try {
            int width = image.getWidth();
            int height = image.getHeight();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            final Result result = decodeResult(getLuminanceSource(data, width, height));

            if (result != null) {
                new Handler(Looper.getMainLooper()).post(() -> resultHandler.handleResult(result));
            }
        } catch (Exception e) {
            Log.w("CameraKit", e.toString());
        } finally {
            image.close();
        }
    }

    @Nullable
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

            if (rawResult == null && source.isRotateSupported()) {
                LuminanceSource rotatedSource = source.rotateCounterClockwise();
                bitmap = new BinaryBitmap(new HybridBinarizer(rotatedSource));
                try {
                    rawResult = mMultiFormatReader.decodeWithState(bitmap);
                } catch (ReaderException ignored) {
                } finally {
                    mMultiFormatReader.reset();
                }
            }
        }
        return rawResult;
    }

    private LuminanceSource getLuminanceSource(byte[] data, int width, int height) {
        Rect rect = CameraViewManager.getFramingRectInPreview(width, height);
        try {
            return new RotateLuminanceSource(data, width, height, rect.left, rect.top,
                    rect.width(), rect.height(), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
