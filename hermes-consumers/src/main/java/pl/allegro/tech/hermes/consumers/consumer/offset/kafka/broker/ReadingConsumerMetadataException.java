package pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class ReadingConsumerMetadataException extends HermesException {

  public ReadingConsumerMetadataException(int errorCode) {
    super(String.format("Cannot read consumer metadata, response error code: %s.", errorCode));
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.INTERNAL_ERROR;
  }
}
