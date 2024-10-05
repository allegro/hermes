package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class DatacenterReadiness {
  private final String datacenter;
  private final ReadinessStatus status;

  @JsonCreator
  public DatacenterReadiness(
      @JsonProperty("datacenter") String datacenter,
      @JsonProperty("status") ReadinessStatus status) {
    this.datacenter = datacenter;
    this.status = status;
  }

  public String getDatacenter() {
    return datacenter;
  }

  public ReadinessStatus getStatus() {
    return status;
  }

  @Override
  public String toString() {
    return "DatacenterReadiness{" + "datacenter='" + datacenter + '\'' + ", status=" + status + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DatacenterReadiness)) {
      return false;
    }
    DatacenterReadiness that = (DatacenterReadiness) o;
    return status == that.status && Objects.equals(datacenter, that.datacenter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(datacenter, status);
  }

  public enum ReadinessStatus {
    READY,
    NOT_READY
  }
}
