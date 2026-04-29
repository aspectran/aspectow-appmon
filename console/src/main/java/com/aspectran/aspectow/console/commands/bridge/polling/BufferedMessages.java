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
package com.aspectran.aspectow.console.commands.bridge.polling;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A central message buffer for HTTP polling clients.
 * It maintains a thread-safe list of messages and allows clients to pull
 * new messages based on their last retrieved index.
 */
public class BufferedMessages {

    private final List<String> messages;

    private final AtomicInteger offset = new AtomicInteger();

    public BufferedMessages(int initialBufferSize) {
        this.messages = new ArrayList<>(initialBufferSize);
    }

    public int getCurrentLineIndex() {
        return offset.get() + messages.size();
    }

    public void push(String message) {
        synchronized (messages) {
            messages.add(message);
        }
    }

    public String[] pop(@NonNull PollingCommandSession session) {
        synchronized (messages) {
            int lastLineIndex = session.getLastLineIndex();
            int currentLineIndex = getCurrentLineIndex();
            if (lastLineIndex < currentLineIndex) {
                int fromIndex = Math.max(0, lastLineIndex - offset.get());
                int toIndex = messages.size();
                if (fromIndex < toIndex) {
                    List<String> subList = messages.subList(fromIndex, toIndex);
                    String[] result = subList.toArray(new String[0]);
                    session.setLastLineIndex(currentLineIndex);
                    return result;
                }
            }
            return null;
        }
    }

    public void shrink(int minLineIndex) {
        synchronized (messages) {
            int count = minLineIndex - offset.get();
            if (count > 0) {
                for (int i = 0; i < count; i++) {
                    messages.remove(0);
                }
                offset.addAndGet(count);
            }
        }
    }

    public void clear() {
        synchronized (messages) {
            messages.clear();
            offset.set(0);
        }
    }

}
