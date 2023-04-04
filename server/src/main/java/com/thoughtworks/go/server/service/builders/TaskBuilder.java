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
package com.thoughtworks.go.server.service.builders;

import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import com.thoughtworks.go.util.FileUtil;
import org.apache.commons.io.FilenameUtils;

import java.io.File;

public interface TaskBuilder<T extends Task> {
    Builder createBuilder(BuilderFactory builderFactory, T task, Pipeline pipeline, UpstreamPipelineResolver resolver);

    static String join(File defaultWorkingDir, String actualFileToUse) {
        if (actualFileToUse == null) {
            return FilenameUtils.separatorsToUnix(defaultWorkingDir.getPath());
        }
        return FileUtil.applyBaseDirIfRelativeAndNormalize(defaultWorkingDir, new File(actualFileToUse));
    }
}
