package com.aaron.egl;


import android.opengl.EGLConfig;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;

/**
 * An interface for customizing the eglCreateWindowSurface and eglDestroySurface calls.
 * <p>
 * This interface must be implemented by clients wishing to call
 */
public interface EGLWindowSurfaceFactory {

    void setNativeWindow(Object nativeWindow);

    /**
     * @return null if the surface cannot be constructed.
     */
    EGLSurface createWindowSurface(EGLDisplay display, EGLConfig config);

    void destroySurface(EGLDisplay display, EGLSurface surface);
}