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

package com.adaptris.core;

import static org.apache.commons.lang.StringUtils.defaultIfEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.Removal;
import com.adaptris.core.util.Args;
import com.adaptris.core.util.ExceptionHelper;
import com.adaptris.util.GuidGenerator;

/**
 * <p>
 * Implementation of default / common behaviour for <code>Service</code>s.
 * Includes basic implementation of <code>MessageEventGenerator</code> which
 * returns the fully qualified name of the class.
 * </p>
 */
public abstract class ServiceImp implements Service {
  // protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());

  protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());

  private transient ComponentState serviceState;
  private transient boolean prepared = false;
  
  @AdvancedConfig
  private String lookupName;
  private String uniqueId;
  private transient boolean isBranching; // defaults to false
  @AdvancedConfig
  @InputFieldDefault(value = "false")
  private Boolean continueOnFail;
  @AdvancedConfig
  @InputFieldDefault(value = "false")
  private Boolean isTrackingEndpoint;
  @AdvancedConfig
  @InputFieldDefault(value = "false")
  @Deprecated
  @Removal(version = "3.9.0")
  private Boolean isConfirmation;

  /**
   * <p>
   * Creates a new instance. Default unique ID is autogenerated using {@link GuidGenerator#getUUID()}.
   * </p>
   */
  public ServiceImp() {
    setUniqueId(new GuidGenerator().getUUID());
    changeState(ClosedState.getInstance());
  }

  public final void init() throws CoreException {
    if (!prepared)
      prepare();
    initService();
  }

  protected abstract void initService() throws CoreException;

  public final void close() {
    closeService();
    prepared = false;
  }

  protected abstract void closeService();


  @Override
  public void stop() {
    // over-ride if required
  }

  @Override
  public void start() throws CoreException {
    // over-ride if required
  }

  @Override
  public String createName() {
    return this.getClass().getName();
  }

  @Override
  public String createQualifier() {
    return defaultIfEmpty(getUniqueId(), "");
  }

  @Override
  public String getUniqueId() {
    return uniqueId;
  }

  @Override
  public void setUniqueId(String s) {
    uniqueId = Args.notNull(s, "uniqueId");
  }

  @Override
  public boolean isBranching() {
    return isBranching;
  }

  @Override
  public boolean continueOnFailure() {
    if (getContinueOnFail() != null) {
      return getContinueOnFail().booleanValue();
    }
    return false;
  }

  /**
   * @return whether or not this service is configured to continue on failure.
   * @see #continueOnFailure()
   */
  public Boolean getContinueOnFail() {
    return continueOnFail;
  }

  /**
   * whether or not this service is configured to continue on failure.
   * 
   * @param b true/false, default if not specified is false.
   */
  public void setContinueOnFail(Boolean b) {
    continueOnFail = b;
  }

  public Boolean getIsTrackingEndpoint() {
    return isTrackingEndpoint;
  }

  /**
   * whether or not this service is is a tracking endpoint.
   * 
   * @param b true/false, default if not specified is false.
   */
  public void setIsTrackingEndpoint(Boolean b) {
    isTrackingEndpoint = b;
  }

  /**
   * 
   * @deprecated since 3.6.2 No-one has ever produced a confirmation service. This will be removed.
   */
  @Deprecated
  @Removal(version = "3.9.0")
  public Boolean getIsConfirmation() {
    return isConfirmation;
  }

  /**
   * whether or not this service is configured a confirmation.
   * 
   * @param b true/false, default if not specified is false.
   * @deprecated since 3.6.2 No-one has ever produced a confirmation service. This will be removed.
   * 
   */
  @Deprecated
  @Removal(version = "3.9.0")
  public void setIsConfirmation(Boolean b) {
    isConfirmation = b;
  }

  @Override
  public boolean isTrackingEndpoint() {
    if (isTrackingEndpoint != null) {
      return isTrackingEndpoint.booleanValue();
    }
    return false;
  }

  @Override
  public boolean isConfirmation() {
    if (isConfirmation != null) {
      return isConfirmation.booleanValue();
    }
    return false;
  }

  /**
   * @deprecated use {@link ExceptionHelper#wrapServiceException(Throwable)} or
   *             {@link ExceptionHelper#rethrowServiceException(Throwable)} instead.
   */
  @Deprecated
  @Removal(version = "3.9.0")
  protected static void rethrowServiceException(Throwable e) throws ServiceException {
    throw ExceptionHelper.wrapServiceException(e);
  }

  /**
   * <p>
   * Updates the state for the component <code>ComponentState</code>.
   * </p>
   */
  public void changeState(ComponentState newState) {
    serviceState = newState;
  }
  
  /**
   * <p>
   * Returns the last record <code>ComponentState</code>.
   * </p>
   * @return the current <code>ComponentState</code>
   */
  public ComponentState retrieveComponentState() {
    return serviceState;
  }
  
  /**
   * <p>
   * Request this component is init'd.
   * </p>
   * @throws CoreException wrapping any underlying Exceptions
   */
  public void requestInit() throws CoreException {
    serviceState.requestInit(this);
  }

  /**
   * <p>
   * Request this component is started.
   * </p>
   * @throws CoreException wrapping any underlying Exceptions
   */
  public void requestStart() throws CoreException {
    serviceState.requestStart(this);
  }

  /**
   * <p>
   * Request this component is stopped.
   * </p>
   */
  public void requestStop() {
    serviceState.requestStop(this);
  }

  /**
   * <p>
   * Request this component is closed.
   * </p>
   */
  public void requestClose() {
    serviceState.requestClose(this);
  }

  public String getLookupName() {
    return lookupName;
  }

  /**
   * Specify the lookup name (if required) when adding this service as a shared component.
   * <p>
   * If you don't know what to fill in here, leave it blank, and the adapter will use the unique-id instead.
   * </p>
   * 
   * @param lookupName the lookup name.
   */
  public void setLookupName(String lookupName) {
    this.lookupName = lookupName;
  }

}
