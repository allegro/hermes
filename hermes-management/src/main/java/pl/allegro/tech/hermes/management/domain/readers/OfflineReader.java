package pl.allegro.tech.hermes.management.domain.readers;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public class OfflineReader {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate lastAccess;
    private String user;

    public OfflineReader() {
    }

    public OfflineReader(LocalDate lastAccess, String user) {
        this.lastAccess = lastAccess;
        this.user = user;
    }

    public LocalDate getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(LocalDate lastAccess) {
        this.lastAccess = lastAccess;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
