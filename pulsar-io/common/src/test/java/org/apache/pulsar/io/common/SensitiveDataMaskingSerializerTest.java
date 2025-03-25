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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import lombok.Data;
import org.apache.pulsar.io.core.annotations.FieldDoc;
import org.testng.annotations.Test;

public class SensitiveDataMaskingSerializerTest {

    @Data
    private static class TestConfig implements Serializable {
        private static final long serialVersionUID = 1L;

        @FieldDoc(
                defaultValue = "",
                help = "Normal field")
        private String normalField = "normalValue";

        @FieldDoc(
                defaultValue = "",
                sensitive = true,
                help = "Sensitive field")
        private String sensitiveField = "secretValue";

        @FieldDoc(
                defaultValue = "",
                help = "Another normal field")
        private String anotherNormalField = "anotherNormalValue";
    }

    @Test
    public void testSensitiveFieldsMaskedInJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        // Apply mixin
        mapper.addMixIn(Serializable.class, SensitiveDataMaskingMixin.class);
        
        TestConfig config = new TestConfig();
        
        // Serialize to JSON
        String json = mapper.writeValueAsString(config);
        
        // Verify normal values in output
        assertTrue(json.contains("normalField"));
        assertTrue(json.contains("normalValue"));
        assertTrue(json.contains("anotherNormalField"));
        assertTrue(json.contains("anotherNormalValue"));
        
        // Verify sensitive values masked
        assertTrue(json.contains("sensitiveField"));
        assertTrue(json.contains("********"));
        
        // Verify actual sensitive value is not present
        assertFalse(json.contains("secretValue"));
    }
    
    @Test
    public void testThatSensitiveValueIsUnmaskedWithoutMixin() throws IOException {
        ObjectMapper plainMapper = new ObjectMapper();
        TestConfig config = new TestConfig();
        
        // Serialize without our mixin
        String json = plainMapper.writeValueAsString(config);
        
        // Sensitive value should be present
        assertTrue(json.contains("sensitiveField"));
        assertTrue(json.contains("secretValue"));
        assertFalse(json.contains("********"));
    }
} 