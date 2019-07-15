package com.aaron.egl;

import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;

public interface EGLBinder {

    void onEglCreated(EGLContext eglContext, EGLDisplay eglDisplay, EGLSurface windowSurface, EGLConfig eglConfig);

    void onDestroyEgl();

    void onEGLPreDrawFrame(EGLContext eglContext, EGLDisplay eglDisplay, EGLConfig eglConfig);

    void onEGLDrawFrame(EGLContext eglContext, EGLDisplay eglDisplay, EGLConfig eglConfig);
}
