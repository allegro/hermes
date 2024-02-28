package pl.allegro.tech.hermes.management.domain.readiness;

import pl.allegro.tech.hermes.api.DatacenterReadiness;

import java.util.List;

public interface DatacenterReadinessRepository {

    List<DatacenterReadiness> getReadiness();

    void setReadiness(List<DatacenterReadiness> datacenterReadiness);
}
