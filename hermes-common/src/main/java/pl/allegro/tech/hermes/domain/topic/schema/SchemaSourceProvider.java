package pl.allegro.tech.hermes.domain.topic.schema;

import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;

import java.util.List;
import java.util.Optional;

public interface SchemaSourceProvider {

    Optional<SchemaSource> get(Topic topic);

    default Optional<SchemaSource> get(Topic topic, int version) {
        throw new UnsupportedOperationException("sry");
    }

    /**
     * @return a sorted list of versions in descending order.
     */
    default List<Integer> versions(Topic topic) {
        throw new UnsupportedOperationException("sorry dude");
    }
}
