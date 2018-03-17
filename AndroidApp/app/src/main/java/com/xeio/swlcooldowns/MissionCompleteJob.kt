package com.xeio.swlcooldowns

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.evernote.android.job.Job
import android.app.NotificationChannel
import android.app.NotificationManager
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
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
                ensureNotificationChannelExists(context)
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

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            var agentBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_VIBRATE)
                    .setSmallIcon(R.drawable.agent_notifiction_icon)
                    .setContentIntent(activityIntent)
                    .setContentTitle(context.getString(R.string.mission_complete).format(character))
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setContentText(text)
                    .setExtras(bundleOf(Pair("textContent", text)))
                    .setSound(Uri.parse(prefs.getString("pref_notification_sound", "")))
                    .setAutoCancel(true)

            notificationManager.notify( cooldown.character.hashCode(), agentBuilder.build())

            cooldown.notified = true
            CooldownData.instance.scheduleNextNotification(context)
        }

        return Result.SUCCESS
    }

    companion object {
        val TAG = "mission_complete_job"
        val NOTIFICATION_CHANNEL_ID = "Mission Completions"

        @RequiresApi(Build.VERSION_CODES.O)
        fun ensureNotificationChannelExists(context: Context){
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            if(notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                val name = context.getString(R.string.mission_complete_channel)
                val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)
                channel.description = context.getString(R.string.mission_complete_channel_desc)
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun areNotificationsEnabled(context: Context) : Boolean
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                return isNotificationChannelEnabled(context)
            }
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.getBoolean("pref_show_notifications", true)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun isNotificationChannelEnabled(context: Context) : Boolean
        {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val channel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
            return channel.importance != NotificationManager.IMPORTANCE_NONE
        }
    }
}