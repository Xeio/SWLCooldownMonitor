package com.xeio.swlcooldowns

import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator

class CooldownJobCreator : JobCreator {
    override fun create(tag: String): Job? {
        when (tag) {
            MissionCompleteJob.TAG -> return MissionCompleteJob()
            else -> return null
        }
    }
}
