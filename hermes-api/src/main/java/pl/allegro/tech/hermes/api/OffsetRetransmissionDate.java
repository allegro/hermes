package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public class OffsetRetransmissionDate {

    @NotNull
    private final OffsetDateTime retransmissionDate;

    public OffsetRetransmissionDate(@JsonProperty("retransmissionDate") OffsetDateTime retransmissionDate) {
        this.retransmissionDate = retransmissionDate;
    }

    public OffsetDateTime getRetransmissionDate() {
        return retransmissionDate;
    }
}
