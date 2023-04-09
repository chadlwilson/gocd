/*
 * Copyright 2023 Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.apiv1.versioninfos.models;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncryptionHelperTest {

    @Test
    void shouldVerifySignatureAndSubordinatePublicKeyWithMasterPublicKey() throws Exception {
        String subordinatePublicKey = contentFromResource("rsa/subordinate-public.pem");
        String signature = contentFromResource("rsa/subordinate-public.pem.sha512");
        String masterPublicKey = contentFromResource("rsa/master-public.pem");

        assertTrue(EncryptionHelper.verifyRSASignature(subordinatePublicKey, signature, masterPublicKey));
    }

    @Test
    void shouldNotVerifyInvalidSignatureOrInvalidSubordinatePublicKeyWithMasterPublicKey() throws Exception {
        String subordinatePublicKey = contentFromResource("rsa/subordinate-public.pem");
        subordinatePublicKey = subordinatePublicKey + "\n";
        String signature = contentFromResource("rsa/subordinate-public.pem.sha512");
        String masterPublicKey = contentFromResource("rsa/master-public.pem");

        assertFalse(EncryptionHelper.verifyRSASignature(subordinatePublicKey, signature, masterPublicKey));
    }

    private String contentFromResource(String name) throws IOException {
        return new String(getClass().getClassLoader().getResourceAsStream(name).readAllBytes(), StandardCharsets.UTF_8);
    }
}
