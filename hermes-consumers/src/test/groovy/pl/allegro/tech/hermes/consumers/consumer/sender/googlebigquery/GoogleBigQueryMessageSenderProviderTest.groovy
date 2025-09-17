package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery

import com.google.api.gax.core.NoCredentialsProvider
import com.google.cloud.bigquery.storage.v1.BigQueryWriteSettings
import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.DeliveryType
import pl.allegro.tech.hermes.api.EndpointAddress
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.SubscriptionMode
import pl.allegro.tech.hermes.api.subscription.metrics.SubscriptionMetricsConfig
import pl.allegro.tech.hermes.consumers.consumer.ResilientMessageSender
import pl.allegro.tech.hermes.consumers.consumer.rate.SerialConsumerRateLimiter
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender
import pl.allegro.tech.hermes.consumers.consumer.sender.SingleRecipientMessageSenderAdapter
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.GoogleBigQueryAvroDataWriterPool
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.GoogleBigQueryAvroMessageTransformer
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.GoogleBigQueryAvroStreamWriterFactory
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.GoogleBigQueryAvroToProtoConverter
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json.GoogleBigQueryJsonDataWriterPool
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json.GoogleBigQueryJsonMessageTransformer
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json.GoogleBigQueryJsonSender
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json.GoogleBigQueryJsonStreamWriterFactory
import pl.allegro.tech.hermes.domain.CredentialsRepository
import spock.lang.Specification

class GoogleBigQueryMessageSenderProviderTest extends Specification {
    def 'should create sender provider'() {
        given:
        GoogleBigQueryMessageSenderProvider provider = new GoogleBigQueryMessageSenderProvider(
                Mock(GoogleBigQuerySenderTargetResolver),
                Mock(GoogleBigQueryJsonMessageTransformer),
                Mock(GoogleBigQueryAvroMessageTransformer),
                Mock(GoogleBigQueryJsonDataWriterPool),
                Mock(GoogleBigQueryAvroDataWriterPool)
        )

        EndpointAddress endpointAddress = EndpointAddress.of("googlebigquery://projects/project/datasets/dataset/tables/table")
        Subscription subscription = Subscription.create("pl.allegro.group.topicname",
                "subscription",
                endpointAddress,
                Subscription.State.PENDING,
                "test",
                Map.of(),
                false,
                null,
                null,
                null,
                contentType,
                DeliveryType.SERIAL,
                [],
                SubscriptionMode.ANYCAST,
                [],
                null,
                null,
                false,
                false,
                0,
                false,
                false,
                SubscriptionMetricsConfig.DISABLED
        )

        ResilientMessageSender resilientMessageSender = Mock(ResilientMessageSender)

        when:

        MessageSender sender = provider.create(subscription, resilientMessageSender)


        then:
        (sender as SingleRecipientMessageSenderAdapter).getAdaptee().getClass().name == adapteeClassName

        where:
        contentType || adapteeClassName
        ContentType.JSON    || "pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json.GoogleBigQueryJsonSender"
        ContentType.AVRO    || "pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.GoogleBigQueryAvroSender"
    }
}
