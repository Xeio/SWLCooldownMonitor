package com.xeio.swlcooldowns.pushservices

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.microsoft.windowsazure.notifications.NotificationsHandler
import com.xeio.swlcooldowns.GetCooldownsJob

class CooldownPushNotificationHandler : NotificationsHandler() {
    override fun onReceive(context: Context, bundle: Bundle) {
        val character = bundle.getString("character", "")
        val messageType = bundle.getString("messageType", "")

        Log.i("CooldownPushNotificationHandler", "Push notification received. Character: ${character} MessageType: ${messageType}")

        GetCooldownsJob.createJob(character)
    }
}