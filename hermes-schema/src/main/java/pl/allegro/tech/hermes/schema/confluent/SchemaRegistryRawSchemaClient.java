package pl.allegro.tech.hermes.schema.confluent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.schema.CouldNotRemoveSchemaException;
import pl.allegro.tech.hermes.schema.InvalidSchemaException;
import pl.allegro.tech.hermes.schema.SchemaRepositoryServerException;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This implementation of RawSchemaClient is compatible with Confluent Schema Registry API
 * except for the deleteAllSchemaVersions, which is basically not supported by the Confluent project
 */
public class SchemaRegistryRawSchemaClient implements RawSchemaClient {

    private static final Logger logger = LoggerFactory.getLogger(SchemaRegistryRawSchemaClient.class);

    private static final String SCHEMA_REPO_CONTENT_TYPE = "application/vnd.schemaregistry.v1+json";

    private final WebTarget target;

    public SchemaRegistryRawSchemaClient(Client client, URI schemaRegistryUri) {
        this.target = client.target(schemaRegistryUri);
    }

    @Override
    public Optional<RawSchema> getSchema(TopicName topic, SchemaVersion version) {
        Response response = target.path("subjects")
                .path(topic.qualifiedName())
                .path("versions")
                .path(Integer.toString(version.value()))
                .request()
                .get();
        return extractSchema(topic.qualifiedName(), response).map(RawSchema::valueOf);
    }

    @Override
    public Optional<RawSchema> getLatestSchema(TopicName topic) {
        Response response = target.path("subjects")
                .path(topic.qualifiedName())
                .path("versions")
                .path("latest")
                .request()
                .get();
        return extractSchema(topic.qualifiedName(), response).map(RawSchema::valueOf);
    }

    private Optional<String> extractSchema(String subject, Response response) {
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            String schema = response.readEntity(SchemaRegistryResponse.class).getSchema();
            return Optional.of(schema);
        } else {
            logger.error("Could not find schema for subject {}, reason: {}", subject, response.getStatus());
            return Optional.empty();
        }
    }

    @Override
    public List<SchemaVersion> getVersions(TopicName topic) {
        Response response = target.path("subjects")
                .path(topic.qualifiedName())
                .path("versions")
                .request()
                .get();
        return extractSchemaVersions(topic.qualifiedName(), response);
    }

    private List<SchemaVersion> extractSchemaVersions(String subject, Response response) {
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return Arrays.stream(response.readEntity(Integer[].class))
                    .sorted(Comparator.reverseOrder())
                    .map(SchemaVersion::valueOf)
                    .collect(Collectors.toList());
        } else {
            logger.error("Could not find schema versions for subject {}, reason: {}", subject, response.getStatus());
            return Collections.emptyList();
        }
    }

    @Override
    public void registerSchema(TopicName topic, RawSchema rawSchema) {
        Response response = target.path("subjects")
                .path(topic.qualifiedName())
                .path("versions")
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(SchemaRegistryRequest.fromRawSchema(rawSchema), SCHEMA_REPO_CONTENT_TYPE));
        checkSchemaRegistration(response.getStatusInfo(), topic.qualifiedName(), response.readEntity(String.class));
    }

    private void checkSchemaRegistration(Response.StatusType statusType, String topicName, String response) {
        switch (statusType.getFamily()) {
            case SUCCESSFUL:
                logger.info("Successful write to schema registry for subject {}", topicName);
                break;
            case CLIENT_ERROR:
                logger.warn("Invalid schema for subject {}. Reason: {}", topicName, response);
                throw new InvalidSchemaException("Invalid schema. Reason: " + response);
            case SERVER_ERROR:
                logger.error("Failure write to schema repo for subject {}. Reason: {}", topicName, response);
                throw new SchemaRepositoryServerException("Failure writing to schema registry. Reason: " + response);
            default:
                logger.error("Unknown response from schema registry. Subject {}, HTTP status {}, Details: {}",
                        topicName, statusType.getStatusCode(), response);
                throw new SchemaRepositoryServerException("Unknown response from schema registry. Reason: " + response);
        }
    }

    @Override
    public void deleteAllSchemaVersions(TopicName topic) {
        Response response = target.path("subjects")
                .path(topic.qualifiedName())
                .path("versions")
                .request()
                .delete();
        checkSchemaRemoval(response.getStatusInfo(), topic.qualifiedName(), response.readEntity(String.class));
    }

    private void checkSchemaRemoval(Response.StatusType statusType, String topicName, String response) {
        switch (statusType.getFamily()) {
            case SUCCESSFUL:
                logger.info("Successful removed schema subject {}", topicName);
                break;
            case CLIENT_ERROR:
            case SERVER_ERROR:
            default:
                logger.warn("Could not remove schema subject {}. Reason: {}", topicName, response);
                throw new CouldNotRemoveSchemaException("Could not remove schema subject. Reason: " + response);
        }
    }
}
