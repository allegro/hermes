package pl.allegro.tech.hermes.management.domain.readiness;

public interface ReadinessRepository {

    boolean isReady();

    void setReadiness(boolean isReady);
}
