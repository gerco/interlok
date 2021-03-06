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

package com.adaptris.core.services.dynamic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adaptris.annotation.Removal;
import com.adaptris.core.CoreException;
import com.adaptris.core.TradingRelationship;
import com.adaptris.core.util.Args;

/**
 * <p>
 * Partial implementation of <code>ServiceNameProvider</code>.
 * </p>
 * 
 * @deprecated since 3.8.4 use {@link DynamicServiceExecutor} with a URL based
 *             {@link ServiceExtractor} instead.
 */
@Deprecated
@Removal(version = "3.11.0")
public abstract class ServiceNameProviderImp implements ServiceNameProvider {

  protected transient Logger log = LoggerFactory.getLogger(this.getClass());

  /**
   * @see com.adaptris.core.services.dynamic.ServiceNameProvider
   *   #obtain(com.adaptris.core.TradingRelationship)
   */
  @Override
  public String obtain(TradingRelationship t) throws CoreException {
    Args.notNull(t, "tradingRelationship");
    TradingRelationship[] ts = new TradingRelationship[1];
    ts[0] = t;

    return this.obtain(ts);
  }

  /**
   * <p>
   * Delegates <code>retrieveName</code> to concrete implementations.
   * </p>
   * @see com.adaptris.core.services.dynamic.ServiceNameProvider
   *   #obtain(com.adaptris.core.TradingRelationship[])
   */
  @Override
  public String obtain(TradingRelationship[] matches) throws CoreException {
    Args.notNull(matches, "tradingRelationships");
    String result = null;

    for (int i = 0; i < matches.length; i++) {
      result = retrieveName(matches[i]);

      if (result != null) {
        log.trace("service logical name [{}] matched on [{}]", result, matches[i]);
        break;
      }
    }
    return result;
  }

  protected abstract String retrieveName(TradingRelationship t)
    throws CoreException;
}
