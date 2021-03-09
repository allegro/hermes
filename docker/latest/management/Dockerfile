FROM gradle:6.8.3-jdk11 AS builder

COPY --chown=gradle:gradle . /home/gradle/src/
WORKDIR /home/gradle/src/
RUN ./gradlew clean distZip -Pdistribution

FROM adoptopenjdk/openjdk11:alpine-jre

RUN apk update \
  && apk add unzip wget bash \
  && rm -rf /var/cache/apk/*

RUN mkdir /app

COPY --from=builder /home/gradle/src/hermes-management/build/distributions/*.zip /app/hermes-management.zip
RUN unzip /app/hermes-management.zip && mv /hermes-management-* /hermes-management

ADD docker/latest/management/management.yaml /etc/hermes/management.yaml
ADD docker/latest/cert/kafka.server.keystore.jks /etc/hermes/kafka.server.keystore.jks
ADD docker/latest/cert/kafka.server.truststore.jks /etc/hermes/kafka.server.truststore.jks
ENV SPRING_CONFIG_LOCATION="file:///etc/hermes/management.yaml"

CMD /hermes-management/bin/hermes-management
