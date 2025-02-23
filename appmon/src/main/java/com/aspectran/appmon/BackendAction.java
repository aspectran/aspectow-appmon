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
package com.aspectran.appmon;

import com.aspectran.appmon.config.DomainInfo;
import com.aspectran.appmon.config.InstanceInfo;
import com.aspectran.appmon.manager.AppMonManager;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.RequestToGet;
import com.aspectran.core.component.bean.annotation.Required;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.web.activity.response.DefaultRestResponse;
import com.aspectran.web.activity.response.RestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @RequestToGet("/${token}/config")
    public RestResponse getConfigData(@Required String token, String instances) {
        try {
            AppMonManager.validateToken(token);
        } catch (InvalidPBTokenException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            return new DefaultRestResponse().forbidden();
        }

        List<DomainInfo> domainInfoList = appMonManager.getDomainInfoList();

        String[] instanceNames = StringUtils.splitWithComma(instances);
        instanceNames = appMonManager.getVerifiedInstanceNames(instanceNames);
        List<InstanceInfo> instanceInfoList = appMonManager.getInstanceInfoList(instanceNames);

        Map<String, Object> data = Map.of(
                "token", AppMonManager.issueToken(),
                "domains", domainInfoList,
                "instances", instanceInfoList
        );
        return new DefaultRestResponse(data).nullWritable(false).ok();
    }

}
