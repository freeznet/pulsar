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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Map;
import lombok.Data;
import org.apache.pulsar.io.core.annotations.FieldDoc;
import org.testng.annotations.Test;

public class SensitiveDataMaskingUtilTest {

    @Data
    private static class TestConfig {
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
    public void testGetMaskedConfig() {
        TestConfig config = new TestConfig();
        Map<String, Object> maskedConfig = SensitiveDataMaskingUtil.getMaskedConfig(config);
        
        assertNotNull(maskedConfig);
        assertEquals(maskedConfig.size(), 3);
        assertEquals(maskedConfig.get("normalField"), "normalValue");
        assertEquals(maskedConfig.get("sensitiveField"), "********");
        assertEquals(maskedConfig.get("anotherNormalField"), "anotherNormalValue");
    }

    @Test
    public void testToMaskedString() {
        TestConfig config = new TestConfig();
        String maskedString = SensitiveDataMaskingUtil.toMaskedString(config);
        
        assertNotNull(maskedString);
        assertTrue(maskedString.contains("normalField=normalValue"));
        assertTrue(maskedString.contains("sensitiveField=********"));
        assertTrue(maskedString.contains("anotherNormalField=anotherNormalValue"));
        
        // Make sure original value is not present
        assertFalse(maskedString.contains("secretValue"));
    }
}