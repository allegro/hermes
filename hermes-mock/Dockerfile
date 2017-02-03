FROM java:8

COPY wiremock/wiremock-standalone-2.5.0.jar /root

COPY runServer.sh /root/runServer.sh
RUN chmod +x /root/runServer.sh

ADD mappings /root/mappings

WORKDIR /root

ENTRYPOINT ["./runServer.sh"]
