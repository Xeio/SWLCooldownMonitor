package com.xeio.swlcooldowns

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import com.evernote.android.job.JobManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.microsoft.windowsazure.notifications.NotificationsManager
import com.xeio.swlcooldowns.pushservices.CooldownPushNotificationHandler
import com.xeio.swlcooldowns.pushservices.NotificationHubSettings
import com.xeio.swlcooldowns.pushservices.RegistrationIntentService

class App : Application(), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onCreate() {
        super.onCreate()

        Log.i("SwlCooldownApp", "Initializing application")

        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        prefs.registerOnSharedPreferenceChangeListener(this)

        NotificationsManager.handleNotifications(applicationContext, NotificationHubSettings.SenderId, CooldownPushNotificationHandler::class.java)
        registerWithNotificationHubs()

        JobManager.create(this).addJobCreator(CooldownJobCreator())

        if(CooldownData.instance.cooldowns.size == 0) {
            GetCooldownsJob.createJob()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when(key)
        {
            "pref_patron" -> {
                GetCooldownsJob.createJob()
            }
            "pref_character_name" -> {
                registerWithNotificationHubs()
                GetCooldownsJob.createJob()
            }
        }
    }

    private fun registerWithNotificationHubs() {
        if (checkPlayServices()) {
            val intent = Intent(this, RegistrationIntentService::class.java)
            startService(intent)
        }
    }

    private fun checkPlayServices(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.i("LOG", "This device is not supported by Google Play Services.")
            return false
        }
        return true
    }
}