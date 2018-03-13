package com.xeio.swlcooldowns

import android.util.Log
import com.evernote.android.job.Job

class GetCooldownsJob : Job() {
    override fun onRunJob(params: Params): Result {
        Log.i("GetCooldownsJob", "Cooldown get job started.")

        val messageType = params.extras.getString("messageType", "")
        val character = params.extras.getString("character", "")

        CooldownData.instance.updateCooldowns(context, character)

        return Result.SUCCESS
    }

    companion object {
        val TAG = "get_cooldowns_job"
    }
}