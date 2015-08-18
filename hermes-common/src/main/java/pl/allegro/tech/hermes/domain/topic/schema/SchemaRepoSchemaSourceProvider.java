package pl.allegro.tech.hermes.domain.topic.schema;

import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.infrastructure.schema.repo.SchemaRepoClient;

import javax.inject.Inject;
import java.util.Optional;

public class SchemaRepoSchemaSourceProvider implements SchemaSourceProvider {

    protected final SchemaRepoClient schemaRepoClient;

    @Inject
    public SchemaRepoSchemaSourceProvider(SchemaRepoClient schemaRepoClient) {
        this.schemaRepoClient = schemaRepoClient;
    }

    @Override
    public Optional<SchemaSource> get(Topic topic) {
        return schemaRepoClient.getLatestSchema(topic.getQualifiedName()).map(SchemaSource::valueOf);
    }
}
