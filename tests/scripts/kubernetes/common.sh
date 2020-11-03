#!/usr/bin/env bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

PULSAR_CHARTS_RELEASE_VERSION="1.3.2"
PULSAR_HOME=$(unset CDPATH && cd $(dirname "${BASH_SOURCE[0]}")/../../.. && pwd)
TESTS_HOME=${PULSAR_HOME}/tests
KUBERNETES_HOME=${TESTS_HOME}/k8s
CHARTS_HOME=${KUBERNETES_HOME}/charts-pulsar-${PULSAR_CHARTS_RELEASE_VERSION}

function kubernetes::ensure_charts_release() {
    echo "Installing pulsar charts release v$PULSAR_CHARTS_RELEASE_VERSION..."
    tmpfile=$(mktemp)
    trap "test -f $tmpfile && rm $tmpfile" RETURN
    mkdir -p $KUBERNETES_HOME
    curl --retry 10 -L -s https://github.com/streamnative/charts/archive/pulsar-${PULSAR_CHARTS_RELEASE_VERSION}.tar.gz | tar -C $KUBERNETES_HOME -zxvf - 
    cd $CHARTS_HOME
    ls -l
}

kubernetes::ensure_charts_release
FUNCTION="true" /bin/bash -e ${CHARTS_HOME}/.ci/chart_test.sh .ci/clusters/values-function.yaml