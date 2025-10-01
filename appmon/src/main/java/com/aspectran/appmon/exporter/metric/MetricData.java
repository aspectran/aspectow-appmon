package com.aspectran.appmon.exporter.metric;

import com.aspectran.appmon.config.MetricInfo;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.json.JsonBuilder;

import java.util.LinkedHashMap;

/**
 * A data transfer object (DTO) for collected metric data.
 * It holds the metric's identity, formatting options, and the actual data points.
 *
 * <p>Created: 2025-07-01</p>
 */
public class MetricData {

    private final String name;

    private final String title;

    private final boolean heading;

    private String format;

    private final LinkedHashMap<String, Object> data = new LinkedHashMap<>();

    /**
     * Instantiates a new MetricData.
     * @param metricInfo the metric configuration
     */
    public MetricData(@NonNull MetricInfo metricInfo) {
        this.name = metricInfo.getName();
        this.title = metricInfo.getTitle();
        this.heading = metricInfo.isHeading();
        this.format = metricInfo.getFormat();
    }

    /**
     * Sets the format string for the metric data, if not already set.
     * @param format the format string
     * @return this MetricData instance
     */
    public MetricData setFormat(String format) {
        if (this.format == null) {
            this.format = format;
        }
        return this;
    }

    /**
     * Gets a data point by its name.
     * @param name the name of the data point
     * @return the value of the data point
     */
    public Object getData(String name) {
        return data.get(name);
    }

    /**
     * Adds a data point to this metric data.
     * @param name the name of the data point
     * @param value the value of the data point
     * @return this MetricData instance
     */
    public MetricData putData(String name, Object value) {
        data.put(name, value);
        return this;
    }

    /**
     * Converts this object to its JSON representation.
     * @return a JSON string
     */
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
