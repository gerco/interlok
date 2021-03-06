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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adaptris.core.services.exception.ConfiguredException;
import com.adaptris.core.services.exception.ThrowExceptionService;
import com.adaptris.core.services.metadata.AddMetadataService;
import com.adaptris.core.services.metadata.PayloadFromMetadataService;
import com.adaptris.core.stubs.EventHandlerAwareConsumer;
import com.adaptris.core.stubs.EventHandlerAwareProducer;
import com.adaptris.core.stubs.EventHandlerAwareService;
import com.adaptris.core.stubs.MockChannel;
import com.adaptris.core.stubs.MockMessageProducer;
import com.adaptris.core.stubs.MockSkipProducerService;
import com.adaptris.core.stubs.MockWorkflowInterceptor;
import com.adaptris.core.util.Args;
import com.adaptris.core.util.MinimalMessageLogger;
import com.adaptris.util.TimeInterval;

@SuppressWarnings("deprecation")
public class StandardWorkflowTest extends ExampleWorkflowCase {

  private static final Logger log = LoggerFactory.getLogger(StandardWorkflowTest.class);

  protected static final String METADATA_KEY = "key1";
  protected static final String METADATA_VALUE = "value";

  public StandardWorkflowTest(java.lang.String testName) {
    super(testName);
  }

  @Override
  protected void setUp() {

  }

  public void testInitialiseWithEventAware() throws Exception {
    EventHandlerAwareProducer prod = new EventHandlerAwareProducer();
    EventHandlerAwareConsumer cons = new EventHandlerAwareConsumer();
    EventHandlerAwareService service = new EventHandlerAwareService();
    MockChannel channel = createChannel(prod, Arrays.asList(new Service[]
    {
      service
    }));
    ((WorkflowImp) channel.getWorkflowList().get(0)).setConsumer(cons);
    log.error("------------{}---------------", getName());
    channel.requestInit();
    EventHandler eh = channel.obtainEventHandler();
    log.error("Obtained [{}]", eh);
    log.error("------------{}---------------", getName());
    assertEquals(eh, prod.retrieveEventHandler());
    assertEquals(eh, cons.retrieveEventHandler());
    assertEquals(eh, service.retrieveEventHandler());
  }

  public void testObtainWorkflowIdWithNoChannelId() throws Exception {
    MockChannel channel = createChannel(new MockMessageProducer(), Arrays.asList(new Service[]
    {
        new AddMetadataService(Arrays.asList(new MetadataElement[]
        {
          new MetadataElement(METADATA_KEY, METADATA_VALUE)
        })), new PayloadFromMetadataService(PAYLOAD_2)
    }));
    StandardWorkflow workflow = (StandardWorkflow) channel.getWorkflowList().get(0);
    channel.setUniqueId(null);
    channel.prepare();
    StringBuffer result = new StringBuffer(workflow.getConsumer().getClass().getName());
    assertNotNull(workflow.getConsumer().getDestination());
    result.append("@");
    result.append(workflow.getProducer().getClass().getName());
    result.append("@");
    result.append(workflow.getConsumer().getDestination().getDestination());
    // This is now expected as we need to use both filter-expression
    // and configured Thread name as the unique identifier.
    result.append("-null");
    result.append("-null");
    assertEquals(result.toString(), workflow.obtainWorkflowId());
  }

  public void testObtainWorkflowIdWithNoUniqueId() throws Exception {
    MockChannel channel = createChannel(new MockMessageProducer(), Arrays.asList(new Service[]
    {
        new AddMetadataService(Arrays.asList(new MetadataElement[]
        {
          new MetadataElement(METADATA_KEY, METADATA_VALUE)
        })), new PayloadFromMetadataService(PAYLOAD_2)
    }));
    channel.setUniqueId("Channel");
    StandardWorkflow workflow = (StandardWorkflow) channel.getWorkflowList().get(0);
    channel.prepare();
    assertTrue(workflow.obtainWorkflowId().endsWith("Channel"));
  }

  public void testObtainWorkflowIdWithUniqueId() throws Exception {
    MockChannel channel = createChannel(new MockMessageProducer(), Arrays.asList(new Service[]
    {
        new AddMetadataService(Arrays.asList(new MetadataElement[]
        {
          new MetadataElement(METADATA_KEY, METADATA_VALUE)
        })), new PayloadFromMetadataService(PAYLOAD_2)
    }));
    channel.setUniqueId("Channel");
    StandardWorkflow workflow = (StandardWorkflow) channel.getWorkflowList().get(0);
    workflow.setUniqueId("Workflow");
    channel.prepare();
    assertEquals("Workflow@Channel", workflow.obtainWorkflowId());
  }

