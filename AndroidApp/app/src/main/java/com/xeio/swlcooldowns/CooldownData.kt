package com.xeio.swlcooldowns

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.URL
import java.net.URLEncoder

class CooldownData
{
    companion object {
        val instance : CooldownData = CooldownData()
    }

    var cooldowns = ArrayList<AgentMissionCoooldown>()
    var characterCount = 0
        private set

    fun updateCooldowns(context: Context, character : String = "")
    {
        Log.i("CooldownData", "Starting cooldown update.")

        if(character == ""){
            cooldowns.clear()
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val charNames = if(character == "") {
                            prefs.getString("pref_character_name", "").lines().map { it.trim() }.filter { it != "" }}
                        else{
                            listOf(character)
                        }

        if(character == ""){
            characterCount = charNames.size
        }

        charNames.forEach { character ->
            Log.i("CooldownData", "Fetching missions for character: $character")

            val patron = prefs.getBoolean("pref_patron", false)
            val newCooldowns = try {
                var params = "char=${URLEncoder.encode(character, "UTF-8")}"
                if (patron) {
                    params += "&patron"
                }
                val json = URL("http://swlcooldowns.azurewebsites.net/api/GetCharacterCooldowns?$params").readText()
                Gson().fromJson<java.util.ArrayList<AgentMissionCoooldown>>(json, object : TypeToken<List<AgentMissionCoooldown>>() {}.type)
            } catch (e: Exception) {
                GetCooldownsJob.createJob(character, 1000 * 60 * 5)
                java.util.ArrayList<AgentMissionCoooldown>()
            }
            newCooldowns.forEach { cooldown ->
                cooldown.lastRetrieved = SystemClock.elapsedRealtime()
                cooldown.character = character
            }

            if (newCooldowns.count { it.timeLeft > 0 } == if(patron){ 3 } else{ 2 })
            {
                //If all missions slots for the character are active, clear that character's notification,
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.cancel(character.hashCode())
            }

            cooldowns.removeIf{ it.character == character}
            cooldowns.addAll(newCooldowns)
            cooldowns.sortBy { it.lastRetrieved + it.timeLeft * 1000 }
        }

        val intent = Intent("updated_cooldowns")
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)

        if(MissionCompleteJob.areNotificationsEnabled(context)) {
            scheduleNextNotification()
        } else {
            JobManager.instance().cancelAllForTag(MissionCompleteJob.TAG)
        }
    }

    fun scheduleNextNotification() {
        val it = cooldowns.sortedBy { it.timeLeft }.firstOrNull{ !it.notified && it.timeLeft > 0}
        if (it != null) {
            Log.i("CooldownData", "Scheduling notification.")

            val extras = PersistableBundleCompat()
            extras.putString("agent", it.agent)
            extras.putString("character", it.character)

            val timeTillNextMission = it.lastRetrieved - SystemClock.elapsedRealtime() + it.timeLeft * 1000

            var builder = JobRequest.Builder(MissionCompleteJob.TAG)
                    .addExtras(extras)
                    .setUpdateCurrent(true)

            builder = if(timeTillNextMission > 5000) {
                builder.setExact(timeTillNextMission)
            } else {
                builder.startNow()
            }

            builder.build().schedule()
        } else {
            JobManager.instance().cancelAllForTag(MissionCompleteJob.TAG)
        }
    }
}