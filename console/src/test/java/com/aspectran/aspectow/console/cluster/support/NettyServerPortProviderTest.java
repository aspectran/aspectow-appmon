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
package com.aspectran.aspectow.console.cluster.support;

import com.aspectran.core.component.bean.BeanRegistry;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.netty.server.NettyServer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test cases for {@link NettyServerPortProvider}.
 *
 * <p>Created: 2026-09-04</p>
 */
class NettyServerPortProviderTest {

    @Test
    void getActivePort_withBeanId_returnsActivePort() {
        NettyServer nettyServer = createMockNettyServer(8080);
        ActivityContext context = createMockContext("netty.server", nettyServer);

        NettyServerPortProvider provider = new NettyServerPortProvider("netty.server");
        provider.setActivityContext(context);

        assertEquals(8080, provider.getActivePort());
    }

    @Test
    void getActivePort_withoutBeanId_returnsActivePort() {
        NettyServer nettyServer = createMockNettyServer(9090);
        ActivityContext context = createMockContext(null, nettyServer);

        NettyServerPortProvider provider = new NettyServerPortProvider();
        provider.setActivityContext(context);

        assertEquals(9090, provider.getActivePort());
    }

    @Test
    void getActivePort_whenServerInactive_returnsNull() {
        NettyServer nettyServer = createMockNettyServer(-1);
        ActivityContext context = createMockContext("netty.server", nettyServer);

        NettyServerPortProvider provider = new NettyServerPortProvider("netty.server");
        provider.setActivityContext(context);

        assertNull(provider.getActivePort());
    }

    @Test
    void getActivePort_whenBeanNotFound_returnsNull() {
        ActivityContext context = createMockContext("netty.server", null);

        NettyServerPortProvider provider = new NettyServerPortProvider("netty.server");
        provider.setActivityContext(context);

        assertNull(provider.getActivePort());
    }

    @Test
    void getActivePort_whenContextNotSet_returnsNull() {
        NettyServerPortProvider provider = new NettyServerPortProvider("netty.server");
        assertNull(provider.getActivePort());
    }

    private NettyServer createMockNettyServer(int activePort) {
        return (NettyServer) Proxy.newProxyInstance(
                NettyServer.class.getClassLoader(),
                new Class<?>[] { NettyServer.class },
                (proxy, method, args) -> {
                    if ("getActivePort".equals(method.getName()) && (args == null || args.length == 0)) {
                        return activePort;
                    }
                    return null;
                }
        );
    }

    private ActivityContext createMockContext(String expectedBeanId, NettyServer nettyServer) {
        BeanRegistry beanRegistry = (BeanRegistry) Proxy.newProxyInstance(
                BeanRegistry.class.getClassLoader(),
                new Class<?>[] { BeanRegistry.class },
                (proxy, method, args) -> {
                    if ("getBean".equals(method.getName())) {
                        if (args.length == 2 && NettyServer.class.equals(args[0])) {
                            if (expectedBeanId != null && expectedBeanId.equals(args[1])) {
                                return nettyServer;
                            }
                        } else if (args.length == 1 && NettyServer.class.equals(args[0])) {
                            if (expectedBeanId == null) {
                                return nettyServer;
                            }
                        }
                    }
                    return null;
                }
        );

        return (ActivityContext) Proxy.newProxyInstance(
                ActivityContext.class.getClassLoader(),
                new Class<?>[] { ActivityContext.class },
                (proxy, method, args) -> {
                    if ("getBeanRegistry".equals(method.getName())) {
                        return beanRegistry;
                    }
                    return null;
                }
        );
    }

}
