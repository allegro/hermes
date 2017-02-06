FROM jeanblanchard/java:8

MAINTAINER Allegro


RUN apk update \
  && apk add unzip wget bash \
  && rm -rf /var/cache/apk/*

ENV ARCHIVE_NAME="hermes-consumers-latest.zip"
ENV URL="http://box.allegro.tech/hermes/${ARCHIVE_NAME}"

RUN wget -nv "${URL}" -O "/tmp/${ARCHIVE_NAME}" \
  && unzip -q "/tmp/${ARCHIVE_NAME}" -d /opt \
  && rm "/tmp/${ARCHIVE_NAME}" \
  && mv /opt/hermes-consumers-* /opt/hermes-consumers

ENV HERMES_CONSUMERS_OPTS="-Darchaius.configurationSource.additionalUrls=file:///etc/hermes/consumers.properties -Dlogback.configurationFile=/etc/hermes/logback.xml"
ADD consumers.properties /etc/hermes/consumers.properties
ADD logback.xml /etc/hermes/logback.xml

CMD /opt/hermes-consumers/bin/hermes-consumers
