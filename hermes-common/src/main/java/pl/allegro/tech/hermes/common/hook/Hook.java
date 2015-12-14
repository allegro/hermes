package pl.allegro.tech.hermes.common.hook;

import org.glassfish.hk2.api.ServiceLocator;

public interface Hook {
    interface Startup {
        void apply(ServiceLocator serviceLocator);
    }

    interface Shutdown {
        void apply();
    }

}
