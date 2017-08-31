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

package com.adaptris.core.fs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.adaptris.core.stubs.TempFileUtils;

public class OlderThanTest {

  @Test
  public void testOlderThan() throws Exception {
    OlderThan filter = new OlderThan("-PT30S");
    File file = writeFile(TempFileUtils.createTrackedFile(filter));
    file.setLastModified(yesterday());
    assertTrue(filter.accept(file));
  }

  @Test
  public void testOlderThanFutureSpec() throws Exception {
    OlderThan filter = new OlderThan("PT1H");
    File file = writeFile(TempFileUtils.createTrackedFile(filter));
    file.setLastModified(yesterday());
    assertTrue(filter.accept(file));
  }

  @Test
  public void testBadDuration() throws Exception {
    OlderThan filter = new OlderThan("-PXXX");
    File file = writeFile(TempFileUtils.createTrackedFile(filter));
    file.setLastModified(yesterday());
    assertFalse(filter.accept(file));
  }

  private File writeFile(File f) throws IOException {
    FileUtils.write(f, "Hello World");
    return f;
  }

  private long yesterday() {
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_YEAR, -1);
    return cal.getTime().getTime();
  }
}
