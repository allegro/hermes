package pl.allegro.tech.hermes.frontend.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.frontend.cache.topic.NotificationBasedTopicsCache;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.metric.ThroughputRegistry;
import pl.allegro.tech.hermes.frontend.validator.MessageValidators;
import pl.allegro.tech.hermes.frontend.validator.TopicMessageValidator;

@Configuration
public class FrontendConfiguration {

  @Bean
  public MessageValidators messageValidators(List<TopicMessageValidator> messageValidators) {
    return new MessageValidators(messageValidators);
  }

  @Bean(initMethod = "start")
  public TopicsCache notificationBasedTopicsCache(
      InternalNotificationsBus internalNotificationsBus,
      GroupRepository groupRepository,
      TopicRepository topicRepository,
      MetricsFacade metricsFacade,
      ThroughputRegistry throughputRegistry,
      KafkaNamesMapper kafkaNamesMapper) {

    return new NotificationBasedTopicsCache(
        internalNotificationsBus,
        groupRepository,
        topicRepository,
        metricsFacade,
        throughputRegistry,
        kafkaNamesMapper);
  }

  @Bean
  public String moduleName() {
    return "producer";
  }
}
