package pl.allegro.tech.hermes.frontend.server

import org.glassfish.hk2.api.ServiceLocator
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.common.config.ConfigFactory
import pl.allegro.tech.hermes.common.config.Configs
import spock.lang.Shared
import spock.lang.Specification

class WaitOnKafkaStartupHookTest extends Specification {

    @Shared
    ServiceLocator serviceLocator = Mock()

    def "should wait for any metadata to be fetched successfully"() {
        given:
        ConfigFactory config = Mock() {
            getIntProperty(Configs.FRONTEND_STARTUP_WAIT_KAFKA_RETRIES) >> 10
            getLongProperty(Configs.FRONTEND_STARTUP_WAIT_KAFKA_INTERVAL) >> 1L
        }
        TopicMetadataLoadingRunner loader = Mock()

        def hook = new WaitOnKafkaStartupHook(loader, config)

        when:
        hook.accept(serviceLocator);

        then:
        11 * loader.refreshMetadata() >> [failureResult()]

        when:
        hook.accept(serviceLocator)

        then:
        1 * loader.refreshMetadata() >> [successfulResult()]
    }

    private static MetadataLoadingResult failureResult() {
        MetadataLoadingResult.failure(TopicName.fromQualifiedName("pl.allegro.test"))
    }

    private static MetadataLoadingResult successfulResult() {
        MetadataLoadingResult.success(TopicName.fromQualifiedName("pl.allegro.test"))
    }

}
