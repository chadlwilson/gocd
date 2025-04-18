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
package com.thoughtworks.go.config.materials.hg;

import com.thoughtworks.go.config.MaterialRevisionsMatchers;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.helper.HgTestRepo;
import com.thoughtworks.go.util.TempDirUtils;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class HgMultipleMaterialsTest {
    private HgTestRepo repo;
    private File pipelineDir;

    @BeforeEach
    public void createRepo(@TempDir Path tempDir) throws IOException {
        repo = new HgTestRepo(tempDir);
        pipelineDir = TempDirUtils.createTempDirectoryIn(tempDir, "working-dir").toFile();
    }

    @Test
    public void shouldCloneMaterialToItsDestFolder() {
        HgMaterial material1 = repo.createMaterial("dest1");

        MaterialRevision materialRevision = new MaterialRevision(material1, material1.latestModification(pipelineDir, new TestSubprocessExecutionContext()));

        materialRevision.updateTo(pipelineDir, ProcessOutputStreamConsumer.inMemoryConsumer(), new TestSubprocessExecutionContext());

        assertThat(new File(pipelineDir, "dest1").exists()).isTrue();
        assertThat(new File(pipelineDir, "dest1/.hg").exists()).isTrue();
    }

    @Test
    public void shouldIgnoreDestinationFolderWhenServerSide() {
        HgMaterial material1 = repo.createMaterial("dest1");

        MaterialRevision materialRevision = new MaterialRevision(material1, material1.latestModification(pipelineDir, new TestSubprocessExecutionContext()));

        materialRevision.updateTo(pipelineDir, ProcessOutputStreamConsumer.inMemoryConsumer(), new TestSubprocessExecutionContext(true));

        assertThat(new File(pipelineDir, "dest1").exists()).isFalse();
        assertThat(new File(pipelineDir, ".hg").exists()).isTrue();
    }

    @Test
    public void shouldFindModificationsForBothMaterials() throws Exception {
        Materials materials = new Materials(repo.createMaterial("dest1"), repo.createMaterial("dest2"));
        repo.commitAndPushFile("SomeDocumentation.txt");

        MaterialRevisions materialRevisions = materials.latestModification(pipelineDir, new TestSubprocessExecutionContext());

        assertThat(materialRevisions.getRevisions().size()).isEqualTo(2);
        assertThat(materialRevisions).anySatisfy(MaterialRevisionsMatchers.containsModifiedBy("SomeDocumentation.txt", "user"));
    }

}
