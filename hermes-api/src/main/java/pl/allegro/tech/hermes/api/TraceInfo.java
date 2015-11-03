package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class TraceInfo {

    private final String traceId;

    @JsonCreator
    public TraceInfo(@JsonProperty("traceId") String traceId) {
        this.traceId = traceId;
    }

    public String getTraceId() {
        return traceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TraceInfo traceInfo = (TraceInfo) o;

        return !(traceId != null ? !traceId.equals(traceInfo.traceId) : traceInfo.traceId != null);

    }

    @Override
    public int hashCode() {
        return Objects.hash(traceId);
    }
}
