/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.tomcat.util.http;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public abstract class CookieProcessorBase implements CookieProcessor {

    private static final String COOKIE_DATE_PATTERN = "EEE, dd-MMM-yyyy HH:mm:ss z";

    protected static final ThreadLocal<DateFormat> COOKIE_DATE_FORMAT =
            new ThreadLocal<DateFormat>() {
                @Override
                protected DateFormat initialValue() {
                    DateFormat df =
                            new SimpleDateFormat(COOKIE_DATE_PATTERN, Locale.US);
                    df.setTimeZone(TimeZone.getTimeZone("GMT"));
                    return df;
                }
            };

    protected static final String ANCIENT_DATE;

    static {
        ANCIENT_DATE = COOKIE_DATE_FORMAT.get().format(new Date(10000));
    }

    private SameSiteCookies sameSiteCookies = SameSiteCookies.UNSET;

    public SameSiteCookies getSameSiteCookies() {
        return sameSiteCookies;
    }

    public void setSameSiteCookies(String sameSiteCookies) {
        this.sameSiteCookies = SameSiteCookies.fromString(sameSiteCookies);
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated This implementation calls the deprecated
     * {@link #generateHeader(Cookie)} method. Implementors should
     * not rely on this method as it is present only for
     * transitional compatibility and will be removed in Tomcat 9.
     */
    @Deprecated
    @Override
    public String generateHeader(Cookie cookie, HttpServletRequest request) {
        return generateHeader(cookie);
    }
}
