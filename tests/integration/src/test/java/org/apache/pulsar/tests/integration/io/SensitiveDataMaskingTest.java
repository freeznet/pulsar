/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.tests.integration.io;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.tests.integration.docker.ContainerExecResult;
import org.apache.pulsar.tests.integration.io.sinks.SinkTester;
import org.apache.pulsar.tests.integration.io.sinks.SensitiveSinkTester;
import org.apache.pulsar.tests.integration.suites.PulsarTestSuite;
import org.apache.pulsar.tests.integration.topologies.PulsarCluster;
import org.testng.annotations.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * Integration test to verify that sensitive configuration data is properly masked
 * in logs when using the Pulsar IO connectors.
 */
@Slf4j
public class SensitiveDataMaskingTest extends PulsarTestSuite {

    private static final String TENANT = "public";
    private static final String NAMESPACE = "default";
    private static final String SINK_NAME = "sensitive-data-sink";
    private static final String INPUT_TOPIC_NAME = "sensitive-data-input-topic";
    private static final int NUM_MESSAGES = 10;

    @Test(groups = {"sink"})
    public void testSensitiveDataMaskedInLogs() throws Exception {
        final String secret = "THIS_IS_A_SECRET_PASSWORD";

        // Setup a test sink with sensitive data
        SensitiveSinkTester tester = new SensitiveSinkTester();
        tester.setSensitiveSecret(secret);
        
        // Create the sink connector
        PulsarCluster cluster = pulsarCluster;
        tester.startServiceContainer(cluster);
        tester.prepareSink();
        
        // Create test topic and consumer
        ensureSubscriptionCreated(
                INPUT_TOPIC_NAME, 
                TENANT + "/" + NAMESPACE + "/" + SINK_NAME, 
                tester.getInputTopicSchema());
                
        // Submit the sink connector with sensitive data
        submitSinkConnector(tester, TENANT, NAMESPACE, SINK_NAME, INPUT_TOPIC_NAME);
        
        // Produce messages to the input topic
        Map<String, String> kvs = produceMessagesToInputTopic(
                INPUT_TOPIC_NAME, NUM_MESSAGES, tester);
        
        // Wait for processing to complete
        tester.validateSinkResult(kvs);
        
        // Check the logs for sensitive data masking
        ContainerExecResult result = cluster.getAnyWorker().execCmd("cat", "/pulsar/logs/functions-worker.log");
        String logs = result.getStdout();
        
        // Verify that configuration was logged
        assertTrue(logs.contains("sinkConfig"), 
               "Logs should contain sink configuration");
        
        // Verify sensitive data is masked
        assertFalse(logs.contains(secret), 
                "Logs should not contain the sensitive password");
        assertTrue(logs.contains("********"), 
               "Logs should contain masked password");
                
        // Clean up the sink
        deleteSink(TENANT, NAMESPACE, SINK_NAME);
        tester.close();
    }

    private void ensureSubscriptionCreated(String inputTopicName, String subscriptionName, Schema<?> schema) throws Exception {
        // Use helper method from PulsarIOTestRunner
        new PulsarIOTestHelper(pulsarCluster).ensureSubscriptionCreated(inputTopicName, subscriptionName, schema);
    }

    private Map<String, String> produceMessagesToInputTopic(String inputTopicName, int numMessages, SinkTester<?> tester) throws Exception {
        // Use helper method from PulsarIOTestRunner
        return new PulsarIOTestHelper(pulsarCluster).produceMessagesToInputTopic(inputTopicName, numMessages, tester);
    }

    private void submitSinkConnector(SinkTester<?> tester, String tenant, String namespace, String sinkName, String inputTopicName) throws Exception {
        // Use helper method from PulsarIOSinkRunner
        new PulsarIOTestHelper(pulsarCluster).submitSinkConnector(tester, tenant, namespace, sinkName, inputTopicName);
    }

    private void deleteSink(String tenant, String namespace, String sinkName) throws Exception {
        // Use helper method from PulsarIOSinkRunner
        new PulsarIOTestHelper(pulsarCluster).deleteSink(tenant, namespace, sinkName);
    }
    
    /**
     * Helper class to reuse methods from PulsarIOTestRunner.
     */
    private static class PulsarIOTestHelper extends PulsarIOTestRunner {
        PulsarIOTestHelper(PulsarCluster cluster) {
            super(cluster, "process");
        }
        
        public void submitSinkConnector(SinkTester<?> tester, String tenant, String namespace, String sinkName, String inputTopicName) throws Exception {
            String[] commands;
            if (tester.getSinkType() != SinkTester.SinkType.UNDEFINED) {
                commands = new String[] {
                        PulsarCluster.ADMIN_SCRIPT,
                        "sink", "create",
                        "--tenant", tenant,
                        "--namespace", namespace,
                        "--name", sinkName,
                        "--sink-type", tester.sinkType().getValue().toLowerCase(),
                        "--sinkConfig", new com.google.gson.Gson().toJson(tester.sinkConfig()),
                        "--inputs", inputTopicName,
                        "--ram", String.valueOf(RUNTIME_INSTANCE_RAM_BYTES)
                };
            } else {
                commands = new String[] {
                        PulsarCluster.ADMIN_SCRIPT,
                        "sink", "create",
                        "--tenant", tenant,
                        "--namespace", namespace,
                        "--name", sinkName,
                        "--archive", tester.getSinkArchive(),
                        "--classname", tester.getSinkClassName(),
                        "--sinkConfig", new com.google.gson.Gson().toJson(tester.sinkConfig()),
                        "--inputs", inputTopicName,
                        "--ram", String.valueOf(RUNTIME_INSTANCE_RAM_BYTES)
                };
            }
            log.info("Run command : {}", StringUtils.join(commands, ' '));
            ContainerExecResult result = pulsarCluster.getAnyWorker().execCmd(commands);
            assertTrue(
                result.getStdout().contains("Created successfully"),
                result.getStdout());
        }
        
        public void deleteSink(String tenant, String namespace, String sinkName) throws Exception {
            final String[] commands = {
                PulsarCluster.ADMIN_SCRIPT,
                "sink",
                "delete",
                "--tenant", tenant,
                "--namespace", namespace,
                "--name", sinkName
            };

            ContainerExecResult result = pulsarCluster.getAnyWorker().execCmd(commands);
            assertTrue(
                result.getStdout().contains("Deleted successfully"),
                result.getStdout()
            );
        }
    }
} 