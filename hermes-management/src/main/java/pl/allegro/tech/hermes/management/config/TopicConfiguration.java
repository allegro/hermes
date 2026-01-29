package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.domain.group.GroupService;
import pl.allegro.tech.hermes.management.domain.topic.TopicOwnerCache;

@Configuration
@EnableConfigurationProperties(CacheProperties.class)
public class TopicConfiguration {

  @Bean
  public TopicOwnerCache topicOwnerCache(
      TopicRepository topicRepository,
      GroupService groupService,
      CacheProperties cacheProperties) {
    return new TopicOwnerCache(
        topicRepository, groupService, cacheProperties.getTopicOwnerRefreshRateInSeconds());
  }
}
