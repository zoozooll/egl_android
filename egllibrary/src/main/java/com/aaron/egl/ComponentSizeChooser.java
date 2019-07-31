package com.aaron.egl;


import android.opengl.EGLConfig;
import android.opengl.EGLDisplay;

import static android.opengl.EGL14.*;

/**
 * Choose a configuration with exactly the specified r,g,b,a sizes,
 * and at least the specified depth and stencil sizes.
 */
public class ComponentSizeChooser extends BaseConfigChooser {

    private int[] mValue;
    // Subclasses can adjust these values:
    protected int mRedSize;
    protected int mGreenSize;
    protected int mBlueSize;
    protected int mAlphaSize;
    protected int mDepthSize;
    protected int mStencilSize;

    public ComponentSizeChooser(int redSize, int greenSize, int blueSize,
                                int alphaSize, int depthSize, int stencilSize) {
        super(new int[]{
               EGL_RED_SIZE, redSize,
               EGL_GREEN_SIZE, greenSize,
               EGL_BLUE_SIZE, blueSize,
               EGL_ALPHA_SIZE, alphaSize,
               EGL_DEPTH_SIZE, depthSize,
               EGL_STENCIL_SIZE, stencilSize,
               EGL_NONE});
        mValue = new int[1];
        mRedSize = redSize;
        mGreenSize = greenSize;
        mBlueSize = blueSize;
        mAlphaSize = alphaSize;
        mDepthSize = depthSize;
        mStencilSize = stencilSize;
    }

    @Override
    public EGLConfig chooseConfig(EGLDisplay display,
                                  EGLConfig[] configs) {
        for (EGLConfig config : configs) {
            int d = findConfigAttrib(display, config,
                    EGL_DEPTH_SIZE, 0);
            int s = findConfigAttrib(display, config,
                    EGL_STENCIL_SIZE, 0);
            if ((d >= mDepthSize) && (s >= mStencilSize)) {
                int r = findConfigAttrib(display, config,
                        EGL_RED_SIZE, 0);
                int g = findConfigAttrib(display, config,
                        EGL_GREEN_SIZE, 0);
                int b = findConfigAttrib(display, config,
                        EGL_BLUE_SIZE, 0);
                int a = findConfigAttrib(display, config,
                        EGL_ALPHA_SIZE, 0);
                if ((r == mRedSize) && (g == mGreenSize)
                        && (b == mBlueSize) && (a == mAlphaSize)) {
                    return config;
                }
            }
        }
        return null;
    }

    private int findConfigAttrib(EGLDisplay display,
                                 EGLConfig config, int attribute, int defaultValue) {

        if (eglGetConfigAttrib(display, config, attribute, mValue, 0)) {
            return mValue[0];
        }
        return defaultValue;
    }
}