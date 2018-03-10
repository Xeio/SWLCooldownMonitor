package com.xeio.swlcooldowns

class AgentMissionCoooldown(val agent : String, val mission : String, val timeLeft : Int, val agentId : Int) {
    var notified : Boolean = false
    var lastRetrieved : Long = 0
    var character : String = ""
}