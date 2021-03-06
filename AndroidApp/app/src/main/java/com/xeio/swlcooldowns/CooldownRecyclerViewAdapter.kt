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
            if(CooldownData.instance.characterCount > 1) {
                itemView.characterName.text = cooldown.character
            }else {
                itemView.characterName.visibility = View.GONE
            }

            if(cooldown.timeLeft > 0) {
                itemView.timer.isCountDown = true
                itemView.timer.base = cooldown.lastRetrieved + cooldown.timeLeft * 1000
                itemView.timer.onChronometerTickListener = this
                itemView.timer.start()
            }else{
                itemView.timer.text = itemView.context.getString(R.string.completed)
            }

        }

        override fun onChronometerTick(chronometer: Chronometer)
        {
            if(chronometer.base <= SystemClock.elapsedRealtime())
            {
                chronometer.stop()
                chronometer.text = chronometer.context.getString(R.string.completed)
            }
        }
    }

}