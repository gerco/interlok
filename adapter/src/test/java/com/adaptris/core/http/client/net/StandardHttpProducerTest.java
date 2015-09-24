package com.adaptris.core.http.client.net;

import static com.adaptris.core.http.jetty.JettyHelper.createChannel;
import static com.adaptris.core.http.jetty.JettyHelper.createConsumer;
import static com.adaptris.core.http.jetty.JettyHelper.createWorkflow;

import java.util.Arrays;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.Channel;
import com.adaptris.core.ConfiguredProduceDestination;
import com.adaptris.core.CoreConstants;
import com.adaptris.core.DefaultMessageFactory;
import com.adaptris.core.NullConnection;
import com.adaptris.core.ServiceList;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.StandaloneRequestor;
import com.adaptris.core.StandardWorkflow;
import com.adaptris.core.http.AdapterResourceAuthenticator;
import com.adaptris.core.http.HttpProducerExample;
import com.adaptris.core.http.MetadataContentTypeProvider;
import com.adaptris.core.http.client.ConfiguredRequestMethodProvider;
import com.adaptris.core.http.client.MetadataRequestMethodProvider;
import com.adaptris.core.http.client.RequestMethodProvider;
import com.adaptris.core.http.jetty.HashUserRealmProxy;
import com.adaptris.core.http.jetty.HttpConnection;
import com.adaptris.core.http.jetty.HttpConsumerTest;
import com.adaptris.core.http.jetty.JettyHelper;
import com.adaptris.core.http.jetty.MessageConsumer;
import com.adaptris.core.http.jetty.ResponseProducer;
import com.adaptris.core.http.jetty.SecurityConstraint;
import com.adaptris.core.http.server.HttpStatusProvider.HttpStatus;
import com.adaptris.core.metadata.RegexMetadataFilter;
import com.adaptris.core.services.metadata.PayloadFromMetadataService;
import com.adaptris.core.stubs.MockMessageProducer;

public class StandardHttpProducerTest extends HttpProducerExample {
  private static final String TEXT = "ABCDEFG";

  public StandardHttpProducerTest(String name) {
    super(name);
  }

  @Override
  protected void setUp() throws Exception {
  }

  public void testSetHandleRedirection() throws Exception {
    StandardHttpProducer p = new StandardHttpProducer();
    assertTrue(p.handleRedirection());
    p.setAllowRedirect(true);
    assertNotNull(p.getAllowRedirect());
    assertEquals(Boolean.TRUE, p.getAllowRedirect());
    assertTrue(p.handleRedirection());
    p.setAllowRedirect(false);
    assertNotNull(p.getAllowRedirect());
    assertEquals(Boolean.FALSE, p.getAllowRedirect());
    assertFalse(p.handleRedirection());
  }

  public void testSetIgnoreServerResponse() throws Exception {
    StandardHttpProducer p = new StandardHttpProducer();
    assertFalse(p.ignoreServerResponseCode());
    p.setIgnoreServerResponseCode(true);
    assertNotNull(p.getIgnoreServerResponseCode());
    assertEquals(Boolean.TRUE, p.getIgnoreServerResponseCode());
    assertTrue(p.ignoreServerResponseCode());
    p.setIgnoreServerResponseCode(false);
    assertNotNull(p.getIgnoreServerResponseCode());
    assertEquals(Boolean.FALSE, p.getIgnoreServerResponseCode());
    assertFalse(p.ignoreServerResponseCode());
  }


