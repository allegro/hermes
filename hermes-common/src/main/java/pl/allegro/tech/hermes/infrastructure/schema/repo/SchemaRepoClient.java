package pl.allegro.tech.hermes.infrastructure.schema.repo;

import java.util.List;
import java.util.Optional;

public interface SchemaRepoClient {

    void registerSubject(String subject);

    boolean isSubjectRegistered(String subject);

    void registerSchema(String subject, String schema);

    Optional<String> getLatestSchema(String subject);

    Optional<String> getSchema(String subject, int version);

    /**
     * @return a sorted list of versions in descending order.
     */
    List<Integer> getSchemaVersions(String subject);
}
