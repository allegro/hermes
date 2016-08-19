package pl.allegro.tech.hermes.management.domain.topic.blacklist;

public interface BlacklistRepository {

    void add(String qualifiedTopicName);

    void remove(String qualifiedTopicName);

    boolean isBlacklisted(String qualifiedTopicName);
}
