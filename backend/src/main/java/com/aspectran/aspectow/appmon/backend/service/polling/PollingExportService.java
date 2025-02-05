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
package com.aspectran.aspectow.appmon.backend.service.polling;

import com.aspectran.aspectow.appmon.backend.config.EndpointInfo;
import com.aspectran.aspectow.appmon.backend.config.EndpointPollingConfig;
import com.aspectran.aspectow.appmon.backend.config.InstanceInfo;
import com.aspectran.aspectow.appmon.backend.service.BackendSession;
import com.aspectran.aspectow.appmon.backend.service.ExportService;
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

@Component("/backend/polling")
public class PollingExportService implements ExportService {

    private static final Logger logger = LoggerFactory.getLogger(PollingExportService.class);

    private final AppMonManager appMonManager;

    private final PollingBackendSessionManager endpointSessionManager;

    @Autowired
    public PollingExportService(@NonNull AppMonManager appMonManager) throws Exception {
        this.appMonManager = appMonManager;

        EndpointInfo endpointInfo = appMonManager.getResidentEndpointInfo();
        EndpointPollingConfig pollingConfig = endpointInfo.getPollingConfig();
        if (pollingConfig != null && pollingConfig.isEnabled()) {
            this.endpointSessionManager = new PollingBackendSessionManager(appMonManager, pollingConfig.getInitialBufferSize());
            this.endpointSessionManager.initialize();
            appMonManager.addExportService(this);
        } else {
            this.endpointSessionManager = null;
        }
    }

    @Destroy
    public void destroy() throws Exception {
        if (endpointSessionManager != null) {
            endpointSessionManager.destroy();
        }
    }

    @RequestToPost("/${token}/join")
    @Transform(FormatType.JSON)
    public Map<String, Object> join(@NonNull Translet translet, String token) throws IOException {
        if (checkServiceUnavailable(token)) {
            return null;
        }

        EndpointInfo endpointInfo = appMonManager.getResidentEndpointInfo();
        EndpointPollingConfig pollingConfig = endpointInfo.getPollingConfig();

        String joinInstances = translet.getParameter("joinInstances");
        String[] instanceNames = appMonManager.getVerifiedInstanceNames(StringUtils.splitCommaDelimitedString(joinInstances));
        if (StringUtils.hasText(joinInstances) && instanceNames.length == 0) {
            return null;
        }

        PollingBackendSession endpointSession = endpointSessionManager.createSession(translet, pollingConfig, instanceNames);
        if (!appMonManager.join(endpointSession)) {
            return null;
        }

        List<InstanceInfo> instanceInfoList = appMonManager.getInstanceInfoList(endpointSession.getJoinedInstances());
        List<String> messages = appMonManager.getLastMessages(endpointSession);
        return Map.of(
                "token", AppMonManager.issueToken(),
                "instances", instanceInfoList,
                "pollingInterval", endpointSession.getPollingInterval(),
                "messages", messages
        );
    }

    @RequestToGet("/${token}/pull")
    @Transform(FormatType.JSON)
    public Map<String, Object> pull(@NonNull Translet translet, String token) throws IOException {
        if (checkServiceUnavailable(token)) {
            return null;
        }

        PollingBackendSession endpointSession = endpointSessionManager.getSession(translet);
        if (endpointSession == null || !endpointSession.isValid()) {
            return null;
        }

        String newToken = AppMonManager.issueToken(endpointSession.getPollingInterval() + 30);
        String[] messages = endpointSessionManager.pull(endpointSession);
        return Map.of(
                "token", newToken,
                "messages", (messages != null ? messages : new String[0])
        );
    }

    @RequestToPost("/${token}/pollingInterval")
    @Transform(FormatType.TEXT)
    public int pollingInterval(@NonNull Translet translet, String token, int speed) {
        if (checkServiceUnavailable(token)) {
            return -1;
        }

        PollingBackendSession endpointSession = endpointSessionManager.getSession(translet);
        if (endpointSession == null) {
            return -1;
        }

        if (speed == 1) {
            endpointSession.setPollingInterval(1000);
        } else {
            EndpointInfo endpointInfo = appMonManager.getResidentEndpointInfo();
            EndpointPollingConfig pollingConfig = endpointInfo.getPollingConfig();
            endpointSession.setPollingInterval(pollingConfig.getPollingInterval());
        }

        return endpointSession.getPollingInterval();
    }

    @Override
    public void broadcast(String message) {
        if (endpointSessionManager != null) {
            endpointSessionManager.push(message);
        }
    }

    @Override
    public void broadcast(BackendSession session, String message) {
    }

    @Override
    public boolean isUsingInstance(String instanceName) {
        if (endpointSessionManager != null) {
            return endpointSessionManager.isUsingInstance(instanceName);
        } else {
            return false;
        }
    }

    private boolean checkServiceUnavailable(String token) {
        if (endpointSessionManager == null) {
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
