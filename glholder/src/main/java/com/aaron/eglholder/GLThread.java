package com.aaron.eglholder;

import android.os.Trace;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.opengles.GL10;

import static com.aaron.eglholder.EGLHolder.*;

/**
 * A generic GL Thread. Takes care of initializing EGL and GL. Delegates
 * to a Renderer instance to do the actual drawing. Can be configured to
 * render continuously or on request.
 * <p>
 * All potentially blocking synchronization is done through the
 * EGLHolder.sGLThreadManager object. This avoids multiple-lock ordering issues.
 */
class GLThread extends Thread {

    GLThread(WeakReference<EGLHolder> glSurfaceViewWeakRef) {
        super();
        mWidth = 0;
        mHeight = 0;
        mRequestRender = true;
        mRenderMode = RENDERMODE_CONTINUOUSLY;
        mWantRenderNotification = false;
        mGLSurfaceViewWeakRef = glSurfaceViewWeakRef;
    }

    @Override
    public void run() {
        setName("GLThread " + getId());
        if (LOG_THREADS) {
            Log.i("GLThread", "starting tid=" + getId());
        }

        try {
            guardedRun();
        } catch (InterruptedException e) {
            // fall thru and exit normally
        } finally {
            EGLHolder.sGLThreadManager.threadExiting(this);
        }
    }

    /*
     * This private method should only be called inside a
     * synchronized(sGLThreadManager) block.
     */
    private void stopEglSurfaceLocked() {
        if (mHaveEglSurface) {
            mHaveEglSurface = false;
            mEglHelper.destroySurface();
        }
    }

