package pl.allegro.tech.hermes.infrastructure.zookeeper.counter;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

@SuppressWarnings("serial")
public class ZookeeperCounterException extends HermesException {

  public ZookeeperCounterException(String key, String message) {
    super("Exception while trying to access counter " + key + " via Zookeeper. " + message);
  }

  public ZookeeperCounterException(String key, Throwable cause) {
    super("Exception while trying to access counter " + key + " via Zookeeper.", cause);
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.INTERNAL_ERROR;
  }
}
