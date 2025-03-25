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
package org.apache.pulsar.io.common;

import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.functions.api.Record;
import org.apache.pulsar.io.core.Sink;
import org.apache.pulsar.io.core.SinkContext;
import org.apache.pulsar.io.core.annotations.FieldDoc;

/**
 * A simple sink for testing sensitive data masking.
 */
@Slf4j
public class SensitiveSink implements Sink<String> {

    /**
     * Configuration for the sensitive sink.
     */
    @Data
    public static class SensitiveSinkConfig {
        @FieldDoc(
                defaultValue = "",
                help = "A normal field that shouldn't be masked")
        private String normalField;

        @FieldDoc(
                defaultValue = "",
                sensitive = true, // This will be masked
                help = "A sensitive field that should be masked")
        private String sensitivePassword;
    }

    private SensitiveSinkConfig config;

    @Override
    public void open(Map<String, Object> configMap, SinkContext sinkContext) throws Exception {
        log.info("Opening sensitive sink with config: {}", configMap);
        config = IOConfigUtils.loadWithSecrets(configMap, SensitiveSinkConfig.class, sinkContext);

        // Log the config - this should mask the sensitivePassword
        log.info("Loaded sensitive sink config: {}", config);
    }

    @Override
    public void write(Record<String> record) throws Exception {
        // Just log the record but don't include the sensitive data
        log.info("Processing record: {}", record.getValue());

        // Complete the record
        record.ack();
    }

    @Override
    public void close() throws Exception {
        // Nothing to close
    }
} 