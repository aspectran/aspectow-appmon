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
package com.aspectran.aspectow.appmon.common.support;

import com.aspectran.aspectow.node.manager.NodePortProvider;
import com.aspectran.core.component.bean.aware.ActivityContextAware;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.netty.server.NettyServer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A bridge class that provides the active port from a Netty server.
 *
 * <p>Created: 2026-09-04</p>
 */
public class NettyServerPortProvider implements NodePortProvider, ActivityContextAware {

    private final String serverBeanId;

    private ActivityContext context;

    /**
     * Constructs a new {@code NettyServerPortProvider}.
     */
    public NettyServerPortProvider() {
        this(null);
    }

    /**
     * Constructs a new {@code NettyServerPortProvider} with the specified bean ID of the Netty server.
     * @param serverBeanId the bean ID of the Netty server
     */
    public NettyServerPortProvider(@Nullable String serverBeanId) {
        this.serverBeanId = serverBeanId;
    }

    /**
     * Sets the activity context.
     * @param context the activity context
     */
    @Override
    public void setActivityContext(@NonNull ActivityContext context) {
        this.context = context;
    }

    /**
     * Retrieves the active port of the Netty server.
     * @return the active port, or {@code null} if it cannot be retrieved
     */
    @Override
    public Integer getActivePort() {
        try {
            NettyServer nettyServer;
            if (serverBeanId != null) {
                nettyServer = context.getBeanRegistry().getBean(NettyServer.class, serverBeanId);
            } else {
                nettyServer = context.getBeanRegistry().getBean(NettyServer.class);
            }
            if (nettyServer != null) {
                int port = nettyServer.getActivePort();
                if (port > 0) {
                    return port;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

}
