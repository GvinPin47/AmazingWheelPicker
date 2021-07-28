package com.test.testwheelviewlib

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TestViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    fun bind(item: CharSequence) {
        itemView.findViewById<TextView>(R.id.testTextView).text = item
    }
}