FROM java:8-jre
MAINTAINER Nick Franklin <n.o.franklin@gmail.com>

# Copy binaries/config
RUN mkdir /target
COPY ThreadMarketData.sh /
COPY thread.properties /
COPY logging.properties /
COPY target/MarketData-0.0.1-SNAPSHOT-jar-with-dependencies.jar /target/

EXPOSE 2001

CMD ["/ThreadMarketData.sh"]