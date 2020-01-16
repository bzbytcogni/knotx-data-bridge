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

import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.MultiMap;
import java.util.List;
import java.util.Map.Entry;

public class HttpResponseData {

  private static final String HTTP_VERSION_KEY = "httpVersion";
  private static final String STATUS_CODE_KEY = "statusCode";
  private static final String STATUS_MESSAGE_KEY = "statusMessage";
  private static final String HEADERS_KEY = "headers";
  private static final String TRAILERS_KEY = "trailers";

  private String httpVersion;
  private String statusCode;
  private String statusMessage;
  private MultiMap headers;
  private MultiMap trailers;

  public HttpResponseData(String httpVersion, String statusCode, String statusMessage,
      MultiMap headers, MultiMap tailers) {
    this.httpVersion = httpVersion;
    this.statusCode = statusCode;
    this.statusMessage = statusMessage;
    this.headers = headers;
    this.trailers = tailers;
  }

  public Object[] toLog() {
    return new Object[]{
        httpVersion,
        statusCode,
        statusMessage,
        multiMapToJson(headers.entries()),
        multiMapToJson(trailers.entries())
    };
  }

  private JsonObject multiMapToJson(List<Entry<String, String>> entries) {
    JsonObject json = new JsonObject();
    entries.forEach(e -> json.put(e.getKey(), e.getValue()));
    return json;
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put(HTTP_VERSION_KEY, httpVersion)
        .put(STATUS_CODE_KEY, statusCode)
        .put(STATUS_MESSAGE_KEY, statusMessage)
        .put(HEADERS_KEY, multiMapToJson(headers.entries()))
        .put(TRAILERS_KEY, multiMapToJson(trailers.entries()));
  }

  public String getHttpVersion() {
    return httpVersion;
  }

  public void setHttpVersion(String httpVersion) {
    this.httpVersion = httpVersion;
  }

  public String getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(String statusCode) {
    this.statusCode = statusCode;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  public MultiMap getHeaders() {
    return headers;
  }

  public void setHeaders(MultiMap headers) {
    this.headers = headers;
  }

  public MultiMap getTrailers() {
    return trailers;
  }

  public void setTrailers(MultiMap trailers) {
    this.trailers = trailers;
  }
}
