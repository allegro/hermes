package pl.allegro.tech.hermes.frontend.server;

public interface ReadinessChecker {

    boolean isReady();

    void start();

    void stop() throws InterruptedException;
}
