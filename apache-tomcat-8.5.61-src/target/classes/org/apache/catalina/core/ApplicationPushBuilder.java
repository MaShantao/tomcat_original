/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.catalina.core;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.SessionTrackingMode;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.catalina.Context;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.connector.Request;
import org.apache.catalina.util.SessionConfig;
import org.apache.coyote.ActionCode;
import org.apache.tomcat.util.buf.HexUtils;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.collections.CaseInsensitiveKeyMap;
import org.apache.tomcat.util.http.CookieProcessor;
import org.apache.tomcat.util.http.parser.HttpParser;
import org.apache.tomcat.util.res.StringManager;

public class ApplicationPushBuilder {

    private static final StringManager sm = StringManager.getManager(ApplicationPushBuilder.class);
    private static final Set<String> DISALLOWED_METHODS = new HashSet<>();

    static {
        DISALLOWED_METHODS.add("POST");
        DISALLOWED_METHODS.add("PUT");
        DISALLOWED_METHODS.add("DELETE");
        DISALLOWED_METHODS.add("CONNECT");
        DISALLOWED_METHODS.add("OPTIONS");
        DISALLOWED_METHODS.add("TRACE");
    }

    private final HttpServletRequest baseRequest;
    private final Request catalinaRequest;
    private final org.apache.coyote.Request coyoteRequest;
    private final String sessionCookieName;
    private final String sessionPathParameterName;
    private final boolean addSessionCookie;
    private final boolean addSessionPathParameter;

    private final Map<String, List<String>> headers = new CaseInsensitiveKeyMap<>();
    private final List<Cookie> cookies = new ArrayList<>();
    private String method = "GET";
    private String path;
    private String queryString;
    private String sessionId;
    private String userName;


