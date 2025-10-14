package pl.allegro.tech.hermes.consumers.config;

public class GoogleBigQueryThreadPoolProperties {
    int poolSize = 100;

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }
}
