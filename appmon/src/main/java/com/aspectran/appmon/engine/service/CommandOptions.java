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

    /** Command to refresh the current view or data */
    public static final String COMMAND_REFRESH = "refresh";

    /** Command to load previous data records */
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
     * Constructs a new empty CommandOptions.
     */
    public CommandOptions() {
        super(parameterKeys);
    }

    /**
     * Constructs a new CommandOptions from a semicolon-delimited string.
     * @param text the semicolon-delimited command string
     */
    public CommandOptions(String text) {
        this(StringUtils.split(text, ";"));
    }

    /**
     * Constructs a new CommandOptions from an array of command lines.
     * @param lines the array of command lines
     * @throws RuntimeException if the command lines cannot be parsed
     */
    public CommandOptions(String[] lines) {
        super(parameterKeys);
        try {
            readFrom(StringUtils.join(lines, AponFormat.NEW_LINE));
        } catch (AponParseException e) {
            throw new RuntimeException("Failed to parse command: " + StringUtils.join(lines, ";"), e);
        }
    }

    /**
     * Returns the name of the command.
     * @return the command name
     */
    public String getCommand() {
        return getString(command);
    }

    /**
     * Sets the name of the command.
     * @param command the command name
     */
    public void setCommand(String command) {
        putValue(CommandOptions.command, command);
    }

    /**
     * Checks if the specified command is equal to the current command.
     * @param command the command name to compare
     * @return true if the commands match, false otherwise
     */
    public boolean hasCommand(String command) {
        return (command != null && command.equals(getCommand()));
    }

    /**
     * Returns the list of instances to join, usually as a comma-separated string.
     * @return the instances to join
     */
    public String getInstancesToJoin() {
        return getString(instancesToJoin);
    }

    /**
     * Sets the list of instances to join.
     * @param instancesToJoin the instances to join
     */
    public void setInstancesToJoin(String instancesToJoin) {
        putValue(CommandOptions.instancesToJoin, instancesToJoin);
    }

    /**
     * Returns the specific instance name.
     * @return the instance name
     */
    public String getInstance() {
        return getString(instance);
    }

    /**
     * Sets the specific instance name.
     * @param instance the instance name
     */
    public void setInstance(String instance) {
        putValue(CommandOptions.instance, instance);
    }

    /**
     * Returns whether a time zone has been specified.
     * @return true if a time zone exists, false otherwise
     */
    public boolean hasTimeZone() {
        return hasValue(timeZone);
    }

    /**
     * Returns the time zone ID.
     * @return the time zone ID
     */
    public String getTimeZone() {
        return getString(timeZone);
    }

    /**
     * Sets the time zone ID.
     * @param timeZone the time zone ID
     */
    public void setTimeZone(String timeZone) {
        putValue(CommandOptions.timeZone, timeZone);
    }

    /**
     * Returns the date unit (e.g., "hour", "day") for time-series data.
     * @return the date unit
     */
    public String getDateUnit() {
        return getString(dateUnit);
    }

    /**
     * Sets the date unit for time-series data.
     * @param dateUnit the date unit
     */
    public void setDateUnit(String dateUnit) {
        putValue(CommandOptions.dateUnit, dateUnit);
    }

    /**
     * Returns the date offset for filtering historical data.
     * @return the date offset string
     */
    public String getDateOffset() {
        return getString(dateOffset);
    }

    /**
     * Sets the date offset for filtering historical data.
     * @param dateOffset the date offset string
     */
    public void setDateOffset(String dateOffset) {
        putValue(CommandOptions.dateOffset, dateOffset);
    }

    /**
     * Returns the log file name.
     * @return the log name
     */
    public String getLogName() {
        return getString(logName);
    }

    /**
     * Sets the log file name.
     * @param logName the log name
     */
    public void setLogName(String logName) {
        putValue(CommandOptions.logName, logName);
    }

    /**
     * Returns the number of lines already loaded from the log.
     * @return the number of loaded lines
     */
    public int getLoadedLines() {
        return getInt(loadedLines);
    }

    /**
     * Sets the number of lines already loaded from the log.
     * @param loadedLines the number of loaded lines
     */
    public void setLoadedLines(int loadedLines) {
        putValue(CommandOptions.loadedLines, loadedLines);
    }

}
