package com.aaron.egllib

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aaron.sample.light.LightActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        id_textView.setOnClickListener {
            Intent(this, LightActivity::class.java).apply {
                startActivity(this)
            }
        }
    }
}
