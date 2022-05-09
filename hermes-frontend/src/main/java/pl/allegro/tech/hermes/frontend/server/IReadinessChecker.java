package pl.allegro.tech.hermes.frontend.server;

public interface IReadinessChecker {

    boolean isReady();
    void start();
    void stop() throws InterruptedException;
}