  public void testOnMessageWithSendEvents() throws Exception {
    MockMessageProducer producer = new MockMessageProducer();
    MockMessageProducer eventProd = new MockMessageProducer();
    DefaultEventHandler evtHandler = new DefaultEventHandler(eventProd);
    MockChannel channel = createChannel(producer, Arrays.asList(new Service[]
    {
        new AddMetadataService(Arrays.asList(new MetadataElement[]
        {
          new MetadataElement(METADATA_KEY, METADATA_VALUE)
        })), new PayloadFromMetadataService(PAYLOAD_2)
    }));
    try {
      channel.setEventHandler(evtHandler);
      StandardWorkflow workflow = (StandardWorkflow) channel.getWorkflowList().get(0);
      workflow.setSendEvents(true);
      channel.prepare();
      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(PAYLOAD_1);
      start(channel);
      workflow.onAdaptrisMessage(msg);
      assertEquals("Make sure all produced", 1, producer.getMessages().size());
      for (Iterator i = producer.getMessages().iterator(); i.hasNext();) {
        AdaptrisMessage m = (AdaptrisMessage) i.next();
        assertEquals(PAYLOAD_2, m.getContent());
        assertTrue("Contains correct metadata key", m.containsKey(METADATA_KEY));
        assertEquals(METADATA_VALUE, m.getMetadataValue(METADATA_KEY));
      }
      waitForMessages(eventProd, 1);
      assertEquals(1, eventProd.messageCount());
    }
    finally {
      stop(channel);
      stop(evtHandler);
    }
  }

  public void testOnMessageWithoutEvents() throws Exception {
    MockMessageProducer producer = new MockMessageProducer();
    MockMessageProducer eventProd = new MockMessageProducer();
    DefaultEventHandler evtHandler = new DefaultEventHandler(eventProd);
    MockChannel channel = createChannel(producer, Arrays.asList(new Service[]
    {
        new AddMetadataService(Arrays.asList(new MetadataElement[]
        {
          new MetadataElement(METADATA_KEY, METADATA_VALUE)
        })), new PayloadFromMetadataService(PAYLOAD_2)
    }));
    try {
      channel.setEventHandler(evtHandler);
      StandardWorkflow workflow = (StandardWorkflow) channel.getWorkflowList().get(0);
      workflow.setSendEvents(false);
      channel.prepare();
      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(PAYLOAD_1);
      start(channel);
      workflow.onAdaptrisMessage(msg);
      assertEquals("Make sure all produced", 1, producer.getMessages().size());
      for (Iterator i = producer.getMessages().iterator(); i.hasNext();) {
        AdaptrisMessage m = (AdaptrisMessage) i.next();
        assertEquals(PAYLOAD_2, m.getContent());
        assertTrue("Contains correct metadata key", m.containsKey(METADATA_KEY));
        assertEquals(METADATA_VALUE, m.getMetadataValue(METADATA_KEY));
      }
      waitForMessages(eventProd, 0);
      assertEquals(0, eventProd.messageCount());
    }
    finally {
      stop(channel);
      stop(evtHandler);
    }
  }

  public void testOnMessageWithInterceptors() throws Exception {
    MockWorkflowInterceptor interceptor = new MockWorkflowInterceptor();
    MockMessageProducer producer = new MockMessageProducer();
    MockChannel channel = createChannel(producer, Arrays.asList(new Service[]
    {
        new AddMetadataService(Arrays.asList(new MetadataElement[]
        {
          new MetadataElement(METADATA_KEY, METADATA_VALUE)
        })), new PayloadFromMetadataService(PAYLOAD_2)
    }));
    int count = 10;
    try {
      StandardWorkflow workflow = (StandardWorkflow) channel.getWorkflowList().get(0);
      workflow.addInterceptor(interceptor);
      channel.prepare();
      start(channel);
      for (int i = 0; i < count; i++) {
        AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(PAYLOAD_1);
        workflow.onAdaptrisMessage(msg);
      }
      assertEquals("Make sure all produced", count, producer.messageCount());
      assertEquals("Make sure all intercepted", count, interceptor.messageCount());
      for (AdaptrisMessage m : producer.getMessages()) {
        assertEquals(PAYLOAD_2, m.getContent());
        assertTrue("Contains correct metadata key", m.containsKey(METADATA_KEY));
        assertEquals(METADATA_VALUE, m.getMetadataValue(METADATA_KEY));
      }
    }
    finally {
      stop(channel);
    }

  }

