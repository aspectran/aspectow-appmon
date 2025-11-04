/*
 * Copyright (c) 2008-present The Aspectran Project
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
package com.aspectran.appmon.anatomy;

import com.aspectran.appmon.engine.manager.AppMonManager;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.FormatType;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.annotation.jsr305.NonNull;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.web.activity.response.DefaultRestResponse;
import com.aspectran.web.activity.response.RestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * A controller that provides framework anatomy data for the viewer.
 */
@Component
@Bean("anatomyActivity")
public class AnatomyActivity {

    private static final Logger logger = LoggerFactory.getLogger(AnatomyActivity.class);

    private final AnatomyService anatomyService;

    @Autowired
    public AnatomyActivity(AnatomyService anatomyService) {
        this.anatomyService = anatomyService;
    }

    /**
     * Dispatches to the anatomy viewer page within the default template.
     */
    @Request("/anatomy/viewer/${token}/${contextName}")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, String> viewer(@NonNull Translet translet, String token, String contextName) {
        if (token == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Required token");
            }
            translet.redirect("/");
            return null;
        }
        try {
            AppMonManager.validateToken(token);
        } catch (Exception e) {
            if (e instanceof InvalidPBTokenException) {
                logger.error("Invalid token: {}", token);
            } else {
                logger.error(e.getMessage(), e);
            }
            if (StringUtils.hasLength(translet.getContextPath())) {
                translet.redirect("/../");
            } else {
                translet.redirect("/");
            }
            return null;
        }
        return Map.of(
                "include", "anatomy/viewer",
                "style", "plate compact",
                "headline", "Framework Anatomy"
        );
    }

    /**
     * Provides framework anatomy data as JSON.
     * @return a map containing the anatomy data, identified by "anatomyData"
     */
    @Request("/anatomy/data")
    @Action("anatomyData")
    @Transform(format = FormatType.JSON)
    public RestResponse data(@NonNull Translet translet) {
        try {
            String token = translet.getRequestAdapter().getHeader("token");
            AppMonManager.validateToken(token);
        } catch (InvalidPBTokenException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            return new DefaultRestResponse().forbidden();
        }
        Map<String, Object> data = anatomyService.getAnatomyData();
        return new DefaultRestResponse(data).nullWritable(false).ok();
    }

}
