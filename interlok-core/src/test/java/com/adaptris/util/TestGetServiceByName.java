/*
 * Copyright 2015 Adaptris Ltd.
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

package com.adaptris.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 * @author lchan
 */
public class TestGetServiceByName extends TestCase
{

  public TestGetServiceByName(java.lang.String testName)
  {
    super(testName);
  }

  public static void main(java.lang.String[] args)
  {
    junit.textui.TestRunner.run(suite());
  }

  public static Test suite()
  {
    TestSuite suite = new TestSuite(TestGetServiceByName.class);
    return suite;
  }

  // Add test methods here, they have to start with 'test' name.
  // for example:
  // public void testHello() {}
  public void testGetHttpsServicePort()
  {
    assertEquals("Get Https port",
                GetServiceByName.getPort("https", "tcp"),
                443);
  }

  public void testGetSmtpServicePort()
  {
    assertEquals("Get SMTP port",
                GetServiceByName.getPort("smtp", "tcp"),
                25);
  }

  public void testGetHttpServicePort()
  {
    assertEquals("Get Http port",
                GetServiceByName.getPort("http", "tcp"),
                80);
  }

  public void testGetNonExistentPort() {
    assertEquals("Non-Existent Port",
                GetServiceByName.getPort("asdfasdfadsf", "tcp"),
                -1);
  }
  @Override
  protected void finalize() throws Throwable
  {
    super.finalize();
  }
}


