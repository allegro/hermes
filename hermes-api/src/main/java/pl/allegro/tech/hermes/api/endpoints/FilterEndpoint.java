package pl.allegro.tech.hermes.api.endpoints;

import pl.allegro.tech.hermes.api.MessageFiltersVerificationInput;
import pl.allegro.tech.hermes.api.MessageFiltersVerificationResult;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("filters")
public interface FilterEndpoint {

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}")
    MessageFiltersVerificationResult verify(@PathParam("topicName") String qualifiedTopicName,
                                            MessageFiltersVerificationInput input);

}
