FROM gradle:5.6.2-jdk11 AS builder

COPY --chown=gradle:gradle . /home/gradle/src/
WORKDIR /home/gradle/src/
RUN ./gradlew clean distZip -Pdistribution

FROM adoptopenjdk/openjdk11:alpine-jre

RUN apk update \
  && apk add unzip wget bash \
  && rm -rf /var/cache/apk/*

RUN mkdir /app

COPY --from=builder /home/gradle/src/hermes-frontend/build/distributions/*.zip /app/hermes-frontend.zip
RUN unzip /app/hermes-frontend.zip && mv /hermes-frontend-* /hermes-frontend

ADD docker/latest/frontend/frontend.properties /etc/hermes/frontend.properties
ADD docker/latest/frontend/logback.xml /etc/hermes/logback.xml
ENV HERMES_FRONTEND_OPTS="-Darchaius.configurationSource.additionalUrls=file:///etc/hermes/frontend.properties -Dlogback.configurationFile=/etc/hermes/logback.xml"

CMD /hermes-frontend/bin/hermes-frontend