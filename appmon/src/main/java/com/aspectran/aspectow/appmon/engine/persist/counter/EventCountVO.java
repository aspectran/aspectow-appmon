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
package com.aspectran.aspectow.appmon.engine.persist.counter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A Value Object (VO) representing event count data for database persistence.
 *
 * <p>Created: 2025-02-14</p>
 */
public class EventCountVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 4071706635617339624L;

    /** The node identifier to which the instance belongs */
    private String nodeId;

    /** The identifier of the instance where the event occurred */
    private String appId;

    /** The identifier of the event being counted */
    private String eventId;

    /** The date and time when the event count was recorded */
    private LocalDateTime datetime;

    /** The cumulative total count of events */
    private long total;

    /** The incremental change in the event count since the last record */
    private long delta;

    /** The number of errors associated with the event */
    private long error;

    /**
     * Returns the node identifier.
     * @return the node identifier
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Sets the node identifier.
     * @param nodeId the node identifier
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Returns the instance identifier.
     * @return the instance identifier
     */
    public String getInstanceId() {
        return appId;
    }

    /**
     * Sets the instance identifier.
     * @param appId the app identifier
     */
    public void setInstanceId(String appId) {
        this.appId = appId;
    }

    /**
     * Returns the event identifier.
     * @return the event identifier
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the event identifier.
     * @param eventId the event identifier
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Returns the date and time of the event count.
     * @return the date and time
     */
    public LocalDateTime getDatetime() {
        return datetime;
    }

    /**
     * Sets the date and time of the event count.
     * @param datetime the date and time
     */
    public void setDatetime(LocalDateTime datetime) {
        this.datetime = datetime;
    }

    /**
     * Returns the cumulative total count.
     * @return the total count
     */
    public long getTotal() {
        return total;
    }

    /**
     * Sets the cumulative total count.
     * @param total the total count
     */
    public void setTotal(long total) {
        this.total = total;
    }

    /**
     * Returns the incremental change in the event count.
     * @return the delta value
     */
    public long getDelta() {
        return delta;
    }

    /**
     * Sets the incremental change in the event count.
     * @param delta the delta value
     */
    public void setDelta(long delta) {
        this.delta = delta;
    }

    /**
     * Returns the number of errors.
     * @return the error count
     */
    public long getError() {
        return error;
    }

    /**
     * Sets the number of errors.
     * @param error the error count
     */
    public void setError(long error) {
        this.error = error;
    }

}
