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
package com.aspectran.appmon.exporter.event;

import com.aspectran.appmon.config.EventInfo;
import com.aspectran.appmon.exporter.AbstractExporter;
import com.aspectran.appmon.exporter.ExporterManager;
import com.aspectran.appmon.exporter.ExporterType;
import com.aspectran.appmon.persist.counter.EventCount;
import com.aspectran.appmon.persist.counter.EventCountRollupListener;
import com.aspectran.appmon.persist.counter.EventCountVO;
import com.aspectran.appmon.persist.db.mapper.EventCountMapper;
import com.aspectran.utils.ToStringBuilder;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.json.JsonBuilder;

import java.util.List;

/**
 * <p>Created: 2024-12-18</p>
 */
public class ChartDataExporter extends AbstractExporter implements EventCountRollupListener {

    private static final ExporterType TYPE = ExporterType.DATA;

    private final ExporterManager exporterManager;

    private final EventInfo eventInfo;

    private final String prefix;

    public ChartDataExporter(@NonNull ExporterManager exporterManager,
                             @NonNull EventInfo eventInfo) {
        super(TYPE);
        this.exporterManager = exporterManager;
        this.eventInfo = eventInfo;
        this.prefix = eventInfo.getInstanceName() + ":" + TYPE + ":" + eventInfo.getName() + ":";
    }

    @Override
    public String getName() {
        return eventInfo.getName();
    }

    @Override
    public void read(@NonNull List<String> messages) {
        messages.add(prefix + readChartData());
    }

    @Override
    public void readIfChanged(@NonNull List<String> messages) {
        messages.add(prefix + readChartData());
    }

    @Override
    public void broadcast(String message) {
        exporterManager.broadcast(prefix + message);
    }

    @Override
    public void onRolledUp(@NonNull EventCount eventCount) {
        String[] labels = new String[] { eventCount.getDatetime() };
        long[] data = new long[] { eventCount.getDelta() };
        String message = toJson(labels, data, true);
        broadcast(message);
    }

    private String readChartData() {
        EventCountMapper.Dao dao = exporterManager.getBean(EventCountMapper.Dao.class);
        List<EventCountVO> list = exporterManager.instantActivity(() -> dao.getChartData(
                eventInfo.getDomainName(),
                eventInfo.getInstanceName(),
                eventInfo.getName()));

        String[] labels = new String[list.size()];
        long[] data = new long[list.size()];
        for (int i = 0; i < list.size(); i++) {
            EventCountVO vo = list.get(i);
            labels[i] = vo.getDatetime();
            data[i] = vo.getDelta();
        }

        return toJson(labels, data, false);
    }

    private String toJson(String[] labels, long[] data, boolean rolledUp) {
        return new JsonBuilder()
                .prettyPrint(false)
                .nullWritable(false)
                .object()
                    .object("chartData")
                        .put("labels", labels)
                        .put("data", data)
                        .put("rolledUp", rolledUp)
                    .endObject()
                .endObject()
                .toString();
    }

    @Override
    public String toString() {
        if (isStopped()) {
            return ToStringBuilder.toString(super.toString(), eventInfo);
        } else {
            return super.toString();
        }
    }

}