  public void testProduceWithContentTypeMetadata() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = HttpHelper.createAndStartChannel(mock);
    StandardHttpProducer stdHttp = new StandardHttpProducer(HttpHelper.createProduceDestination(c));
    stdHttp.setContentTypeProvider(new MetadataContentTypeProvider(HttpHelper.CONTENT_TYPE));
    StandaloneProducer producer = new StandaloneProducer(stdHttp);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(HttpHelper.CONTENT_TYPE, "text/complicated");
    try {
      c.requestStart();
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    }
    finally {
      HttpHelper.stopChannelAndRelease(c);
      stop(producer);
    }
    assertEquals(1, mock.messageCount());
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertTrue(m2.containsKey("Content-Type"));
    assertEquals("text/complicated", m2.getMetadataValue("Content-Type"));
  }


  public void testProduce_MetadataRequestHeaders() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = HttpHelper.createAndStartChannel(mock);
    StandardHttpProducer stdHttp = new StandardHttpProducer(HttpHelper.createProduceDestination(c));
    stdHttp.setRequestHeaderProvider(new MetadataRequestHeaders(new RegexMetadataFilter()));
    StandaloneProducer producer = new StandaloneProducer(stdHttp);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(getName(), getName());
    try {
      c.requestStart();
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    } finally {
      HttpHelper.stopChannelAndRelease(c);
      stop(producer);
    }
    assertEquals(1, mock.messageCount());
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertTrue(m2.containsKey(getName()));
    assertEquals(getName(), m2.getMetadataValue(getName()));
  }


  public void testProduce_WithMetadataMethod() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = HttpHelper.createConnection();
    MessageConsumer mc = createConsumer(HttpHelper.URL_TO_POST_TO);
    ServiceList sl = new ServiceList();
    PayloadFromMetadataService pms = new PayloadFromMetadataService();
    pms.setTemplate(TEXT);
    sl.add(pms);
    sl.add(new StandaloneProducer(new ResponseProducer(HttpStatus.OK_200)));
    Channel c = createChannel(jc, createWorkflow(mc, mock, sl));
    StandardWorkflow workflow = (StandardWorkflow) c.getWorkflowList().get(0);
    StandardHttpProducer stdHttp = new StandardHttpProducer(HttpHelper.createProduceDestination(c));
    stdHttp.setMethodProvider(new MetadataRequestMethodProvider("httpMethod"));
    StandaloneRequestor producer = new StandaloneRequestor(stdHttp);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage();
    msg.addMetadata("httpMethod", "get");
    try {
      start(c);
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    } finally {
      HttpHelper.stopChannelAndRelease(c);
      stop(producer);
    }
    assertEquals(1, mock.messageCount());
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("GET", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals(TEXT, msg.getStringPayload());
  }


  public void testRequest_GetMethod_ZeroBytes() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = HttpHelper.createConnection();
    MessageConsumer mc = createConsumer(HttpHelper.URL_TO_POST_TO);
    ServiceList sl = new ServiceList();
    PayloadFromMetadataService pms = new PayloadFromMetadataService();
    pms.setTemplate(TEXT);
    sl.add(pms);
    sl.add(new StandaloneProducer(new ResponseProducer(HttpStatus.OK_200)));
    Channel c = createChannel(jc, createWorkflow(mc, mock, sl));
    StandardWorkflow workflow = (StandardWorkflow) c.getWorkflowList().get(0);
    StandardHttpProducer stdHttp = new StandardHttpProducer(HttpHelper.createProduceDestination(c));
    stdHttp.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethodProvider.RequestMethod.GET));
    StandaloneRequestor producer = new StandaloneRequestor(stdHttp);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage();
    try {
      start(c);
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    }
    finally {
      HttpHelper.stopChannelAndRelease(c);
      stop(producer);
    }
    assertEquals(1, mock.messageCount());
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("GET", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals(TEXT, msg.getStringPayload());
  }

  public void testRequest_PostMethod_ZeroBytes() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = HttpHelper.createConnection();
    MessageConsumer mc = createConsumer(HttpHelper.URL_TO_POST_TO);
    Channel c = createChannel(jc, createWorkflow(mc, mock, new ServiceList()));
    StandardWorkflow workflow = (StandardWorkflow) c.getWorkflowList().get(0);
    PayloadFromMetadataService pms = new PayloadFromMetadataService();
    pms.setTemplate(TEXT);
    workflow.getServiceCollection().add(pms);
    workflow.getServiceCollection().add(new StandaloneProducer(new ResponseProducer(HttpStatus.OK_200)));
    StandardHttpProducer stdHttp = new StandardHttpProducer(HttpHelper.createProduceDestination(c));
    stdHttp.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethodProvider.RequestMethod.POST));
    StandaloneRequestor producer = new StandaloneRequestor(stdHttp);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage();
    try {
      start(c);
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    }
    finally {
      HttpHelper.stopChannelAndRelease(c);
      stop(producer);
    }
    assertEquals(1, mock.messageCount());
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("POST", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals(TEXT, msg.getStringPayload());
  }

  public void testRequest_EmptyReply() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = HttpHelper.createConnection();
    MessageConsumer mc = createConsumer(HttpHelper.URL_TO_POST_TO);
    Channel c = createChannel(jc, createWorkflow(mc, mock, new ServiceList()));
    StandardWorkflow workflow = (StandardWorkflow) c.getWorkflowList().get(0);
    ResponseProducer responder = new ResponseProducer(HttpStatus.OK_200);
    responder.setSendPayload(false);
    workflow.getServiceCollection().add(new StandaloneProducer(responder));
    StandardHttpProducer stdHttp = new StandardHttpProducer(HttpHelper.createProduceDestination(c));
    stdHttp.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethodProvider.RequestMethod.POST));
    StandaloneRequestor producer = new StandaloneRequestor(stdHttp);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    } finally {
      HttpHelper.stopChannelAndRelease(c);
      stop(producer);
    }
    assertEquals(1, mock.messageCount());
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("POST", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals(0, msg.getSize());
  }



  public void testRequest_MetadataResponseHeaders() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = HttpHelper.createAndStartChannel(mock);
    StandardHttpProducer stdHttp = new StandardHttpProducer(HttpHelper.createProduceDestination(c));
    stdHttp.setContentTypeProvider(new MetadataContentTypeProvider(HttpHelper.CONTENT_TYPE));
    stdHttp.setResponseHeaderHandler(new ResponseHeadersAsMetadata());
    StandaloneRequestor producer = new StandaloneRequestor(stdHttp);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(HttpHelper.CONTENT_TYPE, "text/complicated");
    assertFalse(msg.containsKey("Server"));
    try {
      c.requestStart();
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    } finally {
      HttpHelper.stopChannelAndRelease(c);
      stop(producer);
    }
    assertEquals(1, mock.messageCount());
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertTrue(m2.containsKey("Content-Type"));
    assertEquals("text/complicated", m2.getMetadataValue("Content-Type"));
    assertTrue(msg.containsKey("Server"));
  }

  public void testRequest_GetMethod_NonZeroBytes() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = HttpHelper.createConnection();
    MessageConsumer mc = createConsumer(HttpHelper.URL_TO_POST_TO);
    Channel c = createChannel(jc, createWorkflow(mc, mock, new ServiceList()));
    StandardWorkflow workflow = (StandardWorkflow) c.getWorkflowList().get(0);
    PayloadFromMetadataService pms = new PayloadFromMetadataService();
    pms.setTemplate(TEXT);
    workflow.getServiceCollection().add(pms);
    workflow.getServiceCollection().add(new StandaloneProducer(new ResponseProducer(HttpStatus.OK_200)));
    StandardHttpProducer stdHttp = new StandardHttpProducer(HttpHelper.createProduceDestination(c));
    stdHttp.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethodProvider.RequestMethod.GET));
    StandaloneRequestor producer = new StandaloneRequestor(stdHttp);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    }
    finally {
      stop(c);
      stop(producer);
    }
    assertEquals(1, mock.messageCount());
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("GET", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals(TEXT, msg.getStringPayload());
  }
  
  public void testRequest_GetMethod_NonZeroBytes_WithErrorResponse() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = HttpHelper.createConnection();
    jc.setSendServerVersion(true);
    MessageConsumer mc = createConsumer(HttpHelper.URL_TO_POST_TO);

    ServiceList services = new ServiceList();
    services.add(new PayloadFromMetadataService(TEXT));
    services.add(new StandaloneProducer(new ResponseProducer(HttpStatus.UNAUTHORIZED_401)));
    Channel c = createChannel(jc, createWorkflow(mc, mock, services));
    StandardHttpProducer stdHttp = new StandardHttpProducer(HttpHelper.createProduceDestination(c));
    stdHttp.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethodProvider.RequestMethod.GET));
    stdHttp.setIgnoreServerResponseCode(true);
    stdHttp.setResponseHeaderHandler(new ResponseHeadersAsMetadata("HTTP_"));
    StandaloneRequestor producer = new StandaloneRequestor(stdHttp);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      start(producer);
      producer.doService(msg); // msg will now contain the response!
      waitForMessages(mock, 1);
    }
    finally {
      stop(c);
      stop(producer);
    }
    
    assertEquals(1, mock.messageCount());
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("GET", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals(TEXT, msg.getStringPayload());
    assertEquals("401", msg.getMetadataValue(CoreConstants.HTTP_PRODUCER_RESPONSE_CODE));
    assertNotNull(msg.getMetadata("HTTP_Server"));
  }


  public void testProduce_WithUsernamePassword() throws Exception {
    String threadName = Thread.currentThread().getName();
    Thread.currentThread().setName(getName());
    HashUserRealmProxy hr = new HashUserRealmProxy();
    hr.setFilename(PROPERTIES.getProperty(HttpConsumerTest.JETTY_USER_REALM));

    SecurityConstraint securityConstraint = new SecurityConstraint();
    securityConstraint.setMustAuthenticate(true);
    securityConstraint.setRoles("user");

    hr.setSecurityConstraints(Arrays.asList(securityConstraint));
    HttpConnection jc = HttpHelper.createConnection();
    jc.setSecurityHandler(hr);
    MockMessageProducer mockProducer = new MockMessageProducer();
    MessageConsumer consumer = JettyHelper.createConsumer(HttpHelper.URL_TO_POST_TO);
    Channel channel = JettyHelper.createChannel(jc, consumer, mockProducer);

    StandardHttpProducer stdHttp = new StandardHttpProducer();
    stdHttp.setIgnoreServerResponseCode(true);
    stdHttp.registerConnection(new NullConnection());
    try {
      start(channel);
      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(TEXT);
      stdHttp.setUsername("user");
      stdHttp.setPassword("password");
      start(stdHttp);
      AdaptrisMessage reply = stdHttp.request(msg, HttpHelper.createProduceDestination(channel));
      waitForMessages(mockProducer, 1);
      assertEquals(TEXT, mockProducer.getMessages().get(0).getStringPayload());
    } finally {
      stop(stdHttp);
      HttpHelper.stopChannelAndRelease(channel);
      assertEquals(0, AdapterResourceAuthenticator.getInstance().currentAuthenticators().size());
      Thread.currentThread().setName(threadName);
    }
  }


  @Override
  protected Object retrieveObjectForSampleConfig() {
    StandardHttpProducer producer = new StandardHttpProducer(new ConfiguredProduceDestination("http://myhost.com/url/to/post/to"));

    producer.setUsername("username");
    producer.setPassword("password");

    StandaloneProducer result = new StandaloneProducer(producer);

    return result;
  }

}