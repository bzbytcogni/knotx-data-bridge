/*
 * Copyright (C) 2019 Knot.x Project
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
package io.knotx.databridge.http.action;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * HTTP Action configuration
 */
@DataObject(generateConverter = true)
public class HttpActionOptions {

  private static final long DEFAULT_REQUEST_TIMEOUT = 0L;

  private WebClientOptions webClientOptions;
  private EndpointOptions endpointOptions;
  private ResponseOptions responseOptions;
  private long requestTimeoutMs;
  private String responseType = "json";

  public HttpActionOptions() {
    init();
  }

  public HttpActionOptions(JsonObject json) {
    init();
    HttpActionOptionsConverter.fromJson(json, this);
  }

  private void init() {
    webClientOptions = new WebClientOptions();
    responseOptions = new ResponseOptions();
    requestTimeoutMs = DEFAULT_REQUEST_TIMEOUT;
  }

  public WebClientOptions getWebClientOptions() {
    return webClientOptions;
  }

  /**
   * Set the {@code WebClientOptions} used by the HTTP client to communicate with remote http
   * endpoint. See https://vertx.io/docs/vertx-web-client/dataobjects.html#WebClientOptions for
   * the details what can be configured.
   *
   * @param webClientOptions {@link WebClientOptions} object
   * @return a reference to this, so the API can be used fluently
   */
  public HttpActionOptions setWebClientOptions(WebClientOptions webClientOptions) {
    this.webClientOptions = webClientOptions;
    return this;
  }

  public EndpointOptions getEndpointOptions() {
    return endpointOptions;
  }

  /**
   * Set the details of the remote http endpoint location.
   *
   * @param endpointOptions a {@link EndpointOptions} object
   * @return a reference to this, so the API can be used fluently
   */
  public HttpActionOptions setEndpointOptions(EndpointOptions endpointOptions) {
    this.endpointOptions = endpointOptions;
    return this;
  }

  public ResponseOptions getResponseOptions() {
    return responseOptions;
  }

  public HttpActionOptions setResponseOptions(ResponseOptions responseOptions) {
    this.responseOptions = responseOptions;
    return this;
  }

  public long getRequestTimeoutMs() {
    return requestTimeoutMs;
  }

  /**
   * Configures the amount of time in milliseconds after which if the request does not return any
   * data within, _timeout transition will be returned. Setting zero or a negative value disables
   * the timeout. By default it is set to {@code 0}.
   *
   * @param requestTimeoutMs - request timeout in milliseconds
   * @return a reference to this, so the API can be used fluently
   */
  public HttpActionOptions setRequestTimeoutMs(long requestTimeoutMs) {
    this.requestTimeoutMs = requestTimeoutMs;
    return this;
  }

  public String getResponseType() {
    return responseType;
  }

  /**
   * Configures the expected type of response the endpoint will return. Currently supported:
   * <ul>
   *   <li>
   *     {@code json}
   *     <p>
   *       The response is appended to `_response` key of action's payload as parsed {@link JsonObject JsonObject}/{@link io.vertx.core.json.JsonArray JsonArray}.
   *     </p>
   *   </li>
   *   <li>
   *     {@code raw}
   *     <p>
   *       The response is appended to `_response` key of action's payload as a {@link JsonObject JsonObject} in the following format:
   *       <pre>
   *         {
   *           "body": "<i>raw response body of the endpoint</i>",
   *           "contentType": "<i>{@code Content-Type} header value (if any)</i>",
   *           "encoding": "<i>encoding of the body (if could be determined)</i>
   *         }
   *       </pre>
   *     </p>
   *   </li>
   * </ul>
   * <p>
   * By default it is set to {@code json}.
   *
   * @param responseType - expected response type
   * @return a reference to this, so the API can be used fluently
   */
  public HttpActionOptions setResponseType(String responseType) {
    this.responseType = responseType;
    return this;
  }

  @Override
  public String toString() {
    return "HttpActionOptions{" +
        "webClientOptions=" + webClientOptions +
        ", endpointOptions=" + endpointOptions +
        ", responseOptions=" + responseOptions +
        ", requestTimeoutMs=" + requestTimeoutMs +
        '}';
  }
}
