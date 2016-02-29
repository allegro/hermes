package pl.allegro.tech.hermes.integration.helper.schemarepo;


import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.servlet.GuiceFilter;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.schemarepo.config.Config;
import org.schemarepo.config.ConfigModule;
import org.schemarepo.server.AuxiliaryRESTRepository;
import org.schemarepo.server.HumanOrientedRESTRepository;
import org.schemarepo.server.MachineOrientedRESTRepository;

import javax.inject.Singleton;
import java.util.Properties;

public class SchemaRepo {
    private final Server server;
    private final int port;

    public SchemaRepo(int port) {
        Properties properties = new Properties();
        properties.put(Config.REPO_CLASS, "org.schemarepo.InMemoryRepository");
        Injector injector = Guice.createInjector(new ConfigModule(properties), new ServerModule());
        this.port = port;
        this.server = injector.getInstance(Server.class);
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }

    private class ServerModule extends JerseyServletModule {

        @Override
        protected void configureServlets() {
            serve("/*").with(GuiceContainer.class);
            bind(MachineOrientedRESTRepository.class);
            bind(HumanOrientedRESTRepository.class);
            bind(AuxiliaryRESTRepository.class);
        }

        @Provides
        @Singleton
        public Server provideServer(GuiceFilter guiceFilter, ServletContextHandler handler) {
            Server server = new Server();
            FilterHolder holder = new FilterHolder(guiceFilter);
            handler.addFilter(holder, "/*", null);
            handler.setContextPath("/");
            server.setHandler(handler);
            ServerConnector connector = new ServerConnector(server);
            connector.setPort(port);
            server.setConnectors(new Connector[] { connector });
            return server;
        }
    }

}
