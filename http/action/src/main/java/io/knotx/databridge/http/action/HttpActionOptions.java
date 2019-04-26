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

import io.knotx.databridge.http.action.common.configuration.EndpointOptions;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import java.util.Objects;

@DataObject(generateConverter = true)
public class HttpActionOptions {

  private WebClientOptions webClientOptions;
  private EndpointOptions endpointOptions;

  /**
   * Default constructor
   */
  public HttpActionOptions() {
    init();
  }

  public HttpActionOptions(JsonObject json) {
    init();
    HttpActionOptionsConverter.fromJson(json, this);
  }

  private void init() {
    webClientOptions = new WebClientOptions();
  }

  public WebClientOptions getWebClientOptions() {
    return webClientOptions;
  }

  public HttpActionOptions setWebClientOptions(
      WebClientOptions webClientOptions) {
    this.webClientOptions = webClientOptions;
    return this;
  }

  public EndpointOptions getEndpointOptions() {
    return endpointOptions;
  }

  public HttpActionOptions setEndpointOptions(
      EndpointOptions endpointOptions) {
    this.endpointOptions = endpointOptions;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HttpActionOptions that = (HttpActionOptions) o;
    return Objects.equals(webClientOptions, that.webClientOptions) &&
        Objects.equals(endpointOptions, that.endpointOptions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(webClientOptions, endpointOptions);
  }

  @Override
  public String toString() {
    return "HttpActionOptions{" +
        "webClientOptions=" + webClientOptions +
        ", endpointOptions=" + endpointOptions +
        '}';
  }
}
