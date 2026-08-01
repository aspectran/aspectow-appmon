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
package com.aspectran.aspectow.console.common.db.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing a security audit log entry.
 */
public class AuditLog implements Serializable {

    @Serial
    private static final long serialVersionUID = -7782341908432104L;

    private Long auditId;
    private String username;
    private String eventType;
    private String target;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;

    /**
     * Gets the audit ID.
     * @return the audit ID
     */
    public Long getAuditId() {
        return auditId;
    }

    /**
     * Sets the audit ID.
     * @param auditId the audit ID
     */
    public void setAuditId(Long auditId) {
        this.auditId = auditId;
    }

    /**
     * Gets the username of the user who performed the action.
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username of the user.
     * @param username the username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the event type.
     * @return the event type
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Sets the event type.
     * @param eventType the event type
     */
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    /**
     * Gets the target of the action.
     * @return the target
     */
    public String getTarget() {
        return target;
    }

    /**
     * Sets the target of the action.
     * @param target the target
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Gets the details of the action.
     * @return the details
     */
    public String getDetails() {
        return details;
    }

    /**
     * Sets the details of the action.
     * @param details the details
     */
    public void setDetails(String details) {
        this.details = details;
    }

    /**
     * Gets the IP address.
     * @return the IP address
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Sets the IP address.
     * @param ipAddress the IP address
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Gets the timestamp when the audit entry was created.
     * @return the creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the timestamp when the audit entry was created.
     * @param createdAt the creation timestamp
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
