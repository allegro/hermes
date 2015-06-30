package pl.allegro.tech.hermes.tracker.elasticsearch
import org.elasticsearch.client.Client
import org.elasticsearch.cluster.metadata.AliasMetaData
import org.elasticsearch.cluster.metadata.IndexMetaData
import org.elasticsearch.common.collect.ImmutableOpenMap
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.node.Node
import org.elasticsearch.node.NodeBuilder
import pl.allegro.tech.hermes.tracker.elasticsearch.consumers.ConsumersDailyIndexFactory
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendDailyIndexFactory
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.time.Clock

import static java.time.LocalDate.of
import static java.time.ZoneId.systemDefault
import static java.time.ZoneOffset.UTC

class SchemaManagerTest extends Specification {

    @Shared def clock = Clock.fixed(of(2000, 1, 1).atStartOfDay().toInstant(UTC), systemDefault())

    Client client
    SchemaManager schemaManager

    def setup() {
        Settings settings = ImmutableSettings.settingsBuilder()
                .put("path.data", Files.createTempDirectory("elasticsearch_data_").toFile())
                .put("cluster.name", "hermes").build()

        Node elastic = NodeBuilder.nodeBuilder().local(true).settings(settings).build()
        elastic.start()
        client = elastic.client()


        schemaManager = new SchemaManager(client, new FrontendDailyIndexFactory(clock), new ConsumersDailyIndexFactory(clock))
    }

    def "should create index if at least one does not exist"() {
        given:
        assert getIndices().isEmpty()

        when:
        schemaManager.ensureSchema()

        then:
        def indices = getIndices()
        indices.size() == 2
    }

    def "should create template that adds index to alias"() {
        given:
        schemaManager.ensureSchema()

        when:
        client.admin().indices()
                .prepareCreate(index)
                .execute().actionGet()

        then:
        getIndexAliases(index).containsKey(alias)

        where:
        index                           | alias
        "published_messages_2015_01_01" | SchemaManager.PUBLISHED_ALIAS_NAME
        "sent_messages_2015_01_01"      | SchemaManager.SENT_ALIAS_NAME
    }

    private ImmutableOpenMap<String, IndexMetaData> getIndices() {
        client.admin().cluster().prepareState().execute().actionGet().getState().getMetaData().getIndices()
    }

    private ImmutableOpenMap<String, AliasMetaData> getIndexAliases(String index) {
        client.admin().cluster().prepareState().execute().actionGet().getState().getMetaData().getIndices().get(index).getAliases()
    }
}
