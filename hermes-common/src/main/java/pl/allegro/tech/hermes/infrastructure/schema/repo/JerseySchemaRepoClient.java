package pl.allegro.tech.hermes.infrastructure.schema.repo;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;

public class JerseySchemaRepoClient implements SchemaRepoClient {

    private final WebTarget target;

    public JerseySchemaRepoClient(Client client, URI schemaRepoServerUri) {
        this.target = client.target(schemaRepoServerUri);
    }

    @Override
    public void registerSubject(String subject) {
        target.path(subject).request().put(Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED_TYPE));
    }

    @Override
    public boolean isSubjectRegistered(String subject) {
        return target.path(subject).request().get().getStatus() == Response.Status.OK.getStatusCode();
    }

    @Override
    public void registerSchema(String subject, String schema) {
        target.path(subject).path("register").request().put(Entity.entity(schema, MediaType.TEXT_PLAIN));
    }

    @Override
    public Optional<String> getLatestSchema(String subject) {
        Response response = target.path(subject).path("latest").request().get();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            String schema = parseSchema(response.readEntity(String.class));
            return Optional.of(schema);
        }
        return Optional.empty();
    }

    private String parseSchema(String schemaResponse) {
        return schemaResponse.substring(1 + schemaResponse.indexOf('\t'));
    }
}
