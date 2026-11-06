package com.michaelzhou.viewdemo.todo

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.michaelzhou.viewdemo.R

class TodoAdapter(
    private val onToggle: (Long) -> Unit,
    private val onDelete: (Long) -> Unit
) : RecyclerView.Adapter<TodoAdapter.ViewHolder>() {

    private var items: List<TodoItem> = emptyList()

    fun submitList(list: List<TodoItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val cbDone: CheckBox = view.findViewById(R.id.cbDone)
        private val tvTask: TextView = view.findViewById(R.id.tvTask)
        private val btnDelete: Button = view.findViewById(R.id.btnDelete)

        fun bind(item: TodoItem) {
            tvTask.text = item.text
            cbDone.isChecked = item.isDone
            tvTask.paintFlags = if (item.isDone)
                tvTask.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            else
                tvTask.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            cbDone.setOnClickListener { onToggle(item.id) }
            btnDelete.setOnClickListener { onDelete(item.id) }
        }
    }
}
