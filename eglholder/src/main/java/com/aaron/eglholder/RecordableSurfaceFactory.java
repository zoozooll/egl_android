package com.aaron.eglholder;

import android.util.Log;
import android.view.Surface;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

public class RecordableSurfaceFactory implements EGLWindowSurfaceFactory {

    private static final String TAG = "DefaultWindowSurfaceFactory".substring(0, 23);

    private boolean active;
    private Surface mRecorderSurface;

    public RecordableSurfaceFactory(Surface recorderSurface) {
        this.mRecorderSurface = recorderSurface;
    }

    public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display,
                                          EGLConfig config, Object nativeWindow) {
        EGLSurface result = null;
        try {
            result = egl.eglCreateWindowSurface(display, config, mRecorderSurface, null);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "eglCreateWindowSurface", e);
        }
        return result;
    }

    public void destroySurface(EGL10 egl, EGLDisplay display,
                               EGLSurface surface) {
        egl.eglDestroySurface(display, surface);
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isActive() {
        return active;
    }
}
