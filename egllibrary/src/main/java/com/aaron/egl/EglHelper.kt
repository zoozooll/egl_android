package com.aaron.egl

import android.content.ContentValues.TAG
import android.opengl.*
import android.opengl.EGL14.*
import android.util.Log

internal class EglHelper {
//    var mEgl: EGL10? = null
    var mEglDisplay: EGLDisplay? = null
    var mEglSurface: EGLSurface? = null
    var mEglConfig: EGLConfig? = null
    var mEglContext: EGLContext? = null
    var mEGLContextClientVersion: Int = 2

    var mEGLConfigChooser: EGLConfigChooser? = null
        set(value) {
            if (checkRenderThreadState()) {
                field = value
            }
        }
    var mEGLContextFactory: EGLContextFactory? = null
        set(value) {
            if (checkRenderThreadState()) {
                field = value
            }
        }
    var mEGLWindowSurfaceFactory: EGLWindowSurfaceFactory? = null
        set(value) {
            if (checkRenderThreadState()) {
                field = value
            }
        }

    fun createEglContext() : Boolean {
        if (mEGLConfigChooser == null) {
            mEGLConfigChooser = RecordableEGLConfigChooser(mEGLContextClientVersion)
        }
        if (mEGLContextFactory == null) {
            mEGLContextFactory = DefaultContextFactory(mEGLContextClientVersion)
        }
        if (mEGLWindowSurfaceFactory == null) {
            mEGLWindowSurfaceFactory = DefaultWindowSurfaceFactory()
        }

//        mEgl = EGLContext.getEGL() as EGL10
        mEglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY)

        if (mEglDisplay === EGL_NO_DISPLAY) {
            Log.e(TAG, "eglGetDisplay failed")
            destroyEglContext()
            return false
        }
        val version = IntArray(2)
        if (!eglInitialize(mEglDisplay, version, 0, version, 1)) {
            Log.e(TAG, "eglInitialize failed")
            destroyEglContext()
            return false
        }
        mEglConfig = mEGLConfigChooser!!.chooseConfig(mEglDisplay)

        /*
         * Create an EGL context. We want to do this as rarely as we can, because an
         * EGL context is a somewhat heavy object.
         */
        mEglContext = mEGLContextFactory!!.createContext(mEglDisplay, mEglConfig)
        if (mEglContext === EGL_NO_CONTEXT) {
            Log.e(TAG, "createContext")
            destroyEglContext()
            return false
        }
        mEglSurface = null
        return true
    }

    /**
     * Create an egl surface for the current SurfaceHolder surface. If a surface
     * already exists, unregister it before creating the new surface.
     *
     * @return true if the surface was created successfully.
     */
    fun createSurface(displayObject: Any): Boolean {
        /*
         * Check preconditions.
         */
        if (mEglDisplay == null) {
            Log.e(TAG, "createSurface eglDisplay not initialized")
            return false
        }
        if (mEglConfig == null) {
            Log.e(TAG, "createSurface mEglConfig not initialized")
            return false
        }

        /*
         *  The window size has changed, so we need to create a new
         *  surface.
         */
        destroySurfaceImp()
        mEGLWindowSurfaceFactory!!.setNativeWindow(displayObject)
        /*
         * Create an EGL surface we can render into.
         */
        mEglSurface = mEGLWindowSurfaceFactory!!.createWindowSurface( mEglDisplay, mEglConfig)

        if (mEglSurface == null || mEglSurface === EGL_NO_SURFACE) {
            val error = eglGetError()
            if (error == EGL_BAD_NATIVE_WINDOW) {
                Log.e(TAG, "createSurface false : EGL_BAD_NATIVE_WINDOW.")
            } else {
                Log.e(TAG, "createSurface false .")
            }
            destroySurfaceImp()
            return false
        }

        /*
         * Before we can issue GL commands, we need to make sure
         * the context is current and bound to a surface.
         */
        if (!EGLUtils.makeCurrentSurface(mEglDisplay, mEglContext, mEglSurface)) {
            /*
             * Could not make the context current, probably because the underlying
             * SurfaceView surface has been destroyed.
             */
            logEglErrorAsWarning("EGLHelper", "eglMakeCurrent", eglGetError())
            return false
        }

        return true
    }


    /**
     * Display the current render surface.
     *
     * @return the EGL error code from eglSwapBuffers.
     */
    fun swap(): Int {
        return if (!EGLUtils.swapSurface(mEglDisplay, mEglSurface)) {
            eglGetError()
        } else EGL_SUCCESS
    }

    fun destroySurface() {
        destroySurfaceImp()
    }

    private fun destroySurfaceImp() {
        if (mEglSurface != null && mEglSurface !== EGL_NO_SURFACE) {
            eglMakeCurrent(
                mEglDisplay, EGL_NO_SURFACE,
                EGL_NO_SURFACE,
                EGL_NO_CONTEXT
            )
            mEGLWindowSurfaceFactory!!.destroySurface(mEglDisplay, mEglSurface)
            mEglSurface = null
        }
    }

    fun destroyEglContext() {
        if (mEglContext != null) {
            mEGLContextFactory!!.destroyContext(mEglDisplay, mEglContext)
            mEglContext = null
        }
        if (mEglDisplay != null) {
            eglTerminate(mEglDisplay)
            mEglDisplay = null
        }
    }

    private fun throwEglException(function: String) {
        throwEglException(function, eglGetError())
    }

    private fun checkRenderThreadState(): Boolean {
        if (mEglContext != null) {
            return false
        }
        return true
    }

    companion object {

        fun throwEglException(function: String, error: Int) {
            val message = formatEglError(function, error)
//            Logger.w(
//                "EglHelper", "throwEglException tid=" + Thread.currentThread().id + " "
//                        + message
//            )
            throw RuntimeException(message)
        }

        fun logEglErrorAsWarning(tag: String, function: String, error: Int) {
//            Logger.w(tag, formatEglError(function, error))
        }

        fun formatEglError(function: String, error: Int): String {
            return "$function failed: $error"
        }
    }
}
