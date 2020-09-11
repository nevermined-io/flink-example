FROM flink:scala_2.11-java11
LABEL maintainer="Keyko <root@keyko.io>"

RUN apt-get update \
    && apt-get install openjdk-11-jdk maven -y \
    && apt-get clean

WORKDIR /

RUN update-alternatives --set java /usr/lib/jvm/java-11-openjdk-amd64/bin/java

ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
ENV PATH=/usr/lib/jvm/java-11-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

COPY src /src
COPY pom.xml /
COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh

RUN mvn clean package
RUN mv target/nevermined-flink-example-*.jar /nevermined-example.jar

ENTRYPOINT ["/docker-entrypoint.sh"]
