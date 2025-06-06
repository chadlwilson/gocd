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

package com.thoughtworks.go.apiv4.configrepos.representers

import com.thoughtworks.go.config.BasicEnvironmentConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.remote.PartialConfig
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson

class PartialConfigRepresenterTest {
  @Test
  void 'toJSON() with empty config'() {
    String json = toObjectString({ w -> PartialConfigRepresenter.toJSON(w, new PartialConfig()) })
    assertThatJson(json).isEqualTo([:])
  }

  @Test
  void 'toJSON() with duplicate environments de-duplicated'() {
    def partialConfig = new PartialConfig()

    def rawEnvironments = [
      new BasicEnvironmentConfig(new CaseInsensitiveString("env1")),
      new BasicEnvironmentConfig(new CaseInsensitiveString("env2")),
      new BasicEnvironmentConfig(new CaseInsensitiveString("env1")),
      new BasicEnvironmentConfig(new CaseInsensitiveString("env3")),
      new BasicEnvironmentConfig(new CaseInsensitiveString("env3")),
      new BasicEnvironmentConfig(new CaseInsensitiveString("env4")),
    ]
    partialConfig.getEnvironments().addAll(rawEnvironments)

    def json = toObjectString({ w -> PartialConfigRepresenter.toJSON(w, partialConfig) })

    assertThatJson(json).isEqualTo([
      environments   : [
        [name: "env1"],
        [name: "env2"],
        [name: "env3"],
        [name: "env4"],
      ],
    ])
  }
}
