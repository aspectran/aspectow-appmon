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
package com.aspectran.aspectow.console.monitoring;

import com.aspectran.aspectow.appmon.common.auth.AppMonTokenIssuer;
import com.aspectran.aspectow.appmon.engine.config.AppInfo;
import com.aspectran.aspectow.appmon.engine.manager.AppMonManager;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Hint;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToGet;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.activity.response.DefaultRestResponse;
import com.aspectran.web.activity.response.RestResponse;

import java.util.List;
import java.util.Map;

/**
 * Handles requests for the Application Monitor dashboard.
 * This includes serving the main monitoring page and providing configuration
 * data to backend agents.
 *
 * <p>Created: 2020/02/23</p>
 */
@Component("/appmon")
public class AppMonActivity {

    private final AppMonManager appMonManager;

    /**
     * Instantiates a new DashboardActivity.
     * @param appMonManager the application monitor manager
     */
    @Autowired
    public AppMonActivity(AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
    }

    /**
     * Displays the main monitoring page.
     * @param apps the comma-separated list of apps to monitor
     * @return a map of attributes for rendering the view
     */
    @Request("/dashboard/${apps}")
    @Dispatch("appmon/dashboard")
    @Action("page")
    public Map<String, String> dashboard(String apps) {
        return Map.of(
                "title", "Application Monitoring",
                "style", "monitoring-page",
                "group", "cluster-menu",
                "apps", StringUtils.nullToEmpty(apps),
                "layout", "default"
        );
    }

    /**
     * Displays the monitoring page as a popup.
     * @param apps the comma-separated list of apps to monitor
     * @return a map of attributes for rendering the view
     */
    @Request("/dashboard/popup/${apps}")
    @Dispatch("appmon/dashboard")
    @Action("page")
    @Hint(type = "layout", value = "layout: popup")
    public Map<String, String> dashboardPopup(String apps) {
        return Map.of(
                "title", "Application Monitoring",
                "style", "monitoring-page",
                "apps", StringUtils.nullToEmpty(apps),
                "layout", "popup"
        );
    }

    /**
     * Provides configuration data to a backend agent.
     * @param apps a comma-separated list of app names to get configuration for
     * @return a {@link RestResponse} containing the configuration data
     */
    @RequestToGet("/config/data")
    public RestResponse getConfigData(String apps) {
        Map<String, Object> settings = Map.of(
                "counterPersistInterval", appMonManager.getCounterPersistInterval()
        );

        List<NodeInfo> nodeInfoList = appMonManager.getNodeInfoList();

        String[] appIds = StringUtils.splitWithComma(apps);
        appIds = appMonManager.getVerifiedAppIds(appIds);
        List<AppInfo> appInfoList = appMonManager.getAppInfoList(appIds);

        Map<String, Object> data = Map.of(
                "token", AppMonTokenIssuer.issueToken(30),
                "settings", settings,
                "nodes", nodeInfoList,
                "apps", appInfoList
        );
        return new DefaultRestResponse(data).nullWritable(false).ok();
    }

}
