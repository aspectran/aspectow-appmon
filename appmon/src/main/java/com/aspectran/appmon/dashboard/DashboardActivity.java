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
package com.aspectran.appmon.dashboard;

import com.aspectran.appmon.AboutMe;
import com.aspectran.appmon.engine.config.DomainInfo;
import com.aspectran.appmon.engine.config.InstanceInfo;
import com.aspectran.appmon.engine.manager.AppMonManager;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToGet;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.activity.response.DefaultRestResponse;
import com.aspectran.web.activity.response.RestResponse;

import java.util.List;
import java.util.Map;

/**
 * Handles requests from backend agents (monitored applications).
 * This class provides configuration data to the agents.
 *
 * <p>Created: 2020/02/23</p>
 */
@Component("/dashboard")
public class DashboardActivity {

    private final AppMonManager appMonManager;

    @Autowired
    public DashboardActivity(AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
    }

    /**
     * Displays the main monitoring page after validating the token.
     * @param instances the comma-separated list of instances to monitor
     * @return a map of attributes for rendering the view, or {@code null} if redirected
     */
    @Request("/${instances}")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, String> front(String instances) {
        return Map.of(
                "headinclude", "appmon/_domains",
                "include", "appmon/appmon",
                "style", "fluid compact",
                "version", AboutMe.getVersion(),
                "instances", StringUtils.nullToEmpty(instances)
        );
    }

    /**
     * Provides configuration data to a backend agent after validating the token.
     * @param instances a comma-separated list of instance names to get configuration for
     * @return a {@link RestResponse} containing the configuration data
     */
    @RequestToGet("/config")
    public RestResponse getConfigData(String instances) {
        Map<String, Object> settings = Map.of(
                "counterPersistInterval", appMonManager.getCounterPersistInterval()
        );

        List<DomainInfo> domainInfoList = appMonManager.getDomainInfoList();

        String[] instanceNames = StringUtils.splitWithComma(instances);
        instanceNames = appMonManager.getVerifiedInstanceNames(instanceNames);
        List<InstanceInfo> instanceInfoList = appMonManager.getInstanceInfoList(instanceNames);

        Map<String, Object> data = Map.of(
                "token", AppMonManager.issueToken(),
                "settings", settings,
                "domains", domainInfoList,
                "instances", instanceInfoList
        );
        return new DefaultRestResponse(data).nullWritable(false).ok();
    }

}
