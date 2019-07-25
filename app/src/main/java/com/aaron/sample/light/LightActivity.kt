package com.aaron.sample.light

import android.opengl.GLSurfaceView
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.aaron.eglholder.EGLHolder
import com.aaron.egllib.R

import kotlinx.android.synthetic.main.activity_light.*
import kotlinx.android.synthetic.main.content_light.*

class LightActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_light)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        initGL()
    }

    private fun initGL() {
        glView.mEGLHolder.setEGLContextClientVersion(3)
        val mRenderer = LightRenderer()
        glView.mEGLHolder.setRenderer(mRenderer)
        glView.mEGLHolder.renderMode = EGLHolder.RENDERMODE_CONTINUOUSLY
    }

}
