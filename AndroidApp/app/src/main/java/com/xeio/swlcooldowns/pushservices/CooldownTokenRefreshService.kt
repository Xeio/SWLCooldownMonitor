package com.xeio.swlcooldowns.pushservices

import android.content.Intent
import android.util.Log
import com.google.firebase.iid.FirebaseInstanceIdService

class CooldownTokenRefreshService : FirebaseInstanceIdService() {
    override fun onTokenRefresh() {
        Log.d("CooldownTokenRefreshService", "Refreshing GCM Registration Token")
        val intent = Intent(this, RegistrationIntentService::class.java)
        startService(intent)
    }
}