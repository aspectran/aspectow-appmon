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
package com.aspectran.aspectow.appmon.backend.persist.counter.activity;

import com.aspectran.aspectow.appmon.backend.config.EventInfo;
import com.aspectran.aspectow.appmon.backend.persist.counter.AbstractCounterReader;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.rule.AspectAdviceRule;
import com.aspectran.core.context.rule.AspectRule;
import com.aspectran.core.context.rule.JoinpointRule;
import com.aspectran.core.context.rule.params.PointcutParameters;
import com.aspectran.core.context.rule.type.AspectAdviceType;
import com.aspectran.core.context.rule.type.JoinpointTargetType;
import com.aspectran.core.service.CoreService;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.core.service.ServiceHoldingListener;
import com.aspectran.utils.annotation.jsr305.NonNull;

/**
 * <p>Created: 2025-02-12</p>
 */
public class ActivityCounterReader extends AbstractCounterReader {

    private final String aspectId;

    public ActivityCounterReader(@NonNull EventInfo eventInfo) {
        super(eventInfo);
        this.aspectId = getClass().getName() + ".ASPECT@" + hashCode() + "[" + eventInfo.getTarget() + "]";
    }

    @Override
    public void initialize() throws Exception {
        ActivityContext context = CoreServiceHolder.findActivityContext(getEventInfo().getTarget());
        if (context != null) {
            registerAspect(context);
        } else {
            CoreServiceHolder.addServiceHolingListener(new ServiceHoldingListener() {
                @Override
                public void afterServiceHolding(CoreService service) {
                    if (service.getActivityContext() != null) {
                        String contextName = service.getActivityContext().getName();
                        if (contextName != null && contextName.equals(getEventInfo().getTarget())) {
                            registerAspect(service.getActivityContext());
                        }
                    }
                }
            });
        }
    }

    private void registerAspect(@NonNull ActivityContext context) {
        if (context.getAspectRuleRegistry().contains(aspectId)) {
            return;
        }
        try {
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

            AspectAdviceRule afterAspectAdviceRule = aspectRule.newAspectAdviceRule(AspectAdviceType.AFTER);
            afterAspectAdviceRule.setAdviceAction(activity -> {
                getCounterData().count();
                return null;
            });

            context.getAspectRuleRegistry().addAspectRule(aspectRule);
        } catch (Exception e) {
            throw new RuntimeException("Cannot register aspect rule", e);
        }
    }

}
