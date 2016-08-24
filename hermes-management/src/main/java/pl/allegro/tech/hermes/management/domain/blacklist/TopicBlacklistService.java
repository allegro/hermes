package pl.allegro.tech.hermes.management.domain.blacklist;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TopicBlacklistService {

    private final TopicBlacklistRepository topicBlacklistRepository;

    @Autowired
    public TopicBlacklistService(TopicBlacklistRepository topicBlacklistRepository) {
        this.topicBlacklistRepository = topicBlacklistRepository;
    }

    public void blacklist(String qualifiedTopicName) {
        topicBlacklistRepository.add(qualifiedTopicName);
    }

    public void unblacklist(String qualifiedTopicName) {
        topicBlacklistRepository.remove(qualifiedTopicName);
    }

    public boolean isBlacklisted(String qualifiedTopicName) {
        return topicBlacklistRepository.isBlacklisted(qualifiedTopicName);
    }

    public List<String> list() {
        return topicBlacklistRepository.list();
    }

}
