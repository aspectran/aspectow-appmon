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
package com.aspectran.appmon.service.polling;

import com.aspectran.appmon.config.InstanceInfo;
import com.aspectran.appmon.config.PollingConfig;
import com.aspectran.appmon.manager.AppMonManager;
import com.aspectran.appmon.service.ExportService;
import com.aspectran.appmon.service.ServiceSession;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Destroy;
import com.aspectran.core.component.bean.annotation.Initialize;
import com.aspectran.core.component.bean.annotation.Qualifier;
import com.aspectran.core.component.bean.annotation.RequestToGet;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.FormatType;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.security.InvalidPBTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component("/backend")
public class PollingExportService implements ExportService {

    private static final Logger logger = LoggerFactory.getLogger(PollingExportService.class);

    private final AppMonManager appMonManager;

    private final PollingServiceSessionManager sessionManager;

    @Autowired
    public PollingExportService(@NonNull AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
        this.sessionManager = new PollingServiceSessionManager(appMonManager);
    }

    @Initialize
    public void registerExportService() throws Exception {
        sessionManager.initialize();
        appMonManager.getExportServiceManager().addExportService(this);
    }

    @Destroy
    public void destroy() throws Exception {
        sessionManager.destroy();
        appMonManager.getExportServiceManager().removeExportService(this);
    }

    @RequestToPost("/${token}/polling/join")
    @Transform(FormatType.JSON)
    public Map<String, Object> join(@NonNull Translet translet, String token) throws IOException {
        if (checkServiceAvailable(token)) {
            return null;
        }

        PollingConfig pollingConfig = appMonManager.getPollingConfig();

        String specificInstances = translet.getParameter("instances");
        String[] instanceNames = StringUtils.splitWithComma(specificInstances);
        instanceNames = appMonManager.getVerifiedInstanceNames(instanceNames);
        if (StringUtils.hasText(specificInstances) && instanceNames.length == 0) {
            return null;
        }

        PollingServiceSession serviceSession = sessionManager.createSession(translet, pollingConfig, instanceNames);
        if (!appMonManager.getExportServiceManager().join(serviceSession)) {
            return null;
        }

        List<InstanceInfo> instanceInfoList = appMonManager.getInstanceInfoList(serviceSession.getJoinedInstances());
        List<String> messages = appMonManager.getExportServiceManager().getLastMessages(serviceSession);
        return Map.of(
                "token", AppMonManager.issueToken(),
                "instances", instanceInfoList,
                "pollingInterval", serviceSession.getPollingInterval(),
                "messages", messages
        );
    }

    @RequestToGet("/${token}/polling/pull")
    @Transform(FormatType.JSON)
    public Map<String, Object> pull(@NonNull Translet translet,
                                    String token,
                                    @Qualifier("commands[]") String[] commands) throws IOException {
        if (checkServiceAvailable(token)) {
            return null;
        }

        PollingServiceSession serviceSession = sessionManager.getSession(translet);
        if (serviceSession == null || !serviceSession.isValid()) {
            return null;
        }

        if (commands != null) {
            for (String command : commands) {
                if ("refresh".equals(command)) {
                    List<String> newMessages = appMonManager.getExportServiceManager().getNewMessages(serviceSession);
                    for (String msg : newMessages) {
                        sessionManager.push(msg);
                    }
                }
            }
        }

        String newToken = AppMonManager.issueToken(1800); // 30 min.
        String[] messages = sessionManager.pull(serviceSession);
        return Map.of(
                "token", newToken,
                "messages", (messages != null ? messages : new String[0])
        );
    }

    @RequestToPost("/${token}/polling/interval")
    @Transform(FormatType.TEXT)
    public Map<String, Object> pollingInterval(@NonNull Translet translet, String token, int speed) {
        if (checkServiceAvailable(token)) {
            return null;
        }

        PollingServiceSession serviceSession = sessionManager.getSession(translet);
        if (serviceSession == null) {
            return null;
        }

        if (speed == 1) {
            serviceSession.setPollingInterval(1000);
        } else {
            PollingConfig pollingConfig = appMonManager.getPollingConfig();
            serviceSession.setPollingInterval(pollingConfig.getPollingInterval());
        }

        return Map.of(
            "pollingInterval", serviceSession.getPollingInterval()
        );
    }

    @Override
    public void broadcast(String message) {
        sessionManager.push(message);
    }

    @Override
    public void broadcast(ServiceSession serviceSession, String message) {
    }

    @Override
    public boolean isUsingInstance(String instanceName) {
        return sessionManager.isUsingInstance(instanceName);
    }

    private boolean checkServiceAvailable(String token) {
        try {
            AppMonManager.validateToken(token);
            return false;
        } catch (InvalidPBTokenException e) {
            logger.error("Invalid token: {}", token);
            return true;
        }
    }

}
