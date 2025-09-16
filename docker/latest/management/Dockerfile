FROM gradle:8.14.3-jdk21 AS builder
ENV JAVA_TOOL_OPTIONS="-XX:UseSVE=0"

COPY --chown=gradle:gradle . /home/gradle/src/
WORKDIR /home/gradle/src/
RUN gradle clean distZip -Pdistribution

FROM eclipse-temurin:21.0.8_9-jre

RUN apt-get update \
  && apt-get -y install unzip wget bash \
  && rm -rf /var/lib/apt/lists/*

RUN mkdir /app

COPY --from=builder /home/gradle/src/hermes-management/build/distributions/*.zip /app/hermes-management.zip
RUN unzip /app/hermes-management.zip && mv /hermes-management-* /hermes-management

ADD docker/latest/management/management.yaml /etc/hermes/management.yaml
ENV SPRING_CONFIG_LOCATION="file:///etc/hermes/management.yaml"

CMD /hermes-management/bin/hermes-management
