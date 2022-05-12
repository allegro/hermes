package pl.allegro.tech.hermes.benchmark.environment;

import pl.allegro.tech.hermes.frontend.server.IReadinessChecker;

class EmptyReadinessChecker implements IReadinessChecker {

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
    }
}
