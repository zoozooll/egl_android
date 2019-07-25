package com.aaron.egl;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;

class DefaultContextFactory implements EGLContextFactory {
    private int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    private int mEGLContextClientVersion = 2;

    DefaultContextFactory() {

    }

    DefaultContextFactory(int eglVersion) {
        mEGLContextClientVersion = eglVersion;
    }

    public EGLContext createContext(EGLDisplay display, EGLConfig config) {
        int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, mEGLContextClientVersion,
                EGL14.EGL_NONE};

        return EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT,
                mEGLContextClientVersion != 0 ? attrib_list : null, 0);
    }

    public boolean destroyContext(EGLDisplay display,
                                  EGLContext context) {
        if (!EGL14.eglDestroyContext(display, context)) {
            return false;
//            EglHelper.throwEglException("eglDestroyContext " + egl.eglGetError());
        }
        return true;
    }
}