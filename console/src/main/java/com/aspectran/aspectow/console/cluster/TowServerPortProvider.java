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
package com.aspectran.aspectow.console.cluster;

import com.aspectran.aspectow.node.manager.NodePortProvider;
import com.aspectran.core.component.bean.aware.ActivityContextAware;
import com.aspectran.core.context.ActivityContext;

import com.aspectran.undertow.server.TowServer;
import io.undertow.Undertow;
import org.jspecify.annotations.NonNull;

import java.net.InetSocketAddress;

/**
 * A bridge class that provides the active port from an Undertow server.
 *
 * <p>Created: 2026-04-19</p>
 */
public class TowServerPortProvider implements NodePortProvider, ActivityContextAware {

    private final String serverBeanId;

    private ActivityContext context;

    public TowServerPortProvider(String serverBeanId) {
        this.serverBeanId = serverBeanId;
    }

    @Override
    public void setActivityContext(@NonNull ActivityContext context) {
        this.context = context;
    }

    @Override
    public Integer getActivePort() {
        try {
            TowServer towServer = context.getBeanRegistry().getBean(TowServer.class, serverBeanId);
            if (towServer != null) {
                Undertow undertow = towServer.getUndertow();
                if (undertow != null) {
                    for (Undertow.ListenerInfo listenerInfo : undertow.getListenerInfo()) {
                        // Note: Undertow has a typo in the method name 'getProtcol'
                        if ("http".equals(listenerInfo.getProtcol())) {
                            return ((InetSocketAddress)listenerInfo.getAddress()).getPort();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

}
