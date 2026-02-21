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
package com.aspectran.appmon.engine.persist.counter;

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

    /** The domain name to which the instance belongs */
    private String domain;

    /** The name of the instance where the event occurred */
    private String instance;

    /** The name of the event being counted */
    private String event;

    /** The date and time when the event count was recorded */
    private LocalDateTime datetime;

    /** The cumulative total count of events */
    private long total;

    /** The incremental change in the event count since the last record */
    private long delta;

    /** The number of errors associated with the event */
    private long error;

    /**
     * Returns the domain name.
     * @return the domain name
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Sets the domain name.
     * @param domain the domain name
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * Returns the instance name.
     * @return the instance name
     */
    public String getInstance() {
        return instance;
    }

    /**
     * Sets the instance name.
     * @param instance the instance name
     */
    public void setInstance(String instance) {
        this.instance = instance;
    }

    /**
     * Returns the event name.
     * @return the event name
     */
    public String getEvent() {
        return event;
    }

    /**
     * Sets the event name.
     * @param event the event name
     */
    public void setEvent(String event) {
        this.event = event;
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
