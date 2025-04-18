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
package com.thoughtworks.go.config.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NameTypeValidatorTest {

    @Test
    public void shouldValidateNameBasedOnLength() {
        assertThat(new NameTypeValidator().isNameValid("name")).isTrue();
        assertThat(new NameTypeValidator().isNameValid(nameOfLength(255))).isTrue();
        assertThat(new NameTypeValidator().isNameValid(nameOfLength(256))).isFalse();
    }

    @Test
    public void shouldValidateNameBasedOnCharacterType() {
        //[a-zA-Z0-9_\-]{1}[a-zA-Z0-9_\-.]*
        assertThat(new NameTypeValidator().isNameValid("")).isFalse();
        assertThat(new NameTypeValidator().isNameValid("name")).isTrue();

        assertThat(new NameTypeValidator().isNameValid("!")).isFalse();
        assertThat(new NameTypeValidator().isNameValid("name!")).isFalse();

        assertThat(new NameTypeValidator().isNameValid("name_123")).isTrue();
        assertThat(new NameTypeValidator().isNameValid("1")).isTrue();

        assertThat(new NameTypeValidator().isNameValid(".")).isFalse();
        assertThat(new NameTypeValidator().isNameValid("1.")).isTrue();
    }

    private String nameOfLength(final int length) {
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < length; i++) {
            name.append("a");
        }
        return name.toString();
    }
}
