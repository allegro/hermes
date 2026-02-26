package pl.allegro.tech.hermes.frontend.buffer;

import java.time.Duration;

/**
 * @deprecated This feature is deprecated and will be removed in a future version.
 */
@Deprecated
public interface BackupMessagesLoaderParameters {

  Duration getMaxAge();

  int getMaxResendRetries();

  Duration getLoadingPauseBetweenResend();

  Duration getLoadingWaitForBrokerTopicInfo();
}
