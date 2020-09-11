FROM flink
RUN mkdir /flink
COPY target/nevermined-flink-example-*.jar /flink/