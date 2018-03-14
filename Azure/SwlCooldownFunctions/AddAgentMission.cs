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

            if (string.IsNullOrWhiteSpace(character) ||
                string.IsNullOrWhiteSpace(agentName) ||
                string.IsNullOrWhiteSpace(missionName) ||
                !int.TryParse(timeLeftString, out int timeLeft))
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
                EndDate = DateTime.UtcNow.AddSeconds(timeLeft),
            };
            
            await agentCooldowns.ExecuteAsync(TableOperation.InsertOrReplace(cooldown));

            if(existingCooldown == null || Math.Abs((existingCooldown.EndDate - cooldown.EndDate).TotalMinutes) > 5)
            {
                //Trigger an push if the cooldown end dates differ
                log.Info("New mission cooldown, sending push notification");

                var connectionString = System.Configuration.ConfigurationManager.ConnectionStrings["NotificationHub"].ConnectionString;
                var hubClient = NotificationHubClient.CreateClientFromConnectionString(connectionString, "swlcooldownshub");

                var message = JsonConvert.SerializeObject(new
                {
                    data = new
                    {
                        character,
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
