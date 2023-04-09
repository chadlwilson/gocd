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
package com.thoughtworks.go.plugin.access.configrepo;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ConfigRepoMigrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigRepoMigrator.class);

    public String migrate(String oldJSON, int targetVersion) {
        LOGGER.debug("Migrating to version {}: {}", targetVersion, oldJSON);

        Chainr transform = getTransformerFor(targetVersion);
        Object transformedObject = transform.transform(JsonUtils.jsonToMap(oldJSON), getContextMap(targetVersion));
        String transformedJSON = JsonUtils.toJsonString(transformedObject);


        LOGGER.debug("After migration to version {}: {}", targetVersion, transformedJSON);
        return transformedJSON;
    }

    private Chainr getTransformerFor(int targetVersion) {
        String targetVersionFile = String.format("/config-repo/migrations/%s.json", targetVersion);
        try (InputStream resourceAsStream = this.getClass().getResourceAsStream(targetVersionFile)) {
            String transformJSON = new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8);
            return Chainr.fromSpec(JsonUtils.jsonToList(transformJSON));
        } catch (Exception e) {
            throw new RuntimeException("Failed to migrate to version " + targetVersion, e);
        }
    }

    private Map<String, Object> getContextMap(int targetVersion) {
        String contextFile = String.format("/config-repo/contexts/%s.json", targetVersion);
        try (InputStream resourceAsStream = this.getClass().getResourceAsStream(contextFile)) {
            String contextJSON = new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8);
            return JsonUtils.jsonToMap(contextJSON);
        } catch (Exception e) {
            LOGGER.debug(String.format("No context file present for target version '%s'.", targetVersion));
            return null;
        }
    }
}
