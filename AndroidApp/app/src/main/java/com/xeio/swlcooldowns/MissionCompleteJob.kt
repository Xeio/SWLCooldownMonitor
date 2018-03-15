package com.xeio.swlcooldowns

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.evernote.android.job.Job
import android.support.v4.app.NotificationManagerCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.support.annotation.RequiresApi
import org.jetbrains.anko.bundleOf

class MissionCompleteJob : Job() {
    override fun onRunJob(params: Params): Result {
        Log.i("NotificationAlarmReceiver", "Mission complete notification.")

        val agent = params.extras.getString("agent", "")
        val character = params.extras.getString("character", "")

        val cooldown = CooldownData.instance.cooldowns.firstOrNull { it.agent == agent && it.character == character}

        if(cooldown != null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(context)
            }

            val displayIntent = Intent(context, CooldownsDisplay::class.java)
            val activityIntent = PendingIntent.getActivity(context, 0, displayIntent, 0)

            val notificationManager = context.getSystemService(NotificationManager::class.java)

            val notificationId = character.hashCode()
            var text = context.getString(R.string.agent_has_completed_a_mission).format(cooldown.agent)

            val currentNotification = notificationManager.activeNotifications.firstOrNull { it.id == notificationId}
            if(currentNotification != null)
            {
                val currentText = currentNotification.notification.extras.getString("textContent")
                if(!currentText.contains(character)) {
                    text = currentText + "\n" + text
                }
            }

            var agentBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.agent_notifiction_icon)
                    .setContentIntent(activityIntent)
                    .setContentTitle(context.getString(R.string.mission_complete).format(character))
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setContentText(text)
                    .setExtras(bundleOf(Pair("textContent", text)))
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setAutoCancel(true)

            notificationManager.notify( cooldown.character.hashCode(), agentBuilder.build())

            cooldown.notified = true
            CooldownData.instance.scheduleNextNotification(context)
        }

        return Result.SUCCESS
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(context: Context){
        val name = context.getString(R.string.mission_complete_channel)
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = context.getString(R.string.mission_complete_channel_desc)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        val TAG = "mission_complete_job"
        val NOTIFICATION_CHANNEL_ID = "Mission Completions"
    }
}