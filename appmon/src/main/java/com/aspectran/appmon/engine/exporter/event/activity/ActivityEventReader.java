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
package com.aspectran.appmon.engine.exporter.event.activity;

import com.aspectran.appmon.engine.config.EventInfo;
import com.aspectran.appmon.engine.exporter.ExporterManager;
import com.aspectran.appmon.engine.exporter.event.AbstractEventReader;
import com.aspectran.appmon.engine.persist.counter.EventCount;
import com.aspectran.core.component.UnavailableException;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.rule.AdviceRule;
import com.aspectran.core.context.rule.AspectRule;
import com.aspectran.core.context.rule.JoinpointRule;
import com.aspectran.core.context.rule.params.PointcutParameters;
import com.aspectran.core.context.rule.type.JoinpointTargetType;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.utils.json.JsonBuilder;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads Aspectran's activity events by dynamically adding an aspect.
 * This reader injects an advice into the target ActivityContext to capture
 * activity lifecycle events and broadcast them.
 *
 * <p>Created: 2024-12-18</p>
 */
public class ActivityEventReader extends AbstractEventReader {

    private static final Logger logger = LoggerFactory.getLogger(ActivityEventReader.class);

    private String aspectId;

    /**
     * Instantiates a new ActivityEventReader.
     * @param exporterManager the exporter manager
     * @param eventInfo the event configuration
     * @param eventCount the event counter
     */
    public ActivityEventReader(
            @NonNull ExporterManager exporterManager,
            @NonNull EventInfo eventInfo,
            @NonNull EventCount eventCount) {
        super(exporterManager, eventInfo, eventCount);
    }

    @Override
    public void init() throws Exception {
        aspectId = getClass().getName() + ".ASPECT@" + hashCode() + "[" + getEventInfo().getTarget() + "]";
    }

    @Override
    public void start() throws Exception {
        ActivityContext context = CoreServiceHolder.findActivityContext(getEventInfo().getTarget());
        if (context == null) {
            throw new Exception("Could not find ActivityContext named '" + getEventInfo().getTarget() + "'");
        }

        AspectRule aspectRule = new AspectRule();
        aspectRule.setId(aspectId);
        aspectRule.setOrder(0);
        aspectRule.setIsolated(true);

        JoinpointRule joinpointRule = new JoinpointRule();
        joinpointRule.setJoinpointTargetType(JoinpointTargetType.ACTIVITY);
        if (getEventInfo().hasParameters()) {
            PointcutParameters pointcutParameters = new PointcutParameters(getEventInfo().getParameters().toString());
            JoinpointRule.updatePointcutRule(joinpointRule, pointcutParameters);
        }
        aspectRule.setJoinpointRule(joinpointRule);

        AdviceRule beforeAdviceRule = aspectRule.newBeforeAdviceRule();
        beforeAdviceRule.setAdviceAction(activity -> {
            ActivityEventAdvice activityEventAdvice = new ActivityEventAdvice(ActivityEventReader.this);
            activityEventAdvice.before(activity);
            return activityEventAdvice;
        });

        AdviceRule finallyAdviceRule = aspectRule.newFinallyAdviceRule();
        finallyAdviceRule.setAdviceAction(activity -> {
            ActivityEventAdvice activityEventAdvice = activity.getBeforeAdviceResult(aspectId);
            String json = activityEventAdvice.after(activity);
            getEventExporter().broadcast(json);
            return null;
        });

        context.getAspectRuleRegistry().addAspectRule(aspectRule);
    }

    @Override
    public void stop() {
        try {
            ActivityContext context = CoreServiceHolder.findActivityContext(getEventInfo().getTarget());
            if (context != null) {
                try {
                    context.getAspectRuleRegistry().removeAspectRule(aspectId);
                } catch (UnavailableException e) {
                    // ignored
                }
            }
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }

    @Override
    public String read() {
        long interim = getEventCount().getTallying().getTotal();
        long total = interim + getEventCount().getTallied().getTotal();
        long errors = getEventCount().getTallying().getError();

        return new JsonBuilder()
            .prettyPrint(false)
            .nullWritable(false)
            .object()
                .object("activities")
                    .put("total", total)
                    .put("interim", interim)
                    .put("errors", errors)
                .endObject()
            .endObject()
            .toString();
    }

}
