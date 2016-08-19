package pl.allegro.tech.hermes.management.domain.topic.blacklist;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BlacklistService {

    private final BlacklistRepository blacklistRepository;

    @Autowired
    public BlacklistService(BlacklistRepository blacklistRepository) {
        this.blacklistRepository = blacklistRepository;
    }

    public void blacklistTopic(String qualifiedTopicName) {
        blacklistRepository.add(qualifiedTopicName);
    }

    public void unblacklistTopic(String qualifiedTopicName) {
        blacklistRepository.remove(qualifiedTopicName);
    }

    public boolean isBlacklisted(String qualifiedTopicName) {
        return blacklistRepository.isBlacklisted(qualifiedTopicName);
    }


}
