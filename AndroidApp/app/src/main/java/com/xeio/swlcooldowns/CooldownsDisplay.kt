package com.xeio.swlcooldowns

import android.content.*
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_cooldowns_display.*
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.content_cooldowns_display.*
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.microsoft.windowsazure.notifications.NotificationsManager
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.xeio.swlcooldowns.pushservices.CooldownPushNotificationHandler
import com.xeio.swlcooldowns.pushservices.NotificationHubSettings
import com.xeio.swlcooldowns.pushservices.RegistrationIntentService
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest

class CooldownsDisplay : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener  {
    companion object{
        var visible : Boolean = false
            private set
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("CooldownsDisplay", "Creating main activity")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cooldowns_display)
        setSupportActionBar(toolbar)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(this)

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

        NotificationsManager.handleNotifications(this, NotificationHubSettings.SenderId, CooldownPushNotificationHandler::class.java)
        registerWithNotificationHubs()

        JobManager.create(this).addJobCreator(CooldownJobCreator())

        if(CooldownData.instance.cooldowns.size == 0) {
            GetCooldownsJob.createJob()
        } else {
            cooldownsRecyclerView.adapter = AgentCooldownAdapter(CooldownData.instance.cooldowns)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if(key == "pref_character_name"){
            registerWithNotificationHubs()
            GetCooldownsJob.createJob()
        }
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
        cooldownsRecyclerView.adapter = AgentCooldownAdapter(ArrayList<AgentMissionCoooldown>())
    }

    override fun onStop() {
        super.onStop()
        visible = false
        //Stop the chronometers from ticking by clearing the views
        cooldownsRecyclerView.adapter = AgentCooldownAdapter(ArrayList<AgentMissionCoooldown>())
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

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private fun checkPlayServices(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, 9000).show()
            } else {
                Log.i("LOG", "This device is not supported by Google Play Services.")
                finish()
            }
            return false
        }
        return true
    }

    fun registerWithNotificationHubs() {
        if (checkPlayServices()) {
            val intent = Intent(this, RegistrationIntentService::class.java)
            startService(intent)
        }
    }
}
