package pl.allegro.tech.hermes.api.constraints;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pl.allegro.tech.hermes.api.OfflineRetransmissionRequest;

public class TimeRangeForTopicRetransmissionValidator
    implements ConstraintValidator<TimeRangeForTopicRetransmission, OfflineRetransmissionRequest> {

  @Override
  public boolean isValid(
      OfflineRetransmissionRequest offlineRetransmissionRequest,
      ConstraintValidatorContext context) {
    var sourceTopic = offlineRetransmissionRequest.getSourceTopic();
    var startTimestamp = offlineRetransmissionRequest.getStartTimestamp();
    var endTimestamp = offlineRetransmissionRequest.getEndTimestamp();

    if (sourceTopic.isEmpty()) {
      // skip validation for non-topic retransmission
      return true;
    }
    if (startTimestamp.isEmpty() || endTimestamp.isEmpty()) {
      return false;
    }
    return startTimestamp.get().isBefore(endTimestamp.get());
  }
}
