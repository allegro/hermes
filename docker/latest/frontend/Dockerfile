FROM jeanblanchard/java:8

MAINTAINER Allegro

RUN apk update \
  && apk add unzip wget bash \
  && rm -rf /var/cache/apk/*

ENV ARCHIVE_NAME="hermes-frontend-latest.zip"
ENV URL="http://box.allegro.tech/hermes/${ARCHIVE_NAME}"

RUN wget -nv "${URL}" -O "/tmp/${ARCHIVE_NAME}" \
  && unzip -q "/tmp/${ARCHIVE_NAME}" -d /opt \
  && rm "/tmp/${ARCHIVE_NAME}" \
  && mv /opt/hermes-frontend-* /opt/hermes-frontend

ENV HERMES_FRONTEND_OPTS="-Darchaius.configurationSource.additionalUrls=file:///etc/hermes/frontend.properties -Dlogback.configurationFile=/etc/hermes/logback.xml"
ADD frontend.properties /etc/hermes/frontend.properties
ADD logback.xml /etc/hermes/logback.xml

CMD /opt/hermes-frontend/bin/hermes-frontend
