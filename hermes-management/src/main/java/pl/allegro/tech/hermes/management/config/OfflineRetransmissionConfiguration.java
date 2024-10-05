package pl.allegro.tech.hermes.management.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.retransmit.DcAwareOfflineRetransmissionRepository;
import pl.allegro.tech.hermes.management.domain.retransmit.OfflineRetransmissionRepository;
import pl.allegro.tech.hermes.management.domain.retransmit.OfflineRetransmissionService;

@Configuration
public class OfflineRetransmissionConfiguration {
  @Bean
  @Qualifier("dcAwareOfflineRetransmissionRepository")
  OfflineRetransmissionRepository dcAwareOfflineRetransmissionRepository(
      MultiDatacenterRepositoryCommandExecutor commandExecutor,
      @Qualifier("zookeeperOfflineRetransmissionRepository")
          OfflineRetransmissionRepository offlineRetransmissionRepository) {
    return new DcAwareOfflineRetransmissionRepository(
        commandExecutor, offlineRetransmissionRepository);
  }

  @Bean
  OfflineRetransmissionService offlineRetransmissionService(
      @Qualifier("dcAwareOfflineRetransmissionRepository")
          OfflineRetransmissionRepository taskRepository,
      TopicRepository topicRepository) {
    return new OfflineRetransmissionService(taskRepository, topicRepository);
  }
}
