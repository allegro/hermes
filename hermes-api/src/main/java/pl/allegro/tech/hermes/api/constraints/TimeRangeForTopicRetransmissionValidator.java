package pl.allegro.tech.hermes.api.constraints;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pl.allegro.tech.hermes.api.OfflineRetransmissionFromTopicRequest;

public class TimeRangeForTopicRetransmissionValidator
    implements ConstraintValidator<
        TimeRangeForTopicRetransmission, OfflineRetransmissionFromTopicRequest> {

  @Override
  public boolean isValid(
      OfflineRetransmissionFromTopicRequest offlineRetransmissionRequest,
      ConstraintValidatorContext context) {
    var startTimestamp = offlineRetransmissionRequest.getStartTimestamp();
    var endTimestamp = offlineRetransmissionRequest.getEndTimestamp();

    if (startTimestamp == null || endTimestamp == null) {
      return false;
    }
    return startTimestamp.isBefore(endTimestamp);
  }
}
