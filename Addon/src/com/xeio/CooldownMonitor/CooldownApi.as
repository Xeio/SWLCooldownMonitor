import com.GameInterface.AgentSystem;
import com.GameInterface.Browser.Browser;
import com.GameInterface.DistributedValue;
import com.GameInterface.Game.Character;
import com.xeio.CooldownMonitor.CooldownItem;
import mx.utils.Delegate;

class com.xeio.CooldownMonitor.CooldownApi
{
    var m_Browser:Browser;
    var m_cooldowns:Array = [];
    var m_nextUploadTimeout:Number = 0;
    var m_browserStuckTimeout:Number;
    var m_characterName:String;
    var m_lastSentItem:CooldownItem;
    
    public function CooldownApi() 
    {
        m_characterName = Character.GetClientCharacter().GetName();
        m_Browser = undefined;
    }
    
    public function QueueMissionSubmit(cooldown: CooldownItem)
    {
        m_cooldowns.push(cooldown);
        
        if(!m_Browser)
        {
            clearTimeout(m_nextUploadTimeout);
            m_nextUploadTimeout = setTimeout(Delegate.create(this, Upload), 1000);
        }
    }
    
    public function ClearQueue()
    {
        m_cooldowns = [];
    }
    
    private function Upload()
    {
        m_lastSentItem = CooldownItem(m_cooldowns.pop());
        if (!m_lastSentItem)
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
            m_Browser = new Browser(17, 100, 100);
            m_Browser.SignalBrowserShowPage.Connect(PageLoaded,  this);
        }
        
        var timeLeft:Number = AgentSystem.GetMissionCompleteTime(m_lastSentItem.MissionId) - com.GameInterface.Utils.GetServerSyncedTime();
        
        var url:String = DistributedValue.GetDValue("CooldownMonitor_AddCooldownUrl") +
                            "api/AddAgentMission?" +
                            "char=" + escape(m_characterName) +
                            "&agent=" + escape(m_lastSentItem.AgentName) +
                            "&mission=" + escape(m_lastSentItem.MissionName) +
                            "&timeLeft=" + timeLeft;
        
        m_Browser.OpenURL(url);
        
        clearTimeout(m_browserStuckTimeout);
        m_browserStuckTimeout = setTimeout(Delegate.create(this, Timeout), 10000);
    }
    
    public function PageLoaded()
    {
        clearTimeout(m_browserStuckTimeout);
        m_Browser.Stop();
        
        if (this.m_cooldowns.length == 0)
        {
            m_Browser.SignalBrowserShowPage.Disconnect(PageLoaded, this);
            m_Browser.CloseBrowser();
            m_Browser = undefined;
        }
        else
        {
            clearTimeout(m_nextUploadTimeout);
            m_nextUploadTimeout = setTimeout(Delegate.create(this, Upload), 1000);
        }
    }
    
    public function Timeout()
    {
        m_Browser.Stop();
        m_Browser.SignalBrowserShowPage.Disconnect(PageLoaded, this);
        m_Browser.CloseBrowser();
        m_Browser = undefined;
        
        QueueMissionSubmit(m_lastSentItem);
    }
}