/*
 * Copyright (c) 2020-present The Aspectran Project
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
package com.aspectran.appmon.engine.exporter.event.session;

import com.aspectran.utils.json.JsonBuilder;
import com.aspectran.utils.json.JsonString;

/**
 * A data transfer object (DTO) for session-related event data.
 * It holds statistics and lists of recent session activities.
 *
 * <p>Created: 2020/01/11</p>
 */
public class SessionEventData {

    private long numberOfCreated;

    private long numberOfExpired;

    private long numberOfActives;

    private long highestNumberOfActives;

    private long numberOfUnmanaged;

    private long numberOfRejected;

    private String startTime;

    private JsonString[] createdSessions;

    private String[] destroyedSessions;

    private String[] evictedSessions;

    private JsonString[] residedSessions;

    public long getNumberOfCreated() {
        return numberOfCreated;
    }

    public void setNumberOfCreated(long numberOfCreated) {
        this.numberOfCreated = numberOfCreated;
    }

    public long getNumberOfExpired() {
        return numberOfExpired;
    }

    public void setNumberOfExpired(long numberOfExpired) {
        this.numberOfExpired = numberOfExpired;
    }

    public long getNumberOfUnmanaged() {
        return numberOfUnmanaged;
    }

    public long getNumberOfActives() {
        return numberOfActives;
    }

    public void setNumberOfActives(long numberOfActives) {
        this.numberOfActives = numberOfActives;
    }

    public void setNumberOfUnmanaged(long numberOfUnmanaged) {
        this.numberOfUnmanaged = numberOfUnmanaged;
    }

    public long getHighestNumberOfActives() {
        return highestNumberOfActives;
    }

    public void setHighestNumberOfActives(long highestNumberOfActives) {
        this.highestNumberOfActives = highestNumberOfActives;
    }

    public long getNumberOfRejected() {
        return numberOfRejected;
    }

    public void setNumberOfRejected(long numberOfRejected) {
        this.numberOfRejected = numberOfRejected;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public JsonString[] getCreatedSessions() {
        return createdSessions;
    }

    public void setCreatedSessions(JsonString[] createdSessions) {
        this.createdSessions = createdSessions;
    }

    public String[] getDestroyedSessions() {
        return destroyedSessions;
    }

    public void setDestroyedSessions(String[] destroyedSessions) {
        this.destroyedSessions = destroyedSessions;
    }

    public String[] getEvictedSessions() {
        return evictedSessions;
    }

    public void setEvictedSessions(String[] evictedSessions) {
        this.evictedSessions = evictedSessions;
    }

    public JsonString[] getResidedSessions() {
        return residedSessions;
    }

    public void setResidedSessions(JsonString[] residedSessions) {
        this.residedSessions = residedSessions;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SessionEventData that)) {
            return false;
        }
        return (that.numberOfCreated == numberOfCreated &&
                that.numberOfExpired == numberOfExpired &&
                that.numberOfUnmanaged == numberOfUnmanaged &&
                that.numberOfActives == numberOfActives &&
                that.highestNumberOfActives == highestNumberOfActives &&
                that.numberOfRejected == numberOfRejected);
    }

    /**
     * Converts this object to its JSON representation.
     * @return a JSON string
     */
    public String toJson() {
        return new JsonBuilder()
                .prettyPrint(false)
                .put(this)
                .toString();
    }

}
