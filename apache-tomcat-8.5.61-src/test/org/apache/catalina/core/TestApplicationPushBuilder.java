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
package org.apache.catalina.core;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

public class TestApplicationPushBuilder {

    @Test
    public void test01() {
        doTest("foo", StandardCharsets.UTF_8, "foo");
    }

    @Test
    public void test02() {
        doTest("/foo", StandardCharsets.UTF_8, "/foo");
    }

    @Test
    public void test03() {
        doTest("%20foo", StandardCharsets.UTF_8, " foo");
    }

    @Test
    public void test04() {
        doTest("fo%20o", StandardCharsets.UTF_8, "fo o");
    }

    @Test
    public void test05() {
        doTest("foo%20", StandardCharsets.UTF_8, "foo ");
    }

    @Test
    public void test06() {
        doTest("%21foo", StandardCharsets.UTF_8, "!foo");
    }

    @Test
    public void test07() {
        doTest("fo%21o", StandardCharsets.UTF_8, "fo!o");
    }

    @Test
    public void test08() {
        doTest("foo%21", StandardCharsets.UTF_8, "foo!");
    }


    private void doTest(String input, Charset charset, String expected) {
        String result = ApplicationPushBuilder.decode(input, charset);
        Assert.assertEquals(expected, result);
    }
}
