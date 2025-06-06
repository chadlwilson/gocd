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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.TestingClock;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;


public class StageTest {
    private final Instant time1 = ZonedDateTime.of(2008, 2, 22, 12, 22, 23, 0, ZoneId.systemDefault()).toInstant();
    private final Instant time2 = ZonedDateTime.of(2008, 2, 22, 12, 22, 24, 0, ZoneId.systemDefault()).toInstant();
    private final Instant time3 = ZonedDateTime.of(2008, 2, 22, 12, 22, 25, 0, ZoneId.systemDefault()).toInstant();
    private final Instant time4 = ZonedDateTime.of(2008, 2, 22, 12, 22, 26, 0, ZoneId.systemDefault()).toInstant();;

    private JobInstances jobInstances;
    private Stage stage;
    private JobInstance firstJob;
    private JobInstance secondJob;
    private static final Date JOB_SCHEDULE_DATE = new Date();
    private TimeProvider timeProvider;
    private long nextId = 0;

    @BeforeEach
    public void setUp() {
        timeProvider = new TimeProvider() {
            @Override
            public Date currentUtilDate() {
                return JOB_SCHEDULE_DATE;
            }
        };
        firstJob = new JobInstance("first-job", timeProvider);
        secondJob = new JobInstance("second-job", timeProvider);
        jobInstances = new JobInstances(firstJob, secondJob);
        stage = StageMother.custom("test", jobInstances);
    }

    @Test
    public void shouldUpdateCompletedByTransitionIdAndStageState() {
        assertThat(stage.getCompletedByTransitionId()).isNull();
        Instant fiveMinsForNow = Instant.now().plus(5, MINUTES);
        complete(firstJob, fiveMinsForNow);
        complete(secondJob, fiveMinsForNow);
        secondJob.getTransition(JobState.Completed);
        stage.calculateResult();
        assertThat(stage.getCompletedByTransitionId()).isEqualTo(nextId);
        assertThat(stage.getState()).isEqualTo(StageState.Passed);
    }

    @Test
    public void shouldAnswerIsScheduledTrueWhenAllJobsAreInScheduleState() {
        stage.setCounter(1);
        firstJob.setState(JobState.Scheduled);
        secondJob.setState(JobState.Scheduled);
        assertThat(stage.isScheduled()).isTrue();
    }

    @Test
    public void shouldAnswerIsScheduledFalseWhenAJobIsNotInScheduledState() {
        stage.setCounter(1);
        firstJob.setState(JobState.Scheduled);
        secondJob.setState(JobState.Completed);
        assertThat(stage.isScheduled()).isFalse();
    }

    @Test
    public void shouldAnswerIsScheduledFalseWhenAStageIsAReRun() {
        stage.setCounter(2);
        firstJob.setState(JobState.Scheduled);
        secondJob.setState(JobState.Scheduled);
        assertThat(stage.isScheduled()).isFalse();
    }

    @Test
    public void shouldAnswerIsReRunTrueWhenAllJobsAreInScheduleState() {
        stage.setCounter(2);
        stage.setRerunOfCounter(null);
        firstJob.setState(JobState.Scheduled);
        secondJob.setState(JobState.Scheduled);
        assertThat(stage.isReRun()).isTrue();
    }

    @Test
    public void shouldAnswerIsReRunFalseWhenAJobIsNotInScheduledState() {
        stage.setCounter(2);
        stage.setRerunOfCounter(null);
        firstJob.setState(JobState.Scheduled);
        secondJob.setState(JobState.Completed);
        assertThat(stage.isReRun()).isFalse();
    }

    @Test
    public void shouldAnswerIsReRunFalseWhenStageIsScheduledFirstTime() {
        stage.setCounter(1);
        stage.setRerunOfCounter(null);
        firstJob.setState(JobState.Scheduled);
        secondJob.setState(JobState.Scheduled);
        assertThat(stage.isReRun()).isFalse();
    }

    @Test
    public void shouldAnswerIsReRunTrueWhenAllReRunJobsAreInScheduleState() {
        stage.setCounter(2);
        stage.setRerunOfCounter(1);
        firstJob.setRerun(true);
        firstJob.setState(JobState.Scheduled);
        secondJob.setRerun(false);
        secondJob.setState(JobState.Completed);
        assertThat(stage.isReRun()).isTrue();
    }

    @Test
    public void shouldAnswerIsReRunFalseWhenAReRunJobIsNotInScheduleState() {
        stage.setCounter(2);
        stage.setRerunOfCounter(1);
        firstJob.setRerun(true);
        firstJob.setState(JobState.Building);
        secondJob.setRerun(false);
        secondJob.setState(JobState.Completed);
        assertThat(stage.isReRun()).isFalse();
    }

    private void complete(JobInstance job, Instant completingAt) {
        job.completing(JobResult.Passed, Date.from(completingAt));
        job.completed(Date.from(completingAt.plusSeconds(10)));
        assignIdsToAllTransitions(job);
    }

