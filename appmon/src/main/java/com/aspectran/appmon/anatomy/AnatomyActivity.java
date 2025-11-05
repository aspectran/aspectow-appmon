/*
 * Copyright (c) 2008-present The Aspectran Project
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
package com.aspectran.appmon.anatomy;

import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.rule.type.FormatType;
import com.aspectran.core.service.CoreService;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.web.activity.response.DefaultRestResponse;
import com.aspectran.web.activity.response.RestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A controller that provides framework anatomy data for the viewer.
 */
@Component
@Bean("anatomyActivity")
public class AnatomyActivity {

    private static final Logger logger = LoggerFactory.getLogger(AnatomyActivity.class);

    private final AnatomyService anatomyService;

    @Autowired
    public AnatomyActivity(AnatomyService anatomyService) {
        this.anatomyService = anatomyService;
    }

    /**
     * Dispatches to the anatomy viewer page within the default template.
     */
    @Request("/anatomy/${contextName}")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, Object> viewer(String contextName) {
        Map<String, ActivityContext> contexts = prepareContextMap();
        List<String> allContextNames = new ArrayList<>(contexts.keySet());
        if (contextName == null || !allContextNames.contains(contextName)) {
            if (!allContextNames.isEmpty()) {
                contextName = allContextNames.getFirst();
            }
        }
        if (contextName == null) {
            contextName = "0";
        }
        return Map.of(
                "headinclude", "anatomy/_contexts",
                "include", "anatomy/viewer",
                "style", "fluid compact",
                "allContextNames", allContextNames,
                "contextName", contextName
        );
    }

    /**
     * Provides framework anatomy data as JSON.
     * @return a map containing the anatomy data, identified by "anatomyData"
     */
    @Request("/anatomy/${contextName}/data")
    @Transform(format = FormatType.JSON)
    public RestResponse data(String contextName) {
        Map<String, ActivityContext> contexts = prepareContextMap();
        ActivityContext context = contexts.get(contextName);
        if (context == null) {
            return new DefaultRestResponse().notFound();
        }
        Map<String, Object> data = anatomyService.getAnatomyData(context);
        return new DefaultRestResponse("anatomyData", data).nullWritable(false).ok();
    }

    @NonNull
    public static Map<String, ActivityContext> prepareContextMap() {
        List<CoreService> services = new ArrayList<>(CoreServiceHolder.getAllServices());
        Collections.reverse(services);

        Map<String, ActivityContext> contextMap = new LinkedHashMap<>();
        Set<ActivityContext> seenContexts = new LinkedHashSet<>();
        int index = 0;
        for (CoreService service : services) {
            ActivityContext currentContext = service.getActivityContext();
            if (seenContexts.add(currentContext)) {
                String name = service.getContextName();
                if (StringUtils.isEmpty(name)) {
                    name = Integer.toString(index);
                }
                contextMap.putIfAbsent(name, currentContext);
            }
            index++;
        }
        return contextMap;
    }

}
