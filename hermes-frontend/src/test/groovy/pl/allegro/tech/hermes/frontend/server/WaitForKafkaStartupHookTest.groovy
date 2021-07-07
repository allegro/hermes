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

    @Shared
    ConfigFactory config = Mock() {
        getLongProperty(Configs.FRONTEND_KAFKA_HEALTH_CHECK_INTERVAL) >> 1L
        getLongProperty(Configs.FRONTEND_KAFKA_HEALTH_CHECK_WAIT_TIMEOUT) >> 1000L
    }

    def "should wait for any metadata to be fetched successfully"() {
        given:
        TopicMetadataLoadingRunner loader = Mock()
        def hook = new WaitForKafkaStartupHook(new KafkaHealthChecker(loader, config))

        when:
        hook.accept(serviceLocator);

        then:
        10 * loader.refreshMetadata() >> [failureResult()]
        1 * loader.refreshMetadata() >> [successfulResult()]
    }

    def "should not wait if there are no topics"() {
        given:
        TopicMetadataLoadingRunner loader = Mock()
        def hook = new WaitForKafkaStartupHook(new KafkaHealthChecker(loader, config))

        when:
        hook.accept(serviceLocator);

        then:
        1 * loader.refreshMetadata() >> []
    }

    private static MetadataLoadingResult failureResult() {
        MetadataLoadingResult.failure(TopicName.fromQualifiedName("pl.allegro.test"))
    }

    private static MetadataLoadingResult successfulResult() {
        MetadataLoadingResult.success(TopicName.fromQualifiedName("pl.allegro.test"))
    }

}
