package pl.allegro.tech.hermes.management.domain.maintainer;

import com.google.common.collect.ImmutableList;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Maintainer;

import java.util.List;

@Component
@Order(-1)
public class FakeMaintainerSource implements MaintainerSource {

    @Override
    public String name() {
        return "fake";
    }

    @Override
    public boolean exists(String maintainerId) {
        return maintainerId.equals("id-fake");
    }

    @Override
    public List<Maintainer> maintainersMatching(String searchString) {
        return ImmutableList.of(new Maintainer("id-fake", "Fake"));
    }

    @Override
    public Maintainer get(String id) {
        return new Maintainer("id-fake", "Fake");
    }

}
