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
package com.thoughtworks.go.util;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.time.Instant.ofEpochMilli;
import static java.time.ZoneId.systemDefault;

public class TestingClock implements Clock {
    private Instant currentTime;
    private List<Long> sleeps = new ArrayList<>();

    public TestingClock() {
        this(new Date());
    }

    public TestingClock(Date date) {
        this.currentTime = date.toInstant();
    }

    @Override
    public Date currentTime() {
        return Date.from(currentTime);
    }

    @Override
    public Instant currentInstant() {
        return currentTime;
    }

    @Override
    public Timestamp currentTimestamp() {
        return new Timestamp(currentTimeMillis());
    }

    @Override
    public LocalDateTime currentLocalDateTime() {
        return LocalDateTime.ofInstant(ofEpochMilli(currentTimeMillis()), systemDefault());
    }

    @Override
    public long currentTimeMillis() {
        return currentTime.toEpochMilli();
    }

    @Override
    public void sleepForSeconds(long seconds) throws InterruptedException {
        sleepForMillis(seconds * 1000);
    }

    @Override
    public void sleepForMillis(long millis) {
        sleeps.add(millis);
    }

    @Override
    public Instant timeoutTime(Timeout timeout) {
        return timeoutTime(timeout.inMillis());
    }

    @Override
    public Instant timeoutTime(long milliSeconds) {
        return currentTime.plusMillis(milliSeconds);
    }

    public void addSeconds(int numberOfSeconds) {
        currentTime = currentTime.plusSeconds(numberOfSeconds);
    }

    public void setTime(Date date) {
        setTime(date.toInstant());
    }

    public void setTime(Instant instant) {
        currentTime = instant;
    }

    public List<Long> getSleeps() {
        return sleeps;
    }

    public void addMillis(int millis) {
        currentTime = currentTime.plusMillis(millis);
    }
}
