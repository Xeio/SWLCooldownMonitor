import com.GameInterface.Browser.Browser;
import com.GameInterface.DistributedValue;
import com.GameInterface.Game.Character;
import com.xeio.CooldownMonitor.CooldownItem;
import mx.utils.Delegate;

class com.xeio.CooldownMonitor.CooldownApi
{
    var m_Browser:Browser;
    var m_cooldowns:Array = [];
    var m_characterName:String;
    
    public function CooldownApi() 
    {
        m_characterName = Character.GetClientCharacter().GetName();
    }
    
    public function QueueMissionSubmit(cooldown: CooldownItem)
    {
        m_cooldowns.push(cooldown);
        
        if(!m_Browser)
        {
            Upload();
        }
    }
    
    public function ClearQueue()
    {
        m_cooldowns = [];
    }
    
    private function Upload()
    {
        var item:CooldownItem = CooldownItem(m_cooldowns.pop());
        if (!item)
        {
            if (m_Browser)
            {
                m_Browser.SignalBrowserShowPage.Disconnect(PageLoaded, this);
                m_Browser.CloseBrowser();
                m_Browser = undefined;
            }
            return;
        }
        
        if (!m_Browser)
        {
            m_Browser = new Browser(_global.Enums.WebBrowserStates.e_BrowserMode_Browser, 100, 100);
            m_Browser.SignalBrowserShowPage.Connect(PageLoaded,  this);
        }
        
        var url:String = DistributedValue.GetDValue("CooldownMonitor_AddCooldownUrl") +
                            "api/AddAgentMission?" +
                            "char=" + escape(m_characterName) +
                            "&agent=" + escape(item.AgentName) +
                            "&mission=" + escape(item.MissionName) +
                            "&timeLeft=" + item.TimeLeft;
        
        m_Browser.OpenURL(url);
    }
    
    public function PageLoaded()
    {
        if (this.m_cooldowns.length == 0)
        {
            m_Browser.SignalBrowserShowPage.Disconnect(PageLoaded, this);
            m_Browser.CloseBrowser();
            m_Browser = undefined;
        }
        else
        {
            for (var i in m_cooldowns){
                m_cooldowns[i].TimeLeft -= 2;
            }
            setTimeout(Delegate.create(this, Upload), 2000);
        }
    }
}