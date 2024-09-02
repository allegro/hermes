package pl.allegro.tech.hermes.metrics;

import org.apache.commons.text.TextStringBuilder;

public class PathsCompiler {

  public static final String REPLACEMENT_CHAR = "_";

  public static final String HOSTNAME = "$hostname";
  public static final String GROUP = "$group";
  public static final String TOPIC = "$topic";
  public static final String SUBSCRIPTION = "$subscription";

  private final String hostname;

  public PathsCompiler(String hostname) {
    this.hostname = escapeDots(hostname);
  }

  public String compile(String path) {
    return path.replace(HOSTNAME, hostname);
  }

  public String compile(String path, PathContext context) {
    TextStringBuilder pathBuilder = new TextStringBuilder(path);

    context.getGroup().ifPresent(g -> pathBuilder.replaceAll(GROUP, g));
    context.getTopic().ifPresent(t -> pathBuilder.replaceAll(TOPIC, t));
    context.getSubscription().ifPresent(s -> pathBuilder.replaceAll(SUBSCRIPTION, s));

    pathBuilder.replaceAll(HOSTNAME, hostname);

    return pathBuilder.toString();
  }

  private String escapeDots(String value) {
    return value.replaceAll("\\.", REPLACEMENT_CHAR);
  }
}
