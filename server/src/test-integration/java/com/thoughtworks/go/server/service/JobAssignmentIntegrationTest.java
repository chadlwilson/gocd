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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.helper.AgentMother;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.remote.work.BuildWork;
import com.thoughtworks.go.remote.work.NoWork;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class JobAssignmentIntegrationTest {
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoConfigDao cruiseConfigDao;
    @Autowired private BuildAssignmentService assignmentService;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private AgentService agentService;

    private PipelineWithTwoStages fixture;
    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private SystemEnvironment systemEnvironment;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws Exception {
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(cruiseConfigDao);
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, tempDir);
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).usingThreeJobs().onSetUp();
        systemEnvironment = new SystemEnvironment();
    }

    @AfterEach
    public void tearDown() throws Exception {
        fixture.onTearDown();
    }

    @Test
    public void shouldAssignJobToRemoteAgent() {
        AgentInstance local = setupLocalAgent();
        AgentInstance remote = setupRemoteAgent();
        fixture.createPipelineWithFirstStageScheduled();

        assignmentService.onTimer();

        assignmentService.assignWorkToAgent(local);

        assignmentService.onTimer();

        Work work = assignmentService.assignWorkToAgent(remote);
        assertThat(work).isInstanceOf(BuildWork.class);
    }

    @Test
    public void shouldNotAssignJobToRemoteAgentIfReachedLimit() {
        AgentInstance local = setupLocalAgent();
        AgentInstance remote = setupRemoteAgent();
        AgentInstance remote2 = setupRemoteAgent();
        fixture.createPipelineWithFirstStageScheduled();

        assignmentService.onConfigChange(null);

        assignmentService.assignWorkToAgent(local);
        assignmentService.assignWorkToAgent(remote);
        Work work = assignmentService.assignWorkToAgent(remote2);
        assertThat(work).isInstanceOf(NoWork.class);
    }

    @Test
    public void shouldAssignJobToLocalAgentEvenReachedLimit() {
        AgentInstance local = setupLocalAgent();
        AgentInstance remote = setupRemoteAgent();
        fixture.createPipelineWithFirstStageScheduled();

        assignmentService.onTimer();

        assignmentService.assignWorkToAgent(remote);
        Work work = assignmentService.assignWorkToAgent(local);
        assertThat(work).isInstanceOf(BuildWork.class);
    }

    private AgentInstance setupRemoteAgent() {
        Agent agent = AgentMother.remoteAgent();
        agentService.saveOrUpdate(agent);
        AgentInstance instance = AgentInstance.createFromAgent(agent, systemEnvironment, agentStatusChangeListener());
        instance.enable();
        return instance;
    }

    private AgentInstance setupLocalAgent() {
        Agent agent = AgentMother.localAgent();
        agentService.saveOrUpdate(agent);
        return AgentInstance.createFromAgent(agent, systemEnvironment, agentStatusChangeListener());
    }

    private AgentStatusChangeListener agentStatusChangeListener() {
        return agentInstance -> {

        };
    }

}
