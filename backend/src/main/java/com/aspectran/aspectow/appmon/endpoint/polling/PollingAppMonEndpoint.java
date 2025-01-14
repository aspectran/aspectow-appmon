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
package com.aspectran.aspectow.appmon.endpoint.polling;

import com.aspectran.aspectow.appmon.config.EndpointInfo;
import com.aspectran.aspectow.appmon.config.EndpointPollingConfig;
import com.aspectran.aspectow.appmon.config.GroupInfo;
import com.aspectran.aspectow.appmon.endpoint.AppMonEndpoint;
import com.aspectran.aspectow.appmon.endpoint.AppMonSession;
import com.aspectran.aspectow.appmon.manager.AppMonManager;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Destroy;
import com.aspectran.core.component.bean.annotation.RequestToGet;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.FormatType;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.logging.Logger;
import com.aspectran.utils.logging.LoggerFactory;
import com.aspectran.utils.security.InvalidPBTokenException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component("/backend")
public class PollingAppMonEndpoint implements AppMonEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(PollingAppMonEndpoint.class);


    private final AppMonManager appMonManager;

    private final PollingAppMonService appMonService;

    @Autowired
    public PollingAppMonEndpoint(@NonNull AppMonManager appMonManager) throws Exception {
        this.appMonManager = appMonManager;

        EndpointInfo endpointInfo = appMonManager.getResidentEndpointInfo();
        EndpointPollingConfig pollingConfig = endpointInfo.getPollingConfig();
        if (pollingConfig != null && pollingConfig.isEnabled()) {
            this.appMonService = new PollingAppMonService(appMonManager, pollingConfig.getInitialBufferSize());
            this.appMonService.initialize();
            appMonManager.addEndpoint(this);
        } else {
            this.appMonService = null;
        }
    }

    @Destroy
    public void destroy() throws Exception {
        if (appMonService != null) {
            appMonService.destroy();
        }
    }

    @RequestToPost("/endpoint/${token}/join")
    @Transform(FormatType.JSON)
    public Map<String, Object> join(@NonNull Translet translet, String token) throws IOException {
        if (checkServiceUnavailable(token)) {
            return null;
        }

        EndpointInfo endpointInfo = appMonManager.getResidentEndpointInfo();
        EndpointPollingConfig pollingConfig = endpointInfo.getPollingConfig();

        String joinGroups = translet.getParameter("joinGroups");
        String[] joinGroupNames = appMonManager.getVerifiedGroupNames(StringUtils.splitCommaDelimitedString(joinGroups));
        if (StringUtils.hasText(joinGroups) && joinGroupNames.length == 0) {
            return null;
        }

        PollingAppMonSession appMonSession = appMonService.createSession(translet, pollingConfig, joinGroupNames);
        if (!appMonManager.join(appMonSession)) {
            return null;
        }

        List<GroupInfo> groups = appMonManager.getGroupInfoList(appMonSession.getJoinedGroups());
        List<String> messages = appMonManager.getLastMessages(appMonSession);
        return Map.of(
                "token", AppMonManager.issueToken(),
                "groups", groups,
                "pollingInterval", appMonSession.getPollingInterval(),
                "messages", messages
        );
    }

    @RequestToGet("/endpoint/${token}/pull")
    @Transform(FormatType.JSON)
    public Map<String, Object> pull(@NonNull Translet translet, String token) throws IOException {
        if (checkServiceUnavailable(token)) {
            return null;
        }

        PollingAppMonSession appMonSession = appMonService.getSession(translet);
        if (appMonSession == null || !appMonSession.isValid()) {
            return null;
        }

        String newToken = AppMonManager.issueToken(appMonSession.getPollingInterval() + 30);
        String[] messages = appMonService.pull(appMonSession);
        return Map.of(
                "token", newToken,
                "messages", (messages != null ? messages : new String[0])
        );
    }

    @RequestToPost("/endpoint/${token}/pollingInterval")
    @Transform(FormatType.TEXT)
    public int pollingInterval(@NonNull Translet translet, String token, int speed) {
        if (checkServiceUnavailable(token)) {
            return -1;
        }

        PollingAppMonSession appMonSession = appMonService.getSession(translet);
        if (appMonSession == null) {
            return -1;
        }

        if (speed == 1) {
            appMonSession.setPollingInterval(1000);
        } else {
            EndpointInfo endpointInfo = appMonManager.getResidentEndpointInfo();
            EndpointPollingConfig pollingConfig = endpointInfo.getPollingConfig();
            appMonSession.setPollingInterval(pollingConfig.getPollingInterval());
        }

        return appMonSession.getPollingInterval();
    }

    @Override
    public void broadcast(String message) {
        if (appMonService != null) {
            appMonService.push(message);
        }
    }

    @Override
    public void broadcast(AppMonSession session, String message) {
    }

    @Override
    public boolean isUsingGroup(String group) {
        if (appMonService != null) {
            return appMonService.isUsingGroup(group);
        } else {
            return false;
        }
    }

    private boolean checkServiceUnavailable(String token) {
        if (appMonService == null) {
            return true;
        }
        try {
            AppMonManager.validateToken(token);
            return false;
        } catch (InvalidPBTokenException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e);
            }
            return true;
        }
    }

}
