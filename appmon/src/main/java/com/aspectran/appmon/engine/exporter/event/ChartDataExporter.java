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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * An exporter that generates chart data from event counts.
 * It can query historical data from the database and also listen for real-time
 * rollup events from an {@link EventCount} instance.
 *
 * <p>Created: 2024-12-18</p>
 */
public class ChartDataExporter extends AbstractExporter implements EventCountRollupListener {

    private static final Logger logger = LoggerFactory.getLogger(ChartDataExporter.class);

    private static final ExporterType TYPE = ExporterType.DATA;

    private final ExporterManager exporterManager;

    private final EventInfo eventInfo;

    private final String prefix;

    /**
     * Instantiates a new ChartDataExporter.
     * @param exporterManager the exporter manager
     * @param eventInfo the event configuration
     */
    public ChartDataExporter(
            @NonNull ExporterManager exporterManager,
            @NonNull EventInfo eventInfo) {
        super(TYPE);
        this.exporterManager = exporterManager;
        this.eventInfo = eventInfo;
        this.prefix = eventInfo.getInstanceName() + ":" + TYPE + "/chart:" + eventInfo.getName() + ":";
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
        LocalDateTime dt = eventCount.getTallied().getDatetime();
        if (dt == null) {
            return;
        }
        String datetime = dt.truncatedTo(ChronoUnit.MINUTES)
                .atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String[] labels = new String[] { datetime };
        long[] data1 = new long[] { eventCount.getTallied().getDelta() };
        long[] data2 = new long[] { eventCount.getTallied().getError() };
        String message = toJson(null, null, labels, data1, data2, true);
        broadcast(message);
    }

    private String readChartData(@Nullable CommandOptions commandOptions) {
        String timeZone = (commandOptions != null ? commandOptions.getTimeZone() : null);
        String dateUnit = (commandOptions != null ? commandOptions.getDateUnit() : null);
        String dateOffsetStr = (commandOptions != null ? commandOptions.getDateOffset() : null);

        int zoneOffsetInSeconds = 0;
        if (timeZone != null) {
            try {
                zoneOffsetInSeconds = ZonedDateTime.now(ZoneId.of(timeZone)).getOffset().getTotalSeconds();
            } catch (Exception e) {
                // Ignore invalid timeZone
            }
        }

        LocalDateTime dateOffset = null;
        if (dateOffsetStr != null) {
            try {
                ZonedDateTime utcOffset = ZonedDateTime.parse(dateOffsetStr);
                ZonedDateTime truncated = truncateToUnit(utcOffset.toLocalDateTime(), dateUnit, zoneOffsetInSeconds);
                dateOffset = truncated.toLocalDateTime();
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to parse dateOffset: {}", dateOffsetStr);
                }
            }
        }

        final int finalZoneOffsetInSeconds = zoneOffsetInSeconds;
        final LocalDateTime finalDateOffset = dateOffset;
        EventCountMapper.Dao dao = exporterManager.getBean(EventCountMapper.Dao.class);
        List<EventCountVO> list = exporterManager.instantActivity(() -> {
            String domain = eventInfo.getDomainName();
            String instance = eventInfo.getInstanceName();
            String name = eventInfo.getName();
            return switch (dateUnit) {
                case "hour" -> dao.getChartDataByHour(domain, instance, name, finalZoneOffsetInSeconds, finalDateOffset);
                case "day" -> dao.getChartDataByDay(domain, instance, name, finalZoneOffsetInSeconds, finalDateOffset);
                case "month" -> dao.getChartDataByMonth(domain, instance, name, finalZoneOffsetInSeconds, finalDateOffset);
                case "year" -> dao.getChartDataByYear(domain, instance, name, finalZoneOffsetInSeconds, finalDateOffset);
                case null, default -> dao.getChartData(domain, instance, name, finalDateOffset);
            };
        });

        int size = list.size();
        String[] labels = new String[size];
        long[] data1 = new long[size];
        long[] data2 = new long[size];
        for (int i = 0; i < size; i++) {
            EventCountVO vo = list.get(i);
            labels[i] = normalizeDatetime(vo.getDatetime(), dateUnit, zoneOffsetInSeconds);
            data1[i] = vo.getDelta();
            data2[i] = vo.getError();
        }

        String effectiveDateOffset = (dateOffset != null ?
                dateOffset.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null);
        return toJson(dateUnit, effectiveDateOffset, labels, data1, data2, false);
    }

    @Nullable
    private String normalizeDatetime(LocalDateTime datetime, String dateUnit, int zoneOffsetInSeconds) {
        ZonedDateTime truncated = truncateToUnit(datetime, dateUnit, zoneOffsetInSeconds);
        return (truncated != null ? truncated.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null);
    }

    private ZonedDateTime truncateToUnit(LocalDateTime datetime, String dateUnit, int zoneOffsetInSeconds) {
        if (datetime == null) {
            return null;
        }
        ZonedDateTime utcTime = datetime.atZone(ZoneOffset.UTC);
        if (dateUnit == null) {
            return utcTime.truncatedTo(ChronoUnit.MINUTES);
        }
        try {
            ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(zoneOffsetInSeconds);
            ZonedDateTime localTime = utcTime.withZoneSameInstant(zoneOffset);

            ZonedDateTime normalizedLocal = switch (dateUnit) {
                case "hour" -> localTime.truncatedTo(ChronoUnit.HOURS);
                case "day" -> localTime.truncatedTo(ChronoUnit.DAYS);
                case "month" -> localTime.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
                case "year" -> localTime.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
                default -> localTime.truncatedTo(ChronoUnit.MINUTES);
            };

            return normalizedLocal.withZoneSameInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            return utcTime.truncatedTo(ChronoUnit.MINUTES);
        }
    }

    private String toJson(
            String dateUnit, String dateOffset, String[] labels,
            long[] data1, long[] data2, boolean rolledUp) {
        return new JsonBuilder()
                .prettyPrint(false)
                .nullWritable(false)
                .object()
                    .put("dateUnit", dateUnit)
                    .put("dateOffset", dateOffset)
                    .put("labels", labels)
                    .put("data1", data1)
                    .put("data2", data2)
                    .put("rolledUp", rolledUp)
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
