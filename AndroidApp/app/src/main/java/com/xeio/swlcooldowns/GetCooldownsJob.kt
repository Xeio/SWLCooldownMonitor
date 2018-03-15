package com.xeio.swlcooldowns

import android.util.Log
import com.evernote.android.job.Job
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat

class GetCooldownsJob : Job() {
    override fun onRunJob(params: Params): Result {
        Log.i("GetCooldownsJob", "Cooldown get job started.")

        val character = params.extras.getString("character", "")

        CooldownData.instance.updateCooldowns(context, character)

        return Result.SUCCESS
    }

    companion object {
        val TAG = "get_cooldowns_job"

        fun createJob(character: String = "", delay: Long = 0){
            val extras = PersistableBundleCompat()
            extras.putString("character", character)

            var builder = JobRequest.Builder(GetCooldownsJob.TAG)
                    .addExtras(extras)
                    .setUpdateCurrent(true)
                    .startNow()

            builder = if(delay == 0L){
                builder.startNow()
            } else{
                builder.setExecutionWindow(delay, 1000 * 60 * 5)
            }

            builder.build().schedule()
        }
    }
}