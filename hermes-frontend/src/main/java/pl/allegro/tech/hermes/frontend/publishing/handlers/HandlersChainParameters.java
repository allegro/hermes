package pl.allegro.tech.hermes.frontend.publishing.handlers;

import java.time.Duration;

public interface HandlersChainParameters {

  /**
   * Defines the time allowed for processing a publish request to a topic with ack=leader. After
   * this time elapses a TimeoutHandler will be run which, with PersistentBufferExtension enabled,
   * would result in message being sent to local buffer.
   */
  Duration getIdleTimeout();

  /**
   * Defines the time allowed for processing a publish request to a topic with ack=all. After this
   * time elapses a TimeoutHandler will be run which, with PersistentBufferExtension enabled, would
   * result in message being sent to local buffer.
   */
  Duration getLongIdleTimeout();

  /**
   * Defines the time allowed for processing a publish request to a topic with
   * fallbackToRemoteDatacenterEnabled=true. After this time elapses a TimeoutHandler will be run
   * which would result in error being returned to a client.
   */
  Duration getMaxPublishRequestDuration();

  boolean isForceTopicMaxMessageSize();

  boolean isKeepAliveHeaderEnabled();

  Duration getKeepAliveHeaderTimeout();

  boolean isAuthenticationEnabled();

  String getAuthenticationMode();
}
