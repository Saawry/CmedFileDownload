package com.mostafiz.cmed.filedownload

import android.Manifest
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.mostafiz.cmed.filedownload.Constants.Companion.CMED_SP
import com.mostafiz.cmed.filedownload.Constants.Companion.URl
import com.mostafiz.cmed.filedownload.databinding.ActivityMainBinding
import com.mostafiz.cmed.filedownload.databinding.DialogAskPermissionBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding



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


        downloadManager = this.getSystemService(DownloadManager::class.java)

        sharedPref = getSharedPreferences(CMED_SP, MODE_PRIVATE)
        editor = sharedPref.edit()


        initViews()
        permissionCheck()

    }

    private fun initViews() {

        binding.linkTv.text = URl

        binding.downloadButton.setOnClickListener {
            binding.downloadButton.isEnabled = false
            downloadFile(URl)
        }
    }




    fun downloadFile(url: String) {
        val request = DownloadManager.Request(url.toUri())
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .addRequestHeader("Authorization", "Bearer <token>")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "CMED_demo_video.mp4")
         downloadManager.enqueue(request)
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