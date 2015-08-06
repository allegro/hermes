package pl.allegro.tech.hermes.management.infrastructure.schema.schemarepo;

import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.infrastructure.schemarepo.SchemaRepoClient;
import pl.allegro.tech.hermes.infrastructure.schemarepo.SchemaRepoSchemaSourceProvider;
import pl.allegro.tech.hermes.management.domain.topic.schema.SchemaSourceRepository;

public class SchemaRepoSchemaSourceRepository extends SchemaRepoSchemaSourceProvider implements SchemaSourceRepository {

    public SchemaRepoSchemaSourceRepository(SchemaRepoClient schemaRepoClient) {
        super(schemaRepoClient);
    }

    @Override
    public void save(SchemaSource schemaSource, Topic topic) {
        String subjectName = topic.getQualifiedName();
        if (!schemaRepoClient.isSubjectRegistered(subjectName)) {
            schemaRepoClient.registerSubject(subjectName);
        }
        schemaRepoClient.registerSchema(subjectName, schemaSource.value());
    }

    @Override
    public void delete(Topic topic) {
        throw new UnsupportedOperationException("Deleting schemas is not supported by this repository");
    }
}