    public ApplicationPushBuilder(Request catalinaRequest, HttpServletRequest request) {

        baseRequest = request;
        this.catalinaRequest = catalinaRequest;
        coyoteRequest = catalinaRequest.getCoyoteRequest();

        // Populate the initial list of HTTP headers
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            List<String> values = new ArrayList<>();
            headers.put(headerName, values);
            Enumeration<String> headerValues = request.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                values.add(headerValues.nextElement());
            }
        }

        // Remove the headers
        headers.remove("if-match");
        headers.remove("if-none-match");
        headers.remove("if-modified-since");
        headers.remove("if-unmodified-since");
        headers.remove("if-range");
        headers.remove("range");
        headers.remove("expect");
        headers.remove("authorization");
        headers.remove("referer");
        // Also remove the cookie header since it will be regenerated
        headers.remove("cookie");

        // set the referer header
        StringBuffer referer = request.getRequestURL();
        if (request.getQueryString() != null) {
            referer.append('?');
            referer.append(request.getQueryString());
        }
        addHeader("referer", referer.toString());

        // Session
        Context context = catalinaRequest.getContext();
        sessionCookieName = SessionConfig.getSessionCookieName(context);
        sessionPathParameterName = SessionConfig.getSessionUriParamName(context);

        HttpSession session = request.getSession(false);
        if (session != null) {
            sessionId = session.getId();
        }
        if (sessionId == null) {
            sessionId = request.getRequestedSessionId();
        }
        if (!request.isRequestedSessionIdFromCookie() && !request.isRequestedSessionIdFromURL() &&
                sessionId != null) {
            Set<SessionTrackingMode> sessionTrackingModes =
                    request.getServletContext().getEffectiveSessionTrackingModes();
            addSessionCookie = sessionTrackingModes.contains(SessionTrackingMode.COOKIE);
            addSessionPathParameter = sessionTrackingModes.contains(SessionTrackingMode.URL);
        } else {
            addSessionCookie = request.isRequestedSessionIdFromCookie();
            addSessionPathParameter = request.isRequestedSessionIdFromURL();
        }

        // Cookies
        if (request.getCookies() != null) {
            cookies.addAll(Arrays.asList(request.getCookies()));
        }
        for (Cookie responseCookie : catalinaRequest.getResponse().getCookies()) {
            if (responseCookie.getMaxAge() < 0) {
                // Path information not available so can only remove based on
                // name.
                Iterator<Cookie> cookieIterator = cookies.iterator();
                while (cookieIterator.hasNext()) {
                    Cookie cookie = cookieIterator.next();
                    if (cookie.getName().equals(responseCookie.getName())) {
                        cookieIterator.remove();
                    }
                }
            } else {
                cookies.add(new Cookie(responseCookie.getName(), responseCookie.getValue()));
            }
        }
        List<String> cookieValues = new ArrayList<>(1);
        cookieValues.add(generateCookieHeader(cookies,
                catalinaRequest.getContext().getCookieProcessor()));
        headers.put("cookie", cookieValues);

        // Authentication
        if (catalinaRequest.getPrincipal() != null) {
            if ((session == null) || catalinaRequest.getSessionInternal(false).getPrincipal() == null
                    || !(context.getAuthenticator() instanceof AuthenticatorBase)
                    || !((AuthenticatorBase) context.getAuthenticator()).getCache()) {
                // Set a username only if there is no session cache for the principal
                userName = catalinaRequest.getPrincipal().getName();
            }
            setHeader("authorization", "x-push");
        }
    }


    public ApplicationPushBuilder path(String path) {
        if (path.startsWith("/")) {
            this.path = path;
        } else {
            String contextPath = baseRequest.getContextPath();
            int len = contextPath.length() + path.length() + 1;
            StringBuilder sb = new StringBuilder(len);
            sb.append(contextPath);
            sb.append('/');
            sb.append(path);
            this.path = sb.toString();
        }
        return this;
    }


    public String getPath() {
        return path;
    }


    public ApplicationPushBuilder method(String method) {
        String upperMethod = method.trim().toUpperCase(Locale.ENGLISH);
        if (DISALLOWED_METHODS.contains(upperMethod) || upperMethod.length() == 0) {
            throw new IllegalArgumentException(
                    sm.getString("applicationPushBuilder.methodInvalid", upperMethod));
        }
        // Check a token was supplied
        for (char c : upperMethod.toCharArray()) {
            if (!HttpParser.isToken(c)) {
                throw new IllegalArgumentException(
                        sm.getString("applicationPushBuilder.methodNotToken", upperMethod));
            }
        }
        this.method = method;
        return this;
    }


    public String getMethod() {
        return method;
    }


    public ApplicationPushBuilder queryString(String queryString) {
        this.queryString = queryString;
        return this;
    }


    public String getQueryString() {
        return queryString;
    }


    public ApplicationPushBuilder sessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }


    public String getSessionId() {
        return sessionId;
    }


    public ApplicationPushBuilder addHeader(String name, String value) {
        List<String> values = headers.get(name);
        if (values == null) {
            values = new ArrayList<>();
            headers.put(name, values);
        }
        values.add(value);

        return this;
    }


    public ApplicationPushBuilder setHeader(String name, String value) {
        List<String> values = headers.get(name);
        if (values == null) {
            values = new ArrayList<>();
            headers.put(name, values);
        } else {
            values.clear();
        }
        values.add(value);

        return this;
    }


    public ApplicationPushBuilder removeHeader(String name) {
        headers.remove(name);

        return this;
    }


    public Set<String> getHeaderNames() {
        return Collections.unmodifiableSet(headers.keySet());
    }


    public String getHeader(String name) {
        List<String> values = headers.get(name);
        if (values == null) {
            return null;
        } else {
            return values.get(0);
        }
    }


    public void push() {
        if (path == null) {
            throw new IllegalStateException(sm.getString("pushBuilder.noPath"));
        }

        org.apache.coyote.Request pushTarget = new org.apache.coyote.Request();

        pushTarget.method().setString(method);
        // The next three are implied by the Javadoc getPath()
        pushTarget.serverName().setString(baseRequest.getServerName());
        pushTarget.setServerPort(baseRequest.getServerPort());
        pushTarget.scheme().setString(baseRequest.getScheme());

        // Copy headers
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            for (String value : header.getValue()) {
                pushTarget.getMimeHeaders().addValue(header.getKey()).setString(value);
            }
        }

        // Path and query string
        int queryIndex = path.indexOf('?');
        String pushPath;
        String pushQueryString = null;
        if (queryIndex > -1) {
            pushPath = path.substring(0, queryIndex);
            if (queryIndex + 1 < path.length()) {
                pushQueryString = path.substring(queryIndex + 1);
            }
        } else {
            pushPath = path;
        }

        // Session ID (do this before setting the path since it may change it)
        if (sessionId != null) {
            if (addSessionPathParameter) {
                pushPath = pushPath + ";" + sessionPathParameterName + "=" + sessionId;
                pushTarget.addPathParameter(sessionPathParameterName, sessionId);
            }
            if (addSessionCookie) {
                String sessionCookieHeader = sessionCookieName + "=" + sessionId;
                MessageBytes mb = pushTarget.getMimeHeaders().getValue("cookie");
                if (mb == null) {
                    mb = pushTarget.getMimeHeaders().addValue("cookie");
                    mb.setString(sessionCookieHeader);
                } else {
                    mb.setString(mb.getString() + ";" + sessionCookieHeader);
                }
            }
        }

        // Undecoded path - just %nn encoded
        pushTarget.requestURI().setString(pushPath);
        pushTarget.decodedURI().setString(decode(pushPath,
                catalinaRequest.getConnector().getURICharset()));

        // Query string
        if (pushQueryString == null && queryString != null) {
            pushTarget.queryString().setString(queryString);
        } else if (pushQueryString != null && queryString == null) {
            pushTarget.queryString().setString(pushQueryString);
        } else if (pushQueryString != null && queryString != null) {
            pushTarget.queryString().setString(pushQueryString + "&" + queryString);
        }

        // Authorization
        if (userName != null) {
            pushTarget.getRemoteUser().setString(userName);
            pushTarget.setRemoteUserNeedsAuthorization(true);
        }

        coyoteRequest.action(ActionCode.PUSH_REQUEST, pushTarget);

        // Reset for next call to this method
        path = null;
        headers.remove("if-none-match");
        headers.remove("if-modified-since");
    }


    // Package private so it can be tested. charsetName must be in lower case.
    static String decode(String input, Charset charset) {
        int start = input.indexOf('%');
        int end = 0;

        // Shortcut
        if (start == -1) {
            return input;
        }

        StringBuilder result = new StringBuilder(input.length());
        while (start != -1) {
            // Found the start of a %nn sequence. Copy everything from the last
            // end to this start to the output.
            result.append(input.substring(end, start));
            // Advance the end 3 characters: %nn
            end = start + 3;
            while (end < input.length() && input.charAt(end) == '%') {
                end += 3;
            }
            result.append(decodePercentSequence(input.substring(start, end), charset));
            start = input.indexOf('%', end);
        }
        // Append the remaining text
        result.append(input.substring(end));

        return result.toString();
    }


    private static String decodePercentSequence(String sequence, Charset charset) {
        byte[] bytes = new byte[sequence.length() / 3];
        for (int i = 0; i < bytes.length; i += 3) {
            bytes[i] = (byte) ((HexUtils.getDec(sequence.charAt(1 + 3 * i)) << 4) +
                    HexUtils.getDec(sequence.charAt(2 + 3 * i)));
        }

        return new String(bytes, charset);
    }


    private static String generateCookieHeader(List<Cookie> cookies, CookieProcessor cookieProcessor) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Cookie cookie : cookies) {
            if (first) {
                first = false;
            } else {
                result.append(';');
            }
            // The cookie header value generated by the CookieProcessor was
            // originally intended for the Set-Cookie header on the response.
            // However, if passed a Cookie with just a name and value set it
            // will generate an appropriate header for the Cookie header on the
            // pushed request.
            result.append(cookieProcessor.generateHeader(cookie, null));
        }
        return result.toString();
    }
}
