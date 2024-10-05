package pl.allegro.tech.hermes.frontend.config;

import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.tracker.frontend.LogRepository;
import pl.allegro.tech.hermes.tracker.frontend.NoOperationPublishingTracker;
import pl.allegro.tech.hermes.tracker.frontend.PublishingMessageTracker;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

@Configuration
public class FrontendTrackerConfiguration {

  @Bean
  public PublishingMessageTracker publishingMessageTracker(
      List<LogRepository> repositories, Clock clock) {
    return new PublishingMessageTracker(repositories, clock);
  }

  @Bean
  public NoOperationPublishingTracker noOperationPublishingTracker() {
    return new NoOperationPublishingTracker();
  }

  @Bean(destroyMethod = "close")
  public Trackers trackers(List<LogRepository> repositories) {
    return new Trackers(repositories);
  }
}
