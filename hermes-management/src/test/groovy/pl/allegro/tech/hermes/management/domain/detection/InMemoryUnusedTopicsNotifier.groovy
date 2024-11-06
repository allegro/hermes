package pl.allegro.tech.hermes.management.domain.detection

class InMemoryUnusedTopicsNotifier implements UnusedTopicsNotifier {
    private Set<UnusedTopic> notifiedTopics = new HashSet<>();

    @Override
    void notify(List<UnusedTopic> unusedTopics) {
        notifiedTopics.addAll(unusedTopics)
    }

    void reset() {
        notifiedTopics.clear()
    }

    Set<UnusedTopic> getNotifiedTopics() {
        return notifiedTopics
    }
}
