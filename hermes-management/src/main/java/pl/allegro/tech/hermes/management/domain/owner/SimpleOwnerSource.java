package pl.allegro.tech.hermes.management.domain.owner;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Owner;

@Component
@Order(SimpleOwnerSource.ORDER)
public class SimpleOwnerSource implements OwnerSource {

    public static final int ORDER = 0;
    public static final String NAME = "Simple";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean exists(String ownerId) {
        return true;
    }

    @Override
    public Owner get(String id) throws OwnerNotFound {
        return new Owner(id, id);
    }

}
