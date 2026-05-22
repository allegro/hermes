package pl.allegro.tech.hermes.management.domain.owner.validator;

import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSource;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSources;

public class OwnerIdValidator {

  private final OwnerSources ownerSources;

  public OwnerIdValidator(OwnerSources ownerSources) {
    this.ownerSources = ownerSources;
  }

  public void check(OwnerId toCheck) {
    OwnerSource source =
        ownerSources
            .getByName(toCheck.getSource())
            .orElseThrow(
                () ->
                    new OwnerIdValidationException(
                        "Owner source '" + toCheck.getSource() + "' doesn't exist"));

    if (!source.exists(toCheck.getId())) {
      throw new OwnerIdValidationException(
          "Owner '" + toCheck.getId() + "' doesn't exist in source " + toCheck.getSource());
    }
  }
}
