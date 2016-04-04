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

package com.adaptris.core.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

import com.adaptris.core.util.ExceptionHelper;
import com.adaptris.interlok.InterlokException;
import com.adaptris.interlok.config.DataOutputParameter;
import com.adaptris.interlok.types.InterlokMessage;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * This {@code DataOutputParameter} is used when you want to write binary data to the {@link com.adaptris.core.AdaptrisMessage} payload.
 * 
 * 
 * @config binary-stream-payload-output-parameter
 * 
 */
@XStreamAlias("binary-stream-payload-output-parameter")
public class PayloadBinaryStreamOutputParameter implements DataOutputParameter<InputStream> {

  @Override
  public void insert(InputStream data, InterlokMessage msg) throws InterlokException {
    try {
      copyAndClose(data, msg.getOutputStream());
    } catch (IOException e) {
      ExceptionHelper.rethrowCoreException(e);
    }
  }

  private void copyAndClose(InputStream input, OutputStream out) throws IOException {
    try (InputStream autoCloseIn = new BufferedInputStream(input); 
        BufferedOutputStream autoCloseOut = new BufferedOutputStream(out)) {
      IOUtils.copy(autoCloseIn, autoCloseOut);
    }
  }

}
