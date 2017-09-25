/*
 * Copyright 2017 Adaptris Ltd.
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
package com.adaptris.interlok.boot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class InterlokLauncherTest {
  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testMainClass() throws Exception {
    InterlokLauncher launcher = new InterlokLauncher(null);
    assertEquals("com.adaptris.core.management.SimpleBootstrap", launcher.getMainClass());
  }

  @Test
  public void testRebuild() throws Exception {
    InterlokLauncher launcher = new InterlokLauncher(new String[]
    {
        "-ignoreSubDirs", "--adapterClasspath", "./lib", "--configcheck"
    });
    String[] rebuild = launcher.rebuildArgs();
    // It's the intention that empty args get "true" associated with them.
    assertEquals(2, rebuild.length);
    assertEquals("--configcheck", rebuild[0]);
    assertEquals("true", rebuild[1]);
  }

  @Test
  public void testClasspathArchives_NonExistent() throws Exception {
    InterlokLauncher launcher = new InterlokLauncher(new String[]
    {
        "-ignoreSubDirs", "--adapterClasspath", "./config,./lib,./some.jar,./someDir/*,anotherDir/,."
    });
    assertNotNull(launcher.getClassPathArchives());
    // . gets a classpath entry, cos it exists.
    assertEquals(1, launcher.getClassPathArchives().size());

  }

  @Test
  public void testClasspathArchives_NoRecurse() throws Exception {
    String javaHome = System.getProperty("java.home");
    // must be jars in javahome right?
    InterlokLauncher launcher = new InterlokLauncher(new String[]
    {
        "-ignoreSubDirs", "--adapterClasspath", javaHome
    });
    assertNotNull(launcher.getClassPathArchives());
    assertNotSame(0, launcher.getClassPathArchives().size());

  }

  @Test
  public void testClasspathArchives() throws Exception {
    String javaHome = System.getProperty("java.home");
    // must be jars in javahome right?
    InterlokLauncher launcher = new InterlokLauncher(new String[]
    {
        "--adapterClasspath", javaHome
    });
    assertNotNull(launcher.getClassPathArchives());
    assertNotSame(0, launcher.getClassPathArchives().size());
  }

}