FROM jeanblanchard/java:8

MAINTAINER Allegro

RUN apk update && apk add unzip wget bash

ENV ARCHIVE_NAME="hermes-management-latest.zip"
ENV URL="http://box.allegrotech.io/hermes/${ARCHIVE_NAME}"

RUN wget -q "${URL}" -O "/tmp/${ARCHIVE_NAME}" \
  && unzip "/tmp/${ARCHIVE_NAME}" -d /opt \
  && rm "/tmp/${ARCHIVE_NAME}" \
  && mv /opt/hermes-management-* /opt/hermes-management

ENV SPRING_CONFIG_LOCATION="file:///etc/hermes/management.yaml"
ADD management.yaml /etc/hermes/management.yaml

CMD /opt/hermes-management/bin/hermes-management
