package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TopicOfflineStorage {

    private final boolean store;

    private final int retention;

    @JsonCreator
    public TopicOfflineStorage(@JsonProperty("store") boolean store, @JsonProperty("retention") int retention) {
        this.store = store;
        this.retention = retention;
    }

    public boolean isStore() {
        return store;
    }

    public int getRetention() {
        return retention;
    }
}
