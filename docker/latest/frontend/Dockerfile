FROM java:8

MAINTAINER Allegro

RUN apt-get update && apt-get install -y unzip wget \
  && apt-get clean && rm -rf /var/lib/apt/lists/*

ENV ARCHIVE_NAME="hermes-frontend-latest.zip"
ENV URL="http://box.allegrotech.io/hermes/${ARCHIVE_NAME}"

RUN wget -q "${URL}" -O "/tmp/${ARCHIVE_NAME}" \
  && unzip "/tmp/${ARCHIVE_NAME}" -d /opt \
  && rm "/tmp/${ARCHIVE_NAME}" \
  && mv /opt/hermes-frontend-* /opt/hermes-frontend

ENV HERMES_FRONTEND_OPTS="-Darchaius.configurationSource.additionalUrls=file:///etc/hermes/frontend.properties"
ADD frontend.properties /etc/hermes/frontend.properties

CMD /opt/hermes-frontend/bin/hermes-frontend
