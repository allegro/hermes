package pl.allegro.tech.hermes.management.domain.clients;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class OfflineClient {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final LocalDate lastAccess;
    private final String user;

    @JsonCreator
    public OfflineClient(
            @JsonProperty("lastAccess") LocalDate lastAccess,
            @JsonProperty("user") String user) {
        this.lastAccess = lastAccess;
        this.user = user;
    }

    public LocalDate getLastAccess() {
        return lastAccess;
    }

    public String getUser() {
        return user;
    }
}
