FROM gradle:6.8.3-jdk11 AS builder

COPY --chown=gradle:gradle . /home/gradle/src/
WORKDIR /home/gradle/src/
RUN ./gradlew clean distZip -Pdistribution

FROM adoptopenjdk/openjdk11:alpine-jre

RUN apk update \
  && apk add unzip wget bash \
  && rm -rf /var/cache/apk/*

RUN mkdir /app

COPY --from=builder /home/gradle/src/hermes-consumers/build/distributions/*.zip /app/hermes-consumers.zip
RUN unzip /app/hermes-consumers.zip && mv /hermes-consumers-* /hermes-consumers

ADD docker/latest/consumers/consumers.properties /etc/hermes/consumers.properties
ADD docker/latest/consumers/logback.xml /etc/hermes/logback.xml
ADD docker/latest/cert/kafka.server.keystore.jks /etc/hermes/kafka.server.keystore.jks
ADD docker/latest/cert/kafka.server.truststore.jks /etc/hermes/kafka.server.truststore.jks
ENV HERMES_CONSUMERS_OPTS="-Darchaius.configurationSource.additionalUrls=file:///etc/hermes/consumers.properties -Dlogback.configurationFile=/etc/hermes/logback.xml"

CMD /hermes-consumers/bin/hermes-consumers
