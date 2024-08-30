package pl.allegro.tech.hermes.frontend.producer.kafka;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * The purpose of this job is to periodically refresh the cache in {@link
 * org.apache.kafka.clients.producer.KafkaProducer} that stores topic metadata. This is especially
 * important to avoid a cold start, i.e. when a new hermes-frontend instance is launched with the
 * cache being empty. Since the producer relies on topic metadata to send produce requests to Kafka,
 * if the cache is empty, the producer must load the metadata before sending the produce request.
 * Fetching the metadata might be costly, therefore we want to avoid passing on this cost to the
 * Hermes client.
 */
public class ProducerMetadataLoadingJob implements Runnable {

  private final List<KafkaMessageSenders> kafkaMessageSendersList;
  private final ScheduledExecutorService executorService;
  private final boolean enabled;
  private final Duration interval;

  private ScheduledFuture<?> job;

  public ProducerMetadataLoadingJob(
      List<KafkaMessageSenders> kafkaMessageSendersList, boolean enabled, Duration interval) {
    this.kafkaMessageSendersList = kafkaMessageSendersList;
    this.enabled = enabled;
    this.interval = interval;
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("TopicMetadataLoadingJob-%d").build();
    this.executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
  }

  @Override
  public void run() {
    refreshTopicMetadata();
  }

  public void start() {
    if (enabled) {
      refreshTopicMetadata();
      job =
          executorService.scheduleAtFixedRate(
              this, interval.toSeconds(), interval.toSeconds(), TimeUnit.SECONDS);
    }
  }

  public void stop() throws InterruptedException {
    if (enabled) {
      job.cancel(false);
      executorService.shutdown();
      executorService.awaitTermination(1, TimeUnit.MINUTES);
    }
  }

  private void refreshTopicMetadata() {
    for (KafkaMessageSenders kafkaMessageSenders : kafkaMessageSendersList) {
      kafkaMessageSenders.refreshTopicMetadata();
    }
  }
}
