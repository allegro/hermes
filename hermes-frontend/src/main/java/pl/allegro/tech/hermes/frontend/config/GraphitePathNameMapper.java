package pl.allegro.tech.hermes.frontend.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;

/**
 * This metric name mapper allows to handle tags with manageable tree structure in graphite.
 * Placeholders in metric name (e.g. "ack-all.broker-latency.{topic}" are replaced with
 * tags values (matched by tags keys).
 * For example:
 * metric name: ack-all.broker-latency.{topic}
 * tags: {topic=SomeTopic}
 * final metric name in graphite: ack-all.broker-latency.SomeTopic
 */
public class GraphitePathNameMapper implements HierarchicalNameMapper {

    @Override
    public String toHierarchicalName(Meter.Id id, NamingConvention convention) {
        String name = id.getName();

        for (Tag tag : id.getTags()) {
            name = name.replace("{" + tag.getKey() + "}", tag.getValue());
        }

        if (name.contains("{") || name.contains("}")) {
            throw new IllegalArgumentException("Not all placeholders in metric name have corresponding tags! " +
                    "Metric name: " + id.getName() + ", after replacing with tags provided: " + name );
        }

        return name;
    }
}
