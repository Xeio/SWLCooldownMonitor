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

class MissionCompleteJob : Job() {
    override fun onRunJob(params: Params): Result {
        Log.i("NotificationAlarmReceiver", "Mission complete notification.")

        val agentId = params.extras.getInt("agentId", 0)
        val character = params.extras.getString("character", "")

        var cooldown = CooldownData.instance.cooldowns.firstOrNull { it.agentId == agentId && it.character == character}

        if(cooldown != null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(context)
            }

            val myIntent = Intent(context, CooldownsDisplay::class.java)
            val activityIntent = PendingIntent.getActivity(context, 0, myIntent, 0)

            val notificationManager = NotificationManagerCompat.from(context)

//            val groupBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
//                    .setSmallIcon(R.drawable.notification_template_icon_bg)
//                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
//                    .setGroup("MISSION_COMPLETE")
//                    .setAutoCancel(true)
//                    .setGroupSummary(true)
//            notificationManager.notify(0, groupBuilder.build())

            val agentBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.agent_notifiction_icon)
                    .setContentIntent(activityIntent)
                    .setContentTitle(context.getString(R.string.mission_complete))
                    .setContentText("${cooldown.character}: ${cooldown.agent} has completed a mission.")
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setAutoCancel(true)
//                    .setGroup("MISSION_COMPLETE")
            notificationManager.notify(cooldown.agentId xor cooldown.character.hashCode(), agentBuilder.build())

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