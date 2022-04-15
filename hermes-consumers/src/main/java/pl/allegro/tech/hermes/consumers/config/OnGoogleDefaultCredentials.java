package pl.allegro.tech.hermes.consumers.config;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.io.IOException;

public class OnGoogleDefaultCredentials implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        try {
            GoogleCredentials.getApplicationDefault();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
