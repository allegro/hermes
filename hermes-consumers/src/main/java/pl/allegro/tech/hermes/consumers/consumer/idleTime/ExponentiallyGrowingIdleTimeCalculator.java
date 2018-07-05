package pl.allegro.tech.hermes.consumers.consumer.idleTime;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.min;

public class ExponentiallyGrowingIdleTimeCalculator implements IdleTimeCalculator {

    private final long base;
    private final long initialIdleTime;
    private final long maxIdleTime;
    private long currentIdleTime;

    public ExponentiallyGrowingIdleTimeCalculator(long initialIdleTime, long maxIdleTime) {
        this(2, initialIdleTime, maxIdleTime);
    }

    public ExponentiallyGrowingIdleTimeCalculator(long base, long initialIdleTime, long maxIdleTime) {
        checkArgument(base > 0, "base should be greater than zero");
        checkArgument(initialIdleTime > 0, "initialIdleTime should be greater than zero");
        checkArgument(maxIdleTime > 0, "maxIdleTime should be greater than zero");
        checkArgument(initialIdleTime <= maxIdleTime, "maxIdleTime should be grater or equal initialIdleTime");

        this.base = base;
        this.initialIdleTime = initialIdleTime;
        this.maxIdleTime = maxIdleTime;
        this.currentIdleTime = initialIdleTime;
    }

    @Override
    public long increaseIdleTime() {
        long previousIdleTime = this.currentIdleTime;
        this.currentIdleTime = min(this.currentIdleTime * base, maxIdleTime);
        return previousIdleTime;
    }

    @Override
    public long getIdleTime() {
        return currentIdleTime;
    }

    @Override
    public void reset() {
        this.currentIdleTime = initialIdleTime;
    }
}
