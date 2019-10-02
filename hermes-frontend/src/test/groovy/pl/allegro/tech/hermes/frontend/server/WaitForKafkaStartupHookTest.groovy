package pl.allegro.tech.hermes.frontend.server

import org.glassfish.hk2.api.ServiceLocator
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.common.config.ConfigFactory
import pl.allegro.tech.hermes.common.config.Configs
import spock.lang.Shared
import spock.lang.Specification

class WaitForKafkaStartupHookTest extends Specification {

    @Shared
    ServiceLocator serviceLocator = Mock()

    def "should wait for any metadata to be fetched successfully"() {
        given:
        ConfigFactory config = Mock() {
            getLongProperty(Configs.FRONTEND_STARTUP_WAIT_KAFKA_INTERVAL) >> 1L
        }
        TopicMetadataLoadingRunner loader = Mock()

        def hook = new WaitForKafkaStartupHook(loader, config)

        when:
        hook.accept(serviceLocator);

        then:
        10 * loader.refreshMetadata() >> [failureResult()]
        1 * loader.refreshMetadata() >> [successfulResult()]
    }

    private static MetadataLoadingResult failureResult() {
        MetadataLoadingResult.failure(TopicName.fromQualifiedName("pl.allegro.test"))
    }

    private static MetadataLoadingResult successfulResult() {
        MetadataLoadingResult.success(TopicName.fromQualifiedName("pl.allegro.test"))
    }

}
