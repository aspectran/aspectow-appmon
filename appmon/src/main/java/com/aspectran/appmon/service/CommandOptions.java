/*
 * Copyright (c) 2020-2025 The Aspectran Project
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
package com.aspectran.appmon.service;

import com.aspectran.utils.StringUtils;
import com.aspectran.utils.apon.AbstractParameters;
import com.aspectran.utils.apon.AponFormat;
import com.aspectran.utils.apon.AponParseException;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

public class CommandOptions extends AbstractParameters {

    private static final ParameterKey command;
    private static final ParameterKey instancesToJoin;
    private static final ParameterKey instance;
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

        parameterKeys = new ParameterKey[] {
                command,
                instancesToJoin,
                instance,
                timeZone,
                dateUnit,
                dateOffset
        };
    }

    public CommandOptions() {
        super(parameterKeys);
    }

    public CommandOptions(String text) {
        this(StringUtils.split(text, ";"));
    }

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

}