    /*
     * This private method should only be called inside a
     * synchronized(sGLThreadManager) block.
     */
    private void stopEglContextLocked() {
        if (mHaveEglContext) {
            mEglHelper.finish();
            mHaveEglContext = false;
            EGLHolder.sGLThreadManager.releaseEglContextLocked(this);
        }
    }
    private void guardedRun() throws InterruptedException {
        mEglHelper = new EglHelper(mGLSurfaceViewWeakRef);
        mHaveEglContext = false;
        mHaveEglSurface = false;
        mWantRenderNotification = false;

        try {
            GL10 gl = null;
            boolean createEglContext = false;
            boolean createEglSurface = false;
            boolean createGlInterface = false;
            boolean lostEglContext = false;
            boolean sizeChanged = false;
            boolean wantRenderNotification = false;
            boolean doRenderNotification = false;
            boolean askedToReleaseEglContext = false;
            int w = 0;
            int h = 0;
            Runnable event = null;
            Runnable finishDrawingRunnable = null;

            while (true) {
                synchronized (EGLHolder.sGLThreadManager) {
                    while (true) {
                        if (mShouldExit) {
                            return;
                        }

                        if (! mEventQueue.isEmpty()) {
                            event = mEventQueue.remove(0);
                            break;
                        }

                        // Update the pause state.
                        boolean pausing = false;
                        if (mPaused != mRequestPaused) {
                            pausing = mRequestPaused;
                            mPaused = mRequestPaused;
                            EGLHolder.sGLThreadManager.notifyAll();
                            if (LOG_PAUSE_RESUME) {
                                Log.i("GLThread", "mPaused is now " + mPaused + " tid=" + getId());
                            }
                        }

                        // Do we need to give up the EGL context?
                        if (mShouldReleaseEglContext) {
                            if (LOG_SURFACE) {
                                Log.i("GLThread", "releasing EGL context because asked to tid=" + getId());
                            }
                            stopEglSurfaceLocked();
                            stopEglContextLocked();
                            mShouldReleaseEglContext = false;
                            askedToReleaseEglContext = true;
                        }

                        // Have we lost the EGL context?
                        if (lostEglContext) {
                            stopEglSurfaceLocked();
                            stopEglContextLocked();
                            lostEglContext = false;
                        }

                        // When pausing, release the EGL surface:
                        if (pausing && mHaveEglSurface) {
                            if (LOG_SURFACE) {
                                Log.i("GLThread", "releasing EGL surface because paused tid=" + getId());
                            }
                            stopEglSurfaceLocked();
                        }

                        // When pausing, optionally release the EGL Context:
                        if (pausing && mHaveEglContext) {
                            EGLHolder view = mGLSurfaceViewWeakRef.get();
                            boolean preserveEglContextOnPause = view != null && view.mPreserveEGLContextOnPause;
                            if (!preserveEglContextOnPause) {
                                stopEglContextLocked();
                                if (LOG_SURFACE) {
                                    Log.i("GLThread", "releasing EGL context because paused tid=" + getId());
                                }
                            }
                        }

                        // Have we lost the SurfaceView surface?
                        if ((! mHasSurface) && (! mWaitingForSurface)) {
                            if (LOG_SURFACE) {
                                Log.i("GLThread", "noticed surfaceView surface lost tid=" + getId());
                            }
                            if (mHaveEglSurface) {
                                stopEglSurfaceLocked();
                            }
                            mWaitingForSurface = true;
                            mSurfaceIsBad = false;
                            EGLHolder.sGLThreadManager.notifyAll();
                        }

                        // Have we acquired the surface view surface?
                        if (mHasSurface && mWaitingForSurface) {
                            if (LOG_SURFACE) {
                                Log.i("GLThread", "noticed surfaceView surface acquired tid=" + getId());
                            }
                            mWaitingForSurface = false;
                            EGLHolder.sGLThreadManager.notifyAll();
                        }

                        if (doRenderNotification) {
                            if (LOG_SURFACE) {
                                Log.i("GLThread", "sending render notification tid=" + getId());
                            }
                            mWantRenderNotification = false;
                            doRenderNotification = false;
                            mRenderComplete = true;
                            EGLHolder.sGLThreadManager.notifyAll();
                        }

                        if (mFinishDrawingRunnable != null) {
                            finishDrawingRunnable = mFinishDrawingRunnable;
                            mFinishDrawingRunnable = null;
                        }

                        // Ready to draw?
                        if (readyToDraw()) {

                            // If we don't have an EGL context, try to acquire one.
                            if (! mHaveEglContext) {
                                if (askedToReleaseEglContext) {
                                    askedToReleaseEglContext = false;
                                } else {
                                    try {
                                        mEglHelper.start();
                                    } catch (RuntimeException t) {
                                        EGLHolder.sGLThreadManager.releaseEglContextLocked(this);
                                        throw t;
                                    }
                                    mHaveEglContext = true;
                                    createEglContext = true;

                                    EGLHolder.sGLThreadManager.notifyAll();
                                }
                            }

                            if (mHaveEglContext && !mHaveEglSurface) {
                                mHaveEglSurface = true;
                                createEglSurface = true;
                                createGlInterface = true;
                                sizeChanged = true;
                            }

                            if (mHaveEglSurface) {
                                if (mSizeChanged) {
                                    sizeChanged = true;
                                    w = mWidth;
                                    h = mHeight;
                                    mWantRenderNotification = true;
                                    if (LOG_SURFACE) {
                                        Log.i("GLThread",
                                                "noticing that we want render notification tid="
                                                        + getId());
                                    }

                                    // Destroy and recreate the EGL surface.
                                    createEglSurface = true;

                                    mSizeChanged = false;
                                }
                                mRequestRender = false;
                                EGLHolder.sGLThreadManager.notifyAll();
                                if (mWantRenderNotification) {
                                    wantRenderNotification = true;
                                }
                                break;
                            }
                        } else {
                            if (finishDrawingRunnable != null) {
                                Log.w(TAG, "Warning, !readyToDraw() but waiting for " +
                                        "draw finished! Early reporting draw finished.");
                                finishDrawingRunnable.run();
                                finishDrawingRunnable = null;
                            }
                        }
                        // By design, this is the only place in a GLThread thread where we wait().
                        if (LOG_THREADS) {
                            Log.i("GLThread", "waiting tid=" + getId()
                                    + " mHaveEglContext: " + mHaveEglContext
                                    + " mHaveEglSurface: " + mHaveEglSurface
                                    + " mFinishedCreatingEglSurface: " + mFinishedCreatingEglSurface
                                    + " mPaused: " + mPaused
                                    + " mHasSurface: " + mHasSurface
                                    + " mSurfaceIsBad: " + mSurfaceIsBad
                                    + " mWaitingForSurface: " + mWaitingForSurface
                                    + " mWidth: " + mWidth
                                    + " mHeight: " + mHeight
                                    + " mRequestRender: " + mRequestRender
                                    + " mRenderMode: " + mRenderMode);
                        }
                        sGLThreadManager.wait();
                    }
                } // end of synchronized(sGLThreadManager)

                if (event != null) {
                    event.run();
                    event = null;
                    continue;
                }

                if (createEglSurface) {
                    if (LOG_SURFACE) {
                        Log.w("GLThread", "egl createSurface");
                    }
                    if (mEglHelper.createSurface()) {
                        synchronized(sGLThreadManager) {
                            mFinishedCreatingEglSurface = true;
                            sGLThreadManager.notifyAll();
                        }
                    } else {
                        synchronized(sGLThreadManager) {
                            mFinishedCreatingEglSurface = true;
                            mSurfaceIsBad = true;
                            sGLThreadManager.notifyAll();
                        }
                        continue;
                    }
                    createEglSurface = false;
                }

                if (createGlInterface) {
                    gl = (GL10) mEglHelper.createGL();

                    createGlInterface = false;
                }

                if (createEglContext) {
                    if (LOG_RENDERER) {
                        Log.w("GLThread", "onSurfaceCreated");
                    }
                    EGLHolder view = mGLSurfaceViewWeakRef.get();
                    if (view != null) {
                        try {
//                            Trace.traceBegin(Trace.TRACE_TAG_VIEW, "onSurfaceCreated");
                            view.mRenderer.onSurfaceCreated(gl, mEglHelper.mEglConfig);
                        } finally {
//                            Trace.traceEnd(Trace.TRACE_TAG_VIEW);
                        }
                    }
                    createEglContext = false;
                }

                if (sizeChanged) {
                    if (LOG_RENDERER) {
                        Log.w("GLThread", "onSurfaceChanged(" + w + ", " + h + ")");
                    }
                    EGLHolder view = mGLSurfaceViewWeakRef.get();
                    if (view != null) {
                        try {
//                            Trace.traceBegin(Trace.TRACE_TAG_VIEW, "onSurfaceChanged");
                            view.mRenderer.onSurfaceChanged(gl, w, h);
                        } finally {
//                            Trace.traceEnd(Trace.TRACE_TAG_VIEW);
                        }
                    }
                    sizeChanged = false;
                }

                if (LOG_RENDERER_DRAW_FRAME) {
                    Log.w("GLThread", "onDrawFrame tid=" + getId());
                }
                {
                    EGLHolder view = mGLSurfaceViewWeakRef.get();
                    if (view != null) {
                        try {
//                            Trace.traceBegin(Trace.TRACE_TAG_VIEW, "onDrawFrame");
                            view.mRenderer.onDrawFrame(gl);
                            if (finishDrawingRunnable != null) {
                                finishDrawingRunnable.run();
                                finishDrawingRunnable = null;
                            }
                        } finally {
//                            Trace.traceEnd(Trace.TRACE_TAG_VIEW);
                        }
                    }
                }
                int swapError = mEglHelper.swap();
                switch (swapError) {
                    case EGL10.EGL_SUCCESS:
                        break;
                    case EGL11.EGL_CONTEXT_LOST:
                        if (LOG_SURFACE) {
                            Log.i("GLThread", "egl context lost tid=" + getId());
                        }
                        lostEglContext = true;
                        break;
                    default:
                        // Other errors typically mean that the current surface is bad,
                        // probably because the SurfaceView surface has been destroyed,
                        // but we haven't been notified yet.
                        // Log the error to help developers understand why rendering stopped.
                        EglHelper.logEglErrorAsWarning("GLThread", "eglSwapBuffers", swapError);

                        synchronized(EGLHolder.sGLThreadManager) {
                            mSurfaceIsBad = true;
                            EGLHolder.sGLThreadManager.notifyAll();
                        }
                        break;
                }

                if (wantRenderNotification) {
                    doRenderNotification = true;
                    wantRenderNotification = false;
                }
            }

        } finally {
            /*
             * clean-up everything...
             */
            synchronized (EGLHolder.sGLThreadManager) {
                stopEglSurfaceLocked();
                stopEglContextLocked();
            }
        }
    }

