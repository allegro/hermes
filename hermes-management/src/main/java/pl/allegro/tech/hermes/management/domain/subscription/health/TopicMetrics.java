package pl.allegro.tech.hermes.management.domain.subscription.health;

public final class TopicMetrics {
    private final double rate;

    public TopicMetrics(double rate) {
        this.rate = rate;
    }

    public double getRate() {
        return rate;
    }
}
