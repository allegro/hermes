package pl.allegro.tech.hermes.management.api;

import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.topic.schema.SchemaSourceService;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("topics/{topicName}/schema")
public class SchemaEndpoint {

    private final SchemaSourceService schemaSourceService;

    @Autowired
    public SchemaEndpoint(SchemaSourceService schemaSourceService) {
        this.schemaSourceService = schemaSourceService;
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

    @POST
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Roles.GROUP_OWNER, Roles.ADMIN})
    @ApiOperation(value = "Save schema", httpMethod = HttpMethod.POST)
    public Response save(@PathParam("topicName") String qualifiedTopicName, String schema) {
        schemaSourceService.saveSchemaSource(qualifiedTopicName, schema);
        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @RolesAllowed({Roles.GROUP_OWNER, Roles.ADMIN})
    @ApiOperation(value = "Delete schema", httpMethod = HttpMethod.DELETE)
    public Response delete(@PathParam("topicName") String qualifiedTopicName) {
        schemaSourceService.deleteSchemaSource(qualifiedTopicName);
        return Response.status(Response.Status.OK).build();
    }
}
