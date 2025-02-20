package pl.allegro.tech.hermes.frontend.buffer;

import java.time.Duration;

public interface BackupMessagesLoaderParameters {

  Duration getMaxAge();

  int getMaxResendRetries();

  Duration getLoadingPauseBetweenResend();

  Duration getLoadingWaitForBrokerTopicInfo();
}
