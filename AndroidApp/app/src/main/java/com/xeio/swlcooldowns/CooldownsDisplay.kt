package com.xeio.swlcooldowns

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_cooldowns_display.*
import kotlinx.android.synthetic.main.content_cooldowns_display.*

class CooldownsDisplay : AppCompatActivity()  {
    companion object{
        var visible : Boolean = false
            private set
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("CooldownsDisplay", "Creating main activity")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cooldowns_display)
        setSupportActionBar(toolbar)

        cooldownsRecyclerView.layoutManager = LinearLayoutManager(this)

        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if(visible) {
                    Toast.makeText(this@CooldownsDisplay, context.getString(R.string.cooldowns_updated), Toast.LENGTH_SHORT).show()
                }
                cooldownsRecyclerView.adapter = AgentCooldownAdapter(CooldownData.instance.cooldowns)
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, IntentFilter("updated_cooldowns"))

        visible = true

        cooldownsRecyclerView.adapter = AgentCooldownAdapter(CooldownData.instance.cooldowns)
    }

    override fun onResume() {
        super.onResume()
        cooldownsRecyclerView.adapter = AgentCooldownAdapter(CooldownData.instance.cooldowns)
        visible = true
    }

    override fun onPause() {
        super.onPause()
        visible = false
        //Stop the chronometers from ticking by clearing the views
        cooldownsRecyclerView.adapter = AgentCooldownAdapter(ArrayList())
    }

    override fun onStop() {
        super.onStop()
        visible = false
        //Stop the chronometers from ticking by clearing the views
        cooldownsRecyclerView.adapter = AgentCooldownAdapter(ArrayList())
    }

    override fun onStart() {
        super.onStart()
        visible = true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_cooldowns_display, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_refresh -> {
                GetCooldownsJob.createJob()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
