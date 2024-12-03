package pl.allegro.tech.hermes.management.infrastructure.detection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import pl.allegro.tech.hermes.management.domain.detection.InactiveTopicsDetectionJob;
import pl.allegro.tech.hermes.management.infrastructure.leader.ManagementLeadership;

public class InactiveTopicsDetectionScheduler {
  private final InactiveTopicsDetectionJob job;
  private final ManagementLeadership leader;

  private static final Logger logger =
      LoggerFactory.getLogger(InactiveTopicsDetectionScheduler.class);

  public InactiveTopicsDetectionScheduler(
      InactiveTopicsDetectionJob job, ManagementLeadership leader) {
    this.leader = leader;
    this.job = job;
  }

  @Scheduled(cron = "${detection.inactive-topics.cron}")
  public void run() {
    if (leader.isLeader()) {
      logger.info("Inactive topics detection started");
      job.detectAndNotify();
    } else {
      logger.info("Inactive topics detection not started - not a leader");
    }
  }
}
