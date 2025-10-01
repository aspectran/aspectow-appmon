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
package com.aspectran.appmon.persist.counter;

/**
 * A listener for receiving notifications when an {@link EventCount} is rolled up.
 *
 * <p>Created: 2025. 2. 12.</p>
 */
public interface EventCountRollupListener {

    /**
     * Called when an event count is rolled up.
     * @param eventCount the event count that was rolled up
     */
    void onRolledUp(EventCount eventCount);

}
