package pl.allegro.tech.hermes.api.endpoints;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import pl.allegro.tech.hermes.api.MessageFiltersVerificationInput;
import pl.allegro.tech.hermes.api.MessageFiltersVerificationResult;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("filters")
public interface FilterEndpoint {

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}")
    MessageFiltersVerificationResult verify(@PathParam("topicName") String qualifiedTopicName,
                                            MessageFiltersVerificationInput input);

}
