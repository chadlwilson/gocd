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
package com.thoughtworks.go.apiv11.admin.shared.representers.configorigin;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.spark.Routes;

public class ConfigRepoOriginRepresenter {
    public static void toJSON(OutputWriter jsonWriter, RepoConfigOrigin repoConfigOrigin) {
        jsonWriter.addLinks(linksWriter -> linksWriter
                .addLink("self", Routes.ConfigRepos.id(repoConfigOrigin.getConfigRepo().getId()))
                .addAbsoluteLink("doc", Routes.ConfigRepos.DOC)
                .addLink("find", Routes.ConfigRepos.find()));

        jsonWriter.add("type", "config_repo");
        jsonWriter.add("id", repoConfigOrigin.getConfigRepo().getId());
    }
}
