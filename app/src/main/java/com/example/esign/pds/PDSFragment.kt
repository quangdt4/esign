package com.example.esign.pds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.example.esign.DocumentActivity
import com.example.esign.R
import com.example.esign.pds.page.PDSPageViewer

class PDSFragment : Fragment() {
    var mPageViewer: PDSPageViewer? = null
    override fun onCreateView(
        layoutInflater: LayoutInflater,
        viewGroup: ViewGroup?,
        bundle: Bundle?
    ): View? {
        val inflate: View = layoutInflater.inflate(R.layout.fragment_layout, viewGroup, false)
        val linearLayout: LinearLayout = inflate.findViewById<View>(R.id.fragment) as LinearLayout
        try {
            val fASPageViewer = PDSPageViewer(
                viewGroup!!.context,
                activity as DocumentActivity?,
                (activity as DocumentActivity?)!!.document!!.getPage(
                    requireArguments().getInt("pageNum")
                )!!
            )
            mPageViewer = fASPageViewer
            linearLayout.addView(fASPageViewer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return inflate
    }

    override fun onDestroyView() {
        if (mPageViewer != null) {
            mPageViewer!!.cancelRendering()
            mPageViewer = null
        }
        super.onDestroyView()
    }

    companion object {
        fun newInstance(i: Int): PDSFragment {
            val fASFragment = PDSFragment()
            val bundle = Bundle()
            bundle.putInt("pageNum", i)
            fASFragment.arguments = bundle
            return fASFragment
        }
    }
}