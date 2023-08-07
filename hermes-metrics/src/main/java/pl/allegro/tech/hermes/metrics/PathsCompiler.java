package pl.allegro.tech.hermes.metrics;

import org.apache.commons.lang.text.StrBuilder;

public class PathsCompiler {

    public static final String REPLACEMENT_CHAR = "_";

    public static final String HOSTNAME = "$hostname";
    public static final String GROUP = "$group";
    public static final String TOPIC = "$topic";
    public static final String SUBSCRIPTION = "$subscription";
    public static final String KAFKA_TOPIC = "$kafka_topic";
    public static final String PARTITION = "$partition";
    public static final String KAFKA_CLUSTER = "$kafka_cluster";
    public static final String HTTP_CODE = "$http_code";
    public static final String HTTP_CODE_FAMILY = "$http_family_of_code";
    public static final String EXECUTOR_NAME = "$executor_name";
    public static final String OAUTH_PROVIDER_NAME = "$oauth_provider_name";
    public static final String SCHEMA_REPO_TYPE = "$schema_repo_type";

    private final String hostname;

    public PathsCompiler(String hostname) {
        this.hostname = escapeDots(hostname);
    }

    public String compile(String path) {
        return path.replace(HOSTNAME, hostname);
    }

    public String compile(String path, PathContext context) {
        StrBuilder pathBuilder = new StrBuilder(path);

        context.getGroup().ifPresent(g -> pathBuilder.replaceAll(GROUP, g));
        context.getTopic().ifPresent(t -> pathBuilder.replaceAll(TOPIC, t));
        context.getSubscription().ifPresent(s -> pathBuilder.replaceAll(SUBSCRIPTION, s));
        context.getKafkaTopic().ifPresent(k -> pathBuilder.replaceAll(KAFKA_TOPIC, k));
        context.getPartition().ifPresent(p -> pathBuilder.replaceAll(PARTITION, p.toString()));
        context.getKafkaCluster().ifPresent(c -> pathBuilder.replaceAll(KAFKA_CLUSTER, c));
        context.getHttpCode().ifPresent(c -> pathBuilder.replaceAll(HTTP_CODE, c.toString()));
        context.getHttpCodeFamily().ifPresent(cf -> pathBuilder.replaceAll(HTTP_CODE_FAMILY, cf));
        context.getExecutorName().ifPresent(c -> pathBuilder.replaceAll(EXECUTOR_NAME, c));
        context.getoAuthProviderName().ifPresent(c -> pathBuilder.replaceAll(OAUTH_PROVIDER_NAME, c));
        context.getSchemaRepoType().ifPresent(c -> pathBuilder.replaceAll(SCHEMA_REPO_TYPE, c));

        pathBuilder.replaceAll(HOSTNAME, hostname);

        return pathBuilder.toString();
    }

    private String escapeDots(String value) {
        return value.replaceAll("\\.", REPLACEMENT_CHAR);
    }
}
