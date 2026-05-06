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
package com.aspectran.aspectow.appmon.engine.relay;

import com.aspectran.utils.Assert;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.apon.AponFormat;
import com.aspectran.utils.apon.AponParseException;
import com.aspectran.utils.apon.AponRenderStyle;
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

    public static final String COMMAND_JOIN = "join";

    public static final String COMMAND_RELEASE = "release";

    /** Command to refresh the current view or data */
    public static final String COMMAND_REFRESH = "refresh";

    /** Command to load previous data records */
    public static final String COMMAND_LOAD_PREVIOUS = "loadPrevious";

    /** Command to focus on a specific app */
    public static final String COMMAND_FOCUS = "focus";

    private static final ParameterKey command;
    private static final ParameterKey nodeId;
    private static final ParameterKey appId;
    private static final ParameterKey appsToJoin;
    private static final ParameterKey timeZone;
    private static final ParameterKey dateUnit;
    private static final ParameterKey dateOffset;
    private static final ParameterKey logId;
    private static final ParameterKey loadedLines;
    private static final ParameterKey sessionId;

    private static final ParameterKey[] parameterKeys;

    static {
        command = new ParameterKey("command", ValueType.STRING);
        nodeId = new ParameterKey("nodeId", ValueType.STRING);
        appId = new ParameterKey("appId", ValueType.STRING);
        appsToJoin = new ParameterKey("appsToJoin", ValueType.STRING);
        timeZone = new ParameterKey("timeZone", ValueType.STRING);
        dateUnit = new ParameterKey("dateUnit", ValueType.STRING);
        dateOffset = new ParameterKey("dateOffset", ValueType.STRING);
        logId = new ParameterKey("logId", ValueType.STRING);
        loadedLines = new ParameterKey("loadedLines", ValueType.INT);
        sessionId = new ParameterKey("sessionId", ValueType.STRING);

        parameterKeys = new ParameterKey[] {
                command,
                nodeId,
                appId,
                appsToJoin,
                timeZone,
                dateUnit,
                dateOffset,
                logId,
                loadedLines,
                sessionId
        };
    }

    /**
     * Constructs a new empty CommandOptions.
     */
    public CommandOptions() {
        super(parameterKeys);
        setRenderStyle(AponRenderStyle.COMPACT);
    }

    /**
     * Constructs a new CommandOptions from a semicolon-delimited string.
     * @param text the semicolon-delimited command string
     */
    public CommandOptions(String text) {
        super(parameterKeys);
        setRenderStyle(AponRenderStyle.COMPACT);
        try {
            readFrom(parseAsApon(text));
        } catch (AponParseException e) {
            throw new RuntimeException("Failed to parse command: " + text, e);
        }
    }

    /**
     * Constructs a new CommandOptions from an array of command lines.
     * @param lines the array of command lines
     * @throws RuntimeException if the command lines cannot be parsed
     */
    public CommandOptions(String[] lines) {
        super(parameterKeys);
        setRenderStyle(AponRenderStyle.COMPACT);
        try {
            readFrom(StringUtils.join(lines, AponFormat.NEW_LINE));
        } catch (AponParseException e) {
            throw new RuntimeException("Failed to parse command: " + StringUtils.join(lines, ";"), e);
        }
    }

    private String parseAsApon(String text) {
        Assert.hasText(text, "text must not be null or empty");
        if (text.startsWith("[")) {
            int idx = text.indexOf("]");
            if (idx == -1) {
                throw new IllegalArgumentException("Invalid command format: " + text);
            }
            String nodeId = text.substring(1, idx);
            String[] options = StringUtils.split(text.substring(idx + 1), ";");
            String[] lines = new String[options.length + 1];
            lines[0] = CommandOptions.nodeId.getName() + ":" + nodeId;
            System.arraycopy(options, 0, lines, 1, options.length);
            return StringUtils.join(lines, AponFormat.NEW_LINE);
        } else {
            String[] lines = StringUtils.split(text, ";");
            return StringUtils.join(lines, AponFormat.NEW_LINE);
        }
    }

    public String getNodeId() {
        return getString(nodeId);
    }

    public void setNodeId(String nodeId) {
        putValue(CommandOptions.nodeId, nodeId);
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
    public String getAppsToJoin() {
        return getString(appsToJoin);
    }

    /**
     * Sets the list of instances to join.
     * @param appsToJoin the instances to join
     */
    public void setAppsToJoin(String appsToJoin) {
        putValue(CommandOptions.appsToJoin, appsToJoin);
    }

    /**
     * Returns the specific instance name.
     * @return the instance name
     */
    public String getAppId() {
        return getString(appId);
    }

    /**
     * Sets the specific instance name.
     * @param appId the instance name
     */
    public void setAppId(String appId) {
        putValue(CommandOptions.appId, appId);
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
    public String getLogId() {
        return getString(logId);
    }

    /**
     * Sets the log file name.
     * @param logName the log name
     */
    public void setLogId(String logName) {
        putValue(CommandOptions.logId, logName);
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

    public String getSessionId() {
        return getString(sessionId);
    }

    public void setSessionId(String sessionId) {
        putValue(CommandOptions.sessionId, sessionId);
    }

}
