#!/bin/bash
# Copyright 2017 Hortonworks.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at

#   http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

if [ $# -ne 1 ];
then
        echo "USAGE: $0 [-daemon] STREAMLINE_CONFIG_YAML"
        exit 1
fi

# Resolve links - $0 may be a softlink
PRG="${0}"

while [ -h "${PRG}" ]; do
  ls=`ls -ld "${PRG}"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "${PRG}"`/"$link"
  fi
done

bin_dir=`dirname ${PRG}`
base_dir=`cd ${bin_dir}/..;pwd`

if [ "x$STREAMLINE_HEAP_OPTS" = "x" ]; then
    export STREAMLINE_HEAP_OPTS="-Xmx1G -Xms1G"
fi

EXTRA_ARGS="-name StreamlineServer"

# create logs directory
if [ "x$LOG_DIR" = "x" ]; then
    LOG_DIR="$base_dir/logs"
fi

if [ ! -d "$LOG_DIR" ]; then
    mkdir -p "$LOG_DIR"
fi

# Exclude jars not necessary for running commands.
regex="\-(test|src|javadoc|runtime-storm).+(\.jar|\.jar\.asc)$"
should_include_file() {
    if [ "$INCLUDE_TEST_JARS" = true ]; then
        return 0
    fi
    testfile=$1
    if [ -z "$(echo "$testfile" | egrep "$regex")" ] ; then
        return 0
    else
        return 1
    fi
}

# classpath addition for release
shopt -s nullglob

for file in "$base_dir"/libs/*.jar;
do
    if should_include_file "$(basename $file)"; then
        CLASSPATH="$CLASSPATH":"$file"
    fi
done

if [ ! -z "$HADOOP_CONF_DIR" ]; then
 CLASSPATH=$CLASSPATH:$HADOOP_CONF_DIR;
fi

echo "CLASSPATH: ${CLASSPATH}"

CLASSPATH="/home/udocker/uworc/hortonworks-streamline-0.6.0-SNAPSHOT/libs/upkiclient-1.0-20190501.232008-4-shaded.jar":$CLASSPATH
#JAAS config file params
if [ -z "$STREAMLINE_KERBEROS_PARAMS" ]; then
    STREAMLINE_KERBEROS_PARAMS=""
fi

STREAMLINE_VERSION_FILE="-Dstreamline.version.file=$base_dir/VERSION"

COMMAND=$1

case $COMMAND in
  -name)
    DAEMON_NAME=$2
    CONSOLE_OUTPUT_FILE=$LOG_DIR/$DAEMON_NAME.out
    shift 2
    ;;
  -daemon)
    DAEMON_MODE=true
    shift
    ;;
  *)
    ;;
esac

# Which java to use
if [ -z "$JAVA_HOME" ]; then
  JAVA="java"
else
  JAVA="$JAVA_HOME/bin/java"
fi

# add streamline base directory
STREAMLINE_OPTS="-Dstreamline.home=${base_dir} "

# Set Debug options if enabled
if [ "x$STREAMLINE_DEBUG" != "x" ]; then

    # Use default ports
    DEFAULT_JAVA_DEBUG_PORT="5005"

    if [ -z "$JAVA_DEBUG_PORT" ]; then
        JAVA_DEBUG_PORT="$DEFAULT_JAVA_DEBUG_PORT"
    fi

    # Use the defaults if JAVA_DEBUG_OPTS was not set
    DEFAULT_JAVA_DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=${DEBUG_SUSPEND_FLAG:-n},address=$JAVA_DEBUG_PORT"
    if [ -z "$JAVA_DEBUG_OPTS" ]; then
        JAVA_DEBUG_OPTS="$DEFAULT_JAVA_DEBUG_OPTS"
    fi

    echo "Enabling Java debug options: $JAVA_DEBUG_OPTS"
    STREAMLINE_OPTS="$JAVA_DEBUG_OPTS $STREAMLINE_OPTS"
fi

# JVM performance options
if [ -z "$STREAMLINE_JVM_PERFORMANCE_OPTS" ]; then
  STREAMLINE_JVM_PERFORMANCE_OPTS="-server -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -XX:+CMSScavengeBeforeRemark -XX:+DisableExplicitGC -Djava.awt.headless=true"
fi


[ -z $UBER_PORT_HTTP ] && UBER_PORT_HTTP=8080
STREAMLINE_CUSTOM_PORT_COMMAND="-Ddw.server.applicationConnectors[0].port=$UBER_PORT_HTTP"

# Launch mode
if [ "x$DAEMON_MODE" = "xtrue" ]; then
  nohup $JAVA $STREAMLINE_HEAP_OPTS "$STREAMLINE_CUSTOM_PORT_COMMAND" $STREAMLINE_JVM_PERFORMANCE_OPTS $STREAMLINE_KERBEROS_PARAMS $STREAMLINE_VERSION_FILE -cp $CLASSPATH $STREAMLINE_OPTS "com.hortonworks.streamline.webservice.StreamlineApplication" "server" "$1" > "$CONSOLE_OUTPUT_FILE" 2>&1 < /dev/null &
else
  exec $JAVA $STREAMLINE_HEAP_OPTS "$STREAMLINE_CUSTOM_PORT_COMMAND" $STREAMLINE_JVM_PERFORMANCE_OPTS $STREAMLINE_KERBEROS_PARAMS $STREAMLINE_VERSION_FILE -cp $CLASSPATH $STREAMLINE_OPTS "com.hortonworks.streamline.webservice.StreamlineApplication" "server" "$1"
fi
