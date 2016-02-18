package pl.allegro.tech.hermes.infrastructure.schema.repo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.InvalidSchemaException;
import pl.allegro.tech.hermes.common.exception.SchemaRepoException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

public class JerseySchemaRepoClient implements SchemaRepoClient {

    private static final Logger logger = LoggerFactory.getLogger(JerseySchemaRepoClient.class);

    private final WebTarget target;

    public JerseySchemaRepoClient(Client client, URI schemaRepoServerUri) {
        this.target = client.target(schemaRepoServerUri);
    }

    @Override
    public void registerSubject(String subject) {
        Response response = target.path(subject).request().put(Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        if (SUCCESSFUL != response.getStatusInfo().getFamily()) {
            logger.error("Failure subject registration in schema repo. Subject: {}, response code: {}, details: {}",
                    subject, response.getStatus(), response.readEntity(String.class));
            throw new SchemaRepoException("Failure subject registration in schema-repo.");
        }
    }

    @Override
    public boolean isSubjectRegistered(String subject) {
        return target.path(subject).request().get().getStatus() == Response.Status.OK.getStatusCode();
    }

    @Override
    public void registerSchema(String subject, String schema) {
        Response response = target.path(subject).path("register").request().put(Entity.entity(schema, MediaType.TEXT_PLAIN));
        checkSchemaRegistration(response.getStatusInfo(), subject, response.readEntity(String.class));
    }

    private void checkSchemaRegistration(Response.StatusType statusType, String subject, String response) {
        switch (statusType.getFamily()) {
            case SUCCESSFUL:
                logger.info("Successful write to schema repo for subject {}", subject);
                break;
            case CLIENT_ERROR:
                logger.warn("Invalid schema for subject {}. Details: {}", subject, response);
                throw new InvalidSchemaException(String.format("Invalid schema. Reason: %s", response));
            case SERVER_ERROR:
                logger.error("Failure write to schema repo for subject {}. Reason: {}", subject, response);
                throw new SchemaRepoException("Failure writing to schema-repo.");
            default:
                logger.error("Unknown response from schema-repo. Subject {}, http status {}, Details: {}",
                        subject, statusType.getStatusCode(), response);
                throw new SchemaRepoException("Unknown response from schema-repo");
        }
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
