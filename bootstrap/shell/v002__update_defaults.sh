#!/usr/bin/env bash

#
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


# defaults
verbose=false
shell_dir=$(dirname $0)
bootstrap_dir=${shell_dir}/..
[ -z $UBER_RUNTIME_ENVIRONMENT ] && UBER_RUNTIME_ENVIRONMENT=dev
CONFIG_FILE_PATH=${bootstrap_dir}/../conf/streamline-${UBER_RUNTIME_ENVIRONMENT}.yaml

# Which java to use
if [ -z "${JAVA_HOME}" ]; then
  JAVA="java"
else
  JAVA="${JAVA_HOME}/bin/java"
fi

function run_cmd {
  cmd=$*
  if [[ $verbose == "true" ]]
  then
    echo $cmd
  fi
  response=$(eval $cmd)

  if [ $? -ne 0 ] ; then
     echo "Command failed to execute, quiting the migration ..."
     exit 1
  fi

  if [[ $verbose == "true" ]]
  then
    echo $response
  else
    echo $response | grep -o '"responseMessage":[^"]*"[^"]*"'
  fi
  echo "--------------------------------------"
}

function getId {
  str=$1
  echo $str | grep -o -E "\"id\":[0-9]+" | head -n1 | cut -d : -f2
}

function getAdminRoleId {
  cmd="curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -sS -X GET ${CATALOG_ROOT_URL}/roles?name=ROLE_ADMIN -H 'Content-Type: application/json' ${HTTP_HEADERS_FOR_CURL}"
  response=$(eval $cmd)
  getId "$response"
}

function put {
  uri=$1/$2
  data=$3
  cmd="curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -sS -X PUT ${CATALOG_ROOT_URL}$uri --data @$data -H 'Content-Type: application/json' ${HTTP_HEADERS_FOR_CURL}"
  echo "PUT $data"
  run_cmd $cmd
}

function post {
  uri=$1
  data=$2
  cmd="curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -sS -X POST ${CATALOG_ROOT_URL}$uri --data @$data -H 'Content-Type: application/json' ${HTTP_HEADERS_FOR_CURL}"
  echo "POST $data"
  run_cmd $cmd
}

function add_sample_topology_component_bundle {
  echo "POST sample_bundle"
  cmd="curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -sS -X POST -i -F topologyComponentBundle=@$bootstrap_dir/kafka-topology-bundle ${CATALOG_ROOT_URL}/streams/componentbundles/SOURCE/ ${HTTP_HEADERS_FOR_CURL}"
  run_cmd $cmd
}

function add_topology_component_bundle {
  uri=$1
  data=$2
  cmd="curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -sS -X POST -i -F topologyComponentBundle=@$data ${CATALOG_ROOT_URL}$uri ${HTTP_HEADERS_FOR_CURL}"
  echo "POST $data"
  run_cmd $cmd
}

function put_topology_component_bundle {
  uri=$1
  data=$2
  subType=$3
  out=$(curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X GET -H "Content-Type: application/json" -H "Cache-Control: no-cache" "${CATALOG_ROOT_URL}$uri?subType=${subType}&engine=STORM" ${HTTP_HEADERS_FOR_CURL})
  bundleId=$(getId $out)
  cmd="curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -sS -X PUT -i -F topologyComponentBundle=@$data ${CATALOG_ROOT_URL}$uri/$bundleId ${HTTP_HEADERS_FOR_CURL}"
  echo "PUT $data"
  run_cmd $cmd
}

function put_service_bundle {
  uri=$1
  data=$2
  cmd="curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -sS -X PUT ${CATALOG_ROOT_URL}$uri --data @$data -H 'Content-Type: application/json' ${HTTP_HEADERS_FOR_CURL}"
  echo "PUT $data"
  run_cmd $cmd

}

