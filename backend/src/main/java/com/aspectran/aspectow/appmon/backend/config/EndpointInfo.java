package com.aspectran.aspectow.appmon.backend.config;

import com.aspectran.utils.apon.AbstractParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

/**
 * <p>Created: 2025-02-13</p>
 */
public class EndpointInfo extends AbstractParameters {

    private static final ParameterKey mode;
    private static final ParameterKey url;

    private static final ParameterKey[] parameterKeys;

    static {
        mode = new ParameterKey("mode", ValueType.STRING);
        url = new ParameterKey("url", ValueType.STRING);

        parameterKeys = new ParameterKey[] {
            mode,
            url
        };
    }

    public EndpointInfo() {
        super(parameterKeys);
    }

    public String getMode() {
        return getString(EndpointInfo.mode);
    }

    public void setMode(String mode) {
        putValue(EndpointInfo.mode, mode);
    }

    public String getUrl() {
        return getString(url);
    }

    public void setUrl(String url) {
        putValue(EndpointInfo.url, url);
    }

}
