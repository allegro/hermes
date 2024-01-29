package pl.allegro.tech.hermes.integrationtests.setup;

public interface HermesTestApp {

    HermesTestApp start();

    void stop();

    boolean shouldBeRestarted();

    void restoreDefaultSettings();

    int getPort();
}
