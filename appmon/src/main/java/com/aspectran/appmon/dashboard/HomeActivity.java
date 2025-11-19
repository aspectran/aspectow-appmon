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
package com.aspectran.appmon.dashboard;

import com.aspectran.appmon.anatomy.AnatomyActivity;
import com.aspectran.appmon.engine.config.DomainInfo;
import com.aspectran.appmon.engine.config.InstanceInfo;
import com.aspectran.appmon.engine.manager.AppMonManager;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.context.ActivityContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles requests from the frontend user interface.
 * This class is responsible for dispatching views and preparing data for the UI.
 *
 * <p>Created: 2020/02/23</p>
 */
@Component
public class HomeActivity {

    private final AppMonManager appMonManager;

    @Autowired
    public HomeActivity(AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
    }

    /**
     * Handles the root request and displays the main home page.
     * @return a map of attributes for rendering the view
     */
    @Request("/")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, Object> home() {
        List<DomainInfo> domainInfoList = appMonManager.getDomainInfoList();
        List<InstanceInfo> instanceInfoList = appMonManager.getInstanceInfoList();
        Map<String, ActivityContext> contexts = AnatomyActivity.prepareContextMap();
        List<String> allContextNames = new ArrayList<>(contexts.keySet());
        return Map.of(
                "include", "home/main",
                "style", "fluid compact",
                "domainInfoList", domainInfoList,
                "instanceInfoList", instanceInfoList,
                "allContextNames", allContextNames
        );
    }

    /**
     * Handles any other top-level requests and redirects to the home page.
     * @return a map of attributes for rendering the view
     */
    @Request("/${ignore}")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, Object> home2() {
        return home();
    }

    @Request("/auth-expired")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, Object> handleAuthExpired() {
        return Map.of(
                "include", "home/auth-expired",
                "style", "fluid compact"
        );
    }

}
