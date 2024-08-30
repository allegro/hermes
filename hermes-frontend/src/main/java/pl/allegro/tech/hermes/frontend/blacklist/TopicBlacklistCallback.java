package pl.allegro.tech.hermes.frontend.blacklist;

public interface TopicBlacklistCallback {

  default void onTopicBlacklisted(String qualifiedTopicName) {}

  default void onTopicUnblacklisted(String qualifiedTopicName) {}
}
