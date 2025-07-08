package com.aspectran.appmon.exporter.metric;

import com.aspectran.appmon.config.MetricInfo;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.apon.AbstractParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

/**
 * <p>Created: 2025-07-01</p>
 */
public class MetricData extends AbstractParameters {

    private static final ParameterKey name;
    private static final ParameterKey title;
    private static final ParameterKey heading;
    private static final ParameterKey format;
    private static final ParameterKey data;

    private static final ParameterKey[] parameterKeys;

    static {
        name = new ParameterKey("name", ValueType.STRING);
        title = new ParameterKey("title", ValueType.STRING);
        heading = new ParameterKey("heading", ValueType.BOOLEAN);
        format = new ParameterKey("format", ValueType.STRING);
        data = new ParameterKey("data", ValueType.PARAMETERS);

        parameterKeys = new ParameterKey[] {
                name,
                title,
                heading,
                format,
                data
        };
    }

    public MetricData(@NonNull MetricInfo metricInfo) {
        super(parameterKeys);
        putValue(name, metricInfo.getName());
        putValue(title, metricInfo.getTitle());
        putValue(heading, metricInfo.getHeading());
        if (metricInfo.hasFormat()) {
            putValue(format, metricInfo.getFormat());
        }
    }

    public MetricData setFormat(String format) {
        if (!hasValue(MetricData.format)) {
            putValue(MetricData.format, format);
        }
        return this;
    }

    public MetricData putData(String name, Object value) {
        touchParameters(MetricData.data).putValue(name, value);
        return this;
    }

}
