package pl.allegro.tech.hermes.management.api;

import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.domain.topic.schema.SchemaService;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

@Path("topics/{topicName}/schema")
public class SchemaEndpoint {

    private final SchemaService schemaService;
    private final TopicService topicService;

    @Autowired
    public SchemaEndpoint(SchemaService schemaService, TopicService topicService) {
        this.schemaService = schemaService;
        this.topicService = topicService;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get schema", httpMethod = HttpMethod.GET)
    public Response get(@PathParam("topicName") String qualifiedTopicName) {
        Optional<RawSchema> rawSchema = schemaService.getSchema(qualifiedTopicName);
        return rawSchema.map(RawSchema::value)
                .map(v -> Response.ok(v).build())
                .orElse(Response.noContent().build());
    }

    @GET
    @Path("{version}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get schema", httpMethod = HttpMethod.GET)
    public Response get(@PathParam("topicName") String qualifiedTopicName, @PathParam("version") int version) {
        Optional<RawSchema> rawSchema = schemaService.getSchema(qualifiedTopicName, SchemaVersion.valueOf(version));
        return rawSchema.map(RawSchema::value)
                .map(v -> Response.ok(v).build())
                .orElse(Response.noContent().build());
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Roles.TOPIC_OWNER, Roles.ADMIN})
    @ApiOperation(value = "Save schema", httpMethod = HttpMethod.POST)
    public Response save(@PathParam("topicName") String qualifiedTopicName,
                         @DefaultValue("true") @QueryParam(value = "validate") boolean validate,
                         String schema) {
        Topic topic = topicService.getTopicDetails(fromQualifiedName(qualifiedTopicName));
        schemaService.registerSchema(topic, schema, validate);
        notifyFrontendSchemaChanged(qualifiedTopicName);
        return Response.status(Response.Status.CREATED).build();
    }

    private void notifyFrontendSchemaChanged(String qualifiedTopicName) {
        topicService.touchTopic(fromQualifiedName(qualifiedTopicName));
    }

    @DELETE
    @RolesAllowed({Roles.TOPIC_OWNER, Roles.ADMIN})
    @ApiOperation(value = "Delete schema", httpMethod = HttpMethod.DELETE)
    public Response delete(@PathParam("topicName") String qualifiedTopicName) {
        schemaService.deleteAllSchemaVersions(qualifiedTopicName);
        return Response.status(Response.Status.OK).build();
    }
}
