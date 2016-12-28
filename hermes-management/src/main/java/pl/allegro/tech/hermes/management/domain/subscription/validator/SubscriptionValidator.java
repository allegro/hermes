package pl.allegro.tech.hermes.management.domain.subscription.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.management.domain.maintainer.MaintainerSource;
import pl.allegro.tech.hermes.management.domain.maintainer.MaintainerSources;

@Component
public class SubscriptionValidator {

    private final MaintainerSources maintainerSources;

    @Autowired
    public SubscriptionValidator(MaintainerSources maintainerSources) {
        this.maintainerSources = maintainerSources;
    }

    public void check(Subscription toCheck) {
        MaintainerSource source = maintainerSources.getByName(toCheck.getMaintainer().getSource())
                .orElseThrow(() -> new SubscriptionValidationException("Maintainer source '" + toCheck.getMaintainer().getSource() + "' doesn't exist"));

        if (!source.exists(toCheck.getMaintainer().getId())) {
            throw new SubscriptionValidationException("Maintainer '" + toCheck.getMaintainer().getId() +
                    "' doesn't exist in source " + toCheck.getMaintainer().getSource());
        }
    }

}
