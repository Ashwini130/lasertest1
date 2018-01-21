package com.example.bgcamera.config;

import android.hardware.Camera;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@SuppressWarnings("WeakerAccess")
public final class CameraResolution {

    /**
     * This will capture the image at the highest possible resolution. That means if the camera sensor
     * is of 13MP, output image will have resolution of 13MP.
     */
    public static final int HIGH_RESOLUTION = 2006;
    /**
     * This will capture the image at the medium resolution. That means if the camera sensor
     * is of 13MP, it will take image with resolution that is exact middle of the supported camera
     * resolutions ({@link Camera.Parameters#getSupportedPictureSizes()}).
     */
    public static final int MEDIUM_RESOLUTION = 7895;
    /**
     * This will capture the image at the lowest possible resolution. That means if the camera sensor
     * supports minimum 2MP, output image will have resolution of 2MP.
     */
    public static final int LOW_RESOLUTION = 7821;

    private CameraResolution() {
        throw new RuntimeException("Cannot initiate CameraResolution.");
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({HIGH_RESOLUTION, MEDIUM_RESOLUTION, LOW_RESOLUTION})
    public @interface SupportedResolution {
    }
}
