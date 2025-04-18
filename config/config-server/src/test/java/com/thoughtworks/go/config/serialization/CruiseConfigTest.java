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
package com.thoughtworks.go.config.serialization;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CruiseConfigTest {

    @Test
    public void shouldFindAllResourcesOnAllJobs() throws Exception {
        String jobXml = """
                <job name="dev1">
                <tasks><ant /></tasks>
                <resources>
                <resource>one</resource>
                <resource>two</resource>
                </resources>
                </job>""";
        String jobXml2 = """
                <job name="dev2">
                <tasks><ant /></tasks>
                <resources>
                <resource>two</resource>
                <resource>three</resource>
                </resources>
                </job>""";

        ConfigElementImplementationRegistry registry = ConfigElementImplementationRegistryMother.withNoPlugins();
        CruiseConfig config = new MagicalGoConfigXmlLoader(new ConfigCache(), registry).loadConfigHolder(ConfigFileFixture.withJob(jobXml + jobXml2)).config;
        assertThat(config.getAllResources()).contains(new ResourceConfig("one"));
        assertThat(config.getAllResources()).contains(new ResourceConfig("two"));
        assertThat(config.getAllResources()).contains(new ResourceConfig("three"));
        assertThat(config.getAllResources().size()).isEqualTo(3);
    }

    @Test
    public void shouldReturnTrueIfUserIsAdmin() {
        CruiseConfig config = ConfigMigrator.loadWithMigration(ConfigFileFixture.STAGE_AUTH_WITH_ADMIN_AND_AUTH).config;
        assertThat(config.isAdministrator("admin")).isTrue();
    }

    @Test
    public void shouldReturnfalseIfUserIsNotAdmin() {
        CruiseConfig config = ConfigMigrator.loadWithMigration(ConfigFileFixture.STAGE_AUTH_WITH_ADMIN_AND_AUTH).config;
        assertThat(config.isAdministrator("pavan")).isFalse();
    }
}
