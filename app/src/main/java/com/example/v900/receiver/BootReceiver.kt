package com.example.v900.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.v900.service.ForegroundCommService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootReceiver", "Boot completed, starting ForegroundCommService")
            val serviceIntent = Intent(context, ForegroundCommService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
