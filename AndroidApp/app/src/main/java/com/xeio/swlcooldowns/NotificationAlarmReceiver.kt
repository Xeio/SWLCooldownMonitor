package com.xeio.swlcooldowns

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.app.PendingIntent
import android.media.RingtoneManager
import android.util.Log

class NotificationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("NotificationAlarmReceiver", "Mission complete notification.")

        val agentId = intent.getIntExtra("agentId", 999)

        val cooldown = CooldownData.instance.cooldowns.first { it.agentId == agentId }

        if(cooldown != null) {
            val myIntent = Intent(context, CooldownsDisplay::class.java)
            val activityIntent = PendingIntent.getActivity(context, 0, myIntent, 0)

            val groupBuilder = NotificationCompat.Builder(context, "5555")
                    .setSmallIcon(R.drawable.notification_template_icon_bg)
                    .setGroup("MISSION_COMPLETE")
                    .setAutoCancel(true)
                    .setGroupSummary(true)

            val agentBuilder = NotificationCompat.Builder(context, "5555")
                    .setSmallIcon(R.drawable.notification_template_icon_bg)
                    .setContentIntent(activityIntent)
                    .setContentTitle(context.getString(R.string.mission_complete))
                    .setContentText("${cooldown.agent} has completed a mission.")
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setAutoCancel(true)
                    .setGroup("MISSION_COMPLETE")

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.notify(0, groupBuilder.build())
            notificationManager.notify(cooldown.agentId, agentBuilder.build())

            cooldown.notified = true

            CooldownData.instance.scheduleNotifications(context)
        }
    }
}
