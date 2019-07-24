package com.aaron.eglholder;

import android.util.Log;

import javax.microedition.khronos.opengles.GL10;

import static com.aaron.eglholder.EGLHolder.LOG_SURFACE;
import static com.aaron.eglholder.EGLHolder.LOG_THREADS;

public class GLThreadManager {
    private static String TAG = "GLThreadManager";

    public synchronized void threadExiting(GLThread thread) {
        if (LOG_THREADS) {
            Log.i("GLThread", "exiting tid=" +  thread.getId());
        }
        thread.mExited = true;
        notifyAll();
    }

    /*
     * Releases the EGL context. Requires that we are already in the
     * sGLThreadManager monitor when this is called.
     */
    public void releaseEglContextLocked(GLThread thread) {
        notifyAll();
    }
}