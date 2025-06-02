/*
 * Copyright (c) 2020-2025 The Aspectran Project
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
package com.aspectran.appmon.exporter.event.activity;

import com.aspectran.core.activity.Activity;
import com.aspectran.core.adapter.SessionAdapter;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.json.JsonBuilder;

import java.util.concurrent.atomic.AtomicInteger;

import static com.aspectran.appmon.exporter.event.session.SessionEventReader.USER_ACTIVITY_COUNT;

/**
 * <p>Created: 2024-12-19</p>
 */
public class ActivityEventAdvice {

    private final ActivityEventReader activityEventReader;

    private long startTime;

    private String sessionId;

    public ActivityEventAdvice(@NonNull ActivityEventReader activityEventReader) {
        assert activityEventReader.getEventCount() != null;
        this.activityEventReader = activityEventReader;
    }

    public void before(@NonNull Activity activity) {
        startTime = System.currentTimeMillis();

        // Since the servlet container does not allow session creation after
        // the response is committed, the session ID must be secured in advance.
        if (activity.hasSessionAdapter()) {
            sessionId = activity.getSessionAdapter().getId();
        }
    }

    public String after(@NonNull Activity activity) {
        Throwable error = activity.getRootCauseOfRaisedException();
        if (error != null) {
            activityEventReader.getEventCount().error();
        }

        long interim = activityEventReader.getEventCount().getTallying().getTotal();
        long total = interim + activityEventReader.getEventCount().getTallied().getTotal();
        long errors = activityEventReader.getEventCount().getTallying().getError();

        long elapsedTime = System.currentTimeMillis() - startTime;

        int activityCount = 0;
        if (activity.hasSessionAdapter()) {
            SessionAdapter sessionAdapter = activity.getSessionAdapter();
            AtomicInteger counter = sessionAdapter.getAttribute(USER_ACTIVITY_COUNT);
            if (counter != null) {
                activityCount = counter.get();
            }
        }

        return new JsonBuilder()
                .prettyPrint(false)
                .nullWritable(false)
                .object()
                    .object("activities")
                        .put("total", total)
                        .put("interim", interim)
                        .put("errors", errors)
                    .endObject()
                    .put("startTime", startTime)
                    .put("elapsedTime", elapsedTime)
                    .put("thread", Thread.currentThread().getName())
                    .put("sessionId", sessionId)
                    .put("activityCount", activityCount)
                    .put("error", error)
                .endObject()
                .toString();
    }

}
