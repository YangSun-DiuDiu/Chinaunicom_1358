package com.ruoyi.board

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences(BoardApplication.PREFS_NAME, Context.MODE_PRIVATE)
            val roomId = prefs.getString(BoardApplication.KEY_ROOM_ID, null)
            val launchIntent = if (roomId.isNullOrEmpty()) {
                Intent(context, SetupActivity::class.java)
            } else {
                Intent(context, KioskActivity::class.java)
            }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        }
    }
}
