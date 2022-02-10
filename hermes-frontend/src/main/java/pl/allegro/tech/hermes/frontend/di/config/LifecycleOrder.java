package pl.allegro.tech.hermes.frontend.di.config;

public interface LifecycleOrder { //TODO: check if order really matters

	int CACHE_STARTUP = Integer.MIN_VALUE;

	int PERSISTENT_BUFFER_STARTUP = Integer.MIN_VALUE + 1;

	int BEFORE_STARTUP = Integer.MIN_VALUE + 2;

	int SERVER_STARTUP = Integer.MIN_VALUE + 3;

	int AFTER_STARTUP = Integer.MIN_VALUE + 4;

	int getOrder();

}
