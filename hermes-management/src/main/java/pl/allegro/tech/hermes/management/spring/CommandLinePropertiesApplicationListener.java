package pl.allegro.tech.hermes.management.spring;

import joptsimple.OptionParser;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.JOptCommandLinePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import static com.google.common.collect.Lists.newArrayList;

public class CommandLinePropertiesApplicationListener extends ApplicationEnvironmentPreparedEventListener implements Ordered {

    private static final String ENVIRONMENT_PROPERTY = "application.environment";
    public static final String SERVER_PORT_PROPERTY = "server.port";

    private static final String COMMAND_LINE_ARGS = "hermesCommandLineArgs";

    @Override
    protected void onApplicationEnvironmentPreparedEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(cmdLinePropertySource(event.getArgs()));

        String environmentName = environment.getProperty(ENVIRONMENT_PROPERTY);
        environment.addActiveProfile(environmentName);
    }

    private PropertySource<?> cmdLinePropertySource(String[] args) {
        OptionParser parser = new OptionParser();
        parser.acceptsAll(newArrayList(ENVIRONMENT_PROPERTY, "environment", "e")).withRequiredArg().defaultsTo("local");
        parser.acceptsAll(newArrayList(SERVER_PORT_PROPERTY, "port", "p")).withRequiredArg();
        parser.allowsUnrecognizedOptions();
        return new JOptCommandLinePropertySource(COMMAND_LINE_ARGS, parser.parse(args));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
