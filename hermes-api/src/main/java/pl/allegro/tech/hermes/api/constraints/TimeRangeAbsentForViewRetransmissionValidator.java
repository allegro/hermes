package pl.allegro.tech.hermes.api.constraints;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pl.allegro.tech.hermes.api.OfflineRetransmissionRequest;

public class TimeRangeAbsentForViewRetransmissionValidator
    implements ConstraintValidator<
        TimeRangeAbsentForViewRetransmission, OfflineRetransmissionRequest> {

  @Override
  public boolean isValid(
      OfflineRetransmissionRequest offlineRetransmissionRequest,
      ConstraintValidatorContext context) {
    var sourceViewPath = offlineRetransmissionRequest.getSourceViewPath();
    var startTimestamp = offlineRetransmissionRequest.getStartTimestamp();
    var endTimestamp = offlineRetransmissionRequest.getEndTimestamp();

    return sourceViewPath.isEmpty() // skip validation for view retransmission
        || (startTimestamp.isEmpty() && endTimestamp.isEmpty());
  }
}
