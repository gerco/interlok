/*
 * Copyright 2015 Adaptris Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.adaptris.core;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.BooleanUtils;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.AutoPopulated;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.core.util.Args;
import com.adaptris.core.util.LifecycleHelper;
import com.adaptris.util.FifoMutexLock;
import com.adaptris.util.NumberUtils;

public abstract class AdaptrisPollingConsumer extends AdaptrisMessageConsumerImp {

  private static final int THERES_NO_LIMIT = Integer.MAX_VALUE - 1;
  @NotNull
  @AutoPopulated
  @Valid
  private Poller poller;
  @AdvancedConfig
  @InputFieldDefault(value = "false")
  private Boolean reacquireLockBetweenMessages;
  @AdvancedConfig
  @InputFieldDefault(value = "infinite")
  private Integer maxMessagesPerPoll;

  // make logging from FML configurable (default false) when util is released
  // transient
  private transient FifoMutexLock lock;

  public AdaptrisPollingConsumer() {
    changeState(ClosedState.getInstance());
    lock = new FifoMutexLock();
    setPoller(new FixedIntervalPoller());
  }

  /**
   * @see com.adaptris.core.AdaptrisComponent#init()
   */
  @Override
  public void init() throws CoreException {
    poller.registerConsumer(this);
    LifecycleHelper.init(poller);
  }

  /**
   * @see com.adaptris.core.AdaptrisComponent#start()
   */
  @Override
  public void start() throws CoreException {
    releaseLock(); // required when moving from stopped
    LifecycleHelper.start(poller);
  }

  /**
   * @see com.adaptris.core.AdaptrisComponent#stop()
   */
  @Override
  public void stop() {
    releaseLock();
    tryQuietly(() -> {
      lock.acquire();
      return true;
    });
    LifecycleHelper.stop(poller);
  }

  /**
   * @see com.adaptris.core.AdaptrisComponent#close()
   */
  @Override
  public void close() {
    LifecycleHelper.close(poller);
    releaseLock();
  }

  /**
   * Whether or not to continue processing messages.
   * 
   * <p>
   * Concrete sub-classes should call this after processing each message before they start processing the next one.
   * </p>
   * 
   * @return true if it's ok to carry on.
   * @see #setReacquireLockBetweenMessages(Boolean)
   * @see #setMaxMessagesPerPoll(Integer)
   */
  public final boolean continueProcessingMessages(int currentCount) {
    if (retrieveComponentState() != StartedState.getInstance()) {
      return false;
    }
    if (currentCount >= maxMessagesPerPoll()) {
      // we probably should stop, you're at 2^31 - 2 in the event that no max was specified.
      return false;
    }
    if (!reacquireLockBetweenMessages()) {
      return true;
    }
    return reacquireLock();
  }

  final boolean reacquireLock() {
    lock.release();
    return tryQuietly(() -> { return lock.attempt(0L); });
  }

  final boolean attemptLock() {
    return tryQuietly(() -> { return lock.attempt(0L); });
  }

  protected void releaseLock() {
    lock.release();
  }

  /**
   * <p>
   * Implemented by protocol-specific sub-classes.
   * </p>
   */
  protected abstract int processMessages();

  /**
   * <p>
   * Specify whether concrete sub-classes should attempt to reacquire the lock in between processing messages. Releasing then
   * attemtping to reqcquire the log gives other threads an opportunity to obtain the lock. This is significant in high volume
   * environments, particularly where messages are not processed in discreet batches e.g. <code>JmsPollingConsumer</code>.
   * </p>
   * 
   * @param b the lock flag
   */
  public void setReacquireLockBetweenMessages(Boolean b) {
    reacquireLockBetweenMessages = b;
  }

  /**
   * <p>
   * Get the reacquire lock flag.
   * </p>
   * 
   * @return true if the lock should be reacquired between messages
   * @see #setReacquireLockBetweenMessages(Boolean)
   */
  public Boolean getReacquireLockBetweenMessages() {
    return reacquireLockBetweenMessages;
  }

  private boolean reacquireLockBetweenMessages() {
    return BooleanUtils.toBooleanDefaultIfNull(getReacquireLockBetweenMessages(), false);
  }

  public Integer getMaxMessagesPerPoll() {
    return maxMessagesPerPoll;
  }

  /**
   * Set the maximum number of messages that should be processed in any one poll trigger.
   * <p>
   * It can be arbitrarily useful to limit the number of messages processed per poll. For instance, you are using
   * {@link QuartzCronPoller} and you need to ensure a time period when no activity occurs. In the event that a large number of
   * documents are ready to process just before the no-activity-time; then without a maximum the adapter will continue processing
   * until all documents are handled.
   * </p>
   * 
   * @param max the max messages per poll, default is infinite if not specified.
   */
  public void setMaxMessagesPerPoll(Integer max) {
    this.maxMessagesPerPoll = max;
  }

  private int maxMessagesPerPoll() {
    return NumberUtils.toIntDefaultIfNull(getMaxMessagesPerPoll(), THERES_NO_LIMIT);
  }

  public Poller getPoller() {
    return poller;
  }

  /**
   * Set the {@link Poller} to use.
   * 
   * @param s the poller
   */
  public void setPoller(Poller s) {
    poller = Args.notNull(s, "poller");
    poller.registerConsumer(this);
  }


  @Override
  public final void prepare() throws CoreException {
    getPoller().registerConsumer(this);
    LifecycleHelper.prepare(getPoller());
    registerEncoderMessageFactory();
    prepareConsumer();
  }

  protected abstract void prepareConsumer() throws CoreException;

  private static boolean tryQuietly(LockOperator l) {
    try {
      return l.doOperation();
    } catch (InterruptedException e) {
    }
    return false;
  }

  @FunctionalInterface
  protected interface LockOperator {
    boolean doOperation() throws InterruptedException;
  }

}
