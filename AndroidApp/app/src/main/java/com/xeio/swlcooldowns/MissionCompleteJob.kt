package com.xeio.swlcooldowns

import android.annotation.TargetApi
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

class MissionCompleteJob : Job() {
    override fun onRunJob(params: Params): Result {
        Log.i("NotificationAlarmReceiver", "Mission complete notification.")

        val agentId = params.extras.getInt("agentId", 0)

        val cooldown = CooldownData.instance.cooldowns.first { it.agentId == agentId }

        if(cooldown != null){
            createNotificationChannel(context)

            val myIntent = Intent(context, CooldownsDisplay::class.java)
            val activityIntent = PendingIntent.getActivity(context, 0, myIntent, 0)

            val groupBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification_template_icon_bg)
                    .setGroup("MISSION_COMPLETE")
                    .setAutoCancel(true)
                    .setGroupSummary(true)

            val agentBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification_template_icon_bg)
                    .setContentIntent(activityIntent)
                    .setContentTitle(context.getString(R.string.mission_complete))
                    .setContentText("${cooldown.agent} has completed a mission.")
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setAutoCancel(true)
                    .setGroup("MISSION_COMPLETE")

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(0, groupBuilder.build())
            notificationManager.notify(cooldown.agentId, agentBuilder.build())

            cooldown.notified = true

            CooldownData.instance.scheduleNextNotification(context)
        }

        // run your job here
        return Result.SUCCESS
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(context: Context){
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val name = context.getString(R.string.mission_complete_channel)
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = context.getString(R.string.mission_complete_channel_desc)
        // Register the channel with the system
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        val TAG = "mission_complete_job"
        val NOTIFICATION_CHANNEL_ID = "Mission Completions"
    }
}