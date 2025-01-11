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

import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.annotation.jsr305.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PollingAppMonBuffer {

    private final AtomicInteger lineCounter = new AtomicInteger(0);

    private final List<String> buffer;

    public PollingAppMonBuffer(int initialBufferSize) {
        if (initialBufferSize > 0) {
            this.buffer = new ArrayList<>(initialBufferSize);
        } else {
            this.buffer = new ArrayList<>();
        }
    }

    public int push(String line) {
        synchronized (buffer) {
            buffer.add(line);
            return lineCounter.incrementAndGet();
        }
    }

    @Nullable
    public String[] pop(@NonNull PollingAppMonSession session) {
        synchronized (buffer) {
            int maxLineIndex = lineCounter.get() - 1;
            int lineIndex = session.getLastLineIndex();
            if (lineIndex < 0) {
                session.setLastLineIndex(maxLineIndex);
                return (!buffer.isEmpty() ? buffer.toArray(new String[0]) : null);
            } else {
                if (lineIndex < maxLineIndex) {
                    session.setLastLineIndex(maxLineIndex);
                    int offset = maxLineIndex - lineIndex;
                    if (offset < buffer.size()) {
                        int start = buffer.size() - offset;
                        return buffer.subList(start, buffer.size()).toArray(new String[0]);
                    } else {
                        return buffer.toArray(new String[0]);
                    }
                } else if (lineIndex > maxLineIndex) {
                    session.setLastLineIndex(maxLineIndex);
                    return null;
                } else {
                    return null;
                }
            }
        }
    }

    public void shrink(int minLineIndex) {
        synchronized (buffer) {
            int lines = lineCounter.get() - minLineIndex + 1;
            if (lines < buffer.size()) {
                buffer.subList(0, buffer.size() - lines).clear();
            } else if (lines == buffer.size()) {
                buffer.clear();
            }
        }
    }

    public void clear() {
        synchronized (buffer) {
            buffer.clear();
        }
    }

}
