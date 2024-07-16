package pl.allegro.tech.hermes.api.constraints;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pl.allegro.tech.hermes.api.OfflineRetransmissionRequest;

public class OneSourceRetransmissionValidator implements ConstraintValidator<OneSourceRetransmission, OfflineRetransmissionRequest> {

    @Override
    public void initialize(OneSourceRetransmission oneSourceRetransmission) {
    }

    @Override
    public boolean isValid(OfflineRetransmissionRequest offlineRetransmissionRequest, ConstraintValidatorContext context) {
        var sourceViewPath = offlineRetransmissionRequest.getSourceViewPath();
        var sourceTopic = offlineRetransmissionRequest.getSourceTopic();

        return (nonBlank(sourceViewPath) && sourceTopic == null) || (nonBlank(sourceTopic) && sourceViewPath == null);
    }

    private static boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

}
