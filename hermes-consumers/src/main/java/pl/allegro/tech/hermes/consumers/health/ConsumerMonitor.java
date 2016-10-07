package pl.allegro.tech.hermes.consumers.health;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ConsumerMonitor {

    private Map<String, Supplier<Object>> checks = new ConcurrentHashMap<>();

    public void register(String key, Supplier<Object> check) {
        checks.putIfAbsent(key, check);
    }

    public Object check(String key) {
        try {
            return checks.getOrDefault(key, () -> "Unavailable").get();
        } catch (Exception ex) {
            return "Failed to evaluate check, " + ex.getMessage();
        }
    }
}
