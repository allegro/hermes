package pl.allegro.tech.hermes.management.domain.maintainer;

import com.google.common.collect.ImmutableList;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(-1)
public class FakeMaintainerSource implements MaintainerSource {

    @Override
    public String sourceName() {
        return "fake";
    }

    @Override
    public String nameForId(String maintainerId) {
        return maintainerId;
    }

    @Override
    public List<String> maintainersMatching(String searchString) {
        return ImmutableList.of("Fake");
    }

}
