package com.mostafiz.cmed.filedownload

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import com.mostafiz.cmed.filedownload.Constants.Companion.CMED_SP


class DownloadCompletedReceiver : BroadcastReceiver() {
    private lateinit var sharedPref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.intent.action.DOWNLOAD_COMPLETE") {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id != -1L) {
                sharedPref = context!!.getSharedPreferences(CMED_SP, MODE_PRIVATE)
                editor = sharedPref.edit()
                editor.clear().apply()
            }
        }
    }
}