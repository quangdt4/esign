package com.example.esign

import android.content.Context
import android.graphics.Color
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.esign.pki.SaveAsPDFWithCoroutine
import com.example.esign.pki.SaveAsPDFWithCoroutine.Companion.KEY
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date

class MainAdapter(private val ctx: Context, var items: List<File>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
    private val selected_items: SparseBooleanArray
    private var current_selected_idx = -1
    private var mOnItemClickListener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(view: View?, value: File?, position: Int)
        fun onItemLongClick(view: View?, obj: File?, pos: Int)
    }

    fun setOnItemClickListener(mItemClickListener: OnItemClickListener?) {
        mOnItemClickListener = mItemClickListener
    }

    init {
        selected_items = SparseBooleanArray()
    }

    inner class OriginalViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var image: ImageView
        var name: TextView
        var brief: TextView
        var size: TextView
        var lyt_parent: View
        var signed: ImageView

        init {
            image = v.findViewById(R.id.fileImageView)
            name = v.findViewById(R.id.fileItemTextview)
            brief = v.findViewById(R.id.dateItemTimeTextView)
            size = v.findViewById(R.id.sizeItemTimeTextView)
            lyt_parent = v.findViewById(R.id.listItemLinearLayout)
            signed = v.findViewById(R.id.ivSigned)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val vh: RecyclerView.ViewHolder
        val v: View =
            LayoutInflater.from(parent.context).inflate(R.layout.mainitemgrid, parent, false)
        vh = OriginalViewHolder(v)
        return vh
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val obj = items[position]
        if (holder is OriginalViewHolder) {
            val view = holder as OriginalViewHolder
            view.name.text = obj.name
            val lastModDate = Date(obj.lastModified())
            val formatter = SimpleDateFormat("dd-MM-yyyy hh:mm a")
            val strDate = formatter.format(lastModDate)
            view.brief.text = strDate
            view.size.text = GetSize(obj.length())
            view.lyt_parent.setOnClickListener(View.OnClickListener { v ->
                if (mOnItemClickListener == null) return@OnClickListener
                mOnItemClickListener!!.onItemClick(v, obj, position)
            })
            view.lyt_parent.setOnLongClickListener(object : View.OnLongClickListener {
                override fun onLongClick(v: View): Boolean {
                    if (mOnItemClickListener == null) return false
                    mOnItemClickListener!!.onItemLongClick(v, obj, position)
                    return true
                }
            })
            toggleCheckedIcon(holder, position)
            view.image.setImageResource(R.drawable.ic_adobe)
            if (!obj.canWrite()) {
                view.signed.visibility = View.VISIBLE
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    private fun toggleCheckedIcon(holder: RecyclerView.ViewHolder, position: Int) {
        val view = holder as OriginalViewHolder
        if (selected_items.get(position, false)) {
            view.lyt_parent.setBackgroundColor(Color.parseColor("#4A32740A"))
            if (current_selected_idx == position) resetCurrentIndex()
        } else {
            view.lyt_parent.setBackgroundColor(Color.parseColor("#ffffff"))
            if (current_selected_idx == position) resetCurrentIndex()
        }
    }

    fun GetSize(size: Long): String {
        val dictionary = arrayOf("bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")
        var index = 0
        var m = size.toDouble()
        val dec = DecimalFormat("0.00")
        index = 0
        while (index < dictionary.size) {
            if (m < 1024) {
                break
            }
            m = m / 1024
            index++
        }
        return dec.format(m) + " " + dictionary[index]
    }

    private fun resetCurrentIndex() {
        current_selected_idx = -1
    }
}