FROM gradle:8.14.3-jdk21 AS builder
ENV JAVA_TOOL_OPTIONS="-XX:UseSVE=0"

COPY --chown=gradle:gradle . /home/gradle/src/
WORKDIR /home/gradle/src/
RUN gradle clean distZip -Pdistribution

FROM eclipse-temurin:21.0.8_9-jre

RUN apt-get clean \
  && apt-get update \
  && DEBIAN_FRONTEND=noninteractive apt-get -y install unzip wget bash \
  && rm -rf /var/lib/apt/lists/*

RUN mkdir /app

COPY --from=builder /home/gradle/src/hermes-frontend/build/distributions/*.zip /app/hermes-frontend.zip
RUN unzip /app/hermes-frontend.zip && mv /hermes-frontend-* /hermes-frontend

ADD docker/latest/frontend/frontend.yaml /etc/hermes/frontend.yaml
ADD docker/latest/frontend/logback.xml /etc/hermes/logback.xml
ENV HERMES_FRONTEND_OPTS="-Dspring.config.location=file:///etc/hermes/frontend.yaml -Dlogback.configurationFile=/etc/hermes/logback.xml"

CMD /hermes-frontend/bin/hermes-frontend