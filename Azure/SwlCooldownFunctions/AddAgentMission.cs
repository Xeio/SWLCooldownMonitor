using System;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Threading.Tasks;
using Microsoft.Azure.NotificationHubs;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.Azure.WebJobs.Host;
using Microsoft.WindowsAzure.Storage.Table;
using Newtonsoft.Json;

namespace SwlCooldownFunctions
{
    public static class AddAgentMission
    {
        [FunctionName("AddAgentMission")]
        public static async Task<HttpResponseMessage> Run(
            [HttpTrigger(AuthorizationLevel.Anonymous, "get", Route = null)]HttpRequestMessage req,
            [Table("AgentCooldowns")]CloudTable agentCooldowns,
            IBinder binder,
            TraceWriter log)
        {
            var query = req.GetQueryNameValuePairs();

            string character = query.FirstOrDefault(kv => kv.Key == "char").Value;
            string agentName = query.FirstOrDefault(kv => kv.Key == "agent").Value;
            string missionName = query.FirstOrDefault(kv => kv.Key == "mission").Value;
            string timeLeftString = query.FirstOrDefault(kv => kv.Key == "timeLeft").Value;
            string agentIdString = query.FirstOrDefault(kv => kv.Key == "agentId").Value;
            string missionIdString = query.FirstOrDefault(kv => kv.Key == "missionId").Value;

            if (string.IsNullOrWhiteSpace(character) ||
                string.IsNullOrWhiteSpace(agentName) ||
                string.IsNullOrWhiteSpace(missionName) ||
                !int.TryParse(timeLeftString, out int timeLeft) ||
                !int.TryParse(missionIdString, out int missionId) ||
                !int.TryParse(agentIdString, out int agentId))
            {
                log.Info("Invalid request received.");
                return new HttpResponseMessage(HttpStatusCode.BadRequest);
            }

            var existingCooldown = (await agentCooldowns.ExecuteAsync(TableOperation.Retrieve<AgentMissionCooldown>(character, agentName))).Result as AgentMissionCooldown;

            var cooldown = new AgentMissionCooldown
            {
                PartitionKey = character,
                RowKey = agentName,
                CharacterName = character,
                AgentName = agentName,
                MissionName = missionName,
                MissionId = missionId,
                EndDate = DateTime.UtcNow.AddSeconds(timeLeft),
                AgentId = agentId
            };
            
            await agentCooldowns.ExecuteAsync(TableOperation.InsertOrReplace(cooldown));

            if(existingCooldown == null || existingCooldown.MissionId != missionId)
            {
                log.Info("New mission cooldown, sending push notification");

                var connectionString = System.Configuration.ConfigurationManager.ConnectionStrings["NotificationHub"].ConnectionString;
                var hubClient = NotificationHubClient.CreateClientFromConnectionString(connectionString, "swlcooldownshub");

                var message = JsonConvert.SerializeObject(new
                {
                    data = new
                    {
                        messageType = "NewMissions"
                    }
                });

                var outcome = await hubClient.SendGcmNativeNotificationAsync(message, character);

                log.Info("Notification sent, result: " + outcome.State);
            }
            
            return new HttpResponseMessage(HttpStatusCode.OK);
        }
    }
}
