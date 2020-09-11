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

The application is packed in the docker container `keykoio/nevermined-flink-example`. Here the instructions to run the application:
 
```
# Pulling and running the container
docker run --name nevermined-flink --detach keykoio/nevermined-flink-example

# We are gonna execute the flink job counting the words of /opt/flink/README.txt and generating the result in /tmp/result.csv
docker exec -it nevermined-flink /opt/flink/bin/flink run -c io.keyko.nevermined.examples.WordCountJob /nevermined-example.jar --input /opt/flink/README.txt --output /tmp/result.csv

# We get the results
docker exec -it nevermined-flink cat /tmp/result.csv

# Don't forget to stop the container when you are don
docker stop nevermined-flink
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

At this point we assume you have Nevermined running in your local or in a remote environment. If not please check the 
[Nevermined Tools](https://github.com/keyko-io/nevermined-tools).

All the following steps can be done programatically using the Nevermined SDK's or the CLI. For simplicity here we are 
gonna show how to do it via CLI:

````bash
# First we are gonna publish the dataset we want to count the words
ncli assets publish-dataset --service compute --title "RFC IP over Avian Carriers" --urls https://tools.ietf.org/rfc/rfc2549.txt \
    --dateCreated "1999-04-01T10:00:000Z" --author "David Waitzman" --contentType "text/plain" --price "0"

did:nv:c0c0c0

# Now we are gonna register our Flink example
ncli assets publish-algorithm --title "Flink word count" --url https://github.com/keyko-io/nevermined-flink-example/releases/download/v0.1.0/nevermined-flink-example-0.2.0.jar \
    --dateCreated "2020-09-11T12:00:000Z" --author "aitor" --contentType "application/java-archive" \ 
    --language java  --container "flink:scala_2.11-java11" \ 
    --entrypoint "https://github.com/keyko-io/nevermined-flink-example/releases/download/v0.2.0/nevermined-flink-example-0.2.0.jar"

did:nv:a1a1a1

# Having the DIDs of the dataset and the algorithm we register a workflow relating both
ncli assets publish-workflow --title "RFC Word count" --dateCreated "2020-09-11T12:00:000Z" --author "aitor" \
    --inputs did:nv:c0c0c0 --transformation did:nv:a1a1a1 --container flink:scala_2.11-java11

did:nv:b2b2b2

# Now we have everything published we can proceed to execute the workflow
ncli assets exec did:nv:c0c0c0 --workflow did:nv:b2b2b2

0xefefefefef

# We can monitor the execution of the job
ncli monitor exec 0xefefefefef

did:nv:01010101

# And after the job has finished, we can get access to our results
ncli assets download did:nv:01010101

```` 