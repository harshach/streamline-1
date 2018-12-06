#!/usr/bin/env bash

export STREAMLINE_USER=`whoami`
export HTTP_HEADERS_FOR_CURL='-HX-Auth-Params-Email:root@uber.com'
export HTTP_HEADERS_FOR_OTHERS='-H X-Auth-Params-Email:root@uber.com'
export STREAMLINE_HEAP_OPTS='-Xmx4G -Xms4G'
