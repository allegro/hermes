package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class DatacenterReadiness {
    private final String datacenter;
    private final boolean isReady;

    @JsonCreator
    public DatacenterReadiness(@JsonProperty("datacenter") String datacenter, @JsonProperty("isReady") boolean isReady) {
        this.datacenter = datacenter;
        this.isReady = isReady;
    }

    public String getDatacenter() {
        return datacenter;
    }

    @JsonProperty("isReady")
    public boolean isReady() {
        return isReady;
    }

    @Override
    public String toString() {
        return "DatacenterReadiness{" +
                "datacenter='" + datacenter + '\'' +
                ", isReady=" + isReady +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DatacenterReadiness)) return false;
        DatacenterReadiness that = (DatacenterReadiness) o;
        return isReady == that.isReady &&
                Objects.equals(datacenter, that.datacenter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(datacenter, isReady);
    }
}
