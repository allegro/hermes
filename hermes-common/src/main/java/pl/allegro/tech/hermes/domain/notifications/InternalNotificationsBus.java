package pl.allegro.tech.hermes.domain.notifications;

/** All callbacks must be nonblocking. */
public interface InternalNotificationsBus {

  void registerSubscriptionCallback(SubscriptionCallback callback);

  void registerTopicCallback(TopicCallback callback);

  void registerAdminCallback(AdminCallback callback);
}
