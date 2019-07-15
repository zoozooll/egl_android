package com.aaron.egl;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.os.Build;


class RecordableEGLConfigChooser implements EGLConfigChooser {

    protected int[] mConfigSpec;
    private int mEGLContextClientVersion = 2;

    public RecordableEGLConfigChooser(int glesVersion) {
        int[] configSpec = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 0,
                0x3142, 1,
                EGL14.EGL_NONE
        };
        mConfigSpec = filterConfigSpec(configSpec);
        this.mEGLContextClientVersion = glesVersion;
    }

    public EGLConfig chooseConfig(EGLDisplay display) {
        int[] num_config = new int[1];
        if (!EGL14.eglChooseConfig(display, mConfigSpec, 0, null, 0, 0, num_config, 0)) {
            throw new IllegalArgumentException("eglChooseConfig failed");
        }

        int numConfigs = num_config[0];

        if (numConfigs <= 0) {
            throw new IllegalArgumentException(
                    "No configs match configSpec");
        }

        EGLConfig[] configs = new EGLConfig[numConfigs];
        if (!EGL14.eglChooseConfig(display, mConfigSpec, 0, configs, 0,  numConfigs,
                num_config, 0)) {
            throw new IllegalArgumentException("eglChooseConfig#2 failed");
        }
        EGLConfig config = chooseConfig(display, configs);
        if (config == null) {
            throw new IllegalArgumentException("No config chosen");
        }
        return config;
    }

    public EGLConfig chooseConfig(EGLDisplay display,
                                  EGLConfig[] configs) {
        for (EGLConfig config : configs) {
            int d = findConfigAttrib(display, config,
                    EGL14.EGL_DEPTH_SIZE, 0);
            int s = findConfigAttrib(display, config,
                    EGL14.EGL_STENCIL_SIZE, 0);
            if ((d >= mConfigSpec[9]) && (s >= 0)) {
                int r = findConfigAttrib(display, config,
                        EGL14.EGL_RED_SIZE, 0);
                int g = findConfigAttrib(display, config,
                        EGL14.EGL_GREEN_SIZE, 0);
                int b = findConfigAttrib(display, config,
                        EGL14.EGL_BLUE_SIZE, 0);
                int a = findConfigAttrib(display, config,
                        EGL14.EGL_ALPHA_SIZE, 0);
                if ((r == mConfigSpec[1]) && (g == mConfigSpec[3])
                        && (b == mConfigSpec[5]) && (a == mConfigSpec[7])) {
                    return config;
                }
            }
        }
        return null;
    }


    private int[] filterConfigSpec(int[] configSpec) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            configSpec[10] = EGLExt.EGL_RECORDABLE_ANDROID;
        }
        if (mEGLContextClientVersion < 2) {
            return configSpec;
        }
        /* We know none of the subclasses define EGL_RENDERABLE_TYPE.
         * And we know the configSpec is well formed.
         */
        int len = configSpec.length;
        int[] newConfigSpec = new int[len + 2];
        System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1);
        newConfigSpec[len - 1] = EGL14.EGL_RENDERABLE_TYPE;
        if (mEGLContextClientVersion == 2) {
            newConfigSpec[len] = EGL14.EGL_OPENGL_ES2_BIT;  /* EGL_OPENGL_ES2_BIT */
        } else {
            newConfigSpec[len] = EGLExt.EGL_OPENGL_ES3_BIT_KHR; /* EGL_OPENGL_ES3_BIT_KHR */
        }
        newConfigSpec[len + 1] = EGL14.EGL_NONE;
        return newConfigSpec;
    }

    private int findConfigAttrib(EGLDisplay display,
                                 EGLConfig config, int attribute, int defaultValue) {

        int[] mValue = new int[1];
        if (EGL14.eglGetConfigAttrib(display, config, attribute, mValue, 0)) {
            return mValue[0];
        }
        return defaultValue;
    }
}
