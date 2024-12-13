package com.example.to_doapp.fragments
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.to_doapp.R
import com.example.to_doapp.databinding.FragmentHomeBinding
import com.example.to_doapp.utils.adapter.PinnedTaskAdapter
import com.example.to_doapp.utils.adapter.TaskAdapter
import com.example.to_doapp.utils.model.ToDoData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.Collections
import kotlin.math.abs
import kotlin.math.min
class HomeFragment : Fragment(), ToDoDialogFragment.OnDialogNextBtnClickListener,
    TaskAdapter.TaskAdapterInterface, PinnedTaskAdapter.PinnedTaskAdapterInterface {
    private var isSortedByTaskRecent = false
    private var isSortedByTaskPinned = false
    private var isFirstClickPinned = true
    private var isFirstClickRecent = true
    private lateinit var binding: FragmentHomeBinding
    private lateinit var database: DatabaseReference
    private var frag: ToDoDialogFragment? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var authId: String
    private lateinit var taskAdapter: TaskAdapter
    private var toDoItemList: MutableList<ToDoData> = mutableListOf()
    private var pinnedToDoItemList: MutableList<ToDoData> = mutableListOf()
    val pinnedTaskAdapter = PinnedTaskAdapter(mutableListOf())
    private var isSearchViewEnabled = true
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        getTaskFromFirebase()
        //main rec
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                Collections.swap(toDoItemList, fromPosition, toPosition)
                recyclerView.adapter?.notifyItemMoved(fromPosition, toPosition)
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.findViewById<View>(R.id.icMove)?.visibility = View.VISIBLE
                }
            }
            @SuppressLint("NotifyDataSetChanged")
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.findViewById<View>(R.id.icMove)?.visibility = View.GONE
                recyclerView.adapter?.notifyDataSetChanged()
            }
        })
        touchHelper.attachToRecyclerView(binding.mainRecyclerView)
        //pinned rec
        val touchHelperPinned = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                Collections.swap(pinnedToDoItemList, fromPosition, toPosition)
                recyclerView.adapter?.notifyItemMoved(fromPosition, toPosition)
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.findViewById<View>(R.id.icMove)?.visibility = View.VISIBLE
                }
            }
            @SuppressLint("NotifyDataSetChanged")
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.findViewById<View>(R.id.icMove)?.visibility = View.GONE
                (recyclerView.adapter as? PinnedTaskAdapter)?.updateList(pinnedToDoItemList)
            }
        })
        touchHelperPinned.attachToRecyclerView(binding.PinnedRecyclerView)
        binding.addTaskBtnMain.setOnClickListener {
            if (frag != null)
                childFragmentManager.beginTransaction().remove(frag!!).commit()
            frag = ToDoDialogFragment()
            frag!!.setListener(this)
            frag!!.show(
                childFragmentManager,
                ToDoDialogFragment.TAG
            )
        }
        val rootRef = FirebaseDatabase.getInstance().getReference("Tasks")
        binding.Edit.setOnClickListener { toggleEditMode() }
        binding.Done.setOnClickListener { toggleEditMode() }
        binding.searchView.setOnTouchListener { _, _ -> !isSearchViewEnabled }
        binding.searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!isSearchViewEnabled && hasFocus) {
                binding.searchView.clearFocus()
            }
        }
        binding.DeleteAll.setOnClickListener {
            taskAdapter.deleteSelectedItems()
            pinnedTaskAdapter.deleteSelectedItems()
            toggleEditMode()
            Toast.makeText(context, "Items deleted successfully", Toast.LENGTH_SHORT).show()
        }
    }
    private fun toggleEditMode() {
        taskAdapter.toggleSelectionMode()
        pinnedTaskAdapter.toggleSelectionMode()
        isSearchViewEnabled = !isSearchViewEnabled
        binding.searchView.isEnabled = isSearchViewEnabled
        binding.searchView.clearFocus()
        binding.searchView.alpha = if (isSearchViewEnabled) 1.0f else 0.5f
        binding.DeleteAll.visibility = if (binding.DeleteAll.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        binding.noteNum.visibility = if (binding.noteNum.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        binding.addTaskBtnMain.visibility = if (binding.addTaskBtnMain.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        binding.Done.visibility = if (binding.Done.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        binding.Edit.visibility = if (binding.Done.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }
    private fun getTaskFromFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                toDoItemList.clear()
                pinnedToDoItemList.clear()
                for (taskSnapshot in snapshot.children) {
                    val taskId = taskSnapshot.key ?: continue
                    val task = taskSnapshot.child("task").getValue(String::class.java) ?: ""
                    val timestamp = taskSnapshot.child("timestamp").getValue(Long::class.java) ?: continue
                    val isPinned = taskSnapshot.child("isPinned").getValue(Boolean::class.java) ?: false
                    val todoTask = ToDoData(taskId, task, timestamp, isPinned)
                    if (isPinned) {
                        pinnedToDoItemList.add(todoTask)
                    } else {
                        toDoItemList.add(todoTask)
                    }
                    removeAllPinnedTasks()
                    taskAdapter.updateList(toDoItemList)
                    pinnedTaskAdapter.updateList(pinnedToDoItemList)
                }
                taskAdapter.updateList(toDoItemList)
                pinnedTaskAdapter.updateList(pinnedToDoItemList)
                taskAdapter.notifyDataSetChanged()
                pinnedTaskAdapter.notifyDataSetChanged()
                val hasPinnedItem = pinnedToDoItemList.isNotEmpty()
                binding.pinned.visibility = if (hasPinnedItem) View.VISIBLE else View.GONE
                binding.icSortPinned.visibility = if (hasPinnedItem) View.VISIBLE else View.GONE
                binding.AZSortPinned.visibility = if (hasPinnedItem && !isFirstClickPinned) View.VISIBLE else View.GONE
                binding.icTimePinned.visibility = if (binding.AZSortPinned.visibility == View.VISIBLE) View.GONE else if (!isFirstClickPinned && hasPinnedItem) View.VISIBLE else View.GONE
                val hasRecentItem = toDoItemList.isNotEmpty()
                binding.recent.visibility = if (hasRecentItem) View.VISIBLE else View.GONE
                binding.icSortRecent.visibility = if (hasRecentItem) View.VISIBLE else View.GONE
                binding.AZSortRecent.visibility = if (hasRecentItem && !isFirstClickRecent) View.VISIBLE else View.GONE
                binding.icTimeRecent.visibility = if (binding.AZSortRecent.visibility == View.VISIBLE) View.GONE else if (!isFirstClickRecent && hasRecentItem) View.VISIBLE else View.GONE
                binding.view1.visibility = View.GONE
                binding.view2.visibility = View.GONE
                binding.view3.visibility = View.GONE
                binding.view4.visibility = View.GONE
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun init() {
        val sortIcon_recent = binding.icSortRecent
        sortIcon_recent.setOnClickListener {
            if (isSortedByTaskRecent) {
                binding.icTimeRecent.visibility = View.VISIBLE
                binding.AZSortRecent.visibility = View.INVISIBLE
                toDoItemList.sortByDescending { it.timestamp }
                taskAdapter.updateList(toDoItemList)
                Toast.makeText(context, "Sorted by Time", Toast.LENGTH_SHORT).show()
            } else {
                binding.icTimeRecent.visibility = View.INVISIBLE
                binding.AZSortRecent.visibility = View.VISIBLE
                toDoItemList.sortBy { it.task }
                taskAdapter.updateList(toDoItemList)
                Toast.makeText(context, "Sorted A-Z", Toast.LENGTH_SHORT).show()
            }
            isSortedByTaskRecent = !isSortedByTaskRecent
        }
        val sortIcon_pinned = binding.icSortPinned
        sortIcon_pinned.setOnClickListener {
            if (isSortedByTaskPinned) {
                binding.icTimePinned.visibility = View.VISIBLE
                binding.AZSortPinned.visibility = View.INVISIBLE
                pinnedToDoItemList.sortByDescending { it.timestamp }
                pinnedTaskAdapter.updateList(pinnedToDoItemList)
                Toast.makeText(context, "Sorted by Time", Toast.LENGTH_SHORT).show()
            } else {
                binding.icTimePinned.visibility = View.INVISIBLE
                binding.AZSortPinned.visibility = View.VISIBLE
                pinnedToDoItemList.sortBy { it.task }
                pinnedTaskAdapter.updateList(pinnedToDoItemList)
                Toast.makeText(context, "Sorted A-Z", Toast.LENGTH_SHORT).show()
            }
            isSortedByTaskPinned = !isSortedByTaskPinned
        }
        binding.view1.visibility = View.VISIBLE
        val animator1 = ObjectAnimator.ofFloat(binding.view1, "alpha", 1f, 0.3f, 1f)
        animator1.duration = 1300
        animator1.repeatCount = ValueAnimator.INFINITE
        animator1.start()
        binding.view2.visibility = View.VISIBLE
        val animator2 = ObjectAnimator.ofFloat(binding.view2, "alpha", 1f, 0.3f, 1f)
        animator2.duration = 1300
        animator2.repeatCount = ValueAnimator.INFINITE
        animator2.start()
        binding.view3.visibility = View.VISIBLE
        val animator3 = ObjectAnimator.ofFloat(binding.view3, "alpha", 1f, 0.3f, 1f)
        animator3.duration = 1300
        animator3.repeatCount = ValueAnimator.INFINITE
        animator3.start()
        binding.view4.visibility = View.VISIBLE
        val animator4 = ObjectAnimator.ofFloat(binding.view4, "alpha", 1f, 0.3f, 1f)
        animator4.duration = 1300
        animator4.repeatCount = ValueAnimator.INFINITE
        animator4.start()
        auth = FirebaseAuth.getInstance()
        authId = auth.currentUser!!.uid
        database = Firebase.database.reference.child("Tasks")
            .child(authId)
        binding.mainRecyclerView.setHasFixedSize(true)
        binding.mainRecyclerView.layoutManager = LinearLayoutManager(context)
        toDoItemList = mutableListOf()
        pinnedToDoItemList = mutableListOf()
        taskAdapter = TaskAdapter(toDoItemList)
        taskAdapter.setListener(this)
        pinnedTaskAdapter.setListener(this)
        binding.mainRecyclerView.adapter = taskAdapter
        taskAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                updateNoteCount()
            }
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                updateNoteCount()
            }
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                updateNoteCount()
            }
        })
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                filterTasks(newText ?: "")
                return true
            }
        })
        //main
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        taskAdapter.removeItem(position)
                    }
                    ItemTouchHelper.RIGHT -> {
                        val item = taskAdapter.localRemoveItem(position)
                        item.isPinned = true
                        pinnedTaskAdapter.addItem(item)
                        updateTaskInFirebase(item)
                    }
                }
            }
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val itemHeight = itemView.bottom - itemView.top
                val icon: Drawable
                val iconMargin: Int
                val evaluator = ArgbEvaluator()
                val swipeThreshold = 0.9f
                if (dX > 0) {
                    icon = ContextCompat.getDrawable(itemView.context, R.drawable.ic_pin)!!
                    iconMargin = (itemHeight - icon.intrinsicHeight) / 2
                    val iconLeft = itemView.left + iconMargin
                    val iconRight = itemView.left + iconMargin + icon.intrinsicWidth
                    val fraction = abs(dX) / itemView.width
                    val defaultColor = Color.parseColor("#e4e3e9")
                    val finalColor = Color.BLUE
                    val color = evaluator.evaluate(min(fraction / swipeThreshold, 1f), defaultColor, finalColor) as Int
                    icon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
                    icon.setBounds(iconLeft, itemView.top + iconMargin, iconRight, itemView.top + iconMargin + icon.intrinsicHeight)
                } else {
                    icon = ContextCompat.getDrawable(itemView.context, R.drawable.ic_trash)!!
                    iconMargin = (itemHeight - icon.intrinsicHeight) / 2
                    val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    val fraction = abs(dX) / itemView.width
                    val defaultColor = Color.parseColor("#e4e3e9")
                    val finalColor = Color.RED
                    val color = evaluator.evaluate(min(fraction / swipeThreshold, 1f), defaultColor, finalColor) as Int
                    icon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
                    icon.setBounds(iconLeft, itemView.top + iconMargin, iconRight, itemView.top + iconMargin + icon.intrinsicHeight)
                }
                icon.draw(c)
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.mainRecyclerView)
        //pinned
        binding.PinnedRecyclerView.setHasFixedSize(true)
        binding.PinnedRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.PinnedRecyclerView.adapter = pinnedTaskAdapter
        val pinnedItemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        pinnedTaskAdapter.removeItem(position)
                    }
                    ItemTouchHelper.RIGHT -> {
                        val item = pinnedTaskAdapter.localRemoveItem(position)
                        item.isPinned = false
                        taskAdapter.addItem(item)
                        updateTaskInFirebase(item)
                    }
                }
            }
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val itemHeight = itemView.bottom - itemView.top
                val icon: Drawable
                val iconMargin: Int
                val evaluator = ArgbEvaluator()
                val swipeThreshold = 0.9f
                if (dX > 0) {
                    icon = ContextCompat.getDrawable(itemView.context, R.drawable.ic_unpin)!!
                    iconMargin = (itemHeight - icon.intrinsicHeight) / 2
                    val iconLeft = itemView.left + iconMargin
                    val iconRight = itemView.left + iconMargin + icon.intrinsicWidth
                    val fraction = abs(dX) / itemView.width
                    val defaultColor = Color.parseColor("#e4e3e9")
                    val finalColor = Color.BLUE
                    val color = evaluator.evaluate(min(fraction / swipeThreshold, 1f), defaultColor, finalColor) as Int
                    icon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
                    icon.setBounds(iconLeft, itemView.top + iconMargin, iconRight, itemView.top + iconMargin + icon.intrinsicHeight)
                } else {
                    icon = ContextCompat.getDrawable(itemView.context, R.drawable.ic_trash)!!
                    iconMargin = (itemHeight - icon.intrinsicHeight) / 2
                    val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    val fraction = abs(dX) / itemView.width
                    val defaultColor = Color.parseColor("#e4e3e9")
                    val finalColor = Color.RED
                    val color = evaluator.evaluate(min(fraction / swipeThreshold, 1f), defaultColor, finalColor) as Int
                    icon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
                    icon.setBounds(iconLeft, itemView.top + iconMargin, iconRight, itemView.top + iconMargin + icon.intrinsicHeight)
                }
                icon.draw(c)
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        val pinnedItemTouchHelper = ItemTouchHelper(pinnedItemTouchHelperCallback)
        pinnedItemTouchHelper.attachToRecyclerView(binding.PinnedRecyclerView)
    }
    private fun filterTasks(query: String) {
        val filteredList = toDoItemList.filter { it.task.contains(query, ignoreCase = true) }
        taskAdapter.updateList_temp(filteredList)
        val filteredPinnedList = pinnedToDoItemList.filter { it.task.contains(query, ignoreCase = true) }
        pinnedTaskAdapter.updateList(filteredPinnedList)
    }
    private fun updateNoteCount() {
        val itemCount = toDoItemList.size + pinnedToDoItemList.size
        val imageView = binding.root.findViewById<ImageView>(R.id.felt_tip_pen)
        val backImageView = binding.root.findViewById<ImageView>(R.id.felt_tip_pen_back)
        if (itemCount == 0) {
            imageView.visibility = View.VISIBLE
            backImageView.visibility = View.GONE
        } else {
            imageView.visibility = View.GONE
            backImageView.visibility = View.VISIBLE
        }
        binding.noteNum.text = when (itemCount) {
            0 -> "No Notes"
            1 -> "1 Note"
            else -> "$itemCount Notes"
        }
    }
    override fun saveTask(todoTask: String, todoEdit: TextView, isPinned: Boolean) {
        val taskId = database.push().key ?: return
        val newTask = ToDoData(taskId, todoTask, isPinned = false)
        database.child(taskId).setValue(newTask).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(context, "Task Added Successfully", Toast.LENGTH_SHORT).show()
                taskAdapter.addItem(newTask)
                todoEdit.text = null
                updateNoteCount()
            } else {
                Toast.makeText(context, it.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }
        frag!!.dismiss()
    }
    override fun updateTask(toDoData: ToDoData, todoEdit: TextView) {
        val taskRef = database.child(toDoData.taskId)
        taskRef.child("task").setValue(toDoData.task)
        taskRef.child("isPinned").setValue(toDoData.isPinned)
        taskRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    Toast.makeText(context, "Updated Successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to update task in Firebase", Toast.LENGTH_SHORT).show()
                }
                frag!!.dismiss()
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(context, databaseError.message, Toast.LENGTH_SHORT).show()
                frag!!.dismiss()
            }
        })
    }
    private fun removeAllPinnedTasks() {
        val rootRef = FirebaseDatabase.getInstance().getReference("Tasks").child(authId)
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (childSnapshot in dataSnapshot.children) {
                    val objectKey = childSnapshot.key
                    if (objectKey != null && childSnapshot.hasChild("pinned")) {
                        rootRef.child(objectKey).child("pinned").removeValue()
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(context, databaseError.message, Toast.LENGTH_SHORT).show()
            }
        })
    }
    override fun onDeleteItemClicked(toDoData: ToDoData, position: Int) {
        database.child(toDoData.taskId).removeValue().addOnCompleteListener {
            if (it.isSuccessful) {
                updateNoteCount()
            }
        }
    }
    override fun onEditItemClicked(toDoData: ToDoData, position: Int) {

        if (frag != null)
            childFragmentManager.beginTransaction().remove(frag!!).commit()
        frag = ToDoDialogFragment.newInstance(toDoData.taskId, toDoData.task, toDoData.isPinned)
        frag!!.setListener(this)
        frag!!.show(
            childFragmentManager,
            ToDoDialogFragment.TAG
        )
    }
    fun updateTaskInFirebase(toDoData: ToDoData) {
        val taskRef = database.child(toDoData.taskId)
        val taskMap = mapOf(
            "task" to toDoData.task,
            "isPinned" to toDoData.isPinned,
            "timestamp" to toDoData.timestamp
        )
        taskRef.updateChildren(taskMap).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Task updated successfully in Firebase", Toast.LENGTH_SHORT)
                    .show()
                updateNoteCount()
            } else {
                Toast.makeText(context, "Failed to update task in Firebase", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

}