using Microsoft.WindowsAzure.Storage.Table;
using System;

namespace SwlCooldownFunctions
{
    public class AgentMissionCooldown : TableEntity
    {
        public string CharacterName { get; set; }
        public string AgentName { get; set; }
        public string MissionName { get; set; }
        public DateTime EndDate { get; set; }
    }
}
