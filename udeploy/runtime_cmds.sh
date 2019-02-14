#!/bin/bash

set -x

/home/udocker/uworc/hortonworks-streamline-0.1.2-alpha/bootstrap/bootstrap-storage.sh migrate
/home/udocker/uworc/hortonworks-streamline-0.1.2-alpha/bin/streamline start

echo 'waiting for UI...'
until $(curl --output /dev/null --silent --head --fail http://localhost:${UBER_PORT_HTTP}); do
  >&2 printf "."
  sleep 5
done
echo ""

/home/udocker/uworc/hortonworks-streamline-0.1.2-alpha/bootstrap/bootstrap.sh migrate
/home/udocker/uworc/hortonworks-streamline-0.1.2-alpha/bin/streamline stop

# Starting the streamline server in foreground
/home/udocker/uworc/hortonworks-streamline-0.1.2-alpha/bin/streamline start-fg
