package com.aaron.eglholder.view

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.aaron.eglholder.EGLHolder

class GLSurfaceView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) :
    SurfaceView(context, attrs, defStyleAttr, defStyleRes),
    SurfaceHolder.Callback2 {

    private val mContext: Context = context
    private val mEGLHolder = EGLHolder()
    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        mEGLHolder.setSurface(holder)
        mEGLHolder.start()
    }

    override fun surfaceRedrawNeeded(holder: SurfaceHolder?) {
        // Do nothing
    }

    override fun surfaceRedrawNeededAsync(holder: SurfaceHolder?, finishDrawing: Runnable) {
        mEGLHolder.requestRenderAndNotify(finishDrawing)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        mEGLHolder.surfaceChanged(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        mEGLHolder.surfaceDestroyed()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mEGLHolder.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mEGLHolder.stop()
    }
}
