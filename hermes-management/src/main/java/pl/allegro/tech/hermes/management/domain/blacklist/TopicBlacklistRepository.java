package pl.allegro.tech.hermes.management.domain.blacklist;

import java.util.List;

public interface TopicBlacklistRepository {

  void add(String qualifiedTopicName);

  void remove(String qualifiedTopicName);

  boolean isBlacklisted(String qualifiedTopicName);

  List<String> list();
}
