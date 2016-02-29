package pl.allegro.tech.hermes.domain.topic.schema;

import pl.allegro.tech.hermes.api.Topic;

import java.util.List;
import java.util.Optional;

abstract class AbstractSchemaVersionsRepository implements SchemaVersionsRepository {

    @Override
    public boolean schemaVersionExists(Topic topic, int version) {
        return versions(topic).map(x -> x.contains(version)).orElse(false);
    }

    @Override
    public Optional<Integer> latestSchemaVersion(Topic topic) {
        return versions(topic).filter(x -> !x.isEmpty()).map(x -> x.get(0));
    }

    abstract protected Optional<List<Integer>> versions(Topic topic);

}
