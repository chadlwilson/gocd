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
package com.thoughtworks.go.server.domain;

import com.thoughtworks.go.domain.PipelineTimelineEntry;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.helper.PipelineMaterialModificationMother.modification;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

public class PipelineMaterialModificationTest {

    @Test public void shouldThrowNPEIfNullIsPassedIn() {
        try {
            modification(new ArrayList<>(), 1, "123").compareTo(null);
            fail("Should throw NPE. That is the Comparable's contract.");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), is("Cannot compare this object with null"));
        }
    }

    @Test public void shouldReturn0IfComparedToItself() {
        ZonedDateTime now = ZonedDateTime.now();
        PipelineTimelineEntry self = modification(List.of("flyweight"), 1, "123", now);
        assertThat(self.compareTo(self), is(0));

        PipelineTimelineEntry another = modification(List.of("flyweight"), 1, "123", now);
        assertThat(self.compareTo(another), is(0));
        assertThat(another.compareTo(self), is(0));
    }

    @Test public void shouldThrowExceptionIfIfComparedToADifferentClassObject() {
        try {
            modification(List.of("flyweight"), 1, "123").compareTo(new Object());
            fail("Should throw up.");
        } catch (RuntimeException expected) {
        }
    }

    @Test public void shouldCompareWhenThisModificationOccuredBeforeTheOtherModification() {
        PipelineTimelineEntry modification = modification(List.of("flyweight"), 1, "123", ZonedDateTime.now());
        PipelineTimelineEntry that = modification(2, List.of("flyweight"), List.of(ZonedDateTime.now().plusMinutes(1)), 1, "123");

        assertThat(modification.compareTo(that), is(-1));
        assertThat(that.compareTo(modification), is(1));
    }

    @Test public void shouldCompareModsWithMultipleMaterials() {
        List<String> materials = List.of("flyweight", "another");
        ZonedDateTime base = ZonedDateTime.now();

        PipelineTimelineEntry modification = modification(1, materials, List.of(base.plusMinutes(1), base.plusMinutes(3)), 1, "123");
        PipelineTimelineEntry that = modification(2, materials, List.of(base.plusMinutes(4), base.plusMinutes(2)), 1, "123");

        assertThat(modification.compareTo(that), is(-1));
        assertThat(that.compareTo(modification), is(1));
    }

    @Test public void shouldCompareModsWithMultipleMaterialsWithOneMaterialNotChanged() {
        List<String> materials = List.of("flyweight", "another");
        ZonedDateTime base = ZonedDateTime.now();

        PipelineTimelineEntry modification = modification(1, materials, List.of(base, base.plusMinutes(3)), 1, "123");
        PipelineTimelineEntry that = modification(2, materials, List.of(base, base.plusMinutes(2)), 1, "123");

        assertThat(modification.compareTo(that), is(1));
        assertThat(that.compareTo(modification), is(-1));
    }

    @Test public void shouldCompareModsWithNoMaterialsChanged() {
        List<String> materials = List.of("flyweight", "another");
        ZonedDateTime base = ZonedDateTime.now();

        PipelineTimelineEntry modification = modification(1, materials, List.of(base, base.plusMinutes(3)), 1, "123", "pipeline");
        PipelineTimelineEntry that = modification(2, materials, List.of(base, base.plusMinutes(3)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that), is(-1));
        assertThat(that.compareTo(modification), is(1));
    }

    @Test public void shouldBreakTieOnMinimumUsingPipelineCounter() {
        List<String> materials = List.of("first", "second", "third", "fourth");
        ZonedDateTime base = ZonedDateTime.now();

        //Because there is a tie on the lowest value i.e. date 2, use the counter to order
        PipelineTimelineEntry modification = modification(1, materials, List.of(base, base.plusMinutes(3), base.plusMinutes(2), base.plusMinutes(4)), 1, "123", "pipeline");
        PipelineTimelineEntry that = modification(2, materials, List.of(base, base.plusMinutes(2), base.plusMinutes(3), base.plusMinutes(2)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that), is(-1));
        assertThat(that.compareTo(modification), is(1));
    }

    @Test public void shouldCompareModsWith4MaterialsWithOneMaterialNotChanged() {
        List<String> materials = List.of("first", "second", "third", "fourth");
        ZonedDateTime base = ZonedDateTime.now();

        PipelineTimelineEntry modification = modification(1, materials, List.of(base, base.plusMinutes(3), base.plusMinutes(2), base.plusMinutes(4)), 1, "123", "pipeline");
        PipelineTimelineEntry that = modification(2, materials, List.of(base, base.plusMinutes(2), base.plusMinutes(3), base.plusMinutes(1)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that), is(1));
        assertThat(that.compareTo(modification), is(-1));
    }

    @Test public void shouldCompareModsUsingCounterToBreakTies() {
        List<String> materials = List.of("first", "second", "third");
        ZonedDateTime base = ZonedDateTime.now();

        PipelineTimelineEntry modification = modification(1, materials, List.of(base, base.plusMinutes(3), base.plusMinutes(2)), 1, "123", "pipeline");
        PipelineTimelineEntry that = modification(2, materials, List.of(base, base.plusMinutes(2), base.plusMinutes(3)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that), is(-1));
        assertThat(that.compareTo(modification), is(1));
    }

    @Test public void shouldIgnoreExtraMaterialForComparison() {
        ZonedDateTime base = ZonedDateTime.now();

        //Ignore the extra material
        PipelineTimelineEntry modification = modification(1, List.of("first", "second", "third"), List.of(base, base.plusMinutes(3), base.plusMinutes(2)), 1, "123", "pipeline");
        PipelineTimelineEntry that = modification(2, List.of("first", "second"), List.of(base, base.plusMinutes(2)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that), is(1));
        assertThat(that.compareTo(modification), is(-1));

        //Now break the tie using counter and ignore the extra third material
        modification = modification(1, List.of("first", "second", "third"), List.of(base, base.plusMinutes(3), base.plusMinutes(2)), 1, "123", "pipeline");
        that = modification(2, List.of("first", "second"), List.of(base, base.plusMinutes(3)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that), is(-1));
        assertThat(that.compareTo(modification), is(1));
    }
}
