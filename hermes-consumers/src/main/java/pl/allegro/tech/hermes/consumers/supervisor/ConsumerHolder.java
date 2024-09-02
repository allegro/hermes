package pl.allegro.tech.hermes.consumers.supervisor;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import java.util.Iterator;
import java.util.Optional;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;

public class ConsumerHolder implements Iterable<Consumer> {

  private Table<TopicName, String, Consumer> consumers = HashBasedTable.create();

  public synchronized void add(TopicName topicName, String subscriptionName, Consumer consumer) {
    consumers.put(topicName, subscriptionName, consumer);
  }

  public synchronized void remove(TopicName topicName, String subscriptionName) {
    consumers.remove(topicName, subscriptionName);
  }

  public synchronized Optional<Consumer> get(TopicName topicName, String subscriptionName) {
    return Optional.ofNullable(consumers.get(topicName, subscriptionName));
  }

  public synchronized boolean contains(TopicName topicName, String subscriptionName) {
    return consumers.contains(topicName, subscriptionName);
  }

  @Override
  public synchronized Iterator<Consumer> iterator() {
    return Lists.newArrayList(consumers.values()).iterator();
  }

  public synchronized void clear() {
    consumers.clear();
  }
}
