package pl.allegro.tech.hermes.frontend.di.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import pl.allegro.tech.hermes.common.config.ConfigFactory;

import java.util.Objects;

import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_STARTUP_TOPIC_SCHEMA_LOADING_ENABLED;

public class TopicSchemaLoadingStartupHookCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        ConfigFactory config = Objects.requireNonNull(context.getBeanFactory()).getBean(ConfigFactory.class);
        return config.getBooleanProperty(FRONTEND_STARTUP_TOPIC_SCHEMA_LOADING_ENABLED);
    }
}
