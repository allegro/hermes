package pl.allegro.tech.hermes.management.domain.subscription.consumergroup;

import static java.lang.String.format;
import static pl.allegro.tech.hermes.api.ErrorCode.OTHER;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class ConsumerGroupToDeleteNotFoundException extends ManagementException {

  public ConsumerGroupToDeleteNotFoundException(String path) {
    super(format("Consumer group to delete does not exist, for path %s ", path));
  }

  @Override
  public ErrorCode getCode() {
    return OTHER;
  }
}
