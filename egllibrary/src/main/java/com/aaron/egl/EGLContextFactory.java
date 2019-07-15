package com.aaron.egl;


import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;

/**
 * An interface for customizing the eglCreateContext and eglDestroyContext calls.
 * <p>
 * This interface must be implemented by clients wishing to call
 */
public interface EGLContextFactory {
    EGLContext createContext(EGLDisplay display, EGLConfig eglConfig);

    boolean destroyContext(EGLDisplay display, EGLContext context);
}