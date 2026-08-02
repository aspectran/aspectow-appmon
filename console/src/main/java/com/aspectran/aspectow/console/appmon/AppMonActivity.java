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
package com.aspectran.aspectow.console.appmon;

import com.aspectran.aspectow.appmon.common.auth.AppMonTokenIssuer;
import com.aspectran.aspectow.appmon.engine.config.AppInfo;
import com.aspectran.aspectow.appmon.engine.manager.AppMonManager;
import com.aspectran.aspectow.console.auth.UserInfo;
import com.aspectran.aspectow.node.config.GroupInfo;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Hint;
import com.aspectran.core.component.bean.annotation.Profile;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToGet;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.activity.response.DefaultRestResponse;
import com.aspectran.web.activity.response.RestResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles requests for the Application Monitor dashboard.
 * This includes serving the main monitoring page and providing configuration
 * data to backend agents.
 */
@Component("/appmon")
@Profile("[console.ui, console.custom-ui]")
public class AppMonActivity {

    private final AppMonManager appMonManager;

    /**
     * Instantiates a new AppMonActivity.
     * @param appMonManager the application monitor manager
     */
    @Autowired
    public AppMonActivity(AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
    }

    /**
     * Displays the main monitoring page.
     * @param appsToSubscribe the comma-separated list of apps to monitor
     * @return a map of attributes for rendering the view
     */
    @Request("/dashboard/${appsToSubscribe}")
    @Dispatch("appmon/dashboard")
    @Action("page")
    public Map<String, Object> dashboard(String appsToSubscribe) {
        return Map.of(
                "title", "Application Monitoring",
                "style", "monitoring-page",
                "group", "cluster-menu",
                "appsToSubscribe", StringUtils.nullToEmpty(appsToSubscribe),
                "layout", "default",
                "clusterMode", appMonManager.getClusterMode()
        );
    }

    /**
     * Displays the monitoring page as a popup.
     * @param appsToSubscribe the comma-separated list of apps to monitor
     * @return a map of attributes for rendering the view
     */
    @Request("/dashboard/popup/${appsToSubscribe}")
    @Dispatch("appmon/dashboard")
    @Action("page")
    @Hint(type = "layout", value = "layout: popup")
    public Map<String, Object> dashboardPopup(String appsToSubscribe) {
        return Map.of(
                "title", "Application Monitoring",
                "style", "monitoring-page",
                "appsToJoin", StringUtils.nullToEmpty(appsToSubscribe),
                "layout", "popup",
                "clusterMode", appMonManager.getClusterMode()
        );
    }

    @RequestToGet("/config/data")
    public RestResponse getConfigData(Translet translet, String appsToSubscribe) {
        Map<String, Object> settings = Map.of(
                "counterPersistInterval", appMonManager.getCounterPersistInterval(),
                "clusterMode", appMonManager.getClusterMode()
        );

        List<NodeInfo> nodeInfoList = appMonManager.getNodeInfoList();
        List<GroupInfo> groupInfoList = appMonManager.getGroupInfoList();
        List<AppInfo> appInfoList = appMonManager.getClusterAppInfoList();

        String[] appIds = StringUtils.splitWithComma(appsToSubscribe);
        String[] verifiedAppIds = appMonManager.getVerifiedAppIds(appIds, appInfoList);

        if (StringUtils.hasText(appsToSubscribe)) {
            Set<String> verifiedAppIdSet = new HashSet<>(Arrays.asList(verifiedAppIds));
            appInfoList = appInfoList.stream()
                    .filter(app -> verifiedAppIdSet.contains(app.getAppId()))
                    .collect(Collectors.toList());

            Set<String> activeGroupIds = appInfoList.stream()
                    .map(AppInfo::getGroupId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            nodeInfoList = nodeInfoList.stream()
                    .filter(node -> activeGroupIds.contains(node.getGroup()))
                    .collect(Collectors.toList());

            groupInfoList = groupInfoList.stream()
                    .filter(group -> activeGroupIds.contains(group.getId()))
                    .collect(Collectors.toList());
        }
        if (appMonManager.isGatewayMode()) {
            nodeInfoList = new ArrayList<>(nodeInfoList);
            nodeInfoList.sort(Comparator.comparing(NodeInfo::getId, Comparator.nullsLast(String::compareTo)));
        }

        UserInfo userInfo = translet.getSessionAdapter().getAttribute(UserInfo.USERINFO_KEY);
        boolean isDemo = (userInfo != null && userInfo.hasRole("DEMO"));

        Map<String, Object> data = Map.of(
                "token", AppMonTokenIssuer.issueToken(30, isDemo),
                "myNodeId", appMonManager.getNodeId(),
                "myGroupId", appMonManager.getGroupId(),
                "appsToSubscribe", StringUtils.join(verifiedAppIds, ","),
                "settings", settings,
                "nodes", nodeInfoList,
                "groups", groupInfoList,
                "apps", appInfoList
        );
        return new DefaultRestResponse(data).nullWritable(false).ok();
    }

}
