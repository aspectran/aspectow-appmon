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
import com.aspectran.core.activity.InstantAction;
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
        messages.add(prefix + readChartData(null));
    }

    @Override
    public void readIfChanged(@NonNull List<String> messages, String[] options) {
        messages.add(prefix + readChartData(options));
    }

    @Override
    public void broadcast(String message) {
        exporterManager.broadcast(prefix + message);
    }

    @Override
    public void onRolledUp(@NonNull EventCount eventCount) {
        String[] labels = new String[] { eventCount.getTallied().getDatetime() };
        long[] data1 = new long[] { eventCount.getTallied().getDelta() };
        long[] data2 = new long[] { eventCount.getTallied().getError() };
        String message = toJson(null, null, labels, data1, data2, true);
        broadcast(message);
    }

    private String readChartData(String[] options) {
        String dateUnit = null;
        String dateOffset = null;
        if (options != null) {
            for (String option : options) {
                if (dateUnit == null && option.startsWith("dateUnit:")) {
                    dateUnit = option.substring("dateUnit:".length());
                }
                if (dateOffset == null && option.startsWith("dateOffset:")) {
                    dateOffset = option.substring("dateOffset:".length());
                }
            }
        }
        final String finalDateUnit = dateUnit;
        EventCountMapper.Dao dao = exporterManager.getBean(EventCountMapper.Dao.class);
        List<EventCountVO> list = exporterManager.instantActivity(() -> {
            if ("hour".equals(finalDateUnit)) {
                return dao.getChartDataByHour(eventInfo.getDomainName(), eventInfo.getInstanceName(), eventInfo.getName());
            } else if ("day".equals(finalDateUnit)) {
                return dao.getChartDataByDay(eventInfo.getDomainName(), eventInfo.getInstanceName(), eventInfo.getName());
            } else if ("month".equals(finalDateUnit)) {
                return dao.getChartDataByMonth(eventInfo.getDomainName(), eventInfo.getInstanceName(), eventInfo.getName());
            } else if ("year".equals(finalDateUnit)) {
                return dao.getChartDataByYear(eventInfo.getDomainName(), eventInfo.getInstanceName(), eventInfo.getName());
            } else {
                return dao.getChartData(eventInfo.getDomainName(), eventInfo.getInstanceName(), eventInfo.getName());
            }
        });

        String[] labels = new String[list.size()];
        long[] data1 = new long[list.size()];
        long[] data2 = new long[list.size()];
        for (int i = 0; i < list.size(); i++) {
            EventCountVO vo = list.get(i);
            labels[i] = vo.getDatetime();
            data1[i] = vo.getDelta();
            data2[i] = vo.getError();
        }

        return toJson(dateUnit, dateOffset, labels, data1, data2, false);
    }

    private String toJson(String dateUnit, String dateOffset, String[] labels,
                          long[] data1, long[] data2, boolean rolledUp) {
        return new JsonBuilder()
                .prettyPrint(false)
                .nullWritable(false)
                .object()
                    .object("chartData")
                        .put("dateUnit", dateUnit)
                        .put("dateOffset", dateOffset)
                        .put("labels", labels)
                        .put("data1", data1)
                        .put("data2", data2)
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
