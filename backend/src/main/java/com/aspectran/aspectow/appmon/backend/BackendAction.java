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
package com.aspectran.aspectow.appmon.backend;

import com.aspectran.aspectow.appmon.backend.config.EndpointInfo;
import com.aspectran.aspectow.appmon.manager.AppMonManager;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.RequestToGet;
import com.aspectran.core.component.bean.annotation.Required;
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
@Component("/backend")
public class BackendAction {

    private static final Logger logger = LoggerFactory.getLogger(BackendAction.class);

    private final AppMonManager appMonManager;

    @Autowired
    public BackendAction(AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
    }

    @RequestToGet("/endpoints/${token}")
    public RestResponse getEndpoints(@Required String token) {
        try {
            AppMonManager.validateToken(token);
        } catch (InvalidPBTokenException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e);
            }
            return new DefaultRestResponse().forbidden();
        }

        List<EndpointInfo> endpointInfoList = appMonManager.getAvailableEndpointInfoList();

        Map<String, Object> data = Map.of(
                "token", AppMonManager.issueToken(),
                "endpoints", endpointInfoList
        );
        return new DefaultRestResponse(data).ok();
    }

}
