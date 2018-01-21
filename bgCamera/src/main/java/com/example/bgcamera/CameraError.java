package com.example.bgcamera;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class CameraError {

    public static final int ERROR_CAMERA_OPEN_FAILED = 1122;
    public static final int ERROR_CAMERA_PERMISSION_NOT_AVAILABLE = 5472;
    public static final int ERROR_DOES_NOT_HAVE_OVERDRAW_PERMISSION = 3136;
    public static final int ERROR_IMAGE_WRITE_FAILED = 9854;

    private CameraError() {
        throw new RuntimeException("Cannot initiate CameraError.");
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ERROR_CAMERA_PERMISSION_NOT_AVAILABLE,
            ERROR_CAMERA_OPEN_FAILED,
            ERROR_DOES_NOT_HAVE_OVERDRAW_PERMISSION,
            ERROR_IMAGE_WRITE_FAILED})
    public @interface CameraErrorCodes {
    }
}