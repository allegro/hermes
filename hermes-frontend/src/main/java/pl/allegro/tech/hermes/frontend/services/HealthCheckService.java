package pl.allegro.tech.hermes.frontend.services;

import jakarta.inject.Singleton;

@Singleton
public class HealthCheckService {

    private volatile boolean shutdown = true;

    public boolean isShutdown() {
        return shutdown;
    }

    public void shutdown() {
        this.shutdown = true;
    }

    public void startup() {
        this.shutdown = false;
    }

}
