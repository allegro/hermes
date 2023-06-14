package pl.allegro.tech.hermes.management.api;

import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.MessageFiltersVerificationInput;
import pl.allegro.tech.hermes.api.MessageFiltersVerificationResult;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.domain.filtering.FilteringService;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("filters")
public class FilterEndpoint {
    private final FilteringService filteringService;

    public FilterEndpoint(FilteringService filteringService) {
        this.filteringService = filteringService;
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}")
    public MessageFiltersVerificationResult verify(@PathParam("topicName") String qualifiedTopicName,
                                                   MessageFiltersVerificationInput input) {
        return filteringService.verify(input, TopicName.fromQualifiedName(qualifiedTopicName));
    }
}
