package pl.allegro.tech.hermes.domain.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;

import java.io.IOException;
import java.util.Map;

import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;

public class DeliveryTypeMigration {
    private static final Logger logger = LoggerFactory.getLogger(DeliveryTypeMigration.class);

    @SuppressWarnings("unchecked")
    public static Subscription migrate(byte[] data, Subscription subscription, ObjectMapper objectMapper) {
        if (subscription.getDeliveryType() == null) {
            try {
                Map<String, Object> policy = (Map<String, Object>) objectMapper.readValue(data, Map.class).get("subscriptionPolicy");
                return subscription().applyPatch(subscription).withSubscriptionPolicy(subscriptionPolicy().applyPatch(policy).build()).build();
            } catch (IOException e) {
                logger.error("Failed to migrate subscription {} without delivery type", subscription.getId());
            }
        }
        return subscription;
    }
}
