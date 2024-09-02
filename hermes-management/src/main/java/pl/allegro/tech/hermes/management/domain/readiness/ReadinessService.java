package pl.allegro.tech.hermes.management.domain.readiness;

import static pl.allegro.tech.hermes.api.DatacenterReadiness.ReadinessStatus.READY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import pl.allegro.tech.hermes.api.DatacenterReadiness;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;

public class ReadinessService {

  private final MultiDatacenterRepositoryCommandExecutor commandExecutor;
  private final DatacenterReadinessRepository readinessRepository;
  private final List<String> datacenters;

  public ReadinessService(
      MultiDatacenterRepositoryCommandExecutor commandExecutor,
      DatacenterReadinessRepository readinessRepository,
      List<String> datacenters) {
    this.commandExecutor = commandExecutor;
    this.readinessRepository = readinessRepository;
    this.datacenters = datacenters;
  }

  public void setReady(DatacenterReadiness datacenterReadiness) {
    Map<String, DatacenterReadiness> current = getReadiness();
    Map<String, DatacenterReadiness> toSave = new HashMap<>();
    for (String datacenter : datacenters) {
      toSave.put(datacenter, current.get(datacenter));
    }
    toSave.put(datacenterReadiness.getDatacenter(), datacenterReadiness);
    List<DatacenterReadiness> readiness =
        toSave.values().stream().filter(Objects::nonNull).toList();
    commandExecutor.execute(new SetReadinessCommand(readiness));
  }

  public List<DatacenterReadiness> getDatacentersReadiness() {
    Map<String, DatacenterReadiness> current = getReadiness();
    Map<String, DatacenterReadiness> result = new HashMap<>();
    for (String datacenter : datacenters) {
      DatacenterReadiness datacenterReadiness = current.get(datacenter);
      if (datacenterReadiness == null) {
        result.put(datacenter, new DatacenterReadiness(datacenter, READY));
      } else {
        result.put(datacenter, datacenterReadiness);
      }
    }
    return result.values().stream().toList();
  }

  private Map<String, DatacenterReadiness> getReadiness() {
    return readinessRepository.getReadiness().stream()
        .collect(Collectors.toMap(DatacenterReadiness::getDatacenter, Function.identity()));
  }
}
