package com.aaron.eglholder;

import android.util.Log;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

import static com.aaron.eglholder.EGLHolder.LOG_THREADS;

public class DefaultContextFactory implements EGLContextFactory {

    private int mEGLContextClientVersion = 2;
    private int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

    public DefaultContextFactory(int glVersion) {
        mEGLContextClientVersion = glVersion;
    }

    public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
        int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, mEGLContextClientVersion,
                EGL10.EGL_NONE};

        return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT,
                mEGLContextClientVersion != 0 ? attrib_list : null);
    }

    public void destroyContext(EGL10 egl, EGLDisplay display,
                               EGLContext context) {
        if (!egl.eglDestroyContext(display, context)) {
            Log.e("DefaultContextFactory", "display:" + display + " context: " + context);
            if (LOG_THREADS) {
                Log.i("DefaultContextFactory", "tid=" + Thread.currentThread().getId());
            }
            EglHelper.throwEglException("eglDestroyContex", egl.eglGetError());
        }
    }
}