    private void assignIdsToAllTransitions(JobInstance job) {
        for (JobStateTransition jobStateTransition : job.getTransitions()) {
            jobStateTransition.setId(++nextId);
        }
    }

    @Test
    public void shouldReturnMostRecentCompletedTransitionAsCompletedDateIfLatestTransitionIdIsNot() {

        firstJob.assign("AGENT-1", Date.from(time1));
        firstJob.completing(JobResult.Passed, Date.from(time2));
        firstJob.completed(Date.from(time2));

        secondJob.assign("AGENT-2", Date.from(time3));
        secondJob.completing(JobResult.Passed, Date.from(time4));
        secondJob.completed(Date.from(time4));
        secondJob.getTransitions().byState(JobState.Completed).setId(1);

        stage.calculateResult();

        assertThat(stage.completedDate()).isEqualTo(Date.from(time4));
    }

    @Test
    public void shouldReturnNullAsCompletedDateIfNeverCompleted() {
        firstJob.assign("AGENT-1", Date.from(time1));
        secondJob.assign("AGENT-2", Date.from(time3));

        assertNull(stage.completedDate(), "Completed date should be null");
    }

    @Test
    public void stageStateShouldBeUnknownIfNoJobs() {
        Stage newStage = new Stage();
        assertThat(newStage.stageState()).isEqualTo(StageState.Unknown);
    }

    @Test
    public void shouldCalculateTotalTimeFromFirstScheduledJobToLastCompletedJob() {

        final Instant time0 = ZonedDateTime.of(2008, 2, 22, 10, 21, 23, 0, ZoneId.systemDefault()).toInstant();
        timeProvider = new TimeProvider() {
            @Override
            public Date currentUtilDate() {
                return Date.from(time0);
            }
        };

        firstJob = new JobInstance("first-job", timeProvider);
        secondJob = new JobInstance("second-job", timeProvider);

        jobInstances = new JobInstances(firstJob, secondJob);
        stage = StageMother.custom("test", jobInstances);

        firstJob.assign("AGENT-1", Date.from(time1));
        firstJob.completing(JobResult.Passed, Date.from(time2));
        firstJob.completed(Date.from(time2));

        secondJob.assign("AGENT-2", Date.from(time3));
        secondJob.completing(JobResult.Passed, Date.from(time4));
        secondJob.completed(Date.from(time4));

        stage.calculateResult();
        stage.setCreatedTime(new Timestamp(time0.toEpochMilli()));
        stage.setLastTransitionedTime(new Timestamp(time4.toEpochMilli()));

        RunDuration.ActualDuration expectedDuration = new RunDuration.ActualDuration(Duration.between(time0, time4));
        RunDuration.ActualDuration duration = (RunDuration.ActualDuration) stage.getDuration();
        assertThat(duration).isEqualTo(expectedDuration);
        assertThat(duration.getTotalSeconds()).isEqualTo(7263L);
    }

    @Test
    public void shouldReturnZeroDurationForIncompleteStage() {
        firstJob.assign("AGENT-1", Date.from(time1));
        firstJob.changeState(JobState.Building, Date.from(time2));

        assertThat(stage.getDuration()).isEqualTo(RunDuration.IN_PROGRESS_DURATION);
    }

    @Test
    public void shouldReturnLatestTransitionDate() {
        Date date = JOB_SCHEDULE_DATE;
        firstJob.completing(JobResult.Failed, date);
        assertThat(stage.latestTransitionDate()).isEqualTo(date);
    }

    @Test
    public void shouldReturnCreatedDateWhenNoTransitions() {
        stage = new Stage("dev", new JobInstances(), "anonymous", null, "manual", new TimeProvider());
        assertEquals(new Date(stage.getCreatedTime().getTime()), stage.latestTransitionDate());
    }

    @Test
    public void shouldCreateAStageWithAGivenConfigVersion() {
        Stage stage = new Stage("foo-stage", new JobInstances(), "admin", null,"manual", false, false, "git-sha", new TimeProvider());
        assertThat(stage.getConfigVersion()).isEqualTo("git-sha");

        stage = new Stage("foo-stage", new JobInstances(), "admin", null, "manual", new TimeProvider());
        assertThat(stage.getConfigVersion()).isNull();
    }

    @Test
    public void shouldSetTheCurrentTimeAsCreationTimeForRerunOfJobs() {
        Stage stage = new Stage("foo-stage", new JobInstances(), "admin", null,"manual", false, false, "git-sha", new TimeProvider());
        Timestamp createdTimeOfRun1 = stage.getCreatedTime();
        Clock clock = new TestingClock(Instant.now().plus(1, MINUTES));
        stage.prepareForRerunOf(new DefaultSchedulingContext("admin"), "git-sha", clock);
        Timestamp createdTimeOfRun2 = stage.getCreatedTime();

        assertNotEquals(createdTimeOfRun1, createdTimeOfRun2);
        assertEquals(createdTimeOfRun2, clock.currentSqlTimestamp());
    }
}
