package pl.allegro.tech.hermes.management.domain.readiness;

import java.util.List;
import pl.allegro.tech.hermes.api.DatacenterReadiness;

public interface DatacenterReadinessRepository {

  List<DatacenterReadiness> getReadiness();

  void setReadiness(List<DatacenterReadiness> datacenterReadiness);
}
