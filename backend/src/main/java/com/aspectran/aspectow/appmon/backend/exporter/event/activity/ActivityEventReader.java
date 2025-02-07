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
package com.aspectran.aspectow.appmon.backend.exporter.event.activity;

import com.aspectran.aspectow.appmon.backend.config.EventInfo;
import com.aspectran.aspectow.appmon.backend.exporter.event.AbstractEventReader;
import com.aspectran.aspectow.appmon.backend.exporter.event.EventExporter;
import com.aspectran.aspectow.appmon.backend.exporter.event.EventExporterManager;
import com.aspectran.core.component.UnavailableException;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.rule.AspectAdviceRule;
import com.aspectran.core.context.rule.AspectRule;
import com.aspectran.core.context.rule.JoinpointRule;
import com.aspectran.core.context.rule.params.PointcutParameters;
import com.aspectran.core.context.rule.type.AspectAdviceType;
import com.aspectran.core.context.rule.type.JoinpointTargetType;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.json.JsonBuilder;
import com.aspectran.utils.logging.Logger;
import com.aspectran.utils.logging.LoggerFactory;
import com.aspectran.utils.statistic.CounterStatistic;

/**
 * <p>Created: 2024-12-18</p>
 */
public class ActivityEventReader extends AbstractEventReader {

    private static final Logger logger = LoggerFactory.getLogger(ActivityEventReader.class);

    private final String aspectId;

    private CounterStatistic activityCounter;

    public ActivityEventReader(@NonNull EventExporterManager eventExporterManager, @NonNull EventInfo eventInfo) {
        super(eventExporterManager, eventInfo);
        this.aspectId = getClass().getName() + ".ASPECT-" + hashCode();
    }

    public EventExporter getEventExporter() {
        return getEventExporterManager().getExporter(getEventInfo().getName());
    }

    @Override
    public void start() throws Exception {
        ActivityContext context = CoreServiceHolder.findActivityContext(getTarget());
        if (context == null) {
            throw new Exception("Could not find ActivityContext named '" + getTarget() + "'");
        }

        activityCounter = context.getActivityCounter();

        AspectRule aspectRule = new AspectRule();
        aspectRule.setId(aspectId);
        aspectRule.setOrder(-1);
        aspectRule.setIsolated(true);

        JoinpointRule joinpointRule = new JoinpointRule();
        joinpointRule.setJoinpointTargetType(JoinpointTargetType.ACTIVITY);
        if (getEventInfo().hasParameters()) {
            PointcutParameters pointcutParameters = new PointcutParameters(getEventInfo().getParameters().toString());
            JoinpointRule.updatePointcutRule(joinpointRule, pointcutParameters);
        }
        aspectRule.setJoinpointRule(joinpointRule);

        AspectAdviceRule beforeAspectAdviceRule = aspectRule.newAspectAdviceRule(AspectAdviceType.BEFORE);
        beforeAspectAdviceRule.setAdviceAction(activity -> {
            ActivityEventAdvice requestEventAspect = new ActivityEventAdvice();
            requestEventAspect.request(activity);
            return requestEventAspect;
        });

        AspectAdviceRule afterAspectAdviceRule = aspectRule.newAspectAdviceRule(AspectAdviceType.AFTER);
        afterAspectAdviceRule.setAdviceAction(activity -> {
            ActivityEventAdvice requestEventAspect = activity.getBeforeAdviceResult(aspectId);
            String json = requestEventAspect.complete(activity);
            getEventExporter().broadcast(json);
            return null;
        });

        context.getAspectRuleRegistry().addAspectRule(aspectRule);
    }

    @Override
    public void stop() {
        try {
            ActivityContext context = CoreServiceHolder.findActivityContext(getTarget());
            if (context != null) {
                try {
                    context.getAspectRuleRegistry().removeAspectRule(aspectId);
                } catch (UnavailableException e) {
                    // ignored
                }
            }
        } catch (Exception e) {
            logger.warn(e);
        }
    }

    @Override
    public String read() {
        long current = activityCounter.getCurrent();
        long max = activityCounter.getMax();
        long total = activityCounter.getTotal();
        return new JsonBuilder()
            .prettyPrint(false)
            .nullWritable(false)
            .object()
                .object("activities")
                    .put("current", current)
                    .put("max", max)
                    .put("total", total)
                .endObject()
            .endObject()
            .toString();
    }

    @Override
    public String readIfChanged() {
        return null;
    }

}
