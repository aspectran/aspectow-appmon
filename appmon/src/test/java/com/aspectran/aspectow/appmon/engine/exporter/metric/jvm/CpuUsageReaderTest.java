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
package com.aspectran.aspectow.appmon.engine.exporter.metric.jvm;

import com.aspectran.aspectow.appmon.engine.config.MetricInfo;
import com.aspectran.aspectow.appmon.engine.exporter.ExporterManager;
import com.aspectran.aspectow.appmon.engine.exporter.ExporterType;
import com.aspectran.aspectow.appmon.engine.exporter.metric.MetricData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpuUsageReaderTest {

    @Test
    void testCpuUsageReader() throws Exception {
        ExporterManager exporterManager = new ExporterManager(ExporterType.METRIC, null, "testApp") {};

        MetricInfo metricInfo = new MetricInfo();
        metricInfo.putValue("id", "cpu");
        metricInfo.putValue("title", "CPU");

        CpuUsageReader reader = new CpuUsageReader(exporterManager, metricInfo);
        reader.start();

        MetricData data = null;
        for (int i = 0; i < 5; i++) {
            data = reader.getMetricData(false);
            if (data != null) {
                break;
            }
            Thread.sleep(50);
        }

        if (data != null) {
            assertNotNull(data.getData("processCpu"));
            assertNotNull(data.getData("systemCpu"));
            assertNotNull(data.getData("processors"));
            assertTrue(data.toJson().contains("\"format\":\"{processCpu}\""));
            assertTrue(data.toJson().contains("\"unit\":\"%\""));

            // Immediately calling getMetricDataIfChanged when not changed should return null
            assertNull(reader.getMetricDataIfChanged());
        }

        reader.stop();
        assertNull(reader.getMetricData(false));
        assertNull(reader.getMetricDataIfChanged());
    }

}
