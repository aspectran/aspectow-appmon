/*
 * Copyright (c) 2026-present The Aspectran Project
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
package com.aspectran.aspectow.console.scheduler.manager;

import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.schedule.ScheduleRuleRegistry;
import com.aspectran.core.context.rule.ScheduleRule;
import com.aspectran.core.context.rule.ScheduledJobRule;
import com.aspectran.core.context.rule.converter.RulesToParameters;
import com.aspectran.core.context.rule.params.ScheduleParameters;
import com.aspectran.core.scheduler.service.SchedulerService;
import com.aspectran.core.service.CoreService;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.utils.json.JsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * LocalSchedulerService provides refined methods to collect and control
 * schedulers within the local node.
 */
@Component
public class LocalSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(LocalSchedulerService.class);

    /**
     * Collects all schedule information from active CoreServices and returns as JSON.
     * @return the JSON string containing all services and their schedules
     */
    public String getSchedulesAsJson() {
        JsonBuilder jsonBuilder = new JsonBuilder().object();
        jsonBuilder.put("type", "list");
        jsonBuilder.array("services");

        int serviceCount = 0;
        for (CoreService service : CoreServiceHolder.getAllServices()) {
            if (service.getServiceLifeCycle().isActive()) {
                SchedulerService schedulerService = service.getSchedulerService();
                if (schedulerService != null) {
                    ScheduleRuleRegistry registry = schedulerService.getActivityContext().getScheduleRuleRegistry();
                    if (registry != null) {
                        jsonBuilder.object();
                        jsonBuilder.put("serviceName", schedulerService.getServiceName());
                        jsonBuilder.put("contextName", schedulerService.getActivityContext().getName());
                        jsonBuilder.array("schedules");
                        for (ScheduleRule scheduleRule : registry.getScheduleRules()) {
                            ScheduleParameters params = RulesToParameters.toScheduleParameters(scheduleRule);
                            jsonBuilder.put(params);
                        }
                        jsonBuilder.endArray();
                        jsonBuilder.endObject();
                        serviceCount++;
                    }
                }
            }
        }

        jsonBuilder.endArray();
        jsonBuilder.endObject();

        if (logger.isDebugEnabled()) {
            logger.debug("Collected scheduler list from {} active services", serviceCount);
        }
        return jsonBuilder.toString();
    }

    /**
     * Updates the enabled/disabled state of a specific schedule or job.
     * @param serviceName the name of the service
     * @param type either 'schedule' or 'job'
     * @param id the ID of the schedule or the translet name of the job
     * @param disabled true to disable, false to enable
     * @return the result message as JSON
     */
    public String updateState(String serviceName, String type, String id, boolean disabled) {
        boolean changed = false;
        for (CoreService service : CoreServiceHolder.getAllServices()) {
            if (service.getServiceLifeCycle().isActive()) {
                SchedulerService schedulerService = service.getSchedulerService();
                if (schedulerService != null && schedulerService.getServiceName().equals(serviceName)) {
                    ScheduleRuleRegistry registry = schedulerService.getActivityContext().getScheduleRuleRegistry();
                    if (registry != null) {
                        if ("schedule".equals(type)) {
                            ScheduleRule scheduleRule = registry.getScheduleRule(id);
                            if (scheduleRule != null && !scheduleRule.isIsolated()) {
                                scheduleRule.setDisabled(disabled);
                                changed = true;
                            }
                        } else if ("job".equals(type)) {
                            Set<ScheduledJobRule> jobRules = registry.getScheduledJobRules(new String[]{id});
                            if (!jobRules.isEmpty()) {
                                for (ScheduledJobRule jobRule : jobRules) {
                                    if (!jobRule.isIsolated()) {
                                        jobRule.setDisabled(disabled);
                                        changed = true;
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }

        String resultMessage;
        if (changed) {
            resultMessage = (disabled ? "Disabled" : "Enabled") + " " + type + " '" + id + "' in service '" + serviceName + "'";
        } else {
            resultMessage = "Failed to change state for " + type + " '" + id + "' in service '" + serviceName + "' (Not found or isolated)";
        }

        return new JsonBuilder().object()
                .put("type", "result")
                .put("success", changed)
                .put("message", resultMessage)
                .endObject()
                .toString();
    }

}
