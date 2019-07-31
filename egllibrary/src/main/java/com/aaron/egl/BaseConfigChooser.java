package com.aaron.egl;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;

public abstract class BaseConfigChooser implements EGLConfigChooser {

    protected int[] mConfigSpec;
    private int mEGLContextClientVersion = 2;

    public BaseConfigChooser(int[] configSpec) {
        mConfigSpec = filterConfigSpec(configSpec);
    }

    public BaseConfigChooser(int[] configSpec, int mEGLContextClientVersion) {
        mConfigSpec = filterConfigSpec(configSpec);
        this.mEGLContextClientVersion = mEGLContextClientVersion;
    }

    public EGLConfig chooseConfig(EGLDisplay display) {
        int[] num_config = new int[1];
        if (!EGL14.eglChooseConfig(display, mConfigSpec, 0, null, 0, 0,
                num_config, 0)) {
            throw new IllegalArgumentException("eglChooseConfig failed");
        }

        int numConfigs = num_config[0];

        if (numConfigs <= 0) {
            throw new IllegalArgumentException(
                    "No configs match configSpec");
        }

        EGLConfig[] configs = new EGLConfig[numConfigs];
        if (!EGL14.eglChooseConfig(display, mConfigSpec, 0, configs, 0, numConfigs,
                num_config, 0)) {
            throw new IllegalArgumentException("eglChooseConfig#2 failed");
        }
        EGLConfig config = chooseConfig(display, configs);
        if (config == null) {
            throw new IllegalArgumentException("No config chosen");
        }
        return config;
    }

    abstract EGLConfig chooseConfig(EGLDisplay display,
                                    EGLConfig[] configs);

    private int[] filterConfigSpec(int[] configSpec) {
        if (mEGLContextClientVersion != 2 && mEGLContextClientVersion != 3) {
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
}