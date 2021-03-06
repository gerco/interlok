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


/**
 * 
 * Convenience for getting the default marshalling system currently available in the adapter.
 * 
 * 
 * @author gcsiki
 */
public class DefaultMarshaller {

	private static AdaptrisMarshaller marshaller;

  public static AdaptrisMarshaller getDefaultMarshaller() {
		if (marshaller == null) {
			// The default marshaller is xstream marshaller for now
			marshaller = new XStreamMarshaller();
		}
		return marshaller;
	}

	public static void setDefaultMarshaller(AdaptrisMarshaller _marshaller) {
		marshaller = _marshaller;
	}

  /**
   * Convenience method for null protection.
   * 
   * @param m the marshaller you think you want to use
   * @return the marshaller you passed in, or the default one.
   */
  public static AdaptrisMarshaller defaultIfNull(AdaptrisMarshaller m) {
    return m != null ? m : getDefaultMarshaller();
  }

  /**
   * Convenience method to roundtrip an object to text and back.
   * 
   * @param o the object
   * @return a copy of the object having been marshalled to text and back.
   * @throws CoreException on exception.
   */
  public static <T> T roundTrip(T o) throws CoreException {
    return (T) getDefaultMarshaller().unmarshal(getDefaultMarshaller().marshal(o));
  }
}
