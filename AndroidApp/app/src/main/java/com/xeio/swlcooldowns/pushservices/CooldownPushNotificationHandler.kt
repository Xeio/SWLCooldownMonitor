package com.xeio.swlcooldowns.pushservices

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.microsoft.windowsazure.notifications.NotificationsHandler
import com.xeio.swlcooldowns.CooldownData

class CooldownPushNotificationHandler : NotificationsHandler() {
    override fun onReceive(context: Context, bundle: Bundle) {
        Log.i("CooldownPushNotificationHandler", "Push notification received.")

        CooldownData.instance.updateCooldowns(context)
    }
}