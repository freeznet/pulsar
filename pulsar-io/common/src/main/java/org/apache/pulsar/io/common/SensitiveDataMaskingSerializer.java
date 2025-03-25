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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom serializer that masks sensitive fields in config objects when serialized to JSON.
 */
@Slf4j
public class SensitiveDataMaskingSerializer extends JsonSerializer<Serializable> {

    @Override
    public void serialize(Serializable value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        try {
            Map<String, Object> maskedConfig = SensitiveDataMaskingUtil.getMaskedConfig(value);
            gen.writeObject(maskedConfig);
        } catch (Exception e) {
            log.warn("Error while masking sensitive fields, using default serialization", e);
            // Fallback to default serialization
            gen.writeObject(value);
        }
    }
} 