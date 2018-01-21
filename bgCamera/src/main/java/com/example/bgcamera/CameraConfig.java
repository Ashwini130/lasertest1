package com.example.bgcamera;

import android.content.Context;
import android.support.annotation.NonNull;

import com.example.bgcamera.config.CameraFacing;
import com.example.bgcamera.config.CameraImageFormat;
import com.example.bgcamera.config.CameraResolution;
import com.example.bgcamera.config.CameraRotation;

import java.io.File;
public final class CameraConfig {
    private Context mContext;

    @CameraResolution.SupportedResolution
    private int mResolution = CameraResolution.MEDIUM_RESOLUTION;

    @CameraFacing.SupportedCameraFacing
    private int mFacing = CameraFacing.REAR_FACING_CAMERA;

    @CameraImageFormat.SupportedImageFormat
    private int mImageFormat = CameraImageFormat.FORMAT_JPEG;

    @CameraRotation.SupportedRotation
    private int mImageRotation = CameraRotation.ROTATION_0;

    private File mImageFile;

    public CameraConfig() {
    }

    public Builder getBuilder(Context context) {
        mContext = context;
        return new Builder();
    }

    @CameraResolution.SupportedResolution
    int getResolution() {
        return mResolution;
    }

    @CameraFacing.SupportedCameraFacing
    int getFacing() {
        return mFacing;
    }

    @CameraImageFormat.SupportedImageFormat
    public int getImageFormat() {
        return mImageFormat;
    }

    public File getImageFile() {
        return mImageFile;
    }

    @CameraRotation.SupportedRotation
    int getmImageRotation() {
        return mImageRotation;
    }

    public class Builder {

        /**
         * Set the resolution of the output camera image. If you don't specify any resolution,
         * default image resolution will set to {@link CameraResolution#MEDIUM_RESOLUTION}.
         *
         * @param resolution Any resolution from:
         *                   <li>{@link CameraResolution#HIGH_RESOLUTION}</li>
         *                   <li>{@link CameraResolution#MEDIUM_RESOLUTION}</li>
         *                   <li>{@link CameraResolution#LOW_RESOLUTION}</li>
         * @return {@link Builder}
         * @see CameraResolution
         */
        public Builder setCameraResolution(@CameraResolution.SupportedResolution int resolution) {

            //Validate input
            if (resolution != CameraResolution.HIGH_RESOLUTION &&
                    resolution != CameraResolution.MEDIUM_RESOLUTION &&
                    resolution != CameraResolution.LOW_RESOLUTION) {
                throw new RuntimeException("Invalid camera resolution.");
            }

            mResolution = resolution;
            return this;
        }

        /**
         * Set the camera facing with which you want to capture image.
         * Either rear facing camera or front facing camera. If you don't provide any camera facing,
         * default camera facing will be {@link CameraFacing#FRONT_FACING_CAMERA}.
         *
         * @param cameraFacing Any camera facing from:
         *                     <li>{@link CameraFacing#REAR_FACING_CAMERA}</li>
         *                     <li>{@link CameraFacing#FRONT_FACING_CAMERA}</li>
         * @return {@link Builder}
         * @see CameraFacing
         */
        public Builder setCameraFacing(@CameraFacing.SupportedCameraFacing int cameraFacing) {
            //Validate input
            if (cameraFacing != CameraFacing.REAR_FACING_CAMERA &&
                    cameraFacing != CameraFacing.FRONT_FACING_CAMERA) {
                throw new RuntimeException("Invalid camera facing value.");
            }

            mFacing = cameraFacing;
            return this;
        }

        /**
         * Specify the image format for the output image. If you don't specify any output format,
         * default output format will be {@link CameraImageFormat#FORMAT_JPEG}.
         *
         * @param imageFormat Any supported image format from:
         *                    <li>{@link CameraImageFormat#FORMAT_JPEG}</li>
         *                    <li>{@link CameraImageFormat#FORMAT_PNG}</li>
         * @return {@link Builder}
         * @see CameraImageFormat
         */
        public Builder setImageFormat(@CameraImageFormat.SupportedImageFormat int imageFormat) {
            //Validate input
            if (imageFormat != CameraImageFormat.FORMAT_JPEG &&
                    imageFormat != CameraImageFormat.FORMAT_PNG) {
                throw new RuntimeException("Invalid output image format.");
            }

            mImageFormat = imageFormat;
            return this;
        }

        /**
         * Specify the output image rotation. The output image will be rotated by amount of degree specified
         * before stored to the output file. By default there is no rotation applied.
         *
         * @param rotation Any supported rotation from:
         *                 <li>{@link CameraRotation#ROTATION_0}</li>
         *                 <li>{@link CameraRotation#ROTATION_90}</li>
         *                 <li>{@link CameraRotation#ROTATION_180}</li>
         *                 <li>{@link CameraRotation#ROTATION_270}</li>
         * @return {@link Builder}
         * @see CameraRotation
         */
        public Builder setImageRotation(@CameraRotation.SupportedRotation int rotation) {
            //Validate input
            if (rotation != CameraRotation.ROTATION_0
                    && rotation != CameraRotation.ROTATION_90
                    && rotation != CameraRotation.ROTATION_180
                    && rotation != CameraRotation.ROTATION_270) {
                throw new RuntimeException("Invalid image rotation.");
            }

            mImageRotation = rotation;
            return this;
        }

        /**
         * Set the location of the out put image. If you do not set any file for the output image, by
         * default image will be stored in the application's cache directory.
         *
         * @param imageFile {@link File} where you want to store the image.
         * @return {@link Builder}
         */
        public Builder setImageFile(File imageFile) {
            mImageFile = imageFile;
            return this;
        }

        /**
         * Build the configuration.
         *
         * @return {@link CameraConfig}
         */
        public CameraConfig build() {
            if (mImageFile == null) mImageFile = getDefaultStorageFile();
            return CameraConfig.this;
        }

        @NonNull
        private File getDefaultStorageFile() {
            return new File(HiddenCameraUtils.getCacheDir(mContext).getAbsolutePath()
                    + File.pathSeparator
                    + "IMG_" + System.currentTimeMillis()   //IMG_214515184113123.png
                    + (mImageFormat == CameraImageFormat.FORMAT_JPEG ? ".jpeg" : ".png"));
        }
    }
}
