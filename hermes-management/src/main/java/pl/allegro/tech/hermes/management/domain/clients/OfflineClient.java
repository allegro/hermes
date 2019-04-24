package pl.allegro.tech.hermes.management.domain.clients;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class OfflineClient {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final LocalDate lastAccess;
    private final String user;
    private final List<String> owners;

    @JsonCreator
    public OfflineClient(
            @JsonProperty("lastAccess") LocalDate lastAccess,
            @JsonProperty("user") String user,
            @JsonProperty("owners") List<String> owners) {
        this.lastAccess = lastAccess;
        this.user = user;
        this.owners = owners == null ? Collections.emptyList() : owners;
    }

    public LocalDate getLastAccess() {
        return lastAccess;
    }

    public String getUser() {
        return user;
    }

    public List<String> getOwners() {
        return owners;
    }
}
