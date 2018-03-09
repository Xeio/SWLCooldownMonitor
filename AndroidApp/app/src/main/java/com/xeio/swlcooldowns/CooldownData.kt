package com.xeio.swlcooldowns

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.Toast
import com.evernote.android.job.JobRequest
import com.evernote.android.job.JobRescheduleService
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
    var lastRetrieved = System.currentTimeMillis()
        private set
    var lastRetrievedRtc = SystemClock.currentThreadTimeMillis()
        private set

    fun updateCooldowns(context: Context)
    {
        if(CooldownsDisplay.visible) {
            Toast.makeText(context, context.getString(R.string.cooldowns_updating), Toast.LENGTH_SHORT).show()
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val charName = prefs.getString("pref_character_name", "")

        async(CommonPool) {
            if(charName != "") {
                val json = URL("http://swlcooldowns.azurewebsites.net/api/GetCharacterCooldowns?char=${URLEncoder.encode(charName, "UTF-8")}").readText()
                lastRetrieved = SystemClock.elapsedRealtime()
                lastRetrievedRtc = System.currentTimeMillis()

                cooldowns = try {
                    Gson().fromJson<java.util.ArrayList<AgentMissionCoooldown>>(json, object : TypeToken<List<AgentMissionCoooldown>>() {}.getType())
                } catch (e: Exception){
                    java.util.ArrayList<AgentMissionCoooldown>()
                }

                val intent = Intent("updated_cooldowns")
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)

                scheduleNextNotification(context)
            }
        }
    }

    fun scheduleNextNotification(context: Context) {
        val it = cooldowns.sortedBy { it.timeLeft }.filter { !it.notified }.firstOrNull()
        if (it != null && it.timeLeft > 0) {
            Log.i("CooldownData", "Scheduling notification.")

            val extras = PersistableBundleCompat()
            extras.putInt("agentId", it.agentId)

            val timeTillNextMission = lastRetrieved - SystemClock.elapsedRealtime() + it.timeLeft * 1000;

            var builder = JobRequest.Builder(MissionCompleteJob.TAG)
                    .setExact(timeTillNextMission)
                    .addExtras(extras)
                    .setUpdateCurrent(true)

            builder = if(timeTillNextMission > 5000) {
                builder.setExact(timeTillNextMission)
            } else {
                builder.startNow()
            }

            builder.build().schedule()
        }
    }
}