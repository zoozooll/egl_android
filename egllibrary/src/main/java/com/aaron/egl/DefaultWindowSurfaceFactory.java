package com.aaron.egl;


import android.opengl.EGLConfig;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;

public class DefaultWindowSurfaceFactory implements EGLWindowSurfaceFactory {

    private static final String TAG = "DefaultWindowSurfaceFactory".substring(0, 23);

    private Object nativeWindow;

    public void setNativeWindow(Object nativeWindow) {
        this.nativeWindow = nativeWindow;
    }

    public EGLSurface createWindowSurface(EGLDisplay display, EGLConfig config) {
        EGLSurface result = null;
        try {
            result = EGLUtils.createEGLSurface(display, config, nativeWindow);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void destroySurface(EGLDisplay display, EGLSurface surface) {
        EGLUtils.destroySurface(display, surface);
    }
}