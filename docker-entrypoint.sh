#!/usr/bin/env bash

env

/opt/flink/bin/start-cluster.sh

tail -f /dev/null
