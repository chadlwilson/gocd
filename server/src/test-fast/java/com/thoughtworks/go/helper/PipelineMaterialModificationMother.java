/*
 * Copyright 2022 ThoughtWorks, Inc.
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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.domain.PipelineTimelineEntry;

import java.time.ZonedDateTime;
import java.util.*;

import static com.thoughtworks.go.util.DataStructureUtils.a;

public class PipelineMaterialModificationMother {
    private static long id = 1;

    public static PipelineTimelineEntry modification(List<String> materials, final int modId, final String rev, ZonedDateTime... datetime) {
        return modification(1, materials, Arrays.asList(datetime), modId, rev);
    }

    public static PipelineTimelineEntry modification(long id, List<String> materials, List<ZonedDateTime> datetimes, final int counter, final String rev) {
        return modification(id, materials, datetimes, counter, rev, "pipeline");
    }

    public static PipelineTimelineEntry modification(long id, List<String> materials, List<ZonedDateTime> datetimes, int counter, final String rev, final String pipeline) {
        return modification(pipeline, id, materials, datetimes, counter, rev);
    }

    public static PipelineTimelineEntry modification(String pipelineName, long id, List<String> materials, List<ZonedDateTime> datetimes, int counter, final String rev) {
        if (materials.size() != datetimes.size()) {
            throw new RuntimeException("Setup the data properly you (if raghu) stupid stupid stupid (else) lame (end) developer");
        }
        Map<String, List<PipelineTimelineEntry.Revision>> materialToMod = new HashMap<>();
        for (int i = 0; i < materials.size(); i++) {
            String fingerprint = materials.get(i);
            materialToMod.put(fingerprint, a(new PipelineTimelineEntry.Revision(Date.from(datetimes.get(i).toInstant()), rev, fingerprint, PipelineMaterialModificationMother.id++)));
        }
        return new PipelineTimelineEntry(pipelineName, id, counter, materialToMod);
    }
}
