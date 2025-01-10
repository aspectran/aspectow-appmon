/*
 * Copyright (c) 2018-2025 The Aspectran Project
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
package com.aspectran.aspectow.appmon;

import com.aspectran.aspectow.appmon.config.EndpointInfo;
import com.aspectran.aspectow.appmon.manager.AppMonManager;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToGet;
import com.aspectran.core.component.bean.annotation.Required;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.logging.Logger;
import com.aspectran.utils.logging.LoggerFactory;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.web.activity.response.DefaultRestResponse;
import com.aspectran.web.activity.response.RestResponse;

import java.util.List;
import java.util.Map;

/**
 * <p>Created: 2020/02/23</p>
 */
@Component("/monitoring")
public class MonitoringAction {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringAction.class);

    private final AppMonManager appMonManager;

    @Autowired
    public MonitoringAction(AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
    }

    @Request("/${endpoint}")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, String> viewer(String endpoint) {
        return Map.of(
                "headinclude", "appmon/_endpoints",
                "include", "appmon/appmon",
                "style", "fluid compact",
                "token", appMonManager.issueToken(),
                "endpoint", StringUtils.nullToEmpty(endpoint)
        );
    }

    @RequestToGet("/endpoints/${token}")
    public RestResponse getEndpoints(@Required String token) {
        try {
            appMonManager.validateToken(token);
        } catch (InvalidPBTokenException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e);
            }
            return new DefaultRestResponse().forbidden();
        }
        List<EndpointInfo> endpointInfoList = appMonManager.getAvailableEndpointInfoList(token);
        return new DefaultRestResponse(endpointInfoList).ok();
    }

}
