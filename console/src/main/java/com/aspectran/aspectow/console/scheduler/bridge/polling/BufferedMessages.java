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
package com.aspectran.aspectow.console.scheduler.bridge.polling;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A central buffer to store and distribute scheduler management results to polling clients.
 */
public class BufferedMessages {

    private final SortedMap<Integer, String> messageMap = Collections.synchronizedSortedMap(new TreeMap<>());

    private final AtomicInteger lastLineIndex = new AtomicInteger();

    private final int maxBufferSize;

    public BufferedMessages(int maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
    }

    public void push(String message) {
        int index = lastLineIndex.incrementAndGet();
        messageMap.put(index, message);
        if (messageMap.size() > maxBufferSize) {
            messageMap.remove(messageMap.firstKey());
        }
    }

    public String[] pop(PollingSchedulerSession session) {
        int lastLineIndex = session.getLastLineIndex();
        SortedMap<Integer, String> tailMap = messageMap.tailMap(lastLineIndex + 1);
        if (tailMap.isEmpty()) {
            return null;
        }

        String[] messages;
        synchronized (messageMap) {
            messages = tailMap.values().toArray(new String[0]);
            session.setLastLineIndex(tailMap.lastKey());
        }
        return messages;
    }

    public void shrink(int minLineIndex) {
        synchronized (messageMap) {
            messageMap.headMap(minLineIndex).clear();
        }
    }

    public int getCurrentLineIndex() {
        return lastLineIndex.get();
    }

    public void clear() {
        messageMap.clear();
        lastLineIndex.set(0);
    }

    public int size() {
        return messageMap.size();
    }

}
