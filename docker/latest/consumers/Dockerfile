FROM java:8

MAINTAINER Allegro

RUN apt-get update && apt-get install -y unzip wget \
  && apt-get clean && rm -rf /var/lib/apt/lists/*

ENV ARCHIVE_NAME="hermes-consumers-latest.zip"
ENV URL="http://box.allegrotech.io/hermes/${ARCHIVE_NAME}"

RUN wget -q "${URL}" -O "/tmp/${ARCHIVE_NAME}" \
  && unzip "/tmp/${ARCHIVE_NAME}" -d /opt \
  && rm "/tmp/${ARCHIVE_NAME}" \
  && mv /opt/hermes-consumers-* /opt/hermes-consumers

ENV HERMES_CONSUMERS_OPTS="-Darchaius.configurationSource.additionalUrls=file:///etc/hermes/consumers.properties"
ADD consumers.properties /etc/hermes/consumers.properties

CMD /opt/hermes-consumers/bin/hermes-consumers
