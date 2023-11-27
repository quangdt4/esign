package com.example.esign.pds.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.example.esign.pds.PDSFragment
import com.example.esign.pdf.PDSPDFDocument

class PDSPageAdapter(fragmentManager: FragmentManager?, fASDocument: PDSPDFDocument) :
    FragmentStatePagerAdapter(
        fragmentManager!!
    ) {
    private val mDocument: PDSPDFDocument

    init {
        mDocument = fASDocument
    }

    override fun getCount(): Int {
        return mDocument.numPages
    }

    override fun getItem(i: Int): Fragment {
        return PDSFragment.newInstance(i)
    }
}