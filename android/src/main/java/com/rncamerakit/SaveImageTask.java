package com.rncamerakit;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;

import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.facebook.react.common.ReactConstants.TAG;

public class SaveImageTask extends AsyncTask<byte[], Void, Void> {

    private final Context context;
    private final Promise promise;
    private boolean saveToCameraRoll;
    private String bitmapUrl = null;

    public SaveImageTask(Context context, Promise promise, boolean saveToCameraRoll) {
        this.context = context;
        this.promise = promise;
        this.saveToCameraRoll = saveToCameraRoll;
    }

    public SaveImageTask(String bitmapUrl, Context context, Promise promise, boolean saveToCameraRoll) {
        this(context, promise, saveToCameraRoll);
        this.bitmapUrl = bitmapUrl;
        if (this.bitmapUrl != null) {
            this.bitmapUrl = this.bitmapUrl.replace("file://", "");
        }
    }

    private Bitmap getImageBitmapFromRemoteImageFile() {
        Bitmap image;
        try {
            URL url = new URL(bitmapUrl);
            image = BitmapFactory.decodeStream(url.openStream());
        } catch (IOException e) {
            image = null;
        }
        return image;
    }

    private Bitmap getImageBitmapFromLocalImageFile() {
        Bitmap image;
        try (FileInputStream fis = new FileInputStream(new File(bitmapUrl))) {
            image = BitmapFactory.decodeStream(fis);
        } catch (IOException e) {
            e.printStackTrace();
            image = null;
        }
        return image;
    }

    private Bitmap getImageBitmap(byte[]... data) {
        Bitmap image;
        if (bitmapUrl != null) {
            if (Patterns.WEB_URL.matcher(bitmapUrl.toLowerCase()).matches()) {
                image = getImageBitmapFromRemoteImageFile();
            } else {
                image = getImageBitmapFromLocalImageFile();
            }
        } else {
            byte[] rawImageData = data[0];
            image = decodeAndRotateIfNeeded(rawImageData);
        }
        return image;
    }

    @Override
    protected Void doInBackground(byte[]... data) {
        Bitmap image = getImageBitmap(data);
        if (image == null) {
            promise.reject("CameraKit", "Failed to get Bitmap image");
            return null;
        }

        WritableMap imageInfo = saveToCameraRoll ? saveToMediaStore(image) : saveTempImageFile(image);
        if (imageInfo == null)
            promise.reject("CameraKit", "Failed to save image to MediaStore");
        else
            promise.resolve(imageInfo);

        return null;
    }

    private WritableMap saveToMediaStore(Bitmap image) {
        try {
            String fileUri = MediaStore.Images.Media.insertImage(context.getContentResolver(), image, System.currentTimeMillis() + "", "");
            Cursor cursor = context.getContentResolver().query(Uri.parse(fileUri), new String[]{
                    MediaStore.Images.ImageColumns.DATA,
                    MediaStore.Images.ImageColumns.DISPLAY_NAME
            }, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                int pathIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATA);
                int nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DISPLAY_NAME);
                String filePath = cursor.getString(pathIndex);
                String fileName = cursor.getString(nameIndex);
                long fileSize = new File(filePath).length();
                cursor.close();

                return createImageInfo(fileUri, filePath, fileName, fileSize, image.getWidth(), image.getHeight());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving to MediaStore", e);
        }
        return null;
    }

    private Bitmap decodeAndRotateIfNeeded(byte[] rawImageData) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(rawImageData)) {
            Bitmap image = BitmapFactory.decodeStream(inputStream);
            Matrix matrix = new Matrix();
            matrix.postRotate(90); // Placeholder for rotation logic if needed
            return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, false);
        } catch (IOException e) {
            Log.e(TAG, "Error decoding and rotating image", e);
            return null;
        }
    }

    private WritableMap createImageInfo(String fileUri, String id, String fileName, long fileSize, int width, int height) {
        WritableMap imageInfo = Arguments.createMap();
        imageInfo.putString("uri", fileUri);
        imageInfo.putString("id", id);
        imageInfo.putString("name", fileName);
        imageInfo.putInt("size", (int) fileSize);
        imageInfo.putInt("width", width);
        imageInfo.putInt("height", height);
        return imageInfo;
    }

    @Nullable
    private WritableMap saveTempImageFile(Bitmap image) {
        File imageFile = new File(context.getCacheDir(), "temp_Image_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
            image.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            return createImageInfo(Uri.fromFile(imageFile).toString(), imageFile.getAbsolutePath(), imageFile.getName(), imageFile.length(), image.getWidth(), image.getHeight());
        } catch (IOException e) {
            Log.e(TAG, "Error saving temp image file", e);
            return null;
        }
    }
}
