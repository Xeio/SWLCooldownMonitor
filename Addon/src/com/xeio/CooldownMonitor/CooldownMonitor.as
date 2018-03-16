import com.GameInterface.AgentSystem;
import com.GameInterface.AgentSystemAgent;
import com.GameInterface.AgentSystemMission;
import com.Utils.Archive;
import com.xeio.CooldownMonitor.CooldownApi;
import com.xeio.CooldownMonitor.CooldownItem;
import com.xeio.CooldownMonitor.Utils;
import mx.utils.Delegate;

class com.xeio.CooldownMonitor.CooldownMonitor
{    
    private var m_swfRoot: MovieClip;
    
    private var m_cooldownApi: CooldownApi;
    private var m_timeout: Number;
    private var m_lastSentMissions:Array = [];

    public static function main(swfRoot:MovieClip):Void 
    {
        var CooldownMonitor = new CooldownMonitor(swfRoot);

        swfRoot.onLoad = function() { CooldownMonitor.OnLoad(); };
        swfRoot.OnUnload =  function() { CooldownMonitor.OnUnload(); };
        swfRoot.OnModuleActivated = function(config:Archive) { CooldownMonitor.Activate(config); };
        swfRoot.OnModuleDeactivated = function() { return CooldownMonitor.Deactivate(); };
    }

    public function CooldownMonitor(swfRoot: MovieClip) 
    {
        m_swfRoot = swfRoot;
    }

    public function OnUnload()
    {
        m_cooldownApi = undefined;
        AgentSystem.SignalActiveMissionsUpdated.Disconnect(MissionsUpdated, this);
    }

    public function Activate(config: Archive)
    {
    }

    public function Deactivate(): Archive
    {
        var archive: Archive = new Archive();			
        return archive;
    }
	
	public function OnLoad()
	{
        m_cooldownApi = new CooldownApi();
        
        AgentSystem.SignalActiveMissionsUpdated.Connect(ScheduleUpdateCheck, this);
        
        ScheduleUpdateCheck();
	}
    
    public function ScheduleUpdateCheck()
    {
        clearTimeout(m_timeout);
        m_timeout = setTimeout(Delegate.create(this, MissionsUpdated), 2000);
    }
    
    public function MissionsUpdated()
    {
        if (NeedsUpdate())
        {
            m_lastSentMissions = [];
            
            var activeMissions:Array =  AgentSystem.GetActiveMissions();
            for (var i in activeMissions)
            {
                var mission:AgentSystemMission = activeMissions[i];
                m_lastSentMissions.push(mission.m_MissionId);
            }
            
            SendMissions();
        }
    }
    
    public function NeedsUpdate():Boolean
    {
        var activeMissions:Array =  AgentSystem.GetActiveMissions();
        
        if (activeMissions.length > m_lastSentMissions.length)
        {
            return true;
        }
        
        for (var i in activeMissions)
        {
            var mission:AgentSystemMission = activeMissions[i];
            
            if (!Utils.Contains(m_lastSentMissions, mission.m_MissionId))
            {
                return true;
            }
        }
        
        return false;
    }
    
    public function SendMissions()
    {
        m_cooldownApi.ClearQueue();
        var activeMissions:Array =  AgentSystem.GetActiveMissions();
        for (var i in activeMissions)
        {
            var mission:AgentSystemMission = activeMissions[i];
            if (mission.m_MissionId != 0)
            {
                var agent:AgentSystemAgent = AgentSystem.GetAgentOnMission(mission.m_MissionId);
                var cooldown:CooldownItem = new CooldownItem();
                cooldown.AgentName = agent.m_Name;
                cooldown.MissionName = mission.m_MissionName;
                cooldown.TimeLeft = AgentSystem.GetMissionCompleteTime(mission.m_MissionId) - com.GameInterface.Utils.GetServerSyncedTime();
                m_cooldownApi.QueueMissionSubmit(cooldown);
            }
        }        
    }
}