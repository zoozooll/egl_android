package com.aaron.eglholder.view

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
import com.aaron.eglholder.EGLHolder

class GLTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) :
    TextureView(context, attrs, defStyleAttr, defStyleRes),
    TextureView.SurfaceTextureListener {

    val mEGLHolder = EGLHolder()

    init {
        surfaceTextureListener = this
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mEGLHolder.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mEGLHolder.stop()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        mEGLHolder.surfaceCreated(surface)
        mEGLHolder.surfaceChanged(width, height)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        mEGLHolder.surfaceChanged(width, height)
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        mEGLHolder.requestRender()
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        mEGLHolder.surfaceDestroyed()
        return true
    }
}
