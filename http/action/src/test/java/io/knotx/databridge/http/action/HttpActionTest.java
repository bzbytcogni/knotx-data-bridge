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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HttpActionTest {

  @Test
  @DisplayName("Expect success transition when endpoint returned success status code")
  void expectSuccessTransitionWhenSuccessResponse() {

  }

  @Test
  @DisplayName("Expect fragment payload appended with endpoint result when endpoint responded with success status code and JSON body")
  void appendPayloadWhenEndpointResponseWithJsonObject() {
  }

  @Test
  @DisplayName("Expect fragment payload appended with endpoint result when endpoint responded with success status code and JSONArray body")
  void appendPayloadWhenEndpointResponseWithJsonArray() {
  }

  @Test
  @DisplayName("Expect fragment's body not modified when endpoint responded with OK and empty body")
  void fragmentsBodyNotModifiedWhenEmptyResponseBody() {
    //ToDo check if Fragment's body is still present
  }

  @Test
  @DisplayName("Expect response metadata in payload when endpoint returned success status code")
  void responseMetadataInPayloadWhenSuccessResponse() {

  }

  @Test
  @DisplayName("Expect response metadata in payload when endpoint returned error status code")
  void responseMetadataInPayloadWhenErrorResponse() {

  }

  @Test
  @DisplayName("Expect error transition when endpoint returned error status code")
  void errorTransitionWhenErrorStatusCode() {

  }

  @Test
  @DisplayName("Expect error transition when endpoint returned not valid JSON")
  void errorTransitionWhenResponseIsNotJson() {

  }

  @Test
  @DisplayName("Expect error transition when endpoint times out")
  void errorTransitionWhenEndpointTimesOut() {

  }

  @Test
  @DisplayName("Expect error transition when calling not existing endpoint")
  void errorTransitionWhenEndpointDoesNotExist() {

  }

  @Test
  @DisplayName("Expect headers from FragmentContext clientRequest are filtered and sent in endpoint request")
  void headersFromClientRequestFilteredAndSendToEndpoint() {

  }

  @Test
  @DisplayName("Expect additionalHeaders from EndpointOptions are sent in endpoint request")
  void additionalHeadersSentToEndpoint() {

  }

  @Test
  @DisplayName("Expect additionalHeaders override headers from FragmentContext clientRequest")
  void additionalHeadersOverrideClientRequestHeaders() {
  }

  @Test
  @DisplayName("Expect endpoint called with placeholders in path resolved with values from headers from FragmentContext clientRequest")
  void placeholdersInPathResolvedWithHeadersValues() {

  }

  @Test
  @DisplayName("Expect endpoint called with placeholders in path resolved with values from FragmentContext clientRequest query params")
  void placehodersInPathResolvedWithClientRequestQueryParams() {

  }
}