    public boolean ableToDraw() {
        return mHaveEglContext && mHaveEglSurface && readyToDraw();
    }

    private boolean readyToDraw() {
        return (!mPaused) && mHasSurface && (!mSurfaceIsBad)
                && (mWidth > 0) && (mHeight > 0)
                && (mRequestRender || (mRenderMode == RENDERMODE_CONTINUOUSLY));
    }

    public void setRenderMode(int renderMode) {
        if ( !((RENDERMODE_WHEN_DIRTY <= renderMode) && (renderMode <= RENDERMODE_CONTINUOUSLY)) ) {
            throw new IllegalArgumentException("renderMode");
        }
        synchronized(EGLHolder.sGLThreadManager) {
            mRenderMode = renderMode;
            EGLHolder.sGLThreadManager.notifyAll();
        }
    }

    public int getRenderMode() {
        synchronized(EGLHolder.sGLThreadManager) {
            return mRenderMode;
        }
    }

    public void requestRender() {
        synchronized(EGLHolder.sGLThreadManager) {
            mRequestRender = true;
            EGLHolder.sGLThreadManager.notifyAll();
        }
    }

    public void requestRenderAndNotify(Runnable finishDrawing) {
        synchronized(EGLHolder.sGLThreadManager) {
            // If we are already on the GL thread, this means a client callback
            // has caused reentrancy, for example via updating the SurfaceView parameters.
            // We will return to the client rendering code, so here we don't need to
            // do anything.
            if (Thread.currentThread() == this) {
                return;
            }

            mWantRenderNotification = true;
            mRequestRender = true;
            mRenderComplete = false;
            mFinishDrawingRunnable = finishDrawing;

            EGLHolder.sGLThreadManager.notifyAll();
        }
    }

