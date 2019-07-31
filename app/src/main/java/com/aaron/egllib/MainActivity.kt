package com.aaron.egllib

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaron.sample.light.LightActivity
import com.aaron.sample.matrixdemo.TransformActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        /*id_textView.setOnClickListener {
            Intent(this, LightActivity::class.java).apply {
                startActivity(this)
            }
        }*/

        val myDataset = resources.getStringArray(R.array.data_list)
        viewManager = LinearLayoutManager(this)
        viewAdapter = MyAdapter(myDataset)
        list_main.setHasFixedSize(true)
        list_main.layoutManager = viewManager
        list_main.adapter = viewAdapter

    }

    inner class MyAdapter(private val myDataset: Array<String>) :
        RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

        inner class MyViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
            val textView: TextView? = rootView.findViewById<TextView>(android.R.id.text1)
            val icon: ImageView? = rootView.findViewById<ImageView>(android.R.id.icon)
        }


        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): MyAdapter.MyViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)

            return MyViewHolder(view)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.textView?.text = myDataset[position]
            holder.icon?.setImageResource(R.mipmap.ic_launcher)
            holder.itemView.setOnClickListener {
                val type = when(myDataset[position]) {
                    "Light" -> LightActivity::class.java
                    else -> TransformActivity::class.java
                }
                Intent(this@MainActivity, type).apply {
                    startActivity(this)
                }
            }
        }

        override fun getItemCount() = myDataset.size
    }
}
