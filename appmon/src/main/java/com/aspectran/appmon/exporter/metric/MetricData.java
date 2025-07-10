package com.aspectran.appmon.exporter.metric;

import com.aspectran.appmon.config.MetricInfo;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.json.JsonBuilder;

import java.util.LinkedHashMap;

/**
 * <p>Created: 2025-07-01</p>
 */
public class MetricData {

    private final String name;

    private final String title;

    private final boolean heading;

    private String format;

    private final LinkedHashMap<String, Object> data = new LinkedHashMap<>();

    public MetricData(@NonNull MetricInfo metricInfo) {
        this.name = metricInfo.getName();
        this.title = metricInfo.getTitle();
        this.heading = metricInfo.isHeading();
        this.format = metricInfo.getFormat();
    }

    public MetricData setFormat(String format) {
        if (this.format == null) {
            this.format = format;
        }
        return this;
    }

    public Object getData(String name) {
        return data.get(name);
    }

    public MetricData putData(String name, Object value) {
        data.put(name, value);
        return this;
    }

    public String toJson() {
        return new JsonBuilder()
                .prettyPrint(false)
                .nullWritable(false)
                .object()
                    .put("name", name)
                    .put("title", title)
                    .put("heading", heading)
                    .put("format", format)
                    .put("data", data)
                .endObject()
                .toString();
    }

}