  public void testHandleChannelUnavailable() throws Exception {
    MockMessageProducer producer = new MockMessageProducer();
    final MockChannel channel = createChannel(producer, Arrays.asList(new Service[]
    {
        new AddMetadataService(Arrays.asList(new MetadataElement[]
        {
          new MetadataElement(METADATA_KEY, METADATA_VALUE)
        })), new PayloadFromMetadataService(PAYLOAD_2)
    }));
    try {
      StandardWorkflow workflow = (StandardWorkflow) channel.getWorkflowList().get(0);
      workflow.setChannelUnavailableWaitInterval(new TimeInterval(1200L, TimeUnit.MILLISECONDS));
      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(PAYLOAD_1);
      channel.prepare();
      start(channel);
      channel.toggleAvailability(false);
      Timer t = new Timer(true);
      t.schedule(new TimerTask() {
        @Override
        public void run() {
          channel.toggleAvailability(true);
        }

      }, 500);
      workflow.onAdaptrisMessage(msg);

      assertEquals("Make sure all produced", 1, producer.getMessages().size());
      for (Iterator i = producer.getMessages().iterator(); i.hasNext();) {
        AdaptrisMessage m = (AdaptrisMessage) i.next();
        assertEquals(PAYLOAD_2, m.getContent());
        assertTrue("Contains correct metadata key", m.containsKey(METADATA_KEY));
        assertEquals(METADATA_VALUE, m.getMetadataValue(METADATA_KEY));
      }
    }
    finally {
      stop(channel);
    }
  }

  public void testHandleChannelUnavailableForever() throws Exception {
    MockMessageProducer producer = new MockMessageProducer();
    final MockChannel channel = createChannel(producer, Arrays.asList(new Service[]
    {
        new AddMetadataService(Arrays.asList(new MetadataElement[]
        {
          new MetadataElement(METADATA_KEY, METADATA_VALUE)
        })), new PayloadFromMetadataService(PAYLOAD_2)
    }));
    MockMessageProducer meh = new MockMessageProducer();

    try {
      StandardWorkflow workflow = (StandardWorkflow) channel.getWorkflowList().get(0);
      workflow.setMessageErrorHandler(new StandardProcessingExceptionHandler(
    		  new ServiceList(new ArrayList<Service>(Arrays.asList(new Service[]
    	    	      {
    	    	        new StandaloneProducer(meh)
    	    	      })))));
      workflow.setChannelUnavailableWaitInterval(new TimeInterval(100L, TimeUnit.MILLISECONDS));

      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(PAYLOAD_1);
      channel.prepare();
      start(channel);
      channel.toggleAvailability(false);
      workflow.onAdaptrisMessage(msg);
      assertEquals("Make none produced", 0, producer.getMessages().size());
      assertEquals(1, meh.getMessages().size());
      for (Iterator i = meh.getMessages().iterator(); i.hasNext();) {
        AdaptrisMessage m = (AdaptrisMessage) i.next();
        assertEquals(PAYLOAD_1, m.getContent());
        assertFalse("Does not contains correct metadata key", m.containsKey(METADATA_KEY));
      }
    }
    finally {
      stop(channel);
    }
  }

  public void testServiceException() throws Exception {
    MockMessageProducer producer = new MockMessageProducer();
    MockMessageProducer meh = new MockMessageProducer();
    MockChannel channel = createChannel(producer, Arrays.asList(new Service[]
    {
      new ThrowExceptionService(new ConfiguredException("Fail"))
    }));
    try {
      StandardWorkflow workflow = (StandardWorkflow) channel.getWorkflowList().get(0);
      channel.setMessageErrorHandler(new StandardProcessingExceptionHandler(
    		  new ServiceList(new ArrayList<Service>(Arrays.asList(new Service[]
    	    	      {
    	    	        new StandaloneProducer(meh)
    	    	      })))));
      channel.prepare();

      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(PAYLOAD_1);
      start(channel);
      workflow.onAdaptrisMessage(msg);

      assertEquals("Make none produced", 0, producer.getMessages().size());
      assertEquals(1, meh.getMessages().size());
      for (Iterator i = meh.getMessages().iterator(); i.hasNext();) {
        AdaptrisMessage m = (AdaptrisMessage) i.next();
        assertEquals(PAYLOAD_1, m.getContent());
        assertFalse("Does not contains correct metadata key", m.containsKey(METADATA_KEY));
        assertNotNull(m.getObjectHeaders().get(CoreConstants.OBJ_METADATA_EXCEPTION));
        assertNotNull(m.getObjectHeaders().get(CoreConstants.OBJ_METADATA_EXCEPTION_CAUSE));
        assertEquals(ThrowExceptionService.class.getSimpleName(),
            m.getObjectHeaders().get(CoreConstants.OBJ_METADATA_EXCEPTION_CAUSE));
      }
    }
    finally {
      stop(channel);
    }
  }

