package pl.allegro.tech.hermes.consumers.consumer;

public class SerialConsumerState {

    private final double rate;

    SerialConsumerState(double rate) {
        this.rate = rate;
    }

    double getRate() {
        return rate;
    }
}
