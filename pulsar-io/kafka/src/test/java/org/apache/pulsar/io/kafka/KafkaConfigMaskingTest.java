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
package org.apache.pulsar.io.kafka;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

/**
 * Tests that demonstrate masking of sensitive fields in Kafka configuration classes.
 */
public class KafkaConfigMaskingTest {

    @Test
    public void testKafkaSinkConfigMasksSensitiveData() {
        // Create a KafkaSinkConfig with sensitive data
        KafkaSinkConfig sinkConfig = new KafkaSinkConfig()
                .setBootstrapServers("broker1:9092,broker2:9092")
                .setSslTruststorePassword("supersecretpassword")
                .setTopic("test-topic");
        
        // Get the string representation
        String configString = sinkConfig.toString();
        
        // Verify normal fields are present
        assertTrue(configString.contains("bootstrapServers=broker1:9092,broker2:9092"));
        assertTrue(configString.contains("topic=test-topic"));
        
        // Verify sensitive fields are masked
        assertTrue(configString.contains("sslTruststorePassword=********"));
        
        // Verify the actual sensitive value is not present
        assertFalse(configString.contains("supersecretpassword"));
    }
    
    @Test
    public void testKafkaSourceConfigMasksSensitiveData() {
        // Create a KafkaSourceConfig with sensitive data
        KafkaSourceConfig sourceConfig = new KafkaSourceConfig()
                .setBootstrapServers("broker1:9092,broker2:9092") 
                .setSslTruststorePassword("topsecretpassword")
                .setTopic("test-topic")
                .setGroupId("test-group");
        
        // Get the string representation
        String configString = sourceConfig.toString();
        
        // Verify normal fields are present
        assertTrue(configString.contains("bootstrapServers=broker1:9092,broker2:9092"));
        assertTrue(configString.contains("topic=test-topic"));
        assertTrue(configString.contains("groupId=test-group"));
        
        // Verify sensitive fields are masked
        assertTrue(configString.contains("sslTruststorePassword=********"));
        
        // Verify the actual sensitive value is not present
        assertFalse(configString.contains("topsecretpassword"));
    }
}