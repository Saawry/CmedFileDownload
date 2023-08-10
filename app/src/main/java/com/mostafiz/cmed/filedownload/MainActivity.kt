package com.mostafiz.cmed.filedownload

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mostafiz.cmed.filedownload.Constants.Companion.CMED_SP
import com.mostafiz.cmed.filedownload.Constants.Companion.Current_Id
import com.mostafiz.cmed.filedownload.Constants.Companion.Total_File_Size
import com.mostafiz.cmed.filedownload.Constants.Companion.URl
import com.mostafiz.cmed.filedownload.databinding.ActivityMainBinding
import com.mostafiz.cmed.filedownload.databinding.DialogAskPermissionBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    var downloadId = -1L
    var bytesTotal = 0L

    private var _progress = MutableLiveData<Int>()
    val progress: LiveData<Int> = _progress



    private lateinit var sharedPref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor


    lateinit var alertDialog: AlertDialog
    private var firstLaunch = 0
    private var storagePermissionAllowed = true
    var PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private val requestPermissionsLauncherStorage =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
            this::onPermissionGranted
        )

    lateinit var downloadManager: DownloadManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        _progress.postValue(0)



        downloadManager = this.getSystemService(DownloadManager::class.java)

        sharedPref = getSharedPreferences(CMED_SP, MODE_PRIVATE)
        editor = sharedPref.edit()

        downloadId = sharedPref.getLong(Current_Id, -1L)

        if (downloadId != -1L) {
            binding.downloadButton.isEnabled = false
            bytesTotal = sharedPref.getLong(Total_File_Size, 0L)
            getCurrentProgress()
        }

        initViews()
        permissionCheck()

        progress.observe(this) {


            if (it >= 100) {
                runOnUiThread {
                    binding.downloadButton.isEnabled = true
                    binding.statusTv.visibility = View.VISIBLE
                    binding.statusTv.text = "Download Complete"
                    clearCache()
                    _progress.postValue(0)
                }
            } else if (it < 1) {
                runOnUiThread {
                    binding.statusTv.visibility = View.INVISIBLE
                }
            } else {
                runOnUiThread {
                    binding.statusTv.visibility = View.VISIBLE
                    binding.statusTv.text = "Downloading"
                }
            }
        }

    }

    private fun initViews() {

        binding.linkTv.text = URl

        binding.downloadButton.setOnClickListener {
            binding.downloadButton.isEnabled = false
            startDownload()
        }
    }


    private fun startDownload() {
        getContentLength(URl)

        downloadId = downloadFile(URl)
        editor.putLong(Current_Id, downloadId).apply()
    }



    private fun getContentLength(url: String) {

        val outerThread = Thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()

                val response: Response = client.newCall(request).execute()

                bytesTotal = response.body?.contentLength() ?: 0

                editor.putLong(Total_File_Size, bytesTotal).apply()
                getCurrentProgress()

            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        outerThread.start()
    }





    private fun getCurrentProgress() {
        val innerThread = Thread {
            while (progress.value!! < 100) {
                getProgress()
                try {
                    Thread.sleep(500)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
        innerThread.start()
    }



    @SuppressLint("Range")
    private fun getProgress() {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        if (cursor.moveToFirst()) {
            val bytesDownloaded =
                cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))

            _progress.postValue((bytesDownloaded * 100 / bytesTotal).toInt())
            runOnUiThread {
                binding.progressTv.text = progress.value.toString() + "/100"
                binding.downloadedTotalTv.text = "Downloaded (kb): " + bytesDownloaded.toString()
                binding.simpleProgressBar.progress = progress.value!!
            }
        }
        cursor.close()
    }


    private fun downloadFile(url: String): Long {
        val request = DownloadManager.Request(url.toUri())
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .addRequestHeader("Authorization", "Bearer <token>")
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "CMED_demo_video.mp4"
            )
        return downloadManager.enqueue(request)
    }


    private fun clearCache() {
        editor.clear().apply()
    }

    private fun askPermission() {
        val dialogBuilder = AlertDialog.Builder(this)
        val bindingx: DialogAskPermissionBinding =
            DialogAskPermissionBinding.inflate(LayoutInflater.from(this), null, false)
        dialogBuilder.setView(bindingx.root)

        bindingx.btnCancel.setOnClickListener { alertDialog.dismiss() }

        bindingx.btnOk.setOnClickListener {
            alertDialog.dismiss()
            requestPermissionsLauncherStorage.launch(PERMISSIONS)
        }

        alertDialog = dialogBuilder.create()
        alertDialog.show()
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    }


    private fun permissionCheck() {
        if (hasPermissions(PERMISSIONS) == 0) {
            askPermission()
        }
    }


    private fun onPermissionGranted(grantStates: Map<String, Boolean>) {
        for ((key, value) in grantStates) {
            Log.d("permission", "$key - $value")
            if (!value) {
                storagePermissionAllowed = false
                break
            }
        }
        firstLaunch = 1
        editor.putInt("firstLaunch", firstLaunch)
        editor.apply()
        if (!storagePermissionAllowed) {
            checkRational(PERMISSIONS)
        }
    }


    private fun hasPermissions(permissions: Array<String>): Int {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return 0
            }
        }
        return 1
    }


    private fun checkRational(PERMISSIONS: Array<String>) {
        var f = true
        firstLaunch = sharedPref.getInt("firstLaunch", 0)
        if (firstLaunch == 1) {
            for (permission in PERMISSIONS) {
                if (shouldShowRequestPermissionRationale(permission)) {
                    f = false
                    break
                }
            }
        }
        if (f && firstLaunch == 1) {
            showInContextUI()
        }
    }


    private fun showInContextUI() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.storage_permission))
            .setMessage(getString(R.string.storage_permission_disclosure))
            .setNeutralButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(getString(R.string.settings)) { _, _ -> toSettings() }
            .create().show()
    }


    private fun toSettings() {
        val intent = Intent()
        val uri: Uri = Uri.fromParts("package", this.packageName, null)
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).data = uri
        startActivity(intent)
    }

}