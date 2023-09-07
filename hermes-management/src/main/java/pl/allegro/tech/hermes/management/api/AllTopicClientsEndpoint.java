package pl.allegro.tech.hermes.management.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.management.domain.clients.AllTopicClientsService;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

@Component
@Path("topics/{topic}/clients")
public class AllTopicClientsEndpoint {

    private final AllTopicClientsService allTopicClientsService;

    public AllTopicClientsEndpoint(AllTopicClientsService allTopicClientsService) {
        this.allTopicClientsService = allTopicClientsService;
    }

    @GET
    @Produces(APPLICATION_JSON)
    public List<String> getTopicClients(@PathParam("topic") String topic) {
        return allTopicClientsService.getAllClientsByTopic(fromQualifiedName(topic));
    }
}
