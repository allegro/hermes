package pl.allegro.tech.hermes.management.api;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.management.api.auth.HermesSecurityAwareRequestUser;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.domain.topic.schema.SchemaService;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import java.util.Optional;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

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
    @Path("versions/{version}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get schema", httpMethod = HttpMethod.GET)
    public Response getByVersion(@PathParam("topicName") String qualifiedTopicName, @PathParam("version") int version) {
        Optional<RawSchema> rawSchema = schemaService.getSchema(qualifiedTopicName, SchemaVersion.valueOf(version));
        return rawSchema.map(RawSchema::value)
                .map(v -> Response.ok(v).build())
                .orElse(Response.noContent().build());
    }

    @GET
    @Path("ids/{id}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get schema", httpMethod = HttpMethod.GET)
    public Response getById(@PathParam("topicName") String qualifiedTopicName, @PathParam("id") int id) {
        Optional<RawSchema> rawSchema = schemaService.getSchema(qualifiedTopicName, SchemaId.valueOf(id));
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
                         @Context ContainerRequestContext requestContext,
                         String schema) {
        Topic topic = topicService.getTopicDetails(fromQualifiedName(qualifiedTopicName));
        RequestUser user = new HermesSecurityAwareRequestUser(requestContext);
        schemaService.registerSchema(topic, schema, validate);
        notifyFrontendSchemaChanged(qualifiedTopicName, user);
        return Response.status(Response.Status.CREATED).build();
    }

    private void notifyFrontendSchemaChanged(String qualifiedTopicName, RequestUser changedBy) {
        topicService.scheduleTouchTopic(fromQualifiedName(qualifiedTopicName), changedBy);
    }

    @DELETE
    @RolesAllowed({Roles.TOPIC_OWNER, Roles.ADMIN})
    @ApiOperation(value = "Delete schema", httpMethod = HttpMethod.DELETE)
    public Response delete(@PathParam("topicName") String qualifiedTopicName) {
        schemaService.deleteAllSchemaVersions(qualifiedTopicName);
        return Response.status(Response.Status.OK).build();
    }
}
