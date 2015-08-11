package pl.allegro.tech.hermes.management.api;

import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.domain.topic.schema.SchemaSourceRepository;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

@Path("topics/{topicName}/schema")
public class SchemaEndpoint {

    private final TopicService topicService;
    private final SchemaSourceRepository schemaSourceRepository;

    @Autowired
    public SchemaEndpoint(TopicService topicService, SchemaSourceRepository schemaSourceRepository) {
        this.topicService = topicService;
        this.schemaSourceRepository = schemaSourceRepository;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get schema", httpMethod = HttpMethod.GET)
    public Response get(@PathParam("topicName") String qualifiedTopicName) {
        Topic topic = topicService.getTopicDetails(fromQualifiedName(qualifiedTopicName));
        Optional<SchemaSource> schemaSource = schemaSourceRepository.get(topic);
        return schemaSource.map(SchemaSource::value).map(v -> Response.ok(v).build()).orElse(Response.noContent().build());
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Roles.GROUP_OWNER, Roles.ADMIN})
    @ApiOperation(value = "Save schema", httpMethod = HttpMethod.POST)
    public Response save(@PathParam("topicName") String qualifiedTopicName, String schema) {
        Topic topic = topicService.getTopicDetails(fromQualifiedName(qualifiedTopicName));

        if (isNullOrEmpty(schema)) {
            schemaSourceRepository.delete(topic);
        } else {
            schemaSourceRepository.save(SchemaSource.valueOf(schema), topic);
        }
        return responseStatus(Response.Status.CREATED);
    }

    private Response responseStatus(Response.Status responseStatus) {
        return Response.status(responseStatus).build();
    }

}
