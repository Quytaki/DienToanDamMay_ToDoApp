package com.example.to_doapp.utils.adapter
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.to_doapp.R
import com.example.to_doapp.databinding.EachTodoItemBinding
import com.example.to_doapp.utils.model.ToDoData
import java.text.SimpleDateFormat
import java.util.*
class TaskAdapter(private var list: MutableList<ToDoData>) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
    private val TAG = "TaskAdapter"
    private var listener: TaskAdapterInterface? = null
    var isSelectionMode = false
    fun setListener(listener: TaskAdapterInterface) {
        this.listener = listener
    }
    class TaskViewHolder(val binding: EachTodoItemBinding) : RecyclerView.ViewHolder(binding.root) {
        var lastClickTime: Long = 0
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = EachTodoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }
    @SuppressLint("SimpleDateFormat")
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        with(holder) {
            with(list[position]) {
                binding.todoTask.text = this.task
                val date = Date(this.timestamp)
                val sdf = SimpleDateFormat("EEE, dd MMM yyyy, HH:mm", Locale.getDefault())
                binding.textTime.text = sdf.format(date)
                binding.selectionCircle.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
                binding.selectionCircle.isChecked = this.isSelected
                binding.selectionCircle.setOnClickListener {
                    if (isSelectionMode) {
                        toggleSelection(position)
                    }
                }
                val isItemSelected = this.isSelected
                val backgroundDrawableRes = when {
                    list.size == 1 -> if (isItemSelected) R.drawable.selected_rounded_corners else R.drawable.rounded_corners
                    position == 0 -> if (isItemSelected) R.drawable.selected_top_rounded_corners else R.drawable.top_rounded_corners
                    position == list.size - 1 -> if (isItemSelected) R.drawable.selected_bottom_rounded_corners else R.drawable.bottom_rounded_corners
                    else -> if (isItemSelected) R.drawable.selected_sharp_corners else R.drawable.sharp_corners
                }
                binding.root.setBackgroundResource(backgroundDrawableRes)
                binding.icPinImageView.visibility = View.GONE
                holder.binding.root.setOnClickListener {
                    val currentClickTime = System.currentTimeMillis()
                    val elapsedTime = currentClickTime - holder.lastClickTime
                    holder.lastClickTime = currentClickTime
                    if (elapsedTime <= 300) {
                        listener?.onEditItemClicked(list[position], position)
                    }
                }
            }
        }
    }
    fun toggleSelection(position: Int) {
        list[position].isSelected = !list[position].isSelected
        notifyItemChanged(position)
    }
    @SuppressLint("NotifyDataSetChanged")
    fun toggleSelectionMode() {
        isSelectionMode = !isSelectionMode
        if (!isSelectionMode) {
            list.forEach { it.isSelected = false }
        }
        notifyDataSetChanged()
    }
    override fun getItemCount(): Int {
        return list.size
    }
    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<ToDoData>) {
        val oldItemsIterator = list.iterator()
        while (oldItemsIterator.hasNext()) {
            val oldItem = oldItemsIterator.next()
            if (newList.none { it.taskId == oldItem.taskId }) {
                oldItemsIterator.remove()
            }
        }
        for (newItem in newList) {
            val existingItemIndex = list.indexOfFirst { it.taskId == newItem.taskId }
            if (existingItemIndex == -1) {
                list.add(newItem)
            } else {
                list[existingItemIndex] = newItem
            }
        }
        list.forEach { it.isSelected = false }
        notifyDataSetChanged()
    }
    @SuppressLint("NotifyDataSetChanged")
    fun updateList_temp(newList: List<ToDoData>) {
        list = newList.toMutableList()
        list.forEach { it.isSelected = false }
        notifyDataSetChanged()
    }
    @SuppressLint("NotifyDataSetChanged")
    fun deleteSelectedItems() {
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.isSelected) {
                listener?.onDeleteItemClicked(item, list.indexOf(item))
                iterator.remove()
            }
        }
        notifyDataSetChanged()
    }
    interface TaskAdapterInterface {
        fun onDeleteItemClicked(toDoData: ToDoData, position: Int)
        fun onEditItemClicked(toDoData: ToDoData, position: Int)
    }
    fun removeItem(position: Int): ToDoData {
        val item = list[position]
        list.removeAt(position)
        notifyItemRemoved(position)
        listener?.onDeleteItemClicked(item, position)
        return item
    }
    fun localRemoveItem(position: Int): ToDoData {
        val item = list[position]
        list.removeAt(position)
        return item
    }
    fun addItem(item: ToDoData) {
        val existingItemIndex = list.indexOfFirst { it.taskId == item.taskId }
        if (existingItemIndex == -1) {
            list.add(item)
        } else {
            list[existingItemIndex] = item
        }
        notifyDataSetChanged()
    }
}
