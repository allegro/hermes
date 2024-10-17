package pl.allegro.tech.hermes.api.constraints;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pl.allegro.tech.hermes.api.OfflineRetransmissionRequest;

public class ProperTimeRangePresentForTopicRetransmissionValidator
    implements ConstraintValidator<
        ProperTimeRangePresentForTopicRetransmission, OfflineRetransmissionRequest> {

  @Override
  public boolean isValid(
      OfflineRetransmissionRequest offlineRetransmissionRequest,
      ConstraintValidatorContext context) {
    var sourceTopic = offlineRetransmissionRequest.getSourceTopic();
    var startTimestamp = offlineRetransmissionRequest.getStartTimestamp();
    var endTimestamp = offlineRetransmissionRequest.getEndTimestamp();

    if (sourceTopic.isEmpty()) {
      // skip validation for topic table retransmission
      return true;
    }
    if (startTimestamp.isEmpty() || endTimestamp.isEmpty()) {
      return false;
    }
    return startTimestamp.get().isBefore(endTimestamp.get());
  }
}
