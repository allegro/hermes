package pl.allegro.tech.hermes.domain.subscription

import com.fasterxml.jackson.databind.ObjectMapper
import pl.allegro.tech.hermes.api.DeliveryType
import pl.allegro.tech.hermes.api.Subscription
import spock.lang.Specification

class DeliveryTypeMigrationTest extends Specification {

    ObjectMapper mapper = new ObjectMapper();

    def "should migrate old subscriptions to SERIAL delivery type"() {
        given:
        def oldSubscription = '''
        {
          "topicName": "pl.allegro.tech.hermes.monitor",
          "name": "pl.allegro.tech.hermes.monitor.consumer1",
          "endpoint": "service:\\/\\/hermes-monitor\\/monitor\\/consumer1",
          "description": "Hermes monitor",
          "subscriptionPolicy": {
            "rate": 2000,
            "messageTtl": 10,
            "retryClientErrors": false,
            "messageBackoff": 100
          },
          "trackingEnabled": false,
          "supportTeam": "Scrum Team Skylab",
          "contact": "scrum-skylab@allegro.pl",
          "contentType": "JSON",
          "state": "ACTIVE"
        }
        '''

        Subscription subscription = mapper.readValue(oldSubscription, Subscription.class)

        when:
        def migrated = DeliveryTypeMigration.migrate(oldSubscription.bytes, subscription, mapper)

        then:
        migrated.name == subscription.name
        migrated.topicName == subscription.topicName
        migrated.endpoint == subscription.endpoint
        migrated.description == subscription.description
        migrated.contentType == subscription.contentType
        migrated.deliveryType == DeliveryType.SERIAL
        migrated.serialSubscriptionPolicy.rate == 2000
        migrated.serialSubscriptionPolicy.messageTtl == 10
        migrated.serialSubscriptionPolicy.messageBackoff == 100
        !migrated.serialSubscriptionPolicy.isRetryClientErrors()
    }

    def "should not touch subscriptions with delivery type"() {
        given:
        def oldSubscription = '''
        {
          "topicName": "pl.allegro.tech.hermes.monitor",
          "name": "pl.allegro.tech.hermes.monitor.consumer1",
          "endpoint": "service:\\/\\/hermes-monitor\\/monitor\\/consumer1",
          "description": "Hermes monitor",
          "deliveryType": "BATCH",
          "subscriptionPolicy": {
            "batchSize": 2000,
            "batchTime": 10,
            "retryClientErrors": false,
            "batchVolume": 100
          },
          "trackingEnabled": false,
          "supportTeam": "Scrum Team Skylab",
          "contact": "scrum-skylab@allegro.pl",
          "contentType": "JSON",
          "state": "ACTIVE"
        }
        '''

        Subscription subscription = mapper.readValue(oldSubscription, Subscription.class)

        when:
        def migrated = DeliveryTypeMigration.migrate(oldSubscription.bytes, subscription, mapper)

        then:
        subscription == migrated
    }

}
