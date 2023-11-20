package pl.allegro.tech.hermes.integrationtests.setup;

public interface HermesTestApp {

    HermesTestApp start();

    void stop();

    int getPort();
}
