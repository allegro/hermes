package pl.allegro.tech.hermes.frontend.config;


import io.micrometer.core.instrument.dropwizard.DropwizardConfig;

public class FrontendDropwizardConfig implements DropwizardConfig {

    @Override
    public String prefix() {
        return "graphite";
    }

    @Override
    public String get(String key) {
        return null;
    }
}