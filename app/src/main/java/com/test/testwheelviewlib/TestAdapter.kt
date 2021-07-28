package com.test.testwheelviewlib

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bekawestberg.loopinglayout.library.LoopingLayoutManager

class TestAdapter : RecyclerView.Adapter<TestViewHolder>() {
    private var items = listOf<CharSequence>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
        return TestViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_test_holder, parent, false)
        )
    }

    override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
       holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<CharSequence>) {
        items = newItems
        notifyDataSetChanged()
    }
}