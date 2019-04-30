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

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;

class EndpointResponse {

  private final HttpResponseStatus statusCode;
  private String statusMessage;
  private MultiMap headers = MultiMap.caseInsensitiveMultiMap();
  private Buffer body;

  EndpointResponse(HttpResponseStatus statusCode) {
    this.statusCode = statusCode;
  }

  static EndpointResponse fromHttpResponse(HttpResponse<Buffer> response) {
    EndpointResponse endpointResponse = new EndpointResponse(
        HttpResponseStatus.valueOf(response.statusCode()));
    endpointResponse.body = getResponsetBody(response);
    endpointResponse.headers = response.headers();
    endpointResponse.statusMessage = response.statusMessage();
    return endpointResponse;
  }


  public HttpResponseStatus getStatusCode() {
    return statusCode;
  }

  public MultiMap getHeaders() {
    return headers;
  }

  public Buffer getBody() {
    return body;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  @Override
  public String toString() {
    return "EndpointResponse{" +
        "statusCode=" + statusCode +
        ", statusMessage='" + statusMessage + '\'' +
        ", headers=" + headers +
        ", body=" + body +
        '}';
  }

  private static Buffer getResponsetBody(HttpResponse<Buffer> response) {
    if (response.body() != null) {
      return response.body();
    } else {
      return Buffer.buffer();
    }
  }
}
