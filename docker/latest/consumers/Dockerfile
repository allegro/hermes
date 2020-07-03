FROM gradle:5.6.2-jdk11 AS builder

COPY --chown=gradle:gradle settings.gradle build.gradle /home/gradle/src/
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

RUN ["chmod", "+x", "/hermes-consumers/bin/hermes-consumers"]

#ENV JAVA_OPTS="-DTOMCAT=Y -DOracle.server=1234 [...]"
ADD docker/latest/consumers/consumers.properties /etc/hermes/consumers.properties
ADD docker/latest/consumers/logback.xml /etc/hermes/logback.xml
ENV HERMES_CONSUMERS_OPTS="-Darchaius.configurationSource.additionalUrls=file:///etc/hermes/consumers.properties -Dlogback.configurationFile=/etc/hermes/logback.xml"

CMD /hermes-consumers/bin/hermes-consumers
