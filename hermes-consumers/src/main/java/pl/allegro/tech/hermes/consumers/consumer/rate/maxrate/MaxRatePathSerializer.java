package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import static com.google.common.base.Preconditions.checkArgument;

import pl.allegro.tech.hermes.api.SubscriptionName;

public class MaxRatePathSerializer {

  ConsumerInstance consumerInstanceFromContentPath(String path) {
    String[] paths = splitContentPath(path);
    return new ConsumerInstance(
        paths[paths.length - 2], SubscriptionName.fromString(paths[paths.length - 3]));
  }

  ConsumerInstance consumerInstanceFromConsumerPath(String path) {
    String[] paths = splitConsumerPath(path);
    return new ConsumerInstance(
        paths[paths.length - 1], SubscriptionName.fromString(paths[paths.length - 2]));
  }

  String content(String path) {
    String[] paths = splitContentPath(path);
    return paths[paths.length - 1];
  }

  private String[] splitContentPath(String path) {
    String[] paths = path.split("/");
    checkArgument(
        paths.length > 2,
        "Incorrect path format. Expected:'/base/subscription/consumerId/content'. Found:'%s'",
        path);
    return paths;
  }

  private String[] splitConsumerPath(String path) {
    String[] paths = path.split("/");
    checkArgument(
        paths.length > 2,
        "Incorrect path format. Expected:'/base/subscription/consumerId'. Found:'%s'",
        path);
    return paths;
  }
}
