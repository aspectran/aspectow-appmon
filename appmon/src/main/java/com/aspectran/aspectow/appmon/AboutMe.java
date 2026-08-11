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
package com.aspectran.aspectow.appmon;

/**
 * Provides information about the AppMon build.
 * <p>This class retrieves the version of AppMon from the manifest file
 * of the JAR in which it is packaged.</p>
 */
public class AboutMe {

    /** The version of AppMon. */
    public static final String VERSION;

    /** A boolean indicating whether the current version is a stable release. */
    public static final boolean STABLE;

    static {
        Package pkg = AboutMe.class.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null) {
            VERSION = pkg.getImplementationVersion();
        } else {
            VERSION = System.getProperty("aspectow.version", "4.0.x");
        }

        // Show warning when RC# or M# or -SNAPSHOT is in version string
        STABLE = !VERSION.matches("^.*[.-](RC|M|SNAPSHOT|x)[0-9]?$");
    }

    /**
     * This class cannot be instantiated.
     */
    private AboutMe() {
    }

    /**
     * Returns the version of AppMon.
     * @return the version string
     */
    public static String getVersion() {
        return VERSION;
    }

    /**
     * Returns the detailed version information of AppMon.
     * If the version is not a stable release, a warning message is appended.
     * @return the detailed version string
     */
    public static String getVersionDetail() {
        if (STABLE) {
            return VERSION;
        } else {
            return VERSION + " (THIS IS NOT A STABLE RELEASE! DO NOT USE IN PRODUCTION!)";
        }
    }

}
