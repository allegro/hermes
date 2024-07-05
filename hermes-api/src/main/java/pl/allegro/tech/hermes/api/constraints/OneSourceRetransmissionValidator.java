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
        var sourceView = offlineRetransmissionRequest.getSourceView();
        var sourceTopic = offlineRetransmissionRequest.getSourceTopic();

       return  !(sourceView == null || sourceTopic == null) || !(nonBlank(sourceView) || nonBlank(sourceTopic));
    }

    private static boolean nonBlank(String sourceView) {
        return sourceView != null && !sourceView.isBlank();
    }
}
