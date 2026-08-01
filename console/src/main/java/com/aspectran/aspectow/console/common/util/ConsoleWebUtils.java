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
package com.aspectran.aspectow.console.common.util;

import com.aspectran.core.activity.Translet;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.support.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;

/**
 * Utility class providing web-related helper methods for Aspectow Console.
 */
public abstract class ConsoleWebUtils {

    /**
     * Extracts the remote client IP address from the request.
     * Checks the X-Forwarded-For header first for proxies/load balancers,
     * falling back to the remote address of the underlying servlet request.
     * @param translet the current translet
     * @return the remote IP address
     */
    public static String getRemoteAddr(@NonNull Translet translet) {
        String remoteAddr = translet.getRequestAdapter().getHeader(HttpHeaders.X_FORWARDED_FOR);
        if (StringUtils.hasLength(remoteAddr)) {
            if (remoteAddr.contains(",")) {
                remoteAddr = StringUtils.tokenize(remoteAddr, ",", true)[0];
            }
        } else {
            remoteAddr = ((HttpServletRequest)translet.getRequestAdaptee()).getRemoteAddr();
        }
        return remoteAddr;
    }

    /**
     * Escapes HTML special characters in the input string to prevent XSS attacks.
     * @param input the raw input string
     * @return the HTML-escaped string
     */
    public static String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;")
                    .replace("/", "&#x2F;");
    }

    /**
     * Sanitizes user input string by stripping potential script tags.
     * @param input the raw input string
     * @return the sanitized string
     */
    public static String cleanInput(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("(?i)<script.*?>.*?</script>", "")
                    .replaceAll("(?i)<iframe.*?>.*?</iframe>", "")
                    .replaceAll("(?i)javascript:", "");
    }

}
