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
package org.apache.pulsar.tests.integration.io.sinks;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.tests.integration.topologies.PulsarCluster;
import org.testcontainers.containers.GenericContainer;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

/**
 * A sink tester specifically for testing sensitive data masking.
 * Based on the JDBC PostgreSQL tester but simplified for testing purposes.
 */
@Slf4j
public class SensitiveSinkTester extends SinkTester<GenericContainer<?>> {

    private static final String NAME = "sensitive-data";
    
    /**
     * Constructor.
     */
    public SensitiveSinkTester() {
        super(NAME, SinkType.UNDEFINED);
        
        // Add default configuration
        sinkConfig.put("normalField", "normalValue");
    }
    
    /**
     * Sets a sensitive secret to be used in the configuration.
     * This will be verified to be masked in the logs.
     */
    public void setSensitiveSecret(String secret) {
        // Add sensitive field with @FieldDoc(sensitive=true) annotation in the sink
        sinkConfig.put("sensitivePassword", secret);
    }

    @Override
    public String getSinkClassName() {
        return "org.apache.pulsar.io.common.SensitiveSink";
    }

    @Override
    public String getSinkArchive() {
        return "builtin://dummy-connector";
    }

    @Override
    public Schema<?> getInputTopicSchema() {
        return Schema.STRING;
    }

    @Override
    protected GenericContainer<?> createSinkService(PulsarCluster cluster) {
        // No need for an actual service container for this test
        return null;
    }

    @Override
    public void prepareSink() throws Exception {
        // Nothing to prepare
    }

    @Override
    public void validateSinkResult(Map<String, String> kvs) {
        // No validation needed
    }

    @Override
    public void produceMessage(int numMessages, PulsarClient client, String inputTopicName, LinkedHashMap<String, String> kvs) throws Exception {
        @Cleanup
        Producer<String> producer = client.newProducer(Schema.STRING)
                .topic(inputTopicName)
                .create();

        for (int i = 0; i < numMessages; i++) {
            String key = "key-" + i;
            String value = "value-" + i;
            kvs.put(key, value);
            producer.newMessage()
                    .key(key)
                    .value(value)
                    .send();
        }
    }
    
    @Override
    public void close() throws Exception {
        // Nothing to close
    }
} 