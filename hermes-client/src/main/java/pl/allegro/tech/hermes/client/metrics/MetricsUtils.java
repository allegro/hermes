package pl.allegro.tech.hermes.client.metrics;

class MetricsUtils {

    static String getMetricsPrefix(String topic) {
        return "hermes-client." + sanitizeTopic(topic);
    }

    private static String sanitizeTopic(String topic) {
        int lastDot = topic.lastIndexOf(".");
        char[] sanitized = topic.replaceAll("\\.", "_").toCharArray();
        sanitized[lastDot] = '.';
        return String.valueOf(sanitized);
    }
}
