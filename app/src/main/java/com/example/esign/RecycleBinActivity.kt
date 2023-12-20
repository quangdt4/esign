package com.example.esign

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.esign.adapter.MainAdapter
import com.example.esign.utils.CommonUtils.showToast
import com.example.esign.utils.RecyclerViewEmptySupport
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File
import java.util.*

class RecycleBinActivity : AppCompatActivity() {

    private var recyclerView: RecyclerViewEmptySupport? = null
    private var mAdapter: MainAdapter? = null
    private var mBottomSheetDialog: BottomSheetDialog? = null
    private var items: MutableList<File>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_recycle_bin)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setUp()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setUp() {
        recyclerView = findViewById(R.id.mainRecycleView)
        recyclerView!!.setEmptyView(findViewById(R.id.toDoEmptyView))
        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.setHasFixedSize(true)
        createDataSource()
    }

    private fun createDataSource() {
        items = ArrayList()
        val root: File = filesDir
        val myDir = File("$root/DigitalSignatureBin")
        if (!myDir.exists()) {
            myDir.mkdirs()
        }
        val files = myDir.listFiles()
        if (files != null) {
            Arrays.sort(files) { file1, file2 ->
                val result = file2.lastModified() - file1.lastModified()
                if (result < 0) {
                    -1
                } else if (result > 0) {
                    1
                } else {
                    0
                }
            }
        }
        if (files != null) {
            for (i in files.indices) {
                items!!.add(files[i])
            }
        }

        //set data and list adapter
        mAdapter = MainAdapter(this, items!!)
        mAdapter!!.setOnItemClickListener(object : MainAdapter.OnItemClickListener {
            override fun onItemClick(view: View?, value: File?, position: Int) {
                showBottomSheetDialog(value!!)
            }

            override fun onItemLongClick(view: View?, obj: File?, pos: Int) {
                showBottomSheetDialog(obj!!)
            }
        })
        recyclerView!!.adapter = mAdapter
    }

    private fun showBottomSheetDialog(currentFile: File) {
        val view = layoutInflater.inflate(R.layout.sheet_recycle_bin_list, null)
        mBottomSheetDialog = BottomSheetDialog(this)
        mBottomSheetDialog!!.setContentView(view)
        mBottomSheetDialog!!.setupBottomSheetDialog(view, currentFile)
        mBottomSheetDialog!!.show()
        mBottomSheetDialog!!.setOnDismissListener { mBottomSheetDialog = null }
    }

    private fun BottomSheetDialog.setupBottomSheetDialog(view: View, currentFile: File) {
        view.findViewById<View>(R.id.llGetBack).setOnClickListener {
            dismiss()
            getBack(currentFile)
            showToast("Got it back", view.context)
        }
        view.findViewById<View>(R.id.llDelete).setOnClickListener {
            dismiss()
            showCustomDeleteDialog(currentFile)
        }
    }

    private fun getBack(currentFile: File) {
        try {
            val root = filesDir
            val destinationDir = File("$root/DigitalSignature")
            val destinationFile = File(destinationDir, currentFile.name)
            if (destinationFile.exists()) {
                val timestamp = System.currentTimeMillis()
                destinationFile.renameTo(File(destinationDir, "${currentFile.name}_$timestamp"))
            }

            currentFile.copyTo(destinationFile)
            currentFile.delete()
            createDataSource()
            mAdapter!!.notifyItemInserted(items!!.size - 1)

            showToast("Your file is moved to the recycle bin", this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showCustomDeleteDialog(currentFile: File) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Are you sure want to delete this file?")
        builder.setPositiveButton("OK") { _, _ ->
            currentFile.delete()
            createDataSource()
            mAdapter!!.notifyItemInserted(items!!.size - 1)
        }
        builder.setNegativeButton(
            "Cancel"
        ) { _, _ -> }
        val dialog = builder.create()
        dialog.show()
    }
}