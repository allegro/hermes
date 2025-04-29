package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import pl.allegro.tech.hermes.api.OfflineRetransmissionRequest.RetransmissionType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OfflineRetransmissionTaskMonitoringInfo(
    @JsonProperty("type") RetransmissionType type,
    @JsonProperty("taskId") String taskId,
    @JsonProperty("logsUrl") String logsUrl,
    @JsonProperty("metricsUrl") String metricsUrl,
    @JsonProperty("jobDetailsUrl") String jobDetailsUrl) {}
