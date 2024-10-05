package pl.allegro.tech.hermes.management.infrastructure.kafka;

import static java.lang.String.format;
import static pl.allegro.tech.hermes.api.ErrorCode.BROKERS_CLUSTER_NOT_FOUND_EXCEPTION;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class BrokersClusterNotFoundException extends ManagementException {

  public BrokersClusterNotFoundException(String clusterName) {
    super(format("Brokers cluster with name %s not found", clusterName));
  }

  @Override
  public ErrorCode getCode() {
    return BROKERS_CLUSTER_NOT_FOUND_EXCEPTION;
  }
}
