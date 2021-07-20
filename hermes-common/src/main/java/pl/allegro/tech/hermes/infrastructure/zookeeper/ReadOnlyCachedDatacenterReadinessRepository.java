package pl.allegro.tech.hermes.infrastructure.zookeeper;

import pl.allegro.tech.hermes.domain.readiness.ReadinessRepository;

public class ReadOnlyCachedDatacenterReadinessRepository implements ReadinessRepository {

    private final boolean isReady;
    public ReadOnlyCachedDatacenterReadinessRepository(ReadinessRepository readinessRepository) {
        this.isReady = readinessRepository.isReady();
    }

    @Override
    public boolean isReady() {
        return isReady;
    }

    @Override
    public void setReadiness(boolean isReady) {
    }
}
