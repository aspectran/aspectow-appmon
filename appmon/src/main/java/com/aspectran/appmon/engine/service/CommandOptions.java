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
package com.aspectran.appmon.engine.service;

import com.aspectran.utils.StringUtils;
import com.aspectran.utils.apon.AponFormat;
import com.aspectran.utils.apon.AponParseException;
import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

/**
 * Parses and holds command options sent from clients.
 * This class is based on APON (Aspectran Object Notation) to handle key-value pairs.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
public class CommandOptions extends DefaultParameters {

    public static final String COMMAND_REFRESH = "refresh";
    public static final String COMMAND_LOAD_PREVIOUS = "loadPrevious";

    private static final ParameterKey command;
    private static final ParameterKey instancesToJoin;
    private static final ParameterKey instance;
    private static final ParameterKey logName;
    private static final ParameterKey loadedLines;
    private static final ParameterKey timeZone;
    private static final ParameterKey dateUnit;
    private static final ParameterKey dateOffset;

    private static final ParameterKey[] parameterKeys;

    static {
        command = new ParameterKey("command", ValueType.STRING);
        instancesToJoin = new ParameterKey("instancesToJoin", ValueType.STRING);
        instance = new ParameterKey("instance", ValueType.STRING);
        timeZone = new ParameterKey("timeZone", ValueType.STRING);
        dateUnit = new ParameterKey("dateUnit", ValueType.STRING);
        dateOffset = new ParameterKey("dateOffset", ValueType.STRING);
        logName = new ParameterKey("logName", ValueType.STRING);
        loadedLines = new ParameterKey("loadedLines", ValueType.INT);

        parameterKeys = new ParameterKey[] {
                command,
                instancesToJoin,
                instance,
                timeZone,
                dateUnit,
                dateOffset,
                logName,
                loadedLines
        };
    }

    /**
     * Instantiates a new CommandOptions.
     */
    public CommandOptions() {
        super(parameterKeys);
    }

    /**
     * Instantiates a new CommandOptions from a semicolon-delimited string.
     * @param text the command string
     */
    public CommandOptions(String text) {
        this(StringUtils.split(text, ";"));
    }

    /**
     * Instantiates a new CommandOptions from an array of command lines.
     * @param lines the command lines
     */
    public CommandOptions(String[] lines) {
        super(parameterKeys);
        try {
            readFrom(StringUtils.join(lines, AponFormat.NEW_LINE));
        } catch (AponParseException e) {
            throw new RuntimeException("Failed to parse command: " + StringUtils.join(lines, ";"), e);
        }
    }

    public String getCommand() {
        return getString(command);
    }

    public void setCommand(String command) {
        putValue(CommandOptions.command, command);
    }

    public boolean hasCommand(String command) {
        return (command != null && command.equals(getCommand()));
    }

    public String getInstancesToJoin() {
        return getString(instancesToJoin);
    }

    public void setInstancesToJoin(String instancesToJoin) {
        putValue(CommandOptions.instancesToJoin, instancesToJoin);
    }

    public String getInstance() {
        return getString(instance);
    }

    public void setInstance(String instance) {
        putValue(CommandOptions.instance, instance);
    }

    public boolean hasTimeZone() {
        return hasValue(timeZone);
    }

    public String getTimeZone() {
        return getString(timeZone);
    }

    public void setTimeZone(String timeZone) {
        putValue(CommandOptions.timeZone, timeZone);
    }

    public String getDateUnit() {
        return getString(dateUnit);
    }

    public void setDateUnit(String dateUnit) {
        putValue(CommandOptions.dateUnit, dateUnit);
    }

    public String getDateOffset() {
        return getString(dateOffset);
    }

    public void setDateOffset(String dateOffset) {
        putValue(CommandOptions.dateOffset, dateOffset);
    }

    public String getLogName() {
        return getString(logName);
    }

    public void setLogName(String logName) {
        putValue(CommandOptions.logName, logName);
    }

    public int getLoadedLines() {
        return getInt(loadedLines);
    }

    public void setLoadedLines(int loadedLines) {
        putValue(CommandOptions.loadedLines, loadedLines);
    }

}
