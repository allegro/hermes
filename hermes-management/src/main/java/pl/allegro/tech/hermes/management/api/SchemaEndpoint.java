package pl.allegro.tech.hermes.management.api;

import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.domain.topic.schema.MessageSchemaSourceRepository;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

@Path("topics/{topicName}/schema")
public class SchemaEndpoint {

    private final TopicService topicService;
    private final MessageSchemaSourceRepository schemaSourceRepository;

    @Autowired
    public SchemaEndpoint(TopicService topicService, MessageSchemaSourceRepository schemaSourceRepository) {
        this.topicService = topicService;
        this.schemaSourceRepository = schemaSourceRepository;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get schema", response = String.class, httpMethod = HttpMethod.GET)
    public String get(@PathParam("topicName") String qualifiedTopicName) {
        return schemaSourceRepository.get(topicService.getTopicDetails(fromQualifiedName(qualifiedTopicName)));
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Roles.GROUP_OWNER, Roles.ADMIN})
    @ApiOperation(value = "Save schema", httpMethod = HttpMethod.POST)
    public Response save(@PathParam("topicName") String qualifiedTopicName, String schema) {
        schemaSourceRepository.save(schema, topicService.getTopicDetails(fromQualifiedName(qualifiedTopicName)));
        return responseStatus(Response.Status.CREATED);
    }

    private Response responseStatus(Response.Status responseStatus) {
        return Response.status(responseStatus).build();
    }

}
