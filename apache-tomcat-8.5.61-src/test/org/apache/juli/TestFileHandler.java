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

package org.apache.juli;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestFileHandler {

    private static final String PREFIX_1 = "localhost.";
    private static final String PREFIX_2 = "test.";
    private static final String PREFIX_3 = "";
    private static final String PREFIX_4 = "localhost1";
    private static final String SUFFIX_1 = ".log";
    private static final String SUFFIX_2 = ".txt";

    private File logsDir;

    @Before
    public void setUp() throws Exception {
        File logsBase = new File(System.getProperty("tomcat.test.temp", "output/tmp"));
        if (!logsBase.mkdirs() && !logsBase.isDirectory()) {
            Assert.fail("Unable to create logs directory.");
        }
        Path logsBasePath = FileSystems.getDefault().getPath(logsBase.getAbsolutePath());
        logsDir = Files.createTempDirectory(logsBasePath, "test").toFile();

        generateLogFiles(logsDir, PREFIX_1, SUFFIX_2, 3);
        generateLogFiles(logsDir, PREFIX_2, SUFFIX_1, 3);
        generateLogFiles(logsDir, PREFIX_3, SUFFIX_1, 3);
        generateLogFiles(logsDir, PREFIX_4, SUFFIX_1, 3);

        Calendar date = Calendar.getInstance();
        date.add(Calendar.DAY_OF_MONTH, -3);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSS");
        File file = new File(logsDir, PREFIX_1 + formatter.format(date.getTime()) + SUFFIX_1);
        if (!file.createNewFile()) {
            Assert.fail("Unable to create " + file.getAbsolutePath());
        }
    }

    @After
    public void tearDown() {
        File[] files = logsDir.listFiles();
        if (files != null) {
            for (File file : files) {
                Assert.assertTrue("Failed to delete [" + file + "]", file.delete());
            }
            Assert.assertTrue("Failed to create [" + logsDir + "]", logsDir.delete());
        }
    }

    @Test
    public void testCleanOnInitOneHandler() throws Exception {
        generateLogFiles(logsDir, PREFIX_1, SUFFIX_1, 3);

        FileHandler fh1 = new FileHandler(logsDir.getAbsolutePath(), PREFIX_1, SUFFIX_1, 2);

        Thread.sleep(1000);

        Assert.assertTrue(logsDir.list().length == 16);

        fh1.close();
    }

    @Test
    public void testCleanOnInitMultipleHandlers() throws Exception {
        generateLogFiles(logsDir, PREFIX_1, SUFFIX_1, 3);

        FileHandler fh1 = new FileHandler(logsDir.getAbsolutePath(), PREFIX_1, SUFFIX_1, 2);
        FileHandler fh2 = new FileHandler(logsDir.getAbsolutePath(), PREFIX_1, SUFFIX_2, 2);
        FileHandler fh3 = new FileHandler(logsDir.getAbsolutePath(), PREFIX_2, SUFFIX_1, 2);
        FileHandler fh4 = new FileHandler(logsDir.getAbsolutePath(), PREFIX_3, SUFFIX_1, 2);

        Thread.sleep(1000);

        Assert.assertTrue(logsDir.list().length == 16);

        fh1.close();
        fh2.close();
        fh3.close();
        fh4.close();
    }

    @Test
    public void testCleanDisabled() throws Exception {
        generateLogFiles(logsDir, PREFIX_1, SUFFIX_1, 3);

        FileHandler fh1 = new FileHandler(logsDir.getAbsolutePath(), PREFIX_1, SUFFIX_1, -1);

        Thread.sleep(1000);

        Assert.assertTrue(logsDir.list().length == 17);

        fh1.close();
    }

    private void generateLogFiles(File dir, String prefix, String suffix, int amount)
            throws IOException {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        for (int i = 0; i < amount; i++) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
            File file = new File(dir, prefix + formatter.format(cal.getTime()) + suffix);
            if (!file.createNewFile()) {
                Assert.fail("Unable to create " + file.getAbsolutePath());
            }
        }
    }
}
