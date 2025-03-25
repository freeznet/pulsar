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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.io.core.annotations.FieldDoc;

/**
 * Utility class for masking sensitive data in configuration objects.
 *
 * <p>This utility provides methods to mask sensitive fields in configuration objects
 * that are annotated with {@code @FieldDoc(sensitive = true)}.
 *
 * <p>Usage with Jackson serialization:
 * <pre>{@code
 * // Apply mixin to mapper for automatic masking
 * ObjectMapper mapper = new ObjectMapper();
 * mapper.addMixIn(Serializable.class, SensitiveDataMaskingMixin.class);
 *
 * // Any config serialized with this mapper will have sensitive fields masked
 * String json = mapper.writeValueAsString(configObject);
 * }</pre>
 *
 * <p>Usage with Gson serialization:
 * <pre>{@code
 * // Create Gson instance with sensitive data masking
 * Gson gson = GsonSensitiveDataTypeAdapter.createGsonWithSensitiveDataMasking();
 *
 * // Any config serialized with this Gson will have sensitive fields masked
 * String json = gson.toJson(configObject);
 * }</pre>
 *
 * <p>Direct usage for manual masking:
 * <pre>{@code
 * // Get masked config as a Map
 * Map<String, Object> masked = SensitiveDataMaskingUtil.getMaskedConfig(configObject);
 *
 * // Get masked config as a String
 * String maskedString = SensitiveDataMaskingUtil.toMaskedString(configObject);
 * }</pre>
 */
@Slf4j
public class SensitiveDataMaskingUtil {

    private static final String MASK_VALUE = "********";

    /**
     * Creates a copy of the config object with sensitive fields masked for logging.
     *
     * @param config The configuration object to mask
     * @param <T>    The type of the configuration object
     * @return A map with the same keys but with sensitive values masked
     */
    public static <T> Map<String, Object> getMaskedConfig(T config) {
        return getMaskedConfig(config, new HashSet<>());
    }

    /**
     * Internal implementation with cycle detection.
     *
     * @param config  The configuration object to mask
     * @param visited Set of already visited objects to prevent infinite recursion
     * @param <T>     The type of the configuration object
     * @return A map with the same keys but with sensitive values masked
     */
    private static <T> Map<String, Object> getMaskedConfig(T config, Set<Object> visited) {
        Map<String, Object> maskedConfig = new HashMap<>();

        if (config == null) {
            return maskedConfig;
        }

        // Prevent infinite recursion from circular references
        if (visited.contains(config)) {
            return maskedConfig;
        }

        // Add to visited set
        visited.add(config);

        try {
            // Use reflection to get all fields from the config class
            for (Field field : IOConfigUtils.getAllFields(config.getClass())) {
                field.setAccessible(true);
                String fieldName = field.getName();
                Object value;

                try {
                    value = field.get(config);
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

                // Mask sensitive values
                if (isSensitive && value != null) {
                    maskedConfig.put(fieldName, MASK_VALUE);
                } else {
                    maskedConfig.put(fieldName, value);
                }
            }
        } catch (Exception e) {
            log.error("Error creating masked config", e);
        }

        // Remove from visited set to allow reuse in other paths
        visited.remove(config);

        return maskedConfig;
    }

    /**
     * Returns a string representation of the config with sensitive fields masked.
     *
     * @param config The configuration object to mask
     * @param <T>    The type of the configuration object
     * @return A string representation with sensitive values masked
     */
    public static <T> String toMaskedString(T config) {
        return getMaskedConfig(config).toString();
    }
}