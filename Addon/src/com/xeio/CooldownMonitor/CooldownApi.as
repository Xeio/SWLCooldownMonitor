import com.GameInterface.Browser.Browser;
import com.GameInterface.DistributedValue;
import com.GameInterface.Game.Character;
import com.xeio.CooldownMonitor.CooldownItem;
import mx.utils.Delegate;

class com.xeio.CooldownMonitor.CooldownApi
{
    var m_Browser:Browser;
    var m_itemsToSubmit:Array = [];
    var m_characterName:String;
    
    public function CooldownApi() 
    {
        m_characterName = Character.GetClientCharacter().GetName();
    }
    
    public function QueueMissionSubmit(newPrice: CooldownItem)
    {
        m_itemsToSubmit.push(newPrice);
        
        if(!m_Browser)
        {
            Upload();
        }
    }
    
    public function ClearQueue()
    {
        m_itemsToSubmit = [];
    }
    
    private function Upload()
    {
        if (!m_Browser)
        {
            m_Browser = new Browser(_global.Enums.WebBrowserStates.e_BrowserMode_Browser, 100, 100);
            m_Browser.SignalBrowserShowPage.Connect(PageLoaded,  this);
        }
        
        var item:CooldownItem = CooldownItem(m_itemsToSubmit.pop());
        if (!item)
        {
            m_Browser.CloseBrowser();
            m_Browser = undefined;
            return;
        }
        
        var url:String = DistributedValue.GetDValue("CooldownMonitor_AddCooldownUrl") +
                            "api/AddAgentMission?" +
                            "char=" + escape(m_characterName) +
                            "&agent=" + escape(item.AgentName) +
                            "&agentId=" + item.AgentId + 
                            "&mission=" + escape(item.MissionName) +
                            "&missionId=" + item.MissionId +
                            "&timeLeft=" + item.TimeLeft;
        
        m_Browser.OpenURL(url);
    }
    
    public function PageLoaded()
    {
        if (this.m_itemsToSubmit.length == 0)
        {
            m_Browser.CloseBrowser();
            m_Browser = undefined;
        }
        else
        {
            setTimeout(Delegate.create(this, Upload), 1000);
        }
    }
}