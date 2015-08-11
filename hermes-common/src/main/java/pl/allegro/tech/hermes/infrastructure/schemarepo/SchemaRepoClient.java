package pl.allegro.tech.hermes.infrastructure.schemarepo;

import java.util.Optional;

public interface SchemaRepoClient {

    void registerSubject(String subject);

    boolean isSubjectRegistered(String subject);

    void registerSchema(String subject, String schema);

    Optional<String> getLatestSchema(String subject);
}
