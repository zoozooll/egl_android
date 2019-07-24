package com.aaron.eglholder;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * An interface for choosing an EGLConfig configuration from a list of
 * potential configurations.
 * <p>
 * This interface must be implemented by clients wishing to call
 * {@link EGLHolder#setEGLConfigChooser(EGLConfigChooser)}
 */
public interface EGLConfigChooser {
    /**
     * Choose a configuration from the list. Implementors typically
     * implement this method by calling
     * {@link EGL10#eglChooseConfig} and iterating through the results. Please consult the
     * EGL specification available from The Khronos Group to learn how to call eglChooseConfig.
     *
     * @param egl     the EGL10 for the current display.
     * @param display the current display.
     * @return the chosen configuration.
     */
    EGLConfig chooseConfig(EGL10 egl, EGLDisplay display);
}