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
package com.aspectran.aspectow.appmon.dashboard;

import com.aspectran.aspectow.appmon.common.auth.AppMonTokenIssuer;
import com.aspectran.aspectow.appmon.engine.config.AppInfo;
import com.aspectran.aspectow.appmon.engine.manager.AppMonManager;
import com.aspectran.aspectow.node.config.GroupInfo;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
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
 *
 * <p>Created: 2020/02/23</p>
 */
@Component
@Profile("appmon.standalone")
public class DashboardActivity {

    private final AppMonManager appMonManager;

    /**
     * Instantiates a new DashboardActivity.
     * @param appMonManager the application monitor manager
     */
    @Autowired
    public DashboardActivity(AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
    }

    /**
     * Displays the main monitoring page.
     * @param appsToSubscribe the comma-separated list of apps to monitor
     * @return a map of attributes for rendering the view
     */
    @Request("/dashboard/${appsToSubscribe}")
    @Dispatch("dashboard/dashboard")
    @Action("page")
    public Map<String, String> dashboard(String appsToSubscribe) {
        return Map.of(
                "headinclude", "dashboard/_groups",
                "style", "fluid compact",
                "appsToSubscribe", StringUtils.nullToEmpty(appsToSubscribe)
        );
    }

    /**
     * Provides configuration data to a backend agent.
     * @param appsToSubscribe a comma-separated list of app names to get configuration for
     * @return a {@link RestResponse} containing the configuration data
     */
    @RequestToGet("/appmon/config/data")
    public RestResponse getConfigData(String appsToSubscribe) {
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
        }

        Set<String> activeGroupIds = appInfoList.stream()
                .filter(app -> !app.isHidden())
                .map(AppInfo::getGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        groupInfoList = groupInfoList.stream()
                .filter(group -> activeGroupIds.contains(group.getId()))
                .collect(Collectors.toList());

        if (appMonManager.isGatewayMode()) {
            nodeInfoList = new ArrayList<>(nodeInfoList);
            nodeInfoList.sort(Comparator.comparing(NodeInfo::getId, Comparator.nullsLast(String::compareTo)));
        }

        Map<String, Object> data = Map.of(
                "token", AppMonTokenIssuer.issueToken(30),
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
