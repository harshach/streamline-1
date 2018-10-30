#!/bin/bash

set -x

/home/udocker/uworc/hortonworks-streamline-0.6.0-SNAPSHOT/bootstrap/bootstrap-storage.sh migrate ${UBER_RUNTIME_ENVIRONMENT}
/home/udocker/uworc/hortonworks-streamline-0.6.0-SNAPSHOT/bin/streamline start ${UBER_RUNTIME_ENVIRONMENT} ${UBER_PORT_HTTP}

echo 'waiting for UI...'
until $(curl --output /dev/null --silent --head --fail http://localhost:${UBER_PORT_HTTP}); do
  >&2 printf "."
  sleep 5
done
echo ""

/home/udocker/uworc/hortonworks-streamline-0.6.0-SNAPSHOT/bootstrap/bootstrap.sh migrate ${UBER_RUNTIME_ENVIRONMENT}
/home/udocker/uworc/hortonworks-streamline-0.6.0-SNAPSHOT/bin/streamline stop

# Starting the streamline server in foreground
/home/udocker/uworc/hortonworks-streamline-0.6.0-SNAPSHOT/bin/streamline-server-start.sh /home/udocker/uworc/hortonworks-streamline-0.6.0-SNAPSHOT/conf/streamline-${UBER_RUNTIME_ENVIRONMENT}.yaml ${UBER_PORT_HTTP}