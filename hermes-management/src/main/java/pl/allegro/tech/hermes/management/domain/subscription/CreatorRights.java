package pl.allegro.tech.hermes.management.domain.subscription;

import pl.allegro.tech.hermes.api.Subscription;

public interface CreatorRights {

    boolean allowedToManage(Subscription subscription);

    boolean allowedToCreate(Subscription subscription);
}
