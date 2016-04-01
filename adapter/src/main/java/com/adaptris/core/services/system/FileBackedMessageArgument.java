package com.adaptris.core.services.system;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.ServiceException;
import com.adaptris.core.lms.FileBackedMessage;

public class FileBackedMessageArgument implements CommandArgument {

  @Override
  public String retrieveValue(AdaptrisMessage msg) throws ServiceException {
    if(msg instanceof FileBackedMessage) {
      return ((FileBackedMessage)msg).currentSource().getAbsolutePath();
    }
    
    throw new ServiceException("Message is not a file backed message");
  }

}
