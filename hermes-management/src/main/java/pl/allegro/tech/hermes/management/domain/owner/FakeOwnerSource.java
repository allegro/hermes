package pl.allegro.tech.hermes.management.domain.owner;

import com.google.common.collect.ImmutableList;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Owner;

import java.util.List;

/**
 * Temporary implementation for human-testing.
 *
 * TODO: remove before merging the feature branch.
 */
@Component
@Order(-1)
public class FakeOwnerSource implements HintingOwnerSource {

    @Override
    public String name() {
        return "fake";
    }

    @Override
    public boolean exists(String ownerId) {
        return ownerId.equals("id-fake");
    }

    @Override
    public Owner get(String id) {
        return new Owner("id-fake", "Fake");
    }

    @Override
    public List<Owner> ownersMatching(String searchString) {
        return ImmutableList.of(get(""));
    }
}
