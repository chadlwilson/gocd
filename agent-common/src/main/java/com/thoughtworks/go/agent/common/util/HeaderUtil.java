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
package com.thoughtworks.go.agent.common.util;

import org.apache.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HeaderUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderUtil.class);

    public static Map<String, String> parseExtraProperties(Header extraPropertiesHeader) {
        if (extraPropertiesHeader == null || extraPropertiesHeader.getValue() == null || extraPropertiesHeader.getValue().isBlank()) {
            return new HashMap<>();
        }

        try {
            final String headerValue = new String(Base64.getDecoder().decode(extraPropertiesHeader.getValue()), UTF_8);

            if (headerValue.isBlank()) {
                return new HashMap<>();
            }

            return Arrays.stream(headerValue.trim().split(" +"))
                    .map(property -> property.split("="))
                    .collect(Collectors.toMap(
                            keyAndValue -> keyAndValue[0].replaceAll("%20", " "),
                            keyAndValue -> keyAndValue[1].replaceAll("%20", " "),
                            (value1, value2) -> value1,
                            LinkedHashMap::new));
        } catch (Exception e) {
            LOGGER.warn("Failed to parse extra properties header value: {}", extraPropertiesHeader.getValue(), e);
            return new HashMap<>();
        }
    }

}