  public void testProduceException() throws Exception {
    MockMessageProducer producer = new MockMessageProducer() {
      @Override
      public void produce(AdaptrisMessage msg) throws ProduceException {
        throw new ProduceException();
      }

      @Override
      public void produce(AdaptrisMessage msg, ProduceDestination destination) throws ProduceException {
        throw new ProduceException();
      }
    };
    MockMessageProducer meh = new MockMessageProducer();
    MockChannel channel = createChannel(producer, Arrays.asList(new Service[]
    {
      new ThrowExceptionService(new ConfiguredException("Fail"))
    }));
    try {
      StandardWorkflow workflow = (StandardWorkflow) channel.getWorkflowList().get(0);
      channel.setMessageErrorHandler(new StandardProcessingExceptionHandler(
    		  new ServiceList(new ArrayList<Service>(Arrays.asList(new Service[]
    	    	      {
    	    	        new StandaloneProducer(meh)
    	    	      })))));
      channel.prepare();

      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(PAYLOAD_1);
      start(channel);
      workflow.onAdaptrisMessage(msg);

      assertEquals("Make none produced", 0, producer.getMessages().size());
      assertEquals(1, meh.getMessages().size());
      for (Iterator i = meh.getMessages().iterator(); i.hasNext();) {
        AdaptrisMessage m = (AdaptrisMessage) i.next();
        assertEquals(PAYLOAD_1, m.getContent());
        assertFalse("Does not contains correct metadata key", m.containsKey(METADATA_KEY));
      }
    }
    finally {
      stop(channel);
    }
  }

  public void testRuntimeException() throws Exception {
    MockMessageProducer producer = new MockMessageProducer() {
      @Override
      public void produce(AdaptrisMessage msg) throws ProduceException {
        throw new RuntimeException();
      }

      @Override
      public void produce(AdaptrisMessage msg, ProduceDestination destination) throws ProduceException {
        throw new RuntimeException();
      }
    };
    MockMessageProducer meh = new MockMessageProducer();
    MockChannel channel = createChannel(producer, Arrays.asList(new Service[]
    {
        new AddMetadataService(Arrays.asList(new MetadataElement[]
        {
          new MetadataElement(METADATA_KEY, METADATA_VALUE)
        })), new PayloadFromMetadataService(PAYLOAD_2)
    }));
    try {
      StandardWorkflow workflow = (StandardWorkflow) channel.getWorkflowList().get(0);
      channel.setMessageErrorHandler(new StandardProcessingExceptionHandler(
    		  new ServiceList(new ArrayList<Service>(Arrays.asList(new Service[]
    	    	      {
    	    	        new StandaloneProducer(meh)
    	    	      })))));
      channel.prepare();

      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(PAYLOAD_1);
      start(channel);
      workflow.onAdaptrisMessage(msg);

      assertEquals("Make none produced", 0, producer.getMessages().size());
      assertEquals(1, meh.getMessages().size());
      for (Iterator i = meh.getMessages().iterator(); i.hasNext();) {
        AdaptrisMessage m = (AdaptrisMessage) i.next();
        assertEquals(PAYLOAD_1, m.getContent());
        assertFalse("Does not contains correct metadata key", m.containsKey(METADATA_KEY));
      }
    }
    finally {
      stop(channel);
    }
  }

  public void testOnMessage_SkipProducer() throws Exception {
    MockMessageProducer producer = new MockMessageProducer();
    MockMessageProducer serviceProducer = new MockMessageProducer();
    MockChannel channel = createChannel(producer, Arrays.asList(
        new Service[] {new StandaloneProducer(serviceProducer), new MockSkipProducerService()}));
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(PAYLOAD_1);
    StandardWorkflow workflow = (StandardWorkflow) channel.getWorkflowList().get(0);
    try {
      start(channel);
      workflow.onAdaptrisMessage(msg);
      assertEquals(1, serviceProducer.messageCount());
      assertEquals(0, producer.messageCount());
    } finally {
      stop(channel);
    }
  }


