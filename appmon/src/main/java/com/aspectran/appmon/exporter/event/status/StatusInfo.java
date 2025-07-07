package com.aspectran.appmon.exporter.event.status;

import com.aspectran.appmon.config.EventInfo;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.apon.AbstractParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

/**
 * <p>Created: 2025-07-01</p>
 */
public class StatusInfo  extends AbstractParameters {

    private static final ParameterKey name;
    private static final ParameterKey title;
    private static final ParameterKey heading;
    private static final ParameterKey text;
    private static final ParameterKey data;

    private static final ParameterKey[] parameterKeys;

    static {
        name = new ParameterKey("name", ValueType.STRING);
        title = new ParameterKey("title", ValueType.STRING);
        heading = new ParameterKey("heading", ValueType.BOOLEAN);
        text = new ParameterKey("text", ValueType.STRING);
        data = new ParameterKey("data", ValueType.PARAMETERS);

        parameterKeys = new ParameterKey[] {
                name,
                title,
                heading,
                text,
                data
        };
    }

    public StatusInfo(@NonNull EventInfo eventInfo) {
        super(parameterKeys);
        putValue(name, eventInfo.getName());
        putValue(title, eventInfo.getTitle());
        putValue(heading, eventInfo.getHeading());
    }

    public StatusInfo setText(String text) {
        putValue(StatusInfo.text, text);
        return this;
    }

    public StatusInfo putData(String name, Object value) {
        touchParameters(StatusInfo.data).putValue(name, value);
        return this;
    }

}
