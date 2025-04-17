package com.aspectran.appmon.exporter;

import com.aspectran.appmon.config.CommandOptions;
import com.aspectran.utils.lifecycle.AbstractLifeCycle;

import java.util.List;

/**
 * <p>Created: 2025-04-07</p>
 */
public abstract class AbstractExporter extends AbstractLifeCycle implements Exporter {

    private final ExporterType type;

    public AbstractExporter(ExporterType type) {
        this.type = type;
    }

    @Override
    public ExporterType getType() {
        return type;
    }

    @Override
    public void readIfChanged(List<String> messages, CommandOptions commandOptions) {
    }

}
