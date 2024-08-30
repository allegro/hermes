package pl.allegro.tech.hermes.api.constraints;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pl.allegro.tech.hermes.api.OfflineRetransmissionRequest;

public class OneSourceRetransmissionValidator
    implements ConstraintValidator<OneSourceRetransmission, OfflineRetransmissionRequest> {

  public static final String EMPTY_STRING = "";

  @Override
  public boolean isValid(
      OfflineRetransmissionRequest offlineRetransmissionRequest,
      ConstraintValidatorContext context) {
    var sourceViewPath = offlineRetransmissionRequest.getSourceViewPath();
    var sourceTopic = offlineRetransmissionRequest.getSourceTopic();

    return (nonBlank(sourceViewPath.orElse(EMPTY_STRING)) && sourceTopic.isEmpty())
        || (nonBlank(sourceTopic.orElse(EMPTY_STRING)) && sourceViewPath.isEmpty());
  }

  private static boolean nonBlank(String value) {
    return value != null && !value.isBlank();
  }
}
