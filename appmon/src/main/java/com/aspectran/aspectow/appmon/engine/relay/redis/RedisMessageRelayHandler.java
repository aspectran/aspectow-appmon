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
package com.aspectran.aspectow.appmon.engine.relay.redis;

import com.aspectran.aspectow.appmon.engine.manager.AppMonManager;
import com.aspectran.aspectow.node.redis.RedisMessageListener;
import com.aspectran.aspectow.node.redis.RedisMessageSubscriber;
import com.aspectran.core.component.bean.ablility.DisposableBean;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RedisMessageRelayHandler listens to messages from other nodes via Redis
 * and relays them to the local MessageRelayManager.
 */
@Component
public class RedisMessageRelayHandler implements RedisMessageListener, InitializableBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(RedisMessageRelayHandler.class);

    private final AppMonManager appMonManager;

    private final RedisMessageSubscriber messageSubscriber;

    @Autowired
    public RedisMessageRelayHandler(AppMonManager appMonManager, RedisMessageSubscriber messageSubscriber) {
        this.appMonManager = appMonManager;
        this.messageSubscriber = messageSubscriber;
    }

    @Override
    public void initialize() {
        messageSubscriber.addListener(this);
        logger.info("RedisMessageRelayHandler initialized and registered as a listener");
    }

    @Override
    public void destroy() {
        messageSubscriber.removeListener(this);
    }

    @Override
    public void onMessage(String nodeId, String message) {
        if (nodeId.equals(messageSubscriber.getNodeId())) {
            appMonManager.getMessageRelayManager().relay(message);
        }
    }

}
