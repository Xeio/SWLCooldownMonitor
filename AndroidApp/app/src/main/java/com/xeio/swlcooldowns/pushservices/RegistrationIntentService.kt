package com.xeio.swlcooldowns.pushservices

import android.app.IntentService
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log
import com.google.firebase.iid.FirebaseInstanceId
import com.microsoft.windowsazure.messaging.NotificationHub

class RegistrationIntentService : IntentService(TAG) {
    companion object {
        private val TAG = "NotificationHubService"
    }

    override fun onHandleIntent(intent: Intent) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        var regID: String = sharedPreferences.getString("registrationID", "")
        var storedToken: String = sharedPreferences.getString("FCMtoken", "")
        val characterName = sharedPreferences.getString("pref_character_name", "")

        val hub = NotificationHub(NotificationHubSettings.HubName, NotificationHubSettings.HubListenConnectionString, this)

        if(characterName == "") {
            hub.unregister()
            return
        }

        try {
            val FCM_token = FirebaseInstanceId.getInstance().token
            Log.d(TAG, "FCM Registration Token: " + FCM_token)

            // Storing the registration ID that indicates whether the generated token has been
            // sent to your server. If it is not stored, send the token to your server,
            // otherwise your server should have already received the token.
            if (regID == "" || storedToken !== FCM_token ) {
                val hub = NotificationHub(NotificationHubSettings.HubName, NotificationHubSettings.HubListenConnectionString, this)
                regID = hub.register(FCM_token, characterName).registrationId

                Log.d(TAG, "New NH Registration Successfully - RegId : " + regID)

                sharedPreferences.edit().putString("registrationID", regID).apply()
                sharedPreferences.edit().putString("FCMtoken", FCM_token).apply()
            }
        } catch (e: Throwable) {
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            Log.e(TAG,  "Failed to complete registration", e)
        }
    }
}