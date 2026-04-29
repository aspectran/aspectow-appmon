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
package com.aspectran.aspectow.console.commands.manager;

import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.aware.ActivityContextAware;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.service.CoreService;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.daemon.command.CommandResult;
import com.aspectran.daemon.service.DefaultDaemonService;
import com.aspectran.daemon.service.DefaultDaemonServiceBuilder;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LocalCommandService handles the actual execution of daemon commands
 * within the local node using DaemonService.
 */
@Component
public class LocalCommandService implements ActivityContextAware {

    private static final Logger logger = LoggerFactory.getLogger(LocalCommandService.class);

    private ActivityContext activityContext;

    private DefaultDaemonService daemonService;

    @Override
    public void setActivityContext(@NonNull ActivityContext activityContext) {
        this.activityContext = activityContext;
    }

    private synchronized void setupDaemonService() {
        if (daemonService != null) {
            return;
        }

        for (CoreService service : CoreServiceHolder.getAllServices()) {
            if (service instanceof DefaultDaemonService ds) {
                daemonService = ds;
                break;
            }
        }

        if (daemonService == null) {
            CoreService baseService = null;
            if (activityContext != null) {
                baseService = activityContext.getMasterService().getRootService();
            } else {
                for (CoreService service : CoreServiceHolder.getAllServices()) {
                    baseService = service.getRootService();
                    break;
                }
            }

            if (baseService != null) {
                logger.info("No active DaemonService found; starting a new one based on root service [{}]",
                        baseService.getServiceName());
                try {
                    daemonService = DefaultDaemonServiceBuilder.build(baseService);
                    if (daemonService.getServiceLifeCycle().isOrphan()) {
                        daemonService.start();
                    }
                } catch (Exception e) {
                    logger.error("Failed to build and start DaemonService", e);
                }
            }
        }
    }

    /**
     * Executes a daemon command on the local node.
     * @param commandData the command payload in APON/JSON format
     * @return the execution result
     */
    public String execute(String commandData) {
        if (daemonService == null) {
            setupDaemonService();
        }

        if (daemonService != null) {
            try {
                CommandResult commandResult = daemonService.execute(commandData);
                if (!commandResult.isSuccess() && commandResult.getError() != null) {
                    logger.error("Local command execution failed: {}", commandResult.getError());
                }
                return commandResult.getResult();
            } catch (Exception e) {
                logger.error("Error executing local daemon command", e);
                return "[FAILED] Error executing command: " + e.getMessage();
            }
        } else {
            logger.warn("DaemonService is not available for local command processing");
            return "[FAILED] Local DaemonService is not available";
        }
    }

}