function update_custom_processors_with_digest {
  echo "Running update script to update all custom processors with digests"
  cp_upgrade_uri_suffix="/streams/componentbundles/PROCESSOR/custom/upgrade"
  cmd="curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -sS -X PUT ${CATALOG_ROOT_URL}$cp_upgrade_uri_suffix -H 'Content-Type: application/json' ${HTTP_HEADERS_FOR_CURL}"
  run_cmd $cmd
}

#Below command to update storm version will be called by RE script. Need to remove later. Adding now for convenience
update_storm_version_command="$bootstrap_dir/update-storm-version.sh 1.1.0.3.0.0.0-453"
run_cmd $update_storm_version_command

#---------------------------------------------
# Get catalogRootUrl from configuration file
#---------------------------------------------

CONF_READER_MAIN_CLASS=com.hortonworks.streamline.storage.tool.sql.PropertiesReader

for file in "${bootstrap_dir}"/lib/*.jar;
do
    CLASSPATH="$CLASSPATH":"$file"
done

CATALOG_ROOT_URL_PROPERTY_KEY=catalogRootUrl
component_dir=${bootstrap_dir}/components
service_dir=${bootstrap_dir}/services
user_role_dir=${bootstrap_dir}/users_roles
storm_dir=${bootstrap_dir}/engines/storm
piper_dir=${bootstrap_dir}/engines/piper
athenax_dir=${bootstrap_dir}/engines/athenax


echo "Configuration file: ${CONFIG_FILE_PATH}"

CATALOG_ROOT_URL=`exec ${JAVA} -cp ${CLASSPATH} ${CONF_READER_MAIN_CLASS} ${CONFIG_FILE_PATH} ${CATALOG_ROOT_URL_PROPERTY_KEY}`

# if it doesn't exit with code 0, just give up
if [ $? -ne 0 ]; then
  exit 1
fi

if [ ! -z "$UBER_PORT_HTTP" ]; then
  ORIGINAL_CATALOG_URL_PORT=`echo $CATALOG_ROOT_URL | awk -F[/:] '{print $5}'`
  CATALOG_ROOT_URL=${CATALOG_ROOT_URL/$ORIGINAL_CATALOG_URL_PORT/$UBER_PORT_HTTP}
fi

echo "Catalog Root URL: ${CATALOG_ROOT_URL}"
echo "Component bundle Root dir: ${component_dir}"
echo "Service bundle Root dir: ${service_dir}"
echo "User/Role bundle Root dir: ${user_role_dir}"

function update_bundles {
    # === Source ===
    put_topology_component_bundle /streams/componentbundles/SOURCE ${storm_dir}/components/sources/kafka-source-topology-component.json KAFKA
    # === Processor ===

    # === Sink ===
    put_topology_component_bundle /streams/componentbundles/SINK ${storm_dir}/components/sinks/hdfs-sink-topology-component.json HDFS
    put_topology_component_bundle /streams/componentbundles/SINK ${storm_dir}/components/sinks/jdbc-sink-topology-component.json JDBC
    put_topology_component_bundle /streams/componentbundles/SINK ${storm_dir}/components/sinks/hive-sink-topology-component.json HIVE
    put_topology_component_bundle /streams/componentbundles/SINK ${storm_dir}/components/sinks/druid-sink-topology-component.json DRUID
    # === Topology ===
    put_topology_component_bundle /streams/componentbundles/TOPOLOGY ${storm_dir}/topology/storm-topology-component.json TOPOLOGY
    # === Service Bundle ===
    put_service_bundle /servicebundles/KAFKA ${service_dir}/kafka-bundle.json
    put_service_bundle /servicebundles/STORM ${service_dir}/storm-bundle.json
    put_service_bundle /servicebundles/ZOOKEEPER ${service_dir}/zookeeper-bundle.json
    post /servicebundles ${service_dir}/druid-bundle.json
}

function add_udfs {
        dir=$(dirname $0)/../..

        jarFile="$(find ${bootstrap_dir}/udf-jars/ -name 'streamline-functions-*.jar')"
        if [[ ! -f ${jarFile} ]]
        then
          # try local build path
          jarFile="$(find ${dir}/streams/functions/target/ -name 'streamline-functions-*.jar')"
          if [[ ! -f ${jarFile} ]]
          then
            echo "Could not find streamline-functions jar, Exiting ..."
            exit 1
          fi
        fi

        echo "  - variance"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt  -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"VARIANCE_FN", "displayName": "VARIANCE", "description": "Variance", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Variance", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - variancep"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"VARIANCEP_FN", "displayName": "VARIANCEP", "description": "Population variance", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Variancep", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - stddev"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"STDDEV_FN", "displayName": "STDDEV", "description": "Standard deviation", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Stddev", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - stddevp"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt  -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"STDDEVP_FN", "displayName": "STDDEVP", "description": "Population standard deviation", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Stddevp", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - concat"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"CONCAT_FN", "displayName": "CONCAT", "description": "Concatenate", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Concat", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - count"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"COUNT_FN", "displayName": "COUNT","description": "Count", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.LongCount", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - substring"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"SUBSTRING_FN", "displayName": "SUBSTRING", "description": "Returns sub-string of a string starting at some position", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Substring", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - substring"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"SUBSTRING_FN", "displayName": "SUBSTRING", "description": "Returns a sub-string of a string starting at some position and is of given length", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Substring2", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - position"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"POSITION_FN", "displayName": "POSITION", "description": "Returns the position of the first occurrence of sub-string in  a string", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Position", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - position"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"POSITION_FN", "displayName": "POSITION", "description": "Returns the position of the first occurrence of sub-string in  a string starting the search from an index", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Position2", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - avg"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"AVG_FN", "displayName": "AVG","description": "Average", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Mean", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - trim"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"TRIM_FN", "displayName": "TRIM", "description": "Returns a string with any leading and trailing whitespaces removed", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Trim", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - trim2"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"TRIM_FN", "displayName": "TRIM2", "description": "Returns a string with specified leading and trailing character removed", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Trim2", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - ltrim"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"LTRIM_FN", "displayName": "LTRIM", "description": "Removes leading whitespaces from the input", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Ltrim", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - ltrim2"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"LTRIM_FN", "displayName": "LTRIM", "description": "Removes specified leading character from the input", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Ltrim2", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - rtrim"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"RTRIM_FN", "displayName": "RTRIM", "description": "Removes trailing whitespaces from the input", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Rtrim", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - rtrim2"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"RTRIM_FN", "displayName": "RTRIM", "description": "Removes specified trailing character from the input", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Rtrim2", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - overlay"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"OVERLAY_FN", "displayName": "OVERLAY", "description": "Replaces a substring of a string with a replacement string", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Overlay", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - overlay"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"OVERLAY_FN", "displayName": "OVERLAY", "description": "Replaces a substring of a string with a replacement string", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Overlay2", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - divide"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"DIVIDE_FN", "displayName": "DIVIDE", "description": "Divides input with given divisor", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Divide", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - exists"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"EXISTS_FN", "displayName": "EXISTS", "description": "returns 1 if input is not null otherwise returns 0", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Exists", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        echo "  - sum"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"SUM_FN", "displayName": "SUM","description": "Sum", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.NumberSum", "builtin":true};type=application/json' "${HTTP_HEADERS_FOR_CURL}"

        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"ABS", "displayName": "ABS", "description": "Returns the absolute value of the argument", "type":"FUNCTION", "argTypes": ["DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"ACOS", "displayName": "ACOS", "description": "Returns the arccosine of the argument", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"ASIN", "displayName": "ASIN", "description": "Returns the arcsine of the argument", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"ATAN", "displayName": "ATAN", "description": "Returns the arc tangent of the argument", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"ATAN2", "displayName": "ATAN2", "description": "Returns the arc tangent of the argument coordinates", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE", "LONG|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"CEIL", "displayName": "CEIL", "description": "Rounds up, returning the smallest integer that is greater than or equal to the argument", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE"], "returnType": "LONG", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"CHARACTER_LENGTH", "displayName": "CHARACTER_LENGTH", "description": "Returns the number of characters in a character string", "type":"FUNCTION", "argTypes": ["LONG"], "returnType": "STRING", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"CHAR_LENGTH", "displayName": "CHAR_LENGTH", "description": "Returns the number of characters in a character string", "type":"FUNCTION", "argTypes": ["LONG"], "returnType": "STRING", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"COS", "displayName": "COS", "description": "Returns the cosine of the argument", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"COT", "displayName": "COT", "description": "Returns the cotangent of the argument", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"DEGREES", "displayName": "DEGREES", "description": "Converts the argument from radians to degrees", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"EXP", "displayName": "EXP", "description": "Returns e raised to the power of the argument", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"FLOOR", "displayName": "FLOOR", "description": "Rounds down, returning the largest integer that is less than or equal to the argument", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE"], "returnType": "LONG", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"INITCAP", "displayName": "INITCAP", "description": "Convert the first letter of each word in the argument to upper case and the rest to lower case", "type":"FUNCTION", "argTypes": ["STRING"], "returnType": "STRING", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"LN", "displayName": "LN", "description": "Returns the natural logarithm of the argument", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"LOG10", "displayName": "LOG10", "description": "Returns the base 10 logarithm of the argument", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"LOWER", "displayName": "LOWER", "description": "Returns a character string converted to lower case", "type":"FUNCTION", "argTypes": ["STRING"], "returnType": "STRING", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"MOD", "displayName": "MOD", "description": "Returns the remainder (modulus) of the first argument divided by the second argument", "type":"FUNCTION", "argTypes": ["LONG", "LONG"], "returnType": "LONG", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"PI", "displayName": "PI", "description": "Returns a value that is closer than any other value to pi", "type":"FUNCTION", "argTypes": , "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"POWER", "displayName": "POWER", "description": "Returns the value of the first argument raised to the power of the second argument", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE", "LONG|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"RADIANS", "displayName": "RADIANS", "description": "Converts the argument from degrees to radians", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"RAND", "displayName": "RAND", "description": "Generates a random double between 0 and 1 (inclusive)", "type":"FUNCTION", "argTypes": , "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"RAND_INTEGER", "displayName": "RAND_INTEGER", "description": "Generates a random integer between 0 and the argument (exclusive)", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE"], "returnType": "LONG", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"ROUND", "displayName": "ROUND", "description": "Rounds the first argument to the xth places right to the decimal point, where x is the second argument", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE", "LONG"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"SIGN", "displayName": "SIGN", "description": "Returns the signum of the argument", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE"], "returnType": "LONG", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"SIN", "displayName": "SIN", "description": "Returns the sine of the argument", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"SQRT", "displayName": "SQRT", "description": "Returns the square root of the argument", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"TAN", "displayName": "TAN", "description": "Returns the tangent of the argument", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"TRUNCATE", "displayName": "TRUNCATE", "description": "Truncates the first argument to the xth places right to the decimal point, where x is the second argument", "type":"FUNCTION", "argTypes": ["LONG|DOUBLE", "LONG"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"UPPER", "displayName": "UPPER", "description": "Returns a character string converted to upper case", "type":"FUNCTION", "argTypes": ["STRING"], "returnType": "STRING", "className":"builtin", "builtin":true};type=application/json' -F builtin=true "${HTTP_HEADERS_FOR_CURL}"
}

function main {
    echo ""
    echo "===================================================================================="
    echo "Running bootstrap.sh will create streamline default components, notifiers, udfs and roles"

    update_bundles
    add_udfs
    update_custom_processors_with_digest
}

main
