package pl.allegro.tech.hermes.schema;

import java.util.List;

import static java.util.Collections.emptyList;

public class SchemaVersionsResponse {
    private final Status status;
    private final List<SchemaVersion> versions;

    public static SchemaVersionsResponse success(List<SchemaVersion> versions) {
        return new SchemaVersionsResponse(Status.SUCCESS, versions);
    }

    public static SchemaVersionsResponse failure() {
        return new SchemaVersionsResponse(Status.FAILURE, emptyList());
    }

    SchemaVersionsResponse(Status status, List<SchemaVersion> versions) {
        this.status = status;
        this.versions = versions;
    }

    public List<SchemaVersion> get() {
        return versions;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isFailure() {
        return status == Status.FAILURE;
    }

    public boolean versionExists(SchemaVersion version) {
        return versions.contains(version);
    }

    enum Status {
        SUCCESS,
        FAILURE
    }
}
