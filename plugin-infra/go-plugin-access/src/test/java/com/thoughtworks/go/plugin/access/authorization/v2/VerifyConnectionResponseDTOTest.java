/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.plugin.access.authorization.v2;

import com.thoughtworks.go.plugin.domain.common.VerifyConnectionResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

public class VerifyConnectionResponseDTOTest {

    @Test
    public void shouldDeserializeSuccessResponseFromJSON() {
        String json = """
                {
                  "status": "success",
                  "message": "Connection check passed"
                }""";

        VerifyConnectionResponse response = VerifyConnectionResponseDTO.fromJSON(json).response();

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Connection check passed");
        assertNull(response.getValidationResult());
    }

    @Test
    public void shouldDeserializeFailureResponseFromJSON() {
        String json = """
                {
                  "status": "failure",
                  "message": "Connection check failed"
                }""";

        VerifyConnectionResponse response = VerifyConnectionResponseDTO.fromJSON(json).response();

        assertThat(response.getStatus()).isEqualTo("failure");
        assertThat(response.getMessage()).isEqualTo("Connection check failed");
        assertNull(response.getValidationResult());
    }

    @Test
    public void shouldDeserializeValidationFailedResponseFromJSON() {
        String json = """
                {
                  "status": "validation-failed",
                  "message": "Validation failed",
                  "errors": [
                    {      "key": "url",
                      "message": "URL cannot be blank"
                    },
                    {      "key": "password",
                      "message": "Password cannot be blank"
                    }
                  ]
                }""";

        VerifyConnectionResponse response = VerifyConnectionResponseDTO.fromJSON(json).response();

        assertThat(response.getStatus()).isEqualTo("validation-failed");
        assertThat(response.getMessage()).isEqualTo("Validation failed");
        assertFalse(response.getValidationResult().isSuccessful());
        assertThat(response.getValidationResult().getErrors().get(0)).isEqualTo(new com.thoughtworks.go.plugin.domain.common.ValidationError("url", "URL cannot be blank"));
        assertThat(response.getValidationResult().getErrors().get(1)).isEqualTo(new com.thoughtworks.go.plugin.domain.common.ValidationError("password", "Password cannot be blank"));
    }
}
