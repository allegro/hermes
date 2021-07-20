package pl.allegro.tech.hermes.domain.readiness;

public interface ReadinessRepository {
    boolean isReady();
    void setReadiness(boolean isReady);
}
