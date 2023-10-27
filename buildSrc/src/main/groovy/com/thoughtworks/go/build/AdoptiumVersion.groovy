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

package com.thoughtworks.go.build

import java.nio.charset.StandardCharsets

class AdoptiumVersion implements Serializable {
  int feature // e.g 17
  Integer interim // e.g null, or 0
  Integer update  // e.g null, or 4
  Integer patch   // e.g null, or 1
  int build   // e.g 8

  // Examples
  // 17+35 (first release)
  // 17.0.4+8 (normal release)
  // 17.0.4.1+1 (rare emergency patch release)
  def canonicalDisplayVersion() {
    "${[feature, interim, update, patch].findAll({ it != null }).join('.')}+${build}"
  }

  // Examples
  // 17%2B35 (first release)
  // 17.0.4%2B8 (normal release)
  // 17.0.4.1%2B1 (rare emergency patch release)
  def urlSafeDisplayVersion() {
    URLEncoder.encode(canonicalDisplayVersion(), StandardCharsets.UTF_8)
  }

  def toDownloadURLFor(OperatingSystem os, Architecture arch) {
    "https://api.adoptium.net/v3/binary/version/jdk-${urlSafeDisplayVersion()}/${os.adoptiumAlias}/${arch.canonicalName}/jre/hotspot/normal/eclipse"
  }

  def toMetadata() {
    [
      included      : true,
      featureVersion: feature,
      fullVersion   : canonicalDisplayVersion(),
    ]
  }

  def toLicenseMetadata() {
    [
      "moduleName": "net.adoptium:eclipse-temurin-jre",
      "moduleVersion": canonicalDisplayVersion(),
      "moduleUrls": [
        "https://adoptium.net/",
        "https://adoptium.net/about/"
      ],
      "moduleLicenses": [
        [
          "moduleLicense": "GPLv2 with the Classpath Exception",
          "moduleLicenseUrl": "https://openjdk.org/legal/gplv2+ce.html"
        ]
      ]
    ]
  }

  static def noneMetadata() {
    [ included: false ]
  }
}