    public void surfaceCreated() {
        synchronized(EGLHolder.sGLThreadManager) {
            if (LOG_THREADS) {
                Log.i("GLThread", "surfaceCreated tid=" + getId());
            }
            mHasSurface = true;
            mFinishedCreatingEglSurface = false;
            EGLHolder.sGLThreadManager.notifyAll();
            while (mWaitingForSurface
                    && !mFinishedCreatingEglSurface
                    && !mExited) {
                try {
                    EGLHolder.sGLThreadManager.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void surfaceDestroyed() {
        synchronized(EGLHolder.sGLThreadManager) {
            if (LOG_THREADS) {
                Log.i("GLThread", "surfaceDestroyed tid=" + getId());
            }
            mHasSurface = false;
            EGLHolder.sGLThreadManager.notifyAll();
            while((!mWaitingForSurface) && (!mExited)) {
                try {
                    EGLHolder.sGLThreadManager.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void onPause() {
        synchronized (EGLHolder.sGLThreadManager) {
            if (LOG_PAUSE_RESUME) {
                Log.i("GLThread", "onPause tid=" + getId());
            }
            mRequestPaused = true;
            EGLHolder.sGLThreadManager.notifyAll();
            while ((! mExited) && (! mPaused)) {
                if (LOG_PAUSE_RESUME) {
                    Log.i("Main thread", "onPause waiting for mPaused.");
                }
                try {
                    EGLHolder.sGLThreadManager.wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void onResume() {
        synchronized (EGLHolder.sGLThreadManager) {
            if (LOG_PAUSE_RESUME) {
                Log.i("GLThread", "onResume tid=" + getId());
            }
            mRequestPaused = false;
            mRequestRender = true;
            mRenderComplete = false;
            EGLHolder.sGLThreadManager.notifyAll();
            while ((! mExited) && mPaused && (!mRenderComplete)) {
                if (LOG_PAUSE_RESUME) {
                    Log.i("Main thread", "onResume waiting for !mPaused.");
                }
                try {
                    EGLHolder.sGLThreadManager.wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void onWindowResize(int w, int h) {
        synchronized (EGLHolder.sGLThreadManager) {
            mWidth = w;
            mHeight = h;
            mSizeChanged = true;
            mRequestRender = true;
            mRenderComplete = false;

            // If we are already on the GL thread, this means a client callback
            // has caused reentrancy, for example via updating the SurfaceView parameters.
            // We need to process the size change eventually though and update our EGLSurface.
            // So we set the parameters and return so they can be processed on our
            // next iteration.
            if (Thread.currentThread() == this) {
                return;
            }

            EGLHolder.sGLThreadManager.notifyAll();

            // Wait for thread to react to resize and render a frame
            while (! mExited && !mPaused && !mRenderComplete
                    && ableToDraw()) {
                if (LOG_SURFACE) {
                    Log.i("Main thread", "onWindowResize waiting for render complete from tid=" + getId());
                }
                try {
                    EGLHolder.sGLThreadManager.wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void requestExitAndWait() {
        // don't call this from GLThread thread or it is a guaranteed
        // deadlock!
        synchronized(EGLHolder.sGLThreadManager) {
            mShouldExit = true;
            EGLHolder.sGLThreadManager.notifyAll();
            while (! mExited) {
                try {
                    EGLHolder.sGLThreadManager.wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void requestReleaseEglContextLocked() {
        mShouldReleaseEglContext = true;
        EGLHolder.sGLThreadManager.notifyAll();
    }

    /**
     * Queue an "event" to be run on the GL rendering thread.
     * @param r the runnable to be run on the GL rendering thread.
     */
    public void queueEvent(Runnable r) {
        if (r == null) {
            throw new IllegalArgumentException("r must not be null");
        }
        synchronized(EGLHolder.sGLThreadManager) {
            mEventQueue.add(r);
            EGLHolder.sGLThreadManager.notifyAll();
        }
    }

    // Once the thread is started, all accesses to the following member
    // variables are protected by the sGLThreadManager monitor
    private boolean mShouldExit;
    boolean mExited;
    private boolean mRequestPaused;
    private boolean mPaused;
    private boolean mHasSurface;
    private boolean mSurfaceIsBad;
    private boolean mWaitingForSurface;
    private boolean mHaveEglContext;
    private boolean mHaveEglSurface;
    private boolean mFinishedCreatingEglSurface;
    private boolean mShouldReleaseEglContext;
    private int mWidth;
    private int mHeight;
    private int mRenderMode;
    private boolean mRequestRender;
    private boolean mWantRenderNotification;
    private boolean mRenderComplete;
    private ArrayList<Runnable> mEventQueue = new ArrayList<Runnable>();
    private boolean mSizeChanged = true;
    private Runnable mFinishDrawingRunnable = null;

    // End of member variables protected by the sGLThreadManager monitor.

    private EglHelper mEglHelper;

    /**
     * Set once at thread construction time, nulled out when the parent view is garbage
     * called. This weak reference allows the GLSurfaceView to be garbage collected while
     * the GLThread is still alive.
     */
    private WeakReference<EGLHolder> mGLSurfaceViewWeakRef;
}