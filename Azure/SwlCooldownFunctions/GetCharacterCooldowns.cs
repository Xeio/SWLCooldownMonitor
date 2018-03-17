using System;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Threading.Tasks;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.Azure.WebJobs.Host;
using Microsoft.WindowsAzure.Storage.Table;
using Newtonsoft.Json;

namespace SwlCooldownFunctions
{
    public static class GetCharacterCooldowns
    {
        [FunctionName("GetCharacterCooldowns")]
        public static async Task<HttpResponseMessage> Run(
            [HttpTrigger(AuthorizationLevel.Anonymous, "get", Route = null)]HttpRequestMessage req,
            [Table("AgentCooldowns")]CloudTable agentCooldowns,
            TraceWriter log)
        {
            var queryStrings = req.GetQueryNameValuePairs();

            string character = queryStrings.FirstOrDefault(kv => kv.Key == "char").Value;
            bool patron = queryStrings.Any(kv => kv.Key == "patron");

            if (string.IsNullOrWhiteSpace(character))
            {
                return new HttpResponseMessage(HttpStatusCode.BadRequest);
            }

            var query = new TableQuery<AgentMissionCooldown>().Where(TableQuery.GenerateFilterCondition("PartitionKey", QueryComparisons.Equal, character));

            var result = await agentCooldowns.ExecuteQuerySegmentedAsync(query, null);

            var now = DateTime.UtcNow;
            var cooldowns = result.Results
                .OrderByDescending(cd => cd.EndDate)
                .Take(patron ? 3 : 2) //Max number of active missions is 3 currently
                .OrderBy(cd => cd.EndDate)
                .Select(cd => new
                {
                    agent = cd.AgentName,
                    mission = cd.MissionName,
                    timeLeft = cd.EndDate > now ? (int)(cd.EndDate - now).TotalSeconds : 0
                });
            
            return new HttpResponseMessage(HttpStatusCode.OK) {
                Content = new StringContent(JsonConvert.SerializeObject(cooldowns))
            };
        }
    }
}
