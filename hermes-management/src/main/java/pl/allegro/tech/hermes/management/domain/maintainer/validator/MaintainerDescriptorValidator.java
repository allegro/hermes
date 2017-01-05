package pl.allegro.tech.hermes.management.domain.maintainer.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.MaintainerDescriptor;
import pl.allegro.tech.hermes.management.domain.maintainer.MaintainerSource;
import pl.allegro.tech.hermes.management.domain.maintainer.MaintainerSources;

@Component
public class MaintainerDescriptorValidator {

    private final MaintainerSources maintainerSources;

    @Autowired
    public MaintainerDescriptorValidator(MaintainerSources maintainerSources) {
        this.maintainerSources = maintainerSources;
    }

    public void check(MaintainerDescriptor toCheck) {
        MaintainerSource source = maintainerSources.getByName(toCheck.getSource())
                .orElseThrow(() -> new MaintainerDescriptorValidationException("Maintainer source '" + toCheck.getSource() + "' doesn't exist"));

        if (!source.exists(toCheck.getId())) {
            throw new MaintainerDescriptorValidationException("Maintainer '" + toCheck.getId() + "' doesn't exist in source " + toCheck.getSource());
        }
    }
}
