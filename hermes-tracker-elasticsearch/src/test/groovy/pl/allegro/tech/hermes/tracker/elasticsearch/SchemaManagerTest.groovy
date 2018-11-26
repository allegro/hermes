package pl.allegro.tech.hermes.tracker.elasticsearch

import org.junit.ClassRule
import pl.allegro.tech.hermes.tracker.elasticsearch.consumers.ConsumersDailyIndexFactory
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendDailyIndexFactory
import spock.lang.Shared
import spock.lang.Specification

import java.time.Clock

import static java.time.LocalDate.of
import static java.time.ZoneId.systemDefault
import static java.time.ZoneOffset.UTC

class SchemaManagerTest extends Specification {

    @Shared
    def clock = Clock.fixed(of(2000, 1, 1).atStartOfDay().toInstant(UTC), systemDefault())

    @ClassRule
    @Shared
    ElasticsearchResource elasticsearch = new ElasticsearchResource()

    SchemaManager schemaManager

    def setup() {
        elasticsearch.cleanStructures()
        schemaManager = new SchemaManager(
                elasticsearch.client(),
                new FrontendDailyIndexFactory(clock),
                new ConsumersDailyIndexFactory(clock))
    }

    def "should create index if at least one does not exist"() {
        given:
        assert elasticsearch.getIndices().isEmpty()

        when:
        schemaManager.ensureSchema()

        then:
        def indices = elasticsearch.getIndices()
        indices.size() == 2
    }

    def "should create template that adds index to alias"() {
        given:
        schemaManager.ensureSchema()

        when:
        elasticsearch.adminClient()
                .indices()
                .prepareCreate(index)
                .execute().actionGet()

        then:
        elasticsearch.getIndices().get(index).getAliases().containsKey(alias)

        where:
        index                           | alias
        "published_messages_2015_01_01" | SchemaManager.PUBLISHED_ALIAS_NAME
        "sent_messages_2015_01_01"      | SchemaManager.SENT_ALIAS_NAME
    }

}
