package pl.allegro.tech.hermes.domain.readiness;

public interface DatacenterReadinessRepository extends ReadinessRepository {
    boolean datacenterMatches(String datacenter);
}
