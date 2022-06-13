#!/usr/bin/env bash

set -ex

docker build --tag=aleph-shutdown-stacktrace:latest .

docker run --rm --name aleph-shutdown-stacktrace aleph-shutdown-stacktrace:latest \
      -p 8080:8080
