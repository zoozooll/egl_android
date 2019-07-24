package com.aaron.eglholder;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * An interface for customizing the eglCreateWindowSurface and eglDestroySurface calls.
 * <p>
 * This interface must be implemented by clients wishing to call
 * {@link EGLHolder#setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory)}
 */
public interface EGLWindowSurfaceFactory {
    /**
     * @return null if the surface cannot be constructed.
     */
    EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display, EGLConfig config,
                                   Object nativeWindow);

    void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface);

    void setActive(boolean active);

    boolean isActive();
}