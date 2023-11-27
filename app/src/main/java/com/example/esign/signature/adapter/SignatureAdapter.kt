package com.example.esign.signature.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.benzveen.pdfdigitalsignature.Signature.SignatureView
import com.example.esign.R
import com.example.esign.utils.PDSSignatureUtils
import java.io.File

class SignatureAdapter(private val signatures: List<File>) :
    RecyclerView.Adapter<SignatureAdapter.MyViewHolder?>() {
    private var onClickListener: OnItemClickListener? = null
    fun setOnItemClickListener(onClickListener: OnItemClickListener?) {
        this.onClickListener = onClickListener
    }

    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        i: Int
    ): MyViewHolder {
        val v: View = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_signature, viewGroup, false)
        return MyViewHolder(viewGroup.context, v)
    }

    override fun onBindViewHolder(myViewHolder: MyViewHolder, i: Int) {
        var signatureView: SignatureView = myViewHolder.layout.getChildAt(0) as SignatureView
        if (signatureView != null) {
            myViewHolder.layout.removeViewAt(0)
        }
        signatureView = PDSSignatureUtils.showFreeHandView(myViewHolder.context, signatures[i])

        myViewHolder.title.text = "Template ${i + 1}"

        myViewHolder.layout.addView(signatureView)
        myViewHolder.layout.setOnClickListener(View.OnClickListener { v ->
            if (onClickListener == null) return@OnClickListener
            onClickListener!!.onItemClick(v, signatures[i], myViewHolder.adapterPosition)
        })

        signatureView.setOnClickListener(View.OnClickListener { v ->
            if (onClickListener == null) return@OnClickListener
            onClickListener!!.onItemClick(v, signatures[i], myViewHolder.adapterPosition)
        })

        myViewHolder.deleteSignature.setOnClickListener(View.OnClickListener { v ->
            if (onClickListener == null) return@OnClickListener
            onClickListener!!.onDeleteItemClick(v, signatures[i], myViewHolder.adapterPosition)
        })
    }

    override fun getItemCount(): Int {
        return signatures.size
    }

    inner class MyViewHolder(var context: Context, v: View) : RecyclerView.ViewHolder(v) {
        var layout: FrameLayout
        var deleteSignature: ImageButton
        var signatureView: SignatureView
        var title: TextView

        init {
            layout = v.findViewById<FrameLayout>(R.id.freehanditem)
            deleteSignature = v.findViewById<ImageButton>(R.id.deleteSignature)
            signatureView = v.findViewById(R.id.signatureview)
            title = v.findViewById(R.id.tvName)
        }
    }

    interface OnItemClickListener {
        fun onItemClick(view: View?, obj: File?, pos: Int)
        fun onDeleteItemClick(view: View?, obj: File?, pos: Int)
    }
}