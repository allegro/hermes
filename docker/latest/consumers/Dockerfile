FROM gradle:7.6-jdk21 AS builder

COPY --chown=gradle:gradle . /home/gradle/src/
WORKDIR /home/gradle/src/
RUN gradle clean distZip -Pdistribution

FROM eclipse-temurin:21-jre

RUN apt-get update \
  && apt-get -y install unzip wget bash \
  && rm -rf /var/lib/apt/lists/*

RUN mkdir /app

COPY --from=builder /home/gradle/src/hermes-consumers/build/distributions/*.zip /app/hermes-consumers.zip
RUN unzip /app/hermes-consumers.zip && mv /hermes-consumers-* /hermes-consumers

ADD docker/latest/consumers/consumers.yaml /etc/hermes/consumers.yaml
ADD docker/latest/consumers/logback.xml /etc/hermes/logback.xml
ENV HERMES_CONSUMERS_OPTS="-Dspring.config.location=file:///etc/hermes/consumers.yaml -Dlogback.configurationFile=/etc/hermes/logback.xml"

CMD /hermes-consumers/bin/hermes-consumers
