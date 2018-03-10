package com.xeio.swlcooldowns

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.Toast
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import java.net.URL
import java.net.URLEncoder

class CooldownData
{
    companion object {
        val instance : CooldownData = CooldownData()
    }

    var cooldowns = ArrayList<AgentMissionCoooldown>()

    fun updateCooldowns(context: Context, character : String = "")
    {
        if(CooldownsDisplay.visible) {
            Toast.makeText(context, context.getString(R.string.cooldowns_updating), Toast.LENGTH_SHORT).show()
        }

        if(character == ""){
            cooldowns.clear()
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val charNames = if(character == "") {
                            prefs.getString("pref_character_name", "").lines().map { it.trim() }.filter { it != "" }}
                        else{
                            listOf(character)
                        }

        async(CommonPool) {
            if(charNames.any()) {
                charNames.forEach {character ->
                    val json = URL("http://swlcooldowns.azurewebsites.net/api/GetCharacterCooldowns?char=${URLEncoder.encode(character, "UTF-8")}").readText()

                    val newCooldowns = try {
                        Gson().fromJson<java.util.ArrayList<AgentMissionCoooldown>>(json, object : TypeToken<List<AgentMissionCoooldown>>() {}.getType())
                    } catch (e: Exception){
                        java.util.ArrayList<AgentMissionCoooldown>()
                    }
                    newCooldowns.forEach{ cooldown ->
                        cooldown.lastRetrieved = SystemClock.elapsedRealtime()
                        cooldown.character = character
                    }

                    cooldowns.removeIf{ it.character == character}
                    cooldowns.addAll(newCooldowns)
                    cooldowns.sortBy { it.lastRetrieved + it.timeLeft * 1000 }
                }

                val intent = Intent("updated_cooldowns")
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)

                scheduleNextNotification(context)
            }
        }
    }

    fun scheduleNextNotification(context: Context) {
        val it = cooldowns.sortedBy { it.timeLeft }.firstOrNull{ !it.notified && it.timeLeft > 0}
        if (it != null) {
            Log.i("CooldownData", "Scheduling notification.")

            val extras = PersistableBundleCompat()
            extras.putInt("agentId", it.agentId)
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
            JobManager.create(context).cancelAllForTag(MissionCompleteJob.TAG)
        }
    }
}