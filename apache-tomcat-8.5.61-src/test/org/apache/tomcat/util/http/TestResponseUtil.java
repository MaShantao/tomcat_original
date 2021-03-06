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
package org.apache.tomcat.util.http;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import org.apache.tomcat.unittest.TesterResponse;

public class TestResponseUtil {

    @Test
    public void testAddValidWithAll() {
        TesterResponse response = new TesterResponse();
        response.getCoyoteResponse();
        response.addHeader("vary", "host");
        List<String> expected = new ArrayList<>();
        expected.add("*");
        doTestAddVaryFieldName(response, "*", expected);
    }


    @Test
    public void testAddAllWithAll() {
        TesterResponse response = new TesterResponse();
        response.getCoyoteResponse();
        response.addHeader("vary", "*");
        List<String> expected = new ArrayList<>();
        expected.add("*");
        doTestAddVaryFieldName(response, "*", expected);
    }


    @Test
    public void testAddAllWithNone() {
        TesterResponse response = new TesterResponse();
        response.getCoyoteResponse();
        List<String> expected = new ArrayList<>();
        expected.add("*");
        doTestAddVaryFieldName(response, "*", expected);
    }


    @Test
    public void testAddValidWithValidSingleHeader() {
        TesterResponse response = new TesterResponse();
        response.getCoyoteResponse();
        response.addHeader("vary", "foo, bar");
        List<String> expected = new ArrayList<>();
        expected.add("foo");
        expected.add("bar");
        expected.add("too");
        doTestAddVaryFieldName(response, "too", expected);
    }


    @Test
    public void testAddValidWithValidSingleHeaderIncludingAll() {
        TesterResponse response = new TesterResponse();
        response.getCoyoteResponse();
        response.addHeader("vary", "foo, *");
        List<String> expected = new ArrayList<>();
        expected.add("*");
        doTestAddVaryFieldName(response, "too", expected);
    }


    @Test
    public void testAddValidWithValidSingleHeaderAlreadyPresent() {
        TesterResponse response = new TesterResponse();
        response.getCoyoteResponse();
        response.addHeader("vary", "foo, bar");
        List<String> expected = new ArrayList<>();
        expected.add("foo");
        expected.add("bar");
        doTestAddVaryFieldName(response, "foo", expected);
    }


    @Test
    public void testAddValidWithValidHeaders() {
        TesterResponse response = new TesterResponse();
        response.getCoyoteResponse();
        response.addHeader("vary", "foo");
        response.addHeader("vary", "bar");
        List<String> expected = new ArrayList<>();
        expected.add("foo");
        expected.add("bar");
        expected.add("too");
        doTestAddVaryFieldName(response, "too", expected);
    }


    @Test
    public void testAddValidWithValidHeadersIncludingAll() {
        TesterResponse response = new TesterResponse();
        response.getCoyoteResponse();
        response.addHeader("vary", "foo");
        response.addHeader("vary", "*");
        List<String> expected = new ArrayList<>();
        expected.add("*");
        doTestAddVaryFieldName(response, "too", expected);
    }


    @Test
    public void testAddValidWithValidHeadersAlreadyPresent() {
        TesterResponse response = new TesterResponse();
        response.getCoyoteResponse();
        response.addHeader("vary", "foo");
        response.addHeader("vary", "bar");
        List<String> expected = new ArrayList<>();
        expected.add("foo");
        expected.add("bar");
        doTestAddVaryFieldName(response, "foo", expected);
    }


    @Test
    public void testAddValidWithPartiallyValidSingleHeader() {
        TesterResponse response = new TesterResponse();
        response.getCoyoteResponse();
        response.addHeader("vary", "{{{, bar");
        List<String> expected = new ArrayList<>();
        expected.add("bar");
        expected.add("too");
        doTestAddVaryFieldName(response, "too", expected);
    }


    @Test
    public void testAddValidWithPartiallyValidSingleHeaderIncludingAll() {
        TesterResponse response = new TesterResponse();
        response.getCoyoteResponse();
        response.addHeader("vary", "{{{, *");
        List<String> expected = new ArrayList<>();
        expected.add("*");
        doTestAddVaryFieldName(response, "too", expected);
    }


    @Test
    public void testAddValidWithPartiallyValidSingleHeaderAlreadyPresent() {
        TesterResponse response = new TesterResponse();
        response.getCoyoteResponse();
        response.addHeader("vary", "{{{, bar");
        List<String> expected = new ArrayList<>();
        expected.add("bar");
        doTestAddVaryFieldName(response, "bar", expected);
    }


    private void doTestAddVaryFieldName(TesterResponse response, String fieldName,
                                        List<String> expected) {
        ResponseUtil.addVaryFieldName(response, fieldName);
        // There will now only be one Vary header
        String resultHeader = response.getHeader("vary");
        List<String> result = new ArrayList<>();
        // Deliberately do not use Vary.parseVary as it will skip invalid values.
        for (String value : resultHeader.split(",")) {
            result.add(value.trim());
        }
        Assert.assertEquals(expected, result);
    }


    @Test
    public void testMimeHeadersAddAllWithNone() {
        MimeHeaders mh = new MimeHeaders();
        List<String> expected = new ArrayList<>();
        expected.add("*");
        doTestAddVaryFieldName(mh, "*", expected);
    }


    @Test
    public void testMimeHeadersAddValidWithValidHeaders() {
        MimeHeaders mh = new MimeHeaders();
        mh.addValue("vary").setString("foo");
        mh.addValue("vary").setString("bar");
        List<String> expected = new ArrayList<>();
        expected.add("foo");
        expected.add("bar");
        expected.add("too");
        doTestAddVaryFieldName(mh, "too", expected);
    }

    private void doTestAddVaryFieldName(MimeHeaders mh, String fieldName,
                                        List<String> expected) {
        ResponseUtil.addVaryFieldName(mh, fieldName);
        // There will now only be one Vary header
        String resultHeader = mh.getHeader("vary");
        List<String> result = new ArrayList<>();
        // Deliberately do not use Vary.parseVary as it will skip invalid values.
        for (String value : resultHeader.split(",")) {
            result.add(value.trim());
        }
        Assert.assertEquals(expected, result);
    }
}
