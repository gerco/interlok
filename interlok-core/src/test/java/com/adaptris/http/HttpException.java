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

package com.adaptris.http;

/**
 * <p>
 * Root of all custom <code>Exception</code>s in the <code>http</code> package
 * and sub-packages.
 * </p>
 */
public class HttpException extends Exception {

  /**
   * <code>serialVersionUID</code>
   */
  private static final long serialVersionUID = -2006020601L;


  /**
   * <p>
   * Creates a new instance.
   * </p>
   */
  public HttpException() { }


  /**
   * <p>
   * Creates a new instance with a reference to a previous
   * <code>Throwable</code>.
   * </p>
   * @param cause a previous, causal <code>Throwable</code>
   */
  public HttpException(Throwable cause) {
    super(cause);
  }


  /**
   * <p>
   * Creates a new instance with a description of the <code>Exception</code>.
   * </p>
   * @param description description of the <code>Exception</code>
   */
  public HttpException(String description) {
    super(description);
  }


  /**
   * <p>
   * Creates a new instance with a reference to a previous
   * <code>Throwable</code> and a description of the <code>Exception</code>.
   * </p>
   * @param description of the <code>Exception</code>
   * @param cause previous <code>Throwable</code>
   */
  public HttpException(String description, Throwable cause) {
    super(description, cause);
  }
} 
