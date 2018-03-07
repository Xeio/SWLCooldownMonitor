package com.xeio.swlcooldowns

import android.os.SystemClock
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import kotlinx.android.synthetic.main.agent_cooldown_display.view.*
import java.util.*

class AgentCooldownAdapter(val cooldowns: ArrayList<AgentMissionCoooldown>) : RecyclerView.Adapter<AgentCooldownAdapter.ViewHolder>() {

    //this method is returning the view for each item in the list
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgentCooldownAdapter.ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.agent_cooldown_display, parent, false)
        return ViewHolder(v)
    }

    //this method is binding the data on the list
    override fun onBindViewHolder(holder: AgentCooldownAdapter.ViewHolder, position: Int) {
        holder.bindItems(cooldowns[position])
    }

    override fun getItemCount() = cooldowns.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), Chronometer.OnChronometerTickListener {
        fun bindItems(cooldown: AgentMissionCoooldown) {
            itemView.agentName.text = cooldown.agent
            itemView.missionName.text = cooldown.mission

            itemView.timer.isCountDown = true
            itemView.timer.base = CooldownData.instance.lastRetrieved + cooldown.timeLeft * 1000
            itemView.timer.onChronometerTickListener = this
            itemView.timer.start()
        }

        override fun onChronometerTick(chronometer: Chronometer)
        {
            if(chronometer.base <= SystemClock.elapsedRealtime())
            {
                chronometer.stop()
                chronometer.text = itemView.resources.getString(R.string.completed)
            }
        }
    }

}