package com.aaron.eglholder;

import android.util.Log;

import javax.microedition.khronos.opengles.GL10;

import static com.aaron.eglholder.EGLHolder.LOG_SURFACE;
import static com.aaron.eglholder.EGLHolder.LOG_THREADS;

public class GLThreadManager {
    private static String TAG = "GLThreadManager";

    public synchronized void threadExiting(GLThread thread) {
        if (LOG_THREADS) {
            Log.i("GLThread", "exiting tid=" + thread.getId());
        }
        thread.mExited = true;
        if (mEglOwner == thread) {
            mEglOwner = null;
        }
        notifyAll();
    }

    /*
     * Tries once to acquire the right to use an EGL
     * context. Does not block. Requires that we are already
     * in the sGLThreadManager monitor when this is called.
     *
     * @return true if the right to use an EGL context was acquired.
     */
    public boolean tryAcquireEglContextLocked(GLThread thread) {
        if (mEglOwner == thread || mEglOwner == null) {
            mEglOwner = thread;
            notifyAll();
            return true;
        }
        checkGLESVersion();
        if (mMultipleGLESContextsAllowed) {
            return true;
        }
        // Notify the owning thread that it should release the context.
        // TODO: implement a fairness policy. Currently
        // if the owning thread is drawing continuously it will just
        // reacquire the EGL context.
        if (mEglOwner != null) {
            mEglOwner.requestReleaseEglContextLocked();
        }
        return false;
    }

    /*
     * Releases the EGL context. Requires that we are already in the
     * sGLThreadManager monitor when this is called.
     */
    public void releaseEglContextLocked(GLThread thread) {
        if (mEglOwner == thread) {
            mEglOwner = null;
        }
        notifyAll();
    }

    public synchronized boolean shouldReleaseEGLContextWhenPausing() {
        // Release the EGL context when pausing even if
        // the hardware supports multiple EGL contexts.
        // Otherwise the device could run out of EGL contexts.
        return mLimitedGLESContexts;
    }

    public synchronized boolean shouldTerminateEGLWhenPausing() {
        checkGLESVersion();
        return !mMultipleGLESContextsAllowed;
    }

    public synchronized void checkGLDriver(GL10 gl) {
        if (!mGLESDriverCheckComplete) {
            checkGLESVersion();
            String renderer = gl.glGetString(GL10.GL_RENDERER);
            if (mGLESVersion < kGLES_20) {
                mMultipleGLESContextsAllowed =
                        !renderer.startsWith(kMSM7K_RENDERER_PREFIX);
                notifyAll();
            }
            mLimitedGLESContexts = !mMultipleGLESContextsAllowed;
            if (LOG_SURFACE) {
                Log.w(TAG, "checkGLDriver renderer = \"" + renderer + "\" multipleContextsAllowed = "
                        + mMultipleGLESContextsAllowed
                        + " mLimitedGLESContexts = " + mLimitedGLESContexts);
            }
            mGLESDriverCheckComplete = true;
        }
    }

    private void checkGLESVersion() {
        if (!mGLESVersionCheckComplete) {
//                mGLESVersion = SystemProperties.getInt(
//                        "ro.opengles.version",
//                        ConfigurationInfo.GL_ES_VERSION_UNDEFINED);
//                if (mGLESVersion >= kGLES_20) {
//                    mMultipleGLESContextsAllowed = true;
//                }
//                if (LOG_SURFACE) {
//                    Log.w(TAG, "checkGLESVersion mGLESVersion =" +
//                            " " + mGLESVersion + " mMultipleGLESContextsAllowed = " + mMultipleGLESContextsAllowed);
//                }
            mGLESVersionCheckComplete = true;
        }
    }

    /**
     * This check was required for some pre-Android-3.0 hardware. Android 3.0 provides
     * support for hardware-accelerated views, therefore multiple EGL contexts are
     * supported on all Android 3.0+ EGL drivers.
     */
    private boolean mGLESVersionCheckComplete;
    private int mGLESVersion;
    private boolean mGLESDriverCheckComplete;
    private boolean mMultipleGLESContextsAllowed;
    private boolean mLimitedGLESContexts;
    private static final int kGLES_20 = 0x20000;
    private static final String kMSM7K_RENDERER_PREFIX =
            "Q3Dimension MSM7500 ";
    private GLThread mEglOwner;
}