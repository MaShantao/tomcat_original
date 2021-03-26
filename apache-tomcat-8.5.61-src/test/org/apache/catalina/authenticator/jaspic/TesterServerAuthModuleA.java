/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.catalina.authenticator.jaspic;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;

public class TesterServerAuthModuleA implements ServerAuthModule {

    private StringBuilder trace = new StringBuilder("init()-");

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject,
                                      Subject serviceSubject) throws AuthException {
        return null;
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject)
            throws AuthException {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        trace.append("cleanSubject()-");
        messageInfo.getMap().put("trace", trace.toString());
    }

    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy,
                           CallbackHandler handler, @SuppressWarnings("rawtypes") Map options)
            throws AuthException {
        // NO-OP
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class[] getSupportedMessageTypes() {
        return null;
    }
}
