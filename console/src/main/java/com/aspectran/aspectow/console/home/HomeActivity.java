/*
 * Copyright (c) 2026-present The Aspectran Project
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
package com.aspectran.aspectow.console.home;

import com.aspectran.aspectow.console.cluster.NodeConsoleHelper;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;

import java.util.List;
import java.util.Map;

@Component("/")
public class HomeActivity {

    private final NodeConsoleHelper nodeConsoleHelper;

    @Autowired
    public HomeActivity(NodeConsoleHelper nodeConsoleHelper) {
        this.nodeConsoleHelper = nodeConsoleHelper;
    }

    @Request("/")
    @Dispatch("home/home")
    @Action("page")
    public Map<String, Object> home() {
        List<Map<String, Object>> nodes = nodeConsoleHelper.getNodes(false);
        return Map.of(
                "title", "Aspectow Console",
                "headline", "Aspectow Management Console",
                "include", "home",
                "style", "dashboard-page",
                "nodes", nodes
                );
    }

}
