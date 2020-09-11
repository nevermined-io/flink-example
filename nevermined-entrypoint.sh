#!/usr/bin/env bash

/opt/flink/bin/flink run -c io.keyko.nevermined.examples.WordCountJob \
  $NEVERMINED_TRANSFORMATIONS_PATH/nevermined-flink-example-0.1.0.jar \
  --input $NEVERMINED_INPUTS_PATH --output $NEVERMINED_OUTPUTS_PATH/result.csv
