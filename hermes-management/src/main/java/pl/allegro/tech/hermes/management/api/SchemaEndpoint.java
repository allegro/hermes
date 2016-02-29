package pl.allegro.tech.hermes.management.api;

import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.domain.topic.schema.SchemaSourceService;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

@Path("topics/{topicName}/schema")
public class SchemaEndpoint {

    private final SchemaSourceService schemaSourceService;
    private final TopicService topicService;

    @Autowired
    public SchemaEndpoint(SchemaSourceService schemaSourceService, TopicService topicService) {
        this.schemaSourceService = schemaSourceService;
        this.topicService = topicService;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get schema", httpMethod = HttpMethod.GET)
    public Response get(@PathParam("topicName") String qualifiedTopicName) {
        Optional<SchemaSource> schemaSource = schemaSourceService.getSchemaSource(qualifiedTopicName);
        return schemaSource.map(SchemaSource::value)
                .map(v -> Response.ok(v).build())
                .orElse(Response.noContent().build());
    }

    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get schema", httpMethod = HttpMethod.GET)
    public Response get(@PathParam("topicName") String qualifiedTopicName, int version) {
        Optional<SchemaSource> schemaSource = schemaSourceService.getSchemaSource(qualifiedTopicName, version);
        return schemaSource.map(SchemaSource::value)
                .map(v -> Response.ok(v).build())
                .orElse(Response.noContent().build());
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Roles.GROUP_OWNER, Roles.ADMIN})
    @ApiOperation(value = "Save schema", httpMethod = HttpMethod.POST)
    public Response save(@PathParam("topicName") String qualifiedTopicName,
                         @DefaultValue("true") @QueryParam(value = "validate") boolean validate,
                         String schema) {
        schemaSourceService.saveSchemaSource(qualifiedTopicName, schema, validate);
        notifyFrontendSchemaChanged(qualifiedTopicName);
        return Response.status(Response.Status.CREATED).build();
    }

    private void notifyFrontendSchemaChanged(String qualifiedTopicName) {
        topicService.touchTopic(fromQualifiedName(qualifiedTopicName));
    }

    @DELETE
    @RolesAllowed({Roles.GROUP_OWNER, Roles.ADMIN})
    @ApiOperation(value = "Delete schema", httpMethod = HttpMethod.DELETE)
    public Response delete(@PathParam("topicName") String qualifiedTopicName) {
        schemaSourceService.deleteSchemaSource(qualifiedTopicName);
        return Response.status(Response.Status.OK).build();
    }
}
