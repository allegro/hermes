package pl.allegro.tech.hermes.schema;

import static java.util.Collections.emptyList;

import java.util.List;

public class SchemaVersionsResult {
  private final Status status;
  private final List<SchemaVersion> versions;

  public static SchemaVersionsResult succeeded(List<SchemaVersion> versions) {
    return new SchemaVersionsResult(Status.SUCCESS, versions);
  }

  public static SchemaVersionsResult failed() {
    return new SchemaVersionsResult(Status.FAILURE, emptyList());
  }

  private SchemaVersionsResult(Status status, List<SchemaVersion> versions) {
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
