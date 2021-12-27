package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import pl.allegro.tech.hermes.api.jackson.OffsetDateTimeSerializer;

import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public class OffsetRetransmissionDate {

    @NotNull
    private final OffsetDateTime retransmissionDate;

    public OffsetRetransmissionDate(@JsonProperty("retransmissionDate") OffsetDateTime retransmissionDate) {
        this.retransmissionDate = retransmissionDate;
    }

    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    public OffsetDateTime getRetransmissionDate() {
        return retransmissionDate;
    }
}
