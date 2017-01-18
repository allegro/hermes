package pl.allegro.tech.hermes.management.domain.maintainer;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Maintainer;

@Component
@Order(0)
public class SimpleMaintainerSource implements MaintainerSource {

    public static final String NAME = "Simple";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean exists(String maintainerId) {
        return true;
    }

    @Override
    public Maintainer get(String id) throws MaintainerNotFound {
        return new Maintainer(id, id);
    }

}
