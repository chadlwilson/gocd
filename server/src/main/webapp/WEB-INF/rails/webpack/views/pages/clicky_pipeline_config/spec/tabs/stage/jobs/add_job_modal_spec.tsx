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

import m from "mithril";
import Stream from "mithril/stream";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {Stage} from "models/pipeline_configs/stage";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {AddJobModal} from "views/pages/clicky_pipeline_config/tabs/stage/jobs/add_job_modal";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";
import {OperationState} from "views/pages/page_operations";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Add Job Modal", () => {
  const helper = new TestHelper();
  let modal: AddJobModal;

  beforeEach(mount);
  afterEach(helper.unmount.bind(helper));

  it("should render modal title", () => {
    expect(modal.title()).toBe("Add new Job");
  });

  it("should render job settings tab for defining a new job", () => {
    expect(helper.byTestId("add-job-modal")).toBeInDOM();
    expect(helper.byTestId("job-settings-tab")).toBeInDOM();
  });

  it("should allow users to define a basic task as part of new job definition", () => {
    const helpText = "This job requires at least one task. You can add more tasks once this job has been created";

    expect(helper.byTestId("add-job-modal")).toBeInDOM();
    expect(helper.byTestId("initial-task-header")).toContainText("Initial Task");
    expect(helper.byTestId("initial-task-help-text")).toContainText(helpText);
  });

  it("should render task type dropdown", () => {
    const dropdown = helper.q("select", helper.byTestId("add-job-modal"));
    const options  = helper.qa("option", helper.byTestId("add-job-modal"));

    expect(dropdown).toBeInDOM();
    expect(dropdown).toHaveLength(4);

    expect(options[0]).toHaveValue("Ant");
    expect(options[1]).toHaveValue("NAnt");
    expect(options[2]).toHaveValue("Rake");
    expect(options[3]).toHaveValue("Custom Command");
  });

  it("should render exec task view by default", () => {
    expect(helper.byTestId("exec-task-modal")).toBeInDOM();
  });

  it("should render ant task view", () => {
    expect(helper.byTestId("exec-task-modal")).toBeInDOM();

    helper.onchange("select", "Ant");

    expect(helper.byTestId("ant-task-modal")).toBeInDOM();
  });

  it("should render nant task view", () => {
    expect(helper.byTestId("exec-task-modal")).toBeInDOM();

    helper.onchange("select", "NAnt");

    expect(helper.byTestId("nant-task-modal")).toBeInDOM();
  });

  it("should render rake task view", () => {
    expect(helper.byTestId("exec-task-modal")).toBeInDOM();

    helper.onchange("select", "Rake");

    expect(helper.byTestId("rake-task-modal")).toBeInDOM();
  });

  it("should show error message when conflicting job name is specified", () => {
    const conflictingJobName = "JobOne";

    const jobNameInput = helper.byTestId("form-field-input-job-name");
    expect(jobNameInput).toBeInDOM();
    helper.oninput(jobNameInput, conflictingJobName);
    modal.onbeforeupdate({} as m.VnodeDOM<any, any>);

    m.redraw.sync();

    const expectedErrorMsg = "Another job with the same name already exists!";
    expect(helper.q(`#${jobNameInput.id}-error-text`)).toBeInDOM();
    expect(helper.q(`#${jobNameInput.id}-error-text`)).toContainText(expectedErrorMsg);
  });

  it("should remove conflicting name message when conflicting job name is fixed", () => {
    const uniqueJobName      = "my-new-job-name";
    const conflictingJobName = "JobOne";

    expect(helper.byTestId("form-field-input-job-name")).toBeInDOM();

    helper.oninput(helper.byTestId("form-field-input-job-name"), conflictingJobName);
    modal.onbeforeupdate({} as m.VnodeDOM<any, any>);

    m.redraw.sync();

    const expectedErrorMsg = "Another job with the same name already exists!";
    expect(helper.q(`#${helper.byTestId("form-field-input-job-name").id}-error-text`)).toBeInDOM();
    expect(helper.q(`#${helper.byTestId("form-field-input-job-name").id}-error-text`)).toContainText(expectedErrorMsg);

    helper.oninput(helper.byTestId("form-field-input-job-name"), uniqueJobName);
    modal.onbeforeupdate({} as m.VnodeDOM<any, any>);

    m.redraw.sync();

    expect(helper.q(`#${helper.byTestId("form-field-input-job-name").id}-error-text`)).not.toBeInDOM();
  });

  function mount() {
    const stage          = Stage.fromJSON(PipelineConfigTestData.stage("stage1", "JobOne"));
    const routeParams    = {stage_name: stage.name()} as PipelineConfigRouteParams;
    const templateConfig = new TemplateConfig("foo", []);

    const pipelineConfig = new PipelineConfig();
    pipelineConfig.stages().add(stage);

    modal = new AddJobModal(stage, templateConfig, pipelineConfig, routeParams,
                            Stream<OperationState>(OperationState.UNKNOWN),
                            new FlashMessageModelWithTimeout(),
                            jasmine.createSpy(), jasmine.createSpy());

    helper.mount(() => modal.body());
  }
});
