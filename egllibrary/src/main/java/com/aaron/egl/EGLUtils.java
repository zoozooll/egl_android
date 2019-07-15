package com.aaron.egl;

import android.opengl.*;
import android.util.Log;
import androidx.annotation.Nullable;

public class EGLUtils {

    private static final String TAG = "EGLUtils";

    public static @Nullable
    EGLSurface createEGLSurface(EGLDisplay eglDisplay,
                                EGLConfig eglConfig, Object surface) {
        EGLSurface result = null;
        try {
            result = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, null, 0);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        if (checkEGLError()) {
            return null;
        }
        return result;
    }

    public static boolean destroySurface(EGLDisplay eglDisplay,
                                         EGLSurface mEGLSurface) {
//        Logger.i(TAG, "eglDestroySurface");
        return EGL14.eglDestroySurface(eglDisplay, mEGLSurface);
    }

    public static boolean makeCurrentSurface(EGLDisplay eglDisplay, EGLContext eglContext,
                                             EGLSurface mEGLSurface) {
        return EGL14.eglMakeCurrent(eglDisplay, mEGLSurface, mEGLSurface, eglContext);
    }

    public static boolean swapSurface(EGLDisplay eglDisplay, EGLSurface mEGLSurface) {
        return EGL14.eglSwapBuffers(eglDisplay, mEGLSurface);
    }

    public static boolean checkEGLError() {
        int error = EGL14.eglGetError();
        if (error != EGL14.EGL_SUCCESS) {
            Log.e("EglHelper", "checkEGLError Error: " + error);
            return true;
        }
        return false;
    }

}
