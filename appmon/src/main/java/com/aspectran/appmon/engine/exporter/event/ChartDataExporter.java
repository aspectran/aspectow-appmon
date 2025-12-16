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
package com.aspectran.appmon.engine.exporter.event;

import com.aspectran.appmon.engine.config.EventInfo;
import com.aspectran.appmon.engine.exporter.AbstractExporter;
import com.aspectran.appmon.engine.exporter.ExporterManager;
import com.aspectran.appmon.engine.exporter.ExporterType;
import com.aspectran.appmon.engine.persist.counter.EventCount;
import com.aspectran.appmon.engine.persist.counter.EventCountRollupListener;
import com.aspectran.appmon.engine.persist.counter.EventCountVO;
import com.aspectran.appmon.engine.persist.db.mapper.EventCountMapper;
import com.aspectran.appmon.engine.service.CommandOptions;
import com.aspectran.utils.ToStringBuilder;
import com.aspectran.utils.json.JsonBuilder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * An exporter that generates chart data from event counts.
 * It can query historical data from the database and also listen for real-time
 * rollup events from an {@link EventCount} instance.
 *
 * <p>Created: 2024-12-18</p>
 */
public class ChartDataExporter extends AbstractExporter implements EventCountRollupListener {

    private static final ExporterType TYPE = ExporterType.DATA;

    private final ExporterManager exporterManager;

    private final EventInfo eventInfo;

    private final String prefix;

    /**
     * Instantiates a new ChartDataExporter.
     * @param exporterManager the exporter manager
     * @param eventInfo the event configuration
     */
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
    public void read(@NonNull List<String> messages, CommandOptions commandOptions) {
        messages.add(prefix + readChartData(commandOptions));
    }

    @Override
    public void readIfChanged(@NonNull List<String> messages, CommandOptions commandOptions) {
        messages.add(prefix + readChartData(commandOptions));
    }

    @Override
    public void broadcast(String message) {
        exporterManager.broadcast(prefix + message);
    }

    /**
     * Called when an event count is rolled up. Broadcasts the new data point.
     * @param eventCount the event count that was rolled up
     */
    @Override
    public void onRolledUp(@NonNull EventCount eventCount) {
        String[] labels = new String[] { eventCount.getTallied().getDatetime() };
        long[] data1 = new long[] { eventCount.getTallied().getDelta() };
        long[] data2 = new long[] { eventCount.getTallied().getError() };
        String message = toJson(null, null, labels, data1, data2, true);
        broadcast(message);
    }

    private String readChartData(@Nullable CommandOptions commandOptions) {
        String timeZone = (commandOptions != null ? commandOptions.getTimeZone() : null);
        final int zoneOffsetInSeconds;
        if (timeZone != null) {
            ZoneOffset zoneOffset = ZonedDateTime.now(ZoneId.of(timeZone)).getOffset();
            zoneOffsetInSeconds = zoneOffset.getTotalSeconds();
        } else {
            zoneOffsetInSeconds = 0;
        }
        final String dateUnit = (commandOptions != null ? commandOptions.getDateUnit() : null);
        final String dateOffset = (commandOptions != null ? commandOptions.getDateOffset() : null);
        EventCountMapper.Dao dao = exporterManager.getBean(EventCountMapper.Dao.class);
        List<EventCountVO> list = exporterManager.instantActivity(() -> {
            if ("hour".equals(dateUnit)) {
                return dao.getChartDataByHour(eventInfo.getDomainName(), eventInfo.getInstanceName(), eventInfo.getName(), zoneOffsetInSeconds, dateOffset);
            } else if ("day".equals(dateUnit)) {
                return dao.getChartDataByDay(eventInfo.getDomainName(), eventInfo.getInstanceName(), eventInfo.getName(), zoneOffsetInSeconds, dateOffset);
            } else if ("month".equals(dateUnit)) {
                return dao.getChartDataByMonth(eventInfo.getDomainName(), eventInfo.getInstanceName(), eventInfo.getName(), zoneOffsetInSeconds, dateOffset);
            } else if ("year".equals(dateUnit)) {
                return dao.getChartDataByYear(eventInfo.getDomainName(), eventInfo.getInstanceName(), eventInfo.getName(), zoneOffsetInSeconds, dateOffset);
            } else {
                return dao.getChartData(eventInfo.getDomainName(), eventInfo.getInstanceName(), eventInfo.getName(), dateOffset);
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
    protected void doStart() throws Exception {
        // Not used
    }

    @Override
    protected void doStop() throws Exception {
        // Not used
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
