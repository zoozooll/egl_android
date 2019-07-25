package com.aaron.egl

import android.os.*
import android.util.Log

class BaseEGLHolder {

    private var mGLThread: HandlerThread? = null
    private var mGLHandler: Handler? = null
    private var mUiHandler: Handler? = null
    private lateinit var mRenderer: Renderer
    private lateinit var mEGLBinder: EGLBinder

    private var mEGLContextClientVersion = 2
    private var mEglHelper: EglHelper? = null

    fun setEglBinder(eglController: EGLBinder) {
        this.mEGLBinder = eglController
    }

    fun setRenderer(renderer: Renderer) {
        checkRenderThreadState()

        mRenderer = renderer
        mGLThread = HandlerThread("mGLThread")
        mGLThread!!.start()
        mGLHandler = object : Handler(mGLThread!!.looper) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    GLMSG_CREATE -> {
                        val obj = msg.obj
                        onGLCreate(obj)
                    }
                    GLMSG_CHANGE_SIZE -> {
                        onGLSizeChange(msg.arg1, msg.arg2)
                    }
                    GLMSG_DRAW_FRAME -> {
                        if (mEglHelper?.mEglSurface != null)
                            onGLDrawFrame()
                    }
                    GLMSG_DESTROY -> onGLDestroy()
                }
            }
        }
        mUiHandler = Handler(Looper.getMainLooper())
    }

    fun setDisplayWindowSurface(nativeSurface: Any) {
        mEglHelper?.mEGLWindowSurfaceFactory!!.setNativeWindow(nativeSurface)
    }

    fun setEGLContextFactory(factory: EGLContextFactory) {
        checkRenderThreadState()
        mEglHelper?.mEGLContextFactory = factory
    }

    fun setEGLWindowSurfaceFactory(factory: EGLWindowSurfaceFactory) {
        checkRenderThreadState()
        mEglHelper?.mEGLWindowSurfaceFactory = factory
    }

    fun setEGLConfigChooser(configChooser: EGLConfigChooser) {
        checkRenderThreadState()
        mEglHelper?.mEGLConfigChooser = configChooser
    }

    fun setEGLContextClientVersion(version: Int) {
        checkRenderThreadState()
        mEGLContextClientVersion = version
    }


    fun requestRender() {
//        Log.i(TAG, "requestRender")
        mGLHandler!!.sendEmptyMessage(GLMSG_DRAW_FRAME)
    }

    fun surfaceCreated(displayObject: Any) {
        Log.i(TAG, "surfaceCreated")
        val msg = mGLHandler!!.obtainMessage(GLMSG_CREATE)
        msg.obj = displayObject
        mGLHandler!!.sendMessage(msg)
    }

    fun surfaceDestroyed() {
        Log.i(TAG, "surfaceDestroyed")
        mGLHandler?.sendEmptyMessage(GLMSG_DESTROY)
    }

    fun surfaceChanged(w: Int, h: Int) {
        Log.i(TAG, "surfaceChanged")
        val msg = mGLHandler!!.obtainMessage(GLMSG_CHANGE_SIZE)
        msg.arg1 = w
        msg.arg2 = h
        mGLHandler?.sendMessage(msg)
    }

    fun queueEvent(r: Runnable) {
        mGLHandler?.post(r)
    }

    private fun onGLCreate(displayObject: Any) {
        val start = SystemClock.uptimeMillis()
        mEglHelper = EglHelper()
        mEglHelper?.createEglContext()
        mEglHelper?.createSurface(displayObject)
        Log.i(TAG, "onGLCreate ${SystemClock.uptimeMillis() - start}")
        mEGLBinder.onEglCreated(
            mEglHelper!!.mEglContext, mEglHelper!!.mEglDisplay, mEglHelper!!.mEglSurface, mEglHelper!!.mEglConfig
        )
        mRenderer.onSurfaceCreated()
    }

    private fun onGLSizeChange(width: Int, height: Int) {
        Log.i(TAG, "onGLSizeChange $width, $height")
        mRenderer.onSurfaceChanged(width, height)
    }

    private fun onGLDrawFrame() {
//        Log.i(TAG, "onGLDrawFrame")
        mEGLBinder.onEGLPreDrawFrame(
            mEglHelper!!.mEglContext, mEglHelper!!.mEglDisplay, mEglHelper!!.mEglConfig
        )
        mRenderer.onDrawFrame()
        mEGLBinder.onEGLDrawFrame(
            mEglHelper!!.mEglContext, mEglHelper!!.mEglDisplay, mEglHelper!!.mEglConfig
        )
    }

    private fun onGLDestroy() {
        Log.i(TAG, "onGLDestroy")
        mRenderer.onSurfaceDestroyed()
        mEGLBinder.onDestroyEgl()
        mEglHelper?.destroySurface()
        mEglHelper?.destroyEglContext()

        mGLHandler?.removeCallbacksAndMessages(null)
        try {
            mGLThread?.join()
            mGLThread = null
            mGLHandler = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkRenderThreadState(): Boolean {
        if (mGLThread != null) {
            return false
        }
        return true
    }

    companion object {
        private const val TAG = "BaseEGLHolder"

        private const val GLMSG_CREATE = 1
        private const val GLMSG_CHANGE_SIZE = 2
        private const val GLMSG_DRAW_FRAME = 3
        private const val GLMSG_DESTROY = 4
    }

}
