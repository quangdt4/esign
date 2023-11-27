package com.example.esign

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Menu
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.documentfile.provider.DocumentFile
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.esign.signature.SignatureActivity
import com.example.esign.utils.CommonUtils.showToast
import com.example.esign.utils.RecyclerViewEmptySupport
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity() {

    private var recyclerView: RecyclerViewEmptySupport? = null
    private var items: MutableList<File>? = null
    private var mAdapter: MainAdapter? = null
    private var mBottomSheetDialog: BottomSheetDialog? = null
    private var selectedFile: File? = null
    private val mHandler = Handler(Looper.getMainLooper())
    private var fabMenu: FloatingActionMenu? = null

    private val mUpdateTimeTask = Runnable {
        val intent = Intent(applicationContext, SignatureActivity::class.java)
        intent.putExtra("ActivityAction", "Open")
        startActivity(intent)
    }

    private val startSignaturesForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (result != null) {
                createDataSource()
                mAdapter!!.notifyItemInserted(items!!.size - 1)
            }
        }
    }

    private val startDocumentTreeForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (result != null) {
                val uriTree = result.data?.data
                uriTree?.let { copyFileToExternalStorage(it) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUp()

        checkStoragePermission()
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            checkStoragePermission()
        }

        handleExternalData()
    }

    private fun setUp() {
        PDFBoxResourceLoader.init(applicationContext)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val fabDocs = findViewById<FloatingActionButton>(R.id.fabDocs)
        val fabMySign = findViewById<FloatingActionButton>(R.id.fabMySign)
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)

        fabMenu = findViewById(R.id.fabMenu)
        fabMenu?.setOnMenuButtonClickListener {
            if (fabMenu?.isOpened == true) {
                fabMenu?.close(true)
            } else {
                fabMenu!!.open(true)
            }
        }

        fabDocs.setOnClickListener {
            val intent = Intent(applicationContext, DocumentActivity::class.java)
            intent.putExtra("ActivityAction", "FileSearch")
            startSignaturesForResult.launch(intent)
        }

        fabMySign.setOnClickListener {
            mHandler.postDelayed(mUpdateTimeTask, 100)
        }

        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )

        drawer.addDrawerListener(toggle)
        toggle.syncState()

        recyclerView = findViewById(R.id.mainRecycleView)
        recyclerView!!.setEmptyView(findViewById(R.id.toDoEmptyView))
        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.setHasFixedSize(true)
    }

    public override fun onResume() {
        super.onResume()
        fabMenu?.close(true)
        createDataSource()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    private fun copyFileToExternalStorage(uri: Uri) {
        try {
            contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { os ->
                    os.write(
                        selectedFile!!.readBytes()
                    )
                }
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun copy(selectedFile: File, newFile: DocumentFile?) {
        try {
            val out = contentResolver.openOutputStream(newFile!!.uri)
            val inputStream = FileInputStream(selectedFile.path)
            val buffer = ByteArray(1024)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                out!!.write(buffer, 0, read)
            }
            inputStream.close()
            out!!.flush()
            out.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun handleExternalData() {
        val intent = intent
        val action = intent.action
        val type = intent.type
        var imageUri: Uri? = null
        if ((Intent.ACTION_SEND == action || Intent.ACTION_VIEW == action) && type != null) {
            if ("application/pdf" == type) {
                if (Intent.ACTION_SEND == action)
                    imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                else if (Intent.ACTION_VIEW == action)
                    imageUri = intent.data
                if (imageUri != null) {
                    val list = ArrayList<Uri>()
                    list.add(imageUri)
                    startSignatureActivity(list)
                }
            }
        }
    }

    private fun startSignatureActivity(imageUris: ArrayList<Uri>?) {
        val intent = Intent(applicationContext, DocumentActivity::class.java)
        intent.putExtra("ActivityAction", "PDFOpen")
        intent.putExtra("PDFOpen", imageUris)
        startSignaturesForResult.launch(intent)
    }

    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                val alertDialog = AlertDialog.Builder(this).create()
                alertDialog.setTitle("Storage Permission")
                alertDialog.setMessage(
                    "Storage permission is required in order to " + "provide Image to PDF feature, please enable permission in app settings"
                )
                alertDialog.setButton(
                    AlertDialog.BUTTON_NEUTRAL, "Settings"
                ) { dialog, _ ->
                    val i = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:com.example.esign")
                    )
                    startActivity(i)
                    dialog.dismiss()
                }
                alertDialog.show()
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 2
                )
            }
        }
    }

    private fun createDataSource() {
        if (recyclerView != null) {
            items = ArrayList()
            val root = filesDir
            val myDir = File("$root/DigitalSignature")
            if (!myDir.exists()) {
                myDir.mkdirs()
            }
            val files = myDir.listFiles()
            if (files != null) {
                Arrays.sort(files) { o1, o2 ->
                    val result = o2!!.lastModified() - o1!!.lastModified()
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
    }


    /**
     * ------------- Handle bottom sheet -------------
     */
    private fun showBottomSheetDialog(currentFile: File) {
        val view = layoutInflater.inflate(R.layout.sheet_list, null)
        mBottomSheetDialog = BottomSheetDialog(this)
        mBottomSheetDialog!!.setContentView(view)
        mBottomSheetDialog!!.setupBottomSheetDialog(view, currentFile)
        mBottomSheetDialog!!.show()
        mBottomSheetDialog!!.setOnDismissListener { mBottomSheetDialog = null }
    }

    private fun BottomSheetDialog.setupBottomSheetDialog(view: View, currentFile: File) {
        view.findViewById<View>(R.id.lyt_email).setOnClickListener {
            dismiss()
            share(currentFile)
        }
        view.findViewById<View>(R.id.lyt_rename).setOnClickListener {
            dismiss()
            showCustomRenameDialog(currentFile)
        }
        view.findViewById<View>(R.id.lyt_delete).setOnClickListener {
            dismiss()
            showCustomDeleteDialog(currentFile)
        }
        view.findViewById<View>(R.id.lyt_copyTo).setOnClickListener {
            dismiss()
            copyTo(currentFile)
        }
        view.findViewById<View>(R.id.lyt_openFile).setOnClickListener {
            dismiss()
            openFile(currentFile)
        }
    }

    private fun openFile(currentFile: File) {
        val target = Intent(Intent.ACTION_VIEW)
        val contentUri = FileProvider.getUriForFile(
            applicationContext, applicationContext.packageName + ".provider", currentFile
        )
        target.setDataAndType(contentUri, "application/pdf")
        target.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
        target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val intent = Intent.createChooser(target, "Open File")
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showToast("Open file fail", this)
        }
    }

    private fun share(currentFile: File) {
        val contentUri = FileProvider.getUriForFile(
            applicationContext, applicationContext.packageName + ".provider", currentFile
        )
        val target = Intent(Intent.ACTION_SEND)
        target.type = "text/plain"
        target.putExtra(Intent.EXTRA_STREAM, contentUri)
        target.putExtra(Intent.EXTRA_SUBJECT, "Subject")

        if (target.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(target, "Choose an app"))
        } else {
            showToast("No support available", applicationContext)
        }
    }

    private fun copyTo(currentFile: File) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, ".pdf")
        }
        startDocumentTreeForResult.launch(intent)
        selectedFile = currentFile
    }

    private fun showCustomRenameDialog(currentFile: File) {
        val builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val view = inflater.inflate(R.layout.rename_layout, null)
        builder.setView(view)
        val editText = view.findViewById<View>(R.id.renameEditText2) as EditText
        editText.setText(currentFile.name)
        builder.setTitle("Rename")
        builder.setPositiveButton(
            "Rename"
        ) { dialog, _ ->
            val root = filesDir
            val file = File("$root/DigitalSignature", editText.text.toString())
            currentFile.renameTo(file)
            dialog.dismiss()
            createDataSource()
            mAdapter!!.notifyItemInserted(items!!.size - 1)
        }
        builder.setNegativeButton(
            "Cancel"
        ) { _, _ -> }
        val dialog = builder.create()
        dialog.show()
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