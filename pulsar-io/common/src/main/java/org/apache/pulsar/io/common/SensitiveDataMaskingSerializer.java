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
import java.lang.reflect.Field;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.io.core.annotations.FieldDoc;

/**
 * Custom serializer that masks sensitive fields in config objects when serialized to JSON.
 */
@Slf4j
public class SensitiveDataMaskingSerializer extends JsonSerializer<Serializable> {

    private static final String MASK_VALUE = "********";

    @Override
    public void serialize(Serializable value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            serializers.defaultSerializeNull(gen);
            return;
        }

        // Write JSON directly instead of using writeObject to avoid infinite recursion
        gen.writeStartObject();

        try {
            // Use reflection to get all fields from the object class
            for (Field field : IOConfigUtils.getAllFields(value.getClass())) {
                field.setAccessible(true);
                String fieldName = field.getName();
                Object fieldValue;

                try {
                    fieldValue = field.get(value);
                } catch (Exception e) {
                    log.warn("Failed to get value for field {}", fieldName, e);
                    continue;
                }

                // Check if the field is marked as sensitive
                boolean isSensitive = false;
                for (var annotation : field.getAnnotations()) {
                    if (annotation instanceof FieldDoc) {
                        FieldDoc fieldDoc = (FieldDoc) annotation;
                        if (fieldDoc.sensitive()) {
                            isSensitive = true;
                            break;
                        }
                    }
                }

                // Write the field name
                gen.writeFieldName(fieldName);

                // Write masked value for sensitive fields or use standard serialization for others
                if (isSensitive && fieldValue != null) {
                    gen.writeString(MASK_VALUE);
                } else {
                    serializers.defaultSerializeValue(fieldValue, gen);
                }
            }
        } catch (Exception e) {
            log.error("Error serializing with sensitive data masking", e);
            // Fall back to default serialization in case of error
            gen.writeEndObject();
            gen.flush();
            serializers.defaultSerializeValue(value, gen);
            return;
        }

        gen.writeEndObject();
    }
}