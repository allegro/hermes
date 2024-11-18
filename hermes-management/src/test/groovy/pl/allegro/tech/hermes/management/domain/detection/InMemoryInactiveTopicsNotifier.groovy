package pl.allegro.tech.hermes.management.domain.detection

class InMemoryInactiveTopicsNotifier implements InactiveTopicsNotifier {
    private Set<InactiveTopic> notifiedTopics = new HashSet<>();

    @Override
    NotificationResult notify(List<InactiveTopic> inactiveTopics) {
        notifiedTopics.addAll(inactiveTopics)
        Map<String, Boolean> result = new HashMap<>();
        inactiveTopics.stream().forEach { result.put(it.qualifiedTopicName(), true) }
        return new NotificationResult(result)
    }

    void reset() {
        notifiedTopics.clear()
    }

    Set<InactiveTopic> getNotifiedTopics() {
        return notifiedTopics
    }
}
