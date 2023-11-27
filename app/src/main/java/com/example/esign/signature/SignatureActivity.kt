package com.example.esign.signature

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.esign.signature.adapter.SignatureAdapter
import com.example.esign.R
import com.example.esign.utils.RecyclerViewEmptySupport
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.util.Arrays

class SignatureActivity : AppCompatActivity() {
    private var mRecyclerView: RecyclerViewEmptySupport? = null
    var items: MutableList<File>? = null
    var message: String? = null
    private var mAdapter: SignatureAdapter? = null
    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signature)
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)
        val fab: FloatingActionButton = findViewById<FloatingActionButton>(R.id.create_signature)
        fab.setOnClickListener(View.OnClickListener {
            val intent = Intent(getApplicationContext(), FreeHandActivity::class.java)
            startActivityForResult(intent, FREEHAND_Request_CODE)
        })
        InitRecycleViewer()
        val intent: Intent = getIntent()
        message = intent.getStringExtra("ActivityAction")
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun InitRecycleViewer() {
        mRecyclerView = findViewById(R.id.mainRecycleView)
        mRecyclerView!!.setEmptyView(findViewById(R.id.toDoEmptyView))
        mRecyclerView!!.setHasFixedSize(true)
        mRecyclerView!!.setItemAnimator(DefaultItemAnimator())
        mRecyclerView!!.setLayoutManager(LinearLayoutManager(this))
        CreateDataSource()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        super.onActivityResult(requestCode, resultCode, result)
        if (requestCode == FREEHAND_Request_CODE && resultCode == RESULT_OK) {
            if (result != null) {
                CreateDataSource()
                mAdapter!!.notifyItemInserted(items!!.size - 1)
            }
        }
    }

    private fun CreateDataSource() {
        items = ArrayList()
        val root: File = getFilesDir()
        val myDir = File("$root/FreeHand")
        if (!myDir.exists()) {
            myDir.mkdirs()
        }
        val files = myDir.listFiles()
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
        for (i in files.indices) {
            items!!.add(files[i])
        }

        //set data and list adapter
        mAdapter = SignatureAdapter(items!!)
        mAdapter!!.setOnItemClickListener(object : SignatureAdapter.OnItemClickListener {
            override fun onItemClick(view: View?, obj: File?, position: Int) {
                if (message == null) {
                    val resultIntent = Intent()
                    resultIntent.putExtra("FileName", obj!!.path)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            }

            override fun onDeleteItemClick(view: View?, obj: File?, pos: Int) {
                val dialog: AlertDialog
                val builder = AlertDialog.Builder(this@SignatureActivity)
                builder.setMessage("Are you sure you want to delete this Signature?")
                    .setPositiveButton("Delete", object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, id: Int) {
                            if (obj!!.exists()) {
                                obj.delete()
                            }
                            CreateDataSource()
                            mAdapter!!.notifyItemInserted(items!!.size - 1)
                        }
                    })
                    .setNegativeButton("Cancel", object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, id: Int) {
                            dialog.dismiss()
                        }
                    })
                dialog = builder.create()
                dialog.show()
            }
        })
        mRecyclerView!!.setAdapter(mAdapter)
    }

    companion object {
        private const val FREEHAND_Request_CODE = 43
    }
}