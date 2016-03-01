package pl.allegro.tech.hermes.management.domain.subscription.health;

public final class SubscriptionMetrics {
    private final double rate;
    private final double timeoutsRate;
    private final double otherErrorsRate;
    private final double code4xxErrorsRate;
    private final double code5xxErrorsRate;
    private final long lag;

    public SubscriptionMetrics(double rate, double timeoutsRate, double otherErrorsRate, double code4xxErrorsRate, double code5xxErrorsRate, long lag) {
        this.rate = rate;
        this.timeoutsRate = timeoutsRate;
        this.otherErrorsRate = otherErrorsRate;
        this.code4xxErrorsRate = code4xxErrorsRate;
        this.code5xxErrorsRate = code5xxErrorsRate;
        this.lag = lag;
    }

    public double getRate() {
        return rate;
    }

    public double getTimeoutsRate() {
        return timeoutsRate;
    }

    public double getOtherErrorsRate() {
        return otherErrorsRate;
    }

    public double getCode4xxErrorsRate() {
        return code4xxErrorsRate;
    }

    public double getCode5xxErrorsRate() {
        return code5xxErrorsRate;
    }

    public long getLag() {
        return lag;
    }
}
