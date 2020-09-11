# Nevermined Flink project example

Nevermined allows publishing algorithms into the platform with the intention of running them on top of data given by an
external data provider.

Nevermined is an agnostic platform allowing to execute different kind of jobs in the data provider/owner infrastructure 
without moving the data of their premises. 

This example shows as a generic flink application, can be packaged and executed in Nevermined on top of data provided by
 a third-party entity.
 
This example provides a simple flink batch word count. This job calculates the number of times that a word is included
in a file. The integration of this job with Nevermined allows to obtain this final result without making necessary to
provide the full input data to the algorithm provider.

## Getting started

### Requirements

To compile and run this project you need:

* Java 11
* Maven 3
* Apache Flink 1.11 (scala 2.11)

If you want to run using Docker instances you need Docker and Docker compose.

### How to compile?

The following command should compile the application, execute the tests and build the application jar:

```bash
mvn clean package
```

### How to run the application locally?

First you need to have Flink installed in your environment and run the cluster using the command:

```bash
$FLINK_HOME/bin/start-cluster.sh
```

To run the application you can execute the following command:

```bash
$FLINK_HOME/bin/flink run -c io.keyko.nevermined.examples.WordCountJob target/nevermined-flink-example-*.jar --input $INPUT_FILE --output $OUTPUT_FILE
```

### How to run the application in Docker

```
docker pull flink:1.11-scala_2.11-java11
```

## Releasing the application

As soon as you have a new version of the application to be released you only need to tag the application

```bash
git tag $TAG
git push origin $TAG
```

This will generate a new release on github:

```bash
https://github.com/keyko-io/nevermined-flink-example/releases
```

Where you will find available the application binary ready to be shipped. For example for the tag `v0.1.0`:
https://github.com/keyko-io/nevermined-flink-example/releases/download/v0.1.0/nevermined-flink-example-0.1.0.jar

## Nevermined integration

