# Logging

Hermes Frontend, Consumers and Management modules use [Logback](http://logback.qos.ch) as an implementation of logging.
Any option exposed by Logback can be used to configure it.

Below our production configuration is described.

## Pointing to logback.xml location

Simple way to use external `logback.xml` file is to pass its location using JVM flag:

```
-Dlogback.configurationFile=/etc/hermes/logback.xml
```

## Example logback.xml file

Sample `logback.xml` file that will log to file using [async appender](http://logback.qos.ch/manual/appenders.html#AsyncAppender):

```
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/log/hermes/hermes-frontend.log</file>
        <encoder>
            <Pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</Pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.FixedWindowRollingPolicy">
            <fileNamePattern>/var/log/hermes/hermes-frontend-%i.log</fileNamePattern>
            <minIndex>1</minIndex>
            <maxIndex>4</maxIndex>
        </rollingPolicy>

        <triggeringPolicy class="ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy">
            <maxFileSize>100MB</maxFileSize>
        </triggeringPolicy>
    </appender>

    <appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="FILE" />
    </appender>

    <logger name="org.apache.zookeeper" level="ERROR" />

    <!-- Selector spams ERROR level messages every 100ms on IOException -->
    <logger name="org.apache.kafka.common.network.Selector" level="OFF"/>

    <logger name="kafka" level="WARN"/>

    <root level="INFO">
        <appender-ref ref="ASYNC_FILE" />
    </root>
</configuration>
```

This configuration is tuned for Frontend, but Consumers and Management config is more or less the same.

## Structured logging

We use structured logging in Hermes modules for easier log searchability, filtering, and analysis in production
environments.

**Location:** `pl.allegro.tech.hermes.common.logging.LoggingFields`

Always use constants from `LoggingFields` to ensure consistency across all services.

| Constant            | Field Name          | Description                 | Example Value                       |
|---------------------|---------------------|-----------------------------|-------------------------------------|
| `TOPIC_NAME`        | `topic-name`        | Qualified topic name        | `"my.events.topic"`                 |
| `SUBSCRIPTION_NAME` | `subscription-name` | Qualified subscription name | `"my.events.topic$my-subscription"` |

### Example usage and used patterns

#### Direct addKeyValue usage (single log entry)

```java
logger.atInfo()
    .addKeyValue(SUBSCRIPTION_NAME, subscription.getQualifiedName())
    .log("Creating subscription {}", subscription.getQualifiedName());
```

#### LoggingEventBuilder usage (multiple logs, same level)

```java
LoggingEventBuilder subscriptionLogger = logger.atInfo()
    .addKeyValue(SUBSCRIPTION_NAME, subscription.getQualifiedName());
subscriptionLogger.log("First message {}", ...);
subscriptionLogger.log("Second message {}", ...);
```

#### LoggingEventBuilder usage (multiple logs, different levels)

```java
LoggingEventBuilder infoLogger = logger.atInfo()
    .addKeyValue(TOPIC_NAME, topic.getQualifiedName());
LoggingEventBuilder errorLogger = logger.atError()
    .addKeyValue(TOPIC_NAME, topic.getQualifiedName());
```

#### LoggingContext/MDC usage (wrap method execution)

```java
import static pl.allegro.tech.hermes.common.logging.LoggingContext.runWithLogging;

LoggingContext.runWithLogging(
    SUBSCRIPTION_NAME,
    subscription.getQualifiedName().getQualifiedName(),
    () -> {
        // All logs in this scope automatically have SUBSCRIPTION_NAME in MDC
    });
```

#### Wrapper/Delegate usage

```java
public class LoggingConsumer implements Consumer {
    private final Consumer delegate;
    @Override
    public void consume(Runnable signalsInterrupt) {
        LoggingContext.runWithLogging(
            SUBSCRIPTION_NAME,
            delegate.getSubscription().getQualifiedName().getQualifiedName(),
            () -> delegate.consume(signalsInterrupt));
    }
}
```

#### Wrapper method usage (preserving git blame)

```java
// New wrapper method
private void startWithLogging(Signal start) {
    Subscription subscription = getSubscriptionFromPayload(start);
    runWithLogging(SUBSCRIPTION_NAME, subscription.getQualifiedName().getQualifiedName(),
        () -> start(start, subscription));
}
// Original method kept intact - git blame preserved
private void start(Signal start, Subscription subscription) {
    // Original implementation unchanged
}
```
