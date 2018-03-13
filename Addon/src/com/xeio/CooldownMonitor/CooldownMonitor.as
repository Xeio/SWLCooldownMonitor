import com.GameInterface.AgentSystem;
import com.GameInterface.AgentSystemAgent;
import com.GameInterface.AgentSystemMission;
import com.Utils.Archive;
import com.xeio.CooldownMonitor.CooldownApi;
import com.xeio.CooldownMonitor.CooldownItem;
import mx.utils.Delegate;

class com.xeio.CooldownMonitor.CooldownMonitor
{    
    private var m_swfRoot: MovieClip;
    
    private var m_cooldownApi: CooldownApi;

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
        
        AgentSystem.SignalActiveMissionsUpdated.Connect(MissionsUpdated, this);
        
        MissionsUpdated();
	}
    
    public function MissionsUpdated()
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
                cooldown.AgentId = agent.m_AgentId;
                cooldown.MissionName = mission.m_MissionName;
                cooldown.MissionId = mission.m_MissionId;
                cooldown.TimeLeft = AgentSystem.GetMissionCompleteTime(mission.m_MissionId) - com.GameInterface.Utils.GetServerSyncedTime();
                m_cooldownApi.QueueMissionSubmit(cooldown);
            }
        }        
    }
}