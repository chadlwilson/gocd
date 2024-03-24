/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.domain.activity;

import com.thoughtworks.go.domain.JobConfigIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.NullJobInstance;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.JobStatusListener;
import org.jetbrains.annotations.TestOnly;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Understands jobs that are currently in progress
 */
@Component
public class JobStatusCache implements JobStatusListener {
    private static final NullJobInstance NEVER_RUN = new NullJobInstance("NEVER_RUN");
    private final ConcurrentMap<JobConfigIdentifier, JobInstance> jobs = new ConcurrentHashMap<>();
    private final StageDao stageDao;

    @Autowired
    public JobStatusCache(StageDao stageDao) {
        this.stageDao = stageDao;
    }

    @Override
    public void jobStatusChanged(JobInstance job) {
        cache(job);
    }

    private synchronized void cache(JobInstance newJob) {
        jobs.put(newJob.getIdentifier().jobConfigIdentifier(), newJob);
        clearOldJobs(newJob);
    }

    private void clearOldJobs(JobInstance newJob) {
        jobs.entrySet().removeIf(entry ->
            (entry.getValue() != NEVER_RUN || isSameJobConfig(newJob, entry.getKey()))
                && entry.getValue().isSameStageConfig(newJob)
                && !entry.getValue().isSamePipelineInstance(newJob)
        );
    }

    private boolean isSameJobConfig(JobInstance newJob, JobConfigIdentifier cachedId) {
        return newJob.getIdentifier().jobConfigIdentifier().equals(cachedId);
    }

    public JobInstance currentJob(JobConfigIdentifier identifier) {
        return currentJobs(identifier).stream().findFirst().orElse(null);
    }

    public List<JobInstance> currentJobs(final JobConfigIdentifier identifier) {
        if (jobs.get(identifier) == NEVER_RUN) {
            return Collections.emptyList();
        }
        List<JobInstance> cached = instancesMatching(jobs.values(), identifier);
        if (!cached.isEmpty()) {
            return cached;
        }

        List<JobInstance> fromDatabase = instancesMatching(stageDao.mostRecentJobsForStage(identifier.getPipelineName(), identifier.getStageName()), identifier);
        if (fromDatabase.isEmpty()) {
            jobs.put(identifier, NEVER_RUN);
        } else {
            for (JobInstance jobInstance : fromDatabase) {
                cache(jobInstance);
            }
        }
        return fromDatabase;
    }

    private List<JobInstance> instancesMatching(Collection<JobInstance> jobInstances, JobConfigIdentifier identifier) {
        return jobInstances.stream().filter(jobInstance -> jobInstance != NEVER_RUN && jobInstance.matches(identifier)).toList();
    }

    @TestOnly
    public void clear() {
        jobs.clear();
    }
}