  public void testOnMessage_LogPayload() throws Exception {
    MockMessageProducer producer = new MockMessageProducer();
    MockChannel channel = createChannel(producer, Arrays.asList(new Service[] {new NullService()}));
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(PAYLOAD_1);
    StandardWorkflow workflow = (StandardWorkflow) channel.getWorkflowList().get(0);
    workflow.setLogPayload(true);
    try {
      start(channel);
      workflow.onAdaptrisMessage(msg);
      assertEquals(1, producer.messageCount());
    } finally {
      stop(channel);
    }
  }

  public void testOnMessage_MessageLogger() throws Exception {
    MockMessageProducer producer = new MockMessageProducer();
    MockChannel channel = createChannel(producer, Arrays.asList(new Service[] {new NullService()}));
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(PAYLOAD_1);
    StandardWorkflow workflow = (StandardWorkflow) channel.getWorkflowList().get(0);
    workflow.setMessageLogger(new MinimalMessageLogger());
    try {
      start(channel);
      workflow.onAdaptrisMessage(msg);
      assertEquals(1, producer.messageCount());
    } finally {
      stop(channel);
    }
  }


  public void testOnMessage_withConsumeLocation() throws Exception {
    MockMessageProducer producer = new MockMessageProducer();
    MockChannel channel = createChannel(producer, Arrays.asList(new Service[] {new NullService()}));
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(PAYLOAD_1);
    msg.addMessageHeader(getName(), "hello world");
    StandardWorkflow workflow = (StandardWorkflow) channel.getWorkflowList().get(0);
    workflow.setConsumer(new ConsumerWithLocation(getName()));
    try {
      start(channel);
      workflow.onAdaptrisMessage(msg);
      AdaptrisMessage consumed = producer.getMessages().get(0);
      assertTrue(consumed.headersContainsKey(CoreConstants.MESSAGE_CONSUME_LOCATION));
      assertEquals("hello world",
          consumed.getMetadataValue(CoreConstants.MESSAGE_CONSUME_LOCATION));
    } finally {
      stop(channel);
    }
  }

  public void testOnMessage_withConsumeLocation_NoMatch() throws Exception {
    MockMessageProducer producer = new MockMessageProducer();
    MockChannel channel = createChannel(producer, Arrays.asList(new Service[] {new NullService()}));
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(PAYLOAD_1);
    StandardWorkflow workflow = (StandardWorkflow) channel.getWorkflowList().get(0);
    workflow.setConsumer(new ConsumerWithLocation(getName()));
    try {
      start(channel);
      workflow.onAdaptrisMessage(msg);
      AdaptrisMessage consumed = producer.getMessages().get(0);
      assertFalse(consumed.headersContainsKey(CoreConstants.MESSAGE_CONSUME_LOCATION));
    } finally {
      stop(channel);
    }
  }

  @Override
  protected Object retrieveObjectForSampleConfig() {
    Channel c = new Channel();
    c.setUniqueId(UUID.randomUUID().toString());
    StandardWorkflow wf1 = createWorkflowForExampleConfig();
    wf1.setUniqueId("Unthrottled-Workflow");
    c.getWorkflowList().add(wf1);
    return c;
  }


  protected MockChannel createChannel(AdaptrisMessageProducer producer, List<Service> services) throws Exception {
    MockChannel channel = new MockChannel();
    StandardWorkflow workflow = createWorkflowForGenericTests();
    workflow.getConsumer().setDestination(new ConfiguredConsumeDestination("dummy"));
    workflow.setProducer(producer);
    workflow.getServiceCollection().addAll(services);
    channel.getWorkflowList().add(workflow);
    return channel;
  }

  @Override
  protected String createBaseFileName(Object object) {
    return StandardWorkflow.class.getName();
  }

  @Override
  protected StandardWorkflow createWorkflowForGenericTests() {
    return new StandardWorkflow();
  }

  private StandardWorkflow createWorkflowForExampleConfig() {
    StandardWorkflow wf = new StandardWorkflow();
    ConfiguredConsumeDestination dest = new ConfiguredConsumeDestination("dummy-consume-destination");
    NullMessageConsumer consumer = new NullMessageConsumer();
    consumer.setDestination(dest);
    wf.setConsumer(consumer);
    wf.setProducer(new NullMessageProducer());
    return wf;
  }

  private class ConsumerWithLocation extends NullMessageConsumer {
    private String metadataKey;

    public ConsumerWithLocation(String key) {
      metadataKey = Args.notBlank(key, "metadataKey");
    }

    @Override
    public String consumeLocationKey() {
      return metadataKey;
    }
  }
}
