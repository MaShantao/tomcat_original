/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javax.servlet.http;

import org.junit.Test;

/**
 * Basic tests for Cookie in default configuration.
 */
public class TestCookieRFC2109Validator {

    private RFC2109Validator validator = new RFC2109Validator(false);

    @Test
    public void actualCharactersAllowedInName() {
        TestCookie.checkCharInName(validator, TestCookie.TOKEN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void leadingDollar() {
        validator.validate("$Version");
    }
}
