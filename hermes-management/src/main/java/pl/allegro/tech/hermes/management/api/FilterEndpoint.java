package pl.allegro.tech.hermes.management.api;

import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.MessageFiltersVerificationInput;
import pl.allegro.tech.hermes.api.MessageFiltersVerificationResult;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.domain.filtering.FilteringService;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

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
