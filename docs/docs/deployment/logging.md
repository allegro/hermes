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
