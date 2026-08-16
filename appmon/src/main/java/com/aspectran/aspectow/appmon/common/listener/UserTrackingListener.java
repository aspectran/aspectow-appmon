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
package com.aspectran.aspectow.appmon.common.listener;

import com.aspectran.aspectow.appmon.common.support.IPCountryResolver;
import com.aspectran.core.activity.Activity;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.session.Session;
import com.aspectran.core.component.session.SessionListener;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.support.util.WebUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

import static com.aspectran.aspectow.appmon.engine.exporter.event.session.SessionEventReader.USER_COUNTRY_CODE;
import static com.aspectran.aspectow.appmon.engine.exporter.event.session.SessionEventReader.USER_IP_ADDRESS;

/**
 * A listener that tracks user information by listening to session events.
 * It captures the user's IP address and resolves the country code when a session is created.
 *
 * <p>Created: 2024-12-13</p>
 */
public class UserTrackingListener implements SessionListener {

    private final ActivityContext context;

    private final IPCountryResolver ipCountryResolver;

    /**
     * Instantiates a new UserTrackingListener without country code resolution.
     */
    public UserTrackingListener() {
        this(null, null);
    }

    /**
     * Instantiates a new UserTrackingListener.
     * @param context the activity context
     */
    public UserTrackingListener(@Nullable ActivityContext context) {
        this(context, null);
    }

    /**
     * Instantiates a new UserTrackingListener.
     * @param context the activity context
     * @param ipCountryResolver the resolver for determining the country code from an IP address
     */
    public UserTrackingListener(
            @Nullable ActivityContext context,
            @Nullable IPCountryResolver ipCountryResolver) {
        this.context = context;
        this.ipCountryResolver = ipCountryResolver;
    }

    /**
     * Called when a session is created. It retrieves the remote IP address
     * and locale from the current activity and stores them in the session attributes.
     * @param session the session that was created
     */
    @Override
    public void sessionCreated(@NonNull Session session) {
        if (context != null && context.hasCurrentActivity()) {
            Activity activity = context.getCurrentActivity();
            if (activity.hasTranslet()) {
                Translet translet = activity.getTranslet();
                String ipAddress = WebUtils.getRemoteAddr(translet);
                if (StringUtils.hasLength(ipAddress)) {
                    session.setAttribute(USER_IP_ADDRESS, ipAddress);
                    if (ipCountryResolver != null) {
                        Locale locale = translet.getRequestAdapter().getLocale();
                        String countryCode = ipCountryResolver.resolveCountryCode(ipAddress, locale);
                        if (StringUtils.hasLength(countryCode)) {
                            session.setAttribute(USER_COUNTRY_CODE, countryCode);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof UserTrackingListener);
    }

    @Override
    public int hashCode() {
        return UserTrackingListener.class.hashCode();
    }

}
