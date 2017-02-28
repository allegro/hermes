FROM jeanblanchard/java:8

MAINTAINER Allegro

RUN apk update \
  && apk add unzip wget bash \
  && rm -rf /var/cache/apk/*

ENV ARCHIVE_NAME="hermes-management-latest.zip"
ENV URL="http://box.allegro.tech/hermes/${ARCHIVE_NAME}"

RUN wget -nv "${URL}" -O "/tmp/${ARCHIVE_NAME}" \
  && unzip -q "/tmp/${ARCHIVE_NAME}" -d /opt \
  && rm "/tmp/${ARCHIVE_NAME}" \
  && mv /opt/hermes-management-* /opt/hermes-management

ENV SPRING_CONFIG_LOCATION="file:///etc/hermes/management.yaml"
ADD management.yaml /etc/hermes/management.yaml

CMD /opt/hermes-management/bin/hermes-management
