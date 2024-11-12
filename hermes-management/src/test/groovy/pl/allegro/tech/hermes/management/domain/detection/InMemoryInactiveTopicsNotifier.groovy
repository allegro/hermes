package pl.allegro.tech.hermes.management.domain.detection

class InMemoryInactiveTopicsNotifier implements InactiveTopicsNotifier {
    private Set<InactiveTopic> notifiedTopics = new HashSet<>();

    @Override
    void notify(List<InactiveTopic> inactiveTopics) {
        notifiedTopics.addAll(inactiveTopics)
    }

    void reset() {
        notifiedTopics.clear()
    }

    Set<InactiveTopic> getNotifiedTopics() {
        return notifiedTopics
    }
}
