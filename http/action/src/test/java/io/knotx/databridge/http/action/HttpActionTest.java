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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.knotx.databridge.http.action.HttpAction.TIMEOUT_TRANSITION;
import static io.knotx.fragments.handler.api.domain.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.handler.api.domain.FragmentResult.SUCCESS_TRANSITION;
import static org.junit.jupiter.api.Assertions.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.fragments.handler.api.domain.payload.ActionPayload;
import io.knotx.fragments.handler.api.domain.payload.ActionRequest;
import io.knotx.fragments.handler.api.domain.payload.ActionResponse;
import io.knotx.fragments.handler.api.domain.payload.ActionResponseError;
import io.knotx.server.api.context.ClientRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.MultiMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class HttpActionTest {

  private static final String VALID_REQUEST_PATH = "/valid-service";

  private static final String VALID_JSON_RESPONSE_BODY = "{ \"data\": \"service response\"}";

  private static final String VALID_JSON_ARRAY_RESPONSE_BODY = "[ \"first service response\", \" second service response\"]";

  private static final String VALID_EMPTY_RESPONSE_BODY = "";

  private static final Fragment FRAGMENT = new Fragment("type", new JsonObject(), "expectedBody");

  private static final String ACTION_ALIAS = "httpAction";

  private WireMockServer wireMockServer;

  @BeforeEach
  void setUp() {
    this.wireMockServer = new WireMockServer(options().dynamicPort());
    this.wireMockServer.start();
  }

  @Test
  @DisplayName("Expect success transition when endpoint returned success status code")
  void expectSuccessTransitionWhenSuccessResponse(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given, when
    HttpAction tested = successAction(vertx, VALID_JSON_RESPONSE_BODY);

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  @Test
  @DisplayName("Expect action alias key in fragment payload when endpoint responded with success status code")
  void appendActionAliasToPayload(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    HttpAction tested = successAction(vertx, VALID_JSON_RESPONSE_BODY);
    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap()
            .add("requestHeader", "request"), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> assertTrue(
            fragmentResult.getFragment()
                .getPayload()
                .containsKey(ACTION_ALIAS)),
        testContext);
  }

  @Test
  @DisplayName("Expect fragment payload appended with endpoint result when endpoint responded with success status code and JSON body")
  void appendPayloadWhenEndpointResponseWithJsonObject(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given, when
    HttpAction tested = successAction(vertx, VALID_JSON_RESPONSE_BODY);
    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap()
            .add("requestHeader", "request"), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT, fragmentResult -> {
      ActionPayload payload = new ActionPayload(
          fragmentResult.getFragment()
              .getPayload()
              .getJsonObject(ACTION_ALIAS));
      assertTrue(payload.getResponse()
          .isSuccess());
      assertEquals(new JsonObject(VALID_JSON_RESPONSE_BODY), payload.getResult());
    }, testContext);
  }

  @Test
  @DisplayName("Expect fragment payload appended with endpoint result when endpoint responded with success status code and JSONArray body")
  void appendPayloadWhenEndpointResponseWithJsonArrayVertxTestContext(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    HttpAction tested = successAction(vertx, VALID_JSON_ARRAY_RESPONSE_BODY);
    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap()
            .add("requestHeader", "request"), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT, fragmentResult -> {
      ActionPayload payload = new ActionPayload(
          fragmentResult.getFragment()
              .getPayload()
              .getJsonObject(ACTION_ALIAS));
      assertTrue(payload.getResponse()
          .isSuccess());
      assertEquals(new JsonArray(VALID_JSON_ARRAY_RESPONSE_BODY), payload.getResult());
    }, testContext);
  }

  @Test
  @DisplayName("Expect fragment's body not modified when endpoint responded with OK and empty body")
  void fragmentsBodyNotModifiedWhenEmptyResponseBody(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    HttpAction tested = successAction(vertx, VALID_EMPTY_RESPONSE_BODY);
    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap()
            .add("requestHeader", "request"), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> assertEquals(FRAGMENT.getBody(), fragmentResult.getFragment()
            .getBody()),
        testContext);
  }

  @Test
  @DisplayName("Expect response metadata in payload when endpoint returned success status code")
  void responseMetadataInPayloadWhenSuccessResponse(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    HttpAction tested = successAction(vertx, VALID_JSON_RESPONSE_BODY);
    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap()
            .add("requestHeader", "request"), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT, fragmentResult -> {
      ActionPayload payload = new ActionPayload(
          fragmentResult.getFragment()
              .getPayload()
              .getJsonObject(ACTION_ALIAS));
      ActionResponse response = payload.getResponse();
      assertNotNull(response);
      assertTrue(response.isSuccess());
      JsonObject metadata = response.getMetadata();
      assertNotNull(metadata);
      assertEquals("200", metadata.getString("statusCode"));
      assertEquals(new JsonArray().add("response"),
          metadata.getJsonObject("headers")
              .getJsonArray("responseHeader"));
    }, testContext);
  }

  @Test
  @DisplayName("Expect response metadata in payload when endpoint returned error status code")
  void responseMetadataInPayloadWhenErrorResponse(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    HttpAction tested = errorAction(vertx, 500, "Internal Error");

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT, fragmentResult -> {
      ActionPayload payload = new ActionPayload(
          fragmentResult.getFragment()
              .getPayload()
              .getJsonObject(ACTION_ALIAS));
      ActionResponse response = payload.getResponse();
      assertFalse(response.isSuccess());
      ActionResponseError error = response.getError();
      assertNotNull(error);
      assertEquals("500 Internal Server Error", error.getCode());
      assertEquals("Internal Error", error.getMessage());
      JsonObject metadata = response.getMetadata();
      assertNotNull(metadata);
      assertEquals("500", metadata.getString("statusCode"));
      assertEquals(new JsonArray().add("response"),
          metadata.getJsonObject("headers")
              .getJsonArray("responseHeader"));
    }, testContext);
  }

  @Test
  @DisplayName("Expect request metadata in payload when endpoint returned success status code")
  void requestMetadataInPayloadWhenSuccessResponse(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    HttpAction tested = successAction(vertx, VALID_JSON_RESPONSE_BODY);
    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap()
            .add("requestHeader", "request"), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT, fragmentResult -> {
      ActionPayload payload = new ActionPayload(
          fragmentResult.getFragment()
              .getPayload()
              .getJsonObject(ACTION_ALIAS));
      ActionRequest request = payload.getRequest();
      assertNotNull(request);
      assertEquals("HTTP", request.getType());
      assertEquals(VALID_REQUEST_PATH, request.getSource());
      JsonObject metadata = request.getMetadata();
      assertNotNull(metadata);
      assertEquals(new JsonArray().add("request"),
          metadata.getJsonObject("headers")
              .getJsonArray("requestHeader"));
    }, testContext);
  }

  @Test
  @DisplayName("Expect request metadata in payload when endpoint returned error status code")
  void requestMetadataInPayloadWhenErrorResponse(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    HttpAction tested = errorAction(vertx, 500, "Internal Error");

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap()
            .add("requestHeader", "request"), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT, fragmentResult -> {
      ActionPayload payload = new ActionPayload(
          fragmentResult.getFragment()
              .getPayload()
              .getJsonObject(ACTION_ALIAS));
      ActionRequest request = payload.getRequest();
      assertNotNull(request);
      assertEquals("HTTP", request.getType());
      assertEquals(VALID_REQUEST_PATH, request.getSource());
      JsonObject metadata = request.getMetadata();
      assertNotNull(metadata);
      assertEquals(new JsonArray().add("request"),
          metadata.getJsonObject("headers")
              .getJsonArray("requestHeader"));
    }, testContext);

  }

  @Test
  @DisplayName("Expect error transition when endpoint returned error status code")
  void errorTransitionWhenErrorStatusCode(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    HttpAction tested = errorAction(vertx, 500, "Internal Error");
    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap()
            .add("requestHeader", "request"), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> assertEquals(ERROR_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  @Test
  @DisplayName("Expect error transition when endpoint returned not valid JSON")
  void errorTransitionWhenResponseIsNotJson(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    HttpAction tested = successAction(vertx, "<html>Hello</html>");
    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap()
            .add("requestHeader", "request"), HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> assertEquals(ERROR_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  @Test
  @DisplayName("Expect response body for a raw response configured action")
  void successTransitionGivenConfiguredActionWhenResponseIsNotJson(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    String endpointPath = "/api/non-json";

    String responseBody = "<html>Hello</html>";
    wireMockServer.stubFor(get(urlEqualTo(endpointPath))
        .willReturn(aResponse().withBody(responseBody)));

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), endpointPath);

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath(endpointPath)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaderPatterns(Collections.singletonList(Pattern.compile(".*")));

    HttpAction tested = new HttpAction(vertx,
        new HttpActionOptions()
            .setEndpointOptions(endpointOptions)
            .setResponseType("raw"), ACTION_ALIAS);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> {
          assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition());
          JsonObject result = fragmentResult.getFragment().getPayload()
              .getJsonObject("httpAction").getJsonObject("_result");
          assertNotNull(result);
          assertEquals(new JsonObject().put("body", responseBody), result);
        }, testContext);
  }

  @Test
  @DisplayName("Expect response metadata transition for a raw response configured action")
  void validMetadataGivenConfiguredActionWhenResponseHasMetadata(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    String endpointPath = "/api/non-json";

    String responseBody = "<html>Hello</html>";
    wireMockServer.stubFor(get(urlEqualTo(endpointPath))
        .willReturn(aResponse().withBody(responseBody)
            .withHeader("Content-Type", "text/html;charset=utf-8")));

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), endpointPath);

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath(endpointPath)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaderPatterns(Collections.singletonList(Pattern.compile(".*")));

    HttpAction tested = new HttpAction(vertx,
        new HttpActionOptions()
            .setEndpointOptions(endpointOptions)
            .setResponseType("raw"), ACTION_ALIAS);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> {
          assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition());
          JsonObject result = fragmentResult.getFragment().getPayload()
              .getJsonObject("httpAction").getJsonObject("_result");
          assertNotNull(result);
          assertEquals(new JsonObject().put("body", responseBody)
              .put("contentType", "text/html")
              .put("encoding", "UTF-8"), result);
        }, testContext);
  }

  @Test
  @DisplayName("Expect partial metadata transition for a raw response configured action")
  void partialMetadataGivenConfiguredActionWhenResponseHasMetadata(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    String endpointPath = "/api/non-json";

    String responseBody = "<html>Hello</html>";
    wireMockServer.stubFor(get(urlEqualTo(endpointPath))
        .willReturn(aResponse().withBody(responseBody)
            .withHeader("Content-Type", "text/html")));

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), endpointPath);

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath(endpointPath)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaderPatterns(Collections.singletonList(Pattern.compile(".*")));

    HttpAction tested = new HttpAction(vertx,
        new HttpActionOptions()
            .setEndpointOptions(endpointOptions)
            .setResponseType("raw"), ACTION_ALIAS);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> {
          assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition());
          JsonObject result = fragmentResult.getFragment().getPayload()
              .getJsonObject("httpAction").getJsonObject("_result");
          assertNotNull(result);
          assertEquals(new JsonObject().put("body", responseBody)
              .put("contentType", "text/html"), result);
        }, testContext);
  }

  @Test
  @DisplayName("Expect proper JSON body when header Content-Type equal to application/json is given")
  void jsonBodyGivenWithApplicationJsonContentTypeHeader(VertxTestContext testContext, Vertx vertx) throws Throwable {
    String endpointPath = "/api/valid-application-json";
    String responseBody = "{\n" +
        "  \"id\": 21762532,\n" +
        "  \"url\": \"http://knotx.io\",\n" +
        "  \"label\": \"Product\"\n" +
        "}";
    Set<String> predicates = new HashSet<>();
    predicates.add("JSON");

    wireMockServer.stubFor(get(urlEqualTo(endpointPath))
       .willReturn(aResponse().withBody(responseBody)
           .withHeader("Content-Type", "application/json")));

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), endpointPath);

    EndpointOptions endpointOptions = new EndpointOptions();
    endpointOptions.setPath(endpointPath)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaderPatterns(Collections.singletonList(Pattern.compile(".*")));

    ResponseOptions responseOptions = new ResponseOptions()
        .setPredicates(predicates)
        .setForceJson(false);

    HttpActionOptions options = new HttpActionOptions()
        .setEndpointOptions(endpointOptions)
        .setResponseOptions(responseOptions);
    HttpAction tested = new HttpAction(vertx, options, ACTION_ALIAS);

    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> {
          assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition());
          JsonObject result = fragmentResult.getFragment().getPayload()
              .getJsonObject("httpAction").getJsonObject("_result");
          assertNotNull(result);
          assertEquals(new JsonObject()
              .put("id", 21762532)
              .put("url", "http://knotx.io")
              .put("label", "Product"), result);
        }, testContext);
  }

  @Test
  @DisplayName("Error expected when invalid JSON body provided when application/json in Content-Type header is given")
  void invalidJsonBodyGivenWithApplicationJsonContentTypeHeader(VertxTestContext testContext, Vertx vertx) throws Throwable {
    String endpointPath = "/api/invalid-json-json-header";
    String responseBody = "{\n" +
        "  id: 21762532,\n" +
        "  \"url\": \"http://knotx.io\",\n" +
        "  \"label\": \"Product\"\n";
    Set<String> predicates = new HashSet<>();
    predicates.add("JSON");

    wireMockServer.stubFor(get(urlEqualTo(endpointPath))
        .willReturn(aResponse().withBody(responseBody)
            .withHeader("Content-Type", "application/json")));

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), endpointPath);

    EndpointOptions endpointOptions = new EndpointOptions();
    endpointOptions.setPath(endpointPath)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaderPatterns(Collections.singletonList(Pattern.compile(".*")));

    ResponseOptions responseOptions = new ResponseOptions()
        .setPredicates(predicates)
        .setForceJson(false);

    HttpActionOptions options = new HttpActionOptions()
        .setEndpointOptions(endpointOptions)
        .setResponseOptions(responseOptions);
    HttpAction tested = new HttpAction(vertx, options, ACTION_ALIAS);

    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> {
          assertEquals(ERROR_TRANSITION, fragmentResult.getTransition());
        }, testContext);
  }

  @Test
  @DisplayName("Expected error transition when Content-Type not equal to application/json and forceJson set to true")
  void shouldFailWhenJsonForceAndInvalidJsonBodyAndNoApplicationJson(VertxTestContext testContext, Vertx vertx) throws Throwable {
    String endpointPath = "/api/invalid-json-force-json";
    String responseBody = "{\n" +
        "  id: 21762532,\n" +
        "  \"url\": \"http://knotx.io\",\n" +
        "  \"label\": \"Product\"\n";
    Set<String> predicates = new HashSet<>();
    predicates.add("JSON");

    wireMockServer.stubFor(get(urlEqualTo(endpointPath))
        .willReturn(aResponse().withBody(responseBody)
            .withHeader("Content-Type", "text/html")));

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), endpointPath);

    EndpointOptions endpointOptions = new EndpointOptions();
    endpointOptions.setPath(endpointPath)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaderPatterns(Collections.singletonList(Pattern.compile(".*")));

    ResponseOptions responseOptions = new ResponseOptions()
        .setPredicates(predicates)
        .setForceJson(true);

    HttpActionOptions options = new HttpActionOptions()
        .setEndpointOptions(endpointOptions)
        .setResponseOptions(responseOptions);
    HttpAction tested = new HttpAction(vertx, options, ACTION_ALIAS);

    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> {
          assertEquals(ERROR_TRANSITION, fragmentResult.getTransition());
        }, testContext);
  }

  @Test
  @DisplayName("Expected error transition when Content-Type not equal to application/json and JSON response predicate provided")
  void errorTransitionWhenJsonExpectedAndContentTypeNotEqualToApplicationJson(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    String endpointPath = "/api/not-application-json";
    String responseBody = "{\n" +
        "  \"id\": 21762532,\n" +
        "  \"url\": \"http://knotx.io\",\n" +
        "  \"label\": \"Product\"\n" +
        "}";
    Set<String> predicates = new HashSet<>();
    predicates.add("JSON");

    wireMockServer.stubFor(get(urlEqualTo(endpointPath))
        .willReturn(aResponse().withBody(responseBody)
            .withHeader("Content-Type", "text/html")));

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), endpointPath);

    EndpointOptions endpointOptions = new EndpointOptions();
    endpointOptions.setPath(endpointPath)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaderPatterns(Collections.singletonList(Pattern.compile(".*")));

    ResponseOptions responseOptions = new ResponseOptions()
        .setPredicates(predicates)
        .setForceJson(false);

    HttpActionOptions options = new HttpActionOptions()
        .setEndpointOptions(endpointOptions)
        .setResponseOptions(responseOptions);
    HttpAction tested = new HttpAction(vertx, options, ACTION_ALIAS);

    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> {
          assertEquals(ERROR_TRANSITION, fragmentResult.getTransition());
        }, testContext);

  }

  @Test
  @DisplayName("Expected valid JSON response when JSON response predicate given and forceJson set to true")
  void validJsonResponseWhenForceJsonAndNoContentType(VertxTestContext testContext, Vertx vertx) throws Throwable {
    String endpointPath = "/api/not-application-json";
    String responseBody = "{\n" +
        "  \"id\": 21762532,\n" +
        "  \"url\": \"http://knotx.io\",\n" +
        "  \"label\": \"Product\"\n" +
        "}";
    Set<String> predicates = new HashSet<>();
    predicates.add("JSON");

    wireMockServer.stubFor(get(urlEqualTo(endpointPath))
        .willReturn(aResponse().withBody(responseBody)
            .withHeader("Content-Type", "text/html")));

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), endpointPath);

    EndpointOptions endpointOptions = new EndpointOptions();
    endpointOptions.setPath(endpointPath)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaderPatterns(Collections.singletonList(Pattern.compile(".*")));

    ResponseOptions responseOptions = new ResponseOptions()
        .setPredicates(predicates)
        .setForceJson(true);

    HttpActionOptions options = new HttpActionOptions()
        .setEndpointOptions(endpointOptions)
        .setResponseOptions(responseOptions);
    HttpAction tested = new HttpAction(vertx, options, ACTION_ALIAS);

    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> {
          assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition());
          JsonObject result = fragmentResult.getFragment().getPayload()
              .getJsonObject("httpAction").getJsonObject("_result");
          assertNotNull(result);
          assertEquals(new JsonObject()
              .put("id", 21762532)
              .put("url", "http://knotx.io")
              .put("label", "Product"), result);
        }, testContext);
  }

  //todo proper json when no response predicates provided - by default as json
  //todo treat as json when forceJson set to true and no response predicate provided - proper json given
  //todo invalid json when forceJson set to true
  //todo treat as raw text when no response predicates provided

  @Test
  @DisplayName("Expect error transition when endpoint times out")
  void errorTransitionWhenEndpointTimesOut(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given, when
    int requestTimeoutMs = 1000;
    wireMockServer.stubFor(get(urlEqualTo(VALID_REQUEST_PATH))
        .willReturn(aResponse().withFixedDelay(2 * requestTimeoutMs)));

    ClientRequest clientRequest = new ClientRequest();
    clientRequest.setHeaders(MultiMap.caseInsensitiveMultiMap());

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath(VALID_REQUEST_PATH)
        .setDomain("localhost")
        .setPort(wireMockServer.port());

    HttpAction tested = new HttpAction(vertx,
        new HttpActionOptions()
            .setEndpointOptions(endpointOptions)
            .setRequestTimeoutMs(requestTimeoutMs), ACTION_ALIAS);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> assertEquals(TIMEOUT_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  @Test
  @DisplayName("Expect error transition when calling not existing endpoint")
  void errorTransitionWhenEndpointDoesNotExist(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap()
            .add("requestHeader", "request"), "not-existing-endpoint");

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath("not-existing-endpoint")
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaderPatterns(Collections.singletonList(Pattern.compile(".*")));

    HttpAction tested = new HttpAction(vertx,
        new HttpActionOptions().setEndpointOptions(endpointOptions), ACTION_ALIAS);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> assertEquals(ERROR_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  @Test
  @DisplayName("Expect headers from FragmentContext clientRequest are filtered and sent in endpoint request")
  void headersFromClientRequestFilteredAndSendToEndpoint(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    MultiMap clientRequestHeaders = MultiMap.caseInsensitiveMultiMap()
        .add("crHeaderKey", "crHeaderValue");
    HttpAction tested = getHttpActionWithAdditionalHeaders(vertx,
        null, "crHeaderKey", "crHeaderValue");

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        clientRequestHeaders, HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  @Test
  @DisplayName("Expect additionalHeaders from EndpointOptions are sent in endpoint request")
  void additionalHeadersSentToEndpoint(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    MultiMap clientRequestHeaders = MultiMap.caseInsensitiveMultiMap();
    JsonObject additionalHeaders = new JsonObject().put("additionalHeader", "additionalValue");
    HttpAction tested = getHttpActionWithAdditionalHeaders(vertx,
        additionalHeaders, "additionalHeader", "additionalValue");

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        clientRequestHeaders, HttpActionTest.VALID_REQUEST_PATH);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition()),
        testContext);

  }

  @Test
  @DisplayName("Expect additionalHeaders override headers from FragmentContext clientRequest")
  void additionalHeadersOverrideClientRequestHeaders(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    MultiMap clientRequestHeaders = MultiMap.caseInsensitiveMultiMap()
        .add("customHeader", "crHeaderValue");
    JsonObject additionalHeaders = new JsonObject().put("customHeader", "additionalValue");
    HttpAction tested = getHttpActionWithAdditionalHeaders(vertx,
        additionalHeaders, "customHeader", "additionalValue"
    );
    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        clientRequestHeaders, HttpActionTest.VALID_REQUEST_PATH);
    // then
    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  @Test
  @DisplayName("Expect endpoint called with placeholders in path resolved with values from FragmentContext clientRequest headers")
  void placeholdersInPathResolvedWithHeadersValues(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    MultiMap clientRequestHeaders = MultiMap.caseInsensitiveMultiMap()
        .add("bookId", "999000");
    String endpointPath = "/api/book/999000";
    String clientRequestPath = "/book-page";
    String optionsPath = "/api/book/{header.bookId}";

    wireMockServer.stubFor(get(urlEqualTo(endpointPath))
        .willReturn(aResponse().withBody(VALID_JSON_RESPONSE_BODY)));

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        clientRequestHeaders, clientRequestPath);

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath(optionsPath)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaderPatterns(Collections.singletonList(Pattern.compile(".*")));

    HttpAction tested = new HttpAction(vertx,
        new HttpActionOptions().setEndpointOptions(endpointOptions), ACTION_ALIAS);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  @Test
  @DisplayName("Expect endpoint called with placeholders in path resolved with values from FragmentContext clientRequest query params")
  void placeholdersInPathResolvedWithClientRequestQueryParams(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    MultiMap clientRequestParams = MultiMap.caseInsensitiveMultiMap()
        .add("bookId", "999000");
    String endpointPath = "/api/book/999000";
    String clientRequestPath = "/book-page";
    String optionsPath = "/api/book/{param.bookId}";

    wireMockServer.stubFor(get(urlEqualTo(endpointPath))
        .willReturn(aResponse().withBody(VALID_JSON_RESPONSE_BODY)));

    ClientRequest clientRequest = prepareClientRequest(clientRequestParams,
        MultiMap.caseInsensitiveMultiMap(), clientRequestPath);

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath(optionsPath)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaderPatterns(Collections.singletonList(Pattern.compile(".*")));

    HttpAction tested = new HttpAction(vertx,
        new HttpActionOptions().setEndpointOptions(endpointOptions), ACTION_ALIAS);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  @Test
  @DisplayName("Expect endpoint called with placeholders in path resolved with values from FragmentContext clientRequest request uri")
  void placeholdersInPathResolvedWithClientRequesUriParams(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    String endpointPath = "/api/thumbnail.png";
    String clientRequestPath = "/book.png";
    String optionsPath = "/api/thumbnail.{uri.extension}";

    wireMockServer.stubFor(get(urlEqualTo(endpointPath))
        .willReturn(aResponse().withBody(VALID_JSON_RESPONSE_BODY)));

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), clientRequestPath);

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath(optionsPath)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaderPatterns(Collections.singletonList(Pattern.compile(".*")));

    HttpAction tested = new HttpAction(vertx,
        new HttpActionOptions().setEndpointOptions(endpointOptions), ACTION_ALIAS);

    // then
    verifyExecution(tested, clientRequest, FRAGMENT,
        fragmentResult -> assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  @Test
  @DisplayName("Expect endpoint called with placeholders in payload resolved with values from FragmentContext clientRequest request uri")
  void placeholdersInPayloadResolvedWithClientRequesUriParams(VertxTestContext testContext,
      Vertx vertx) throws Throwable {
    // given, when
    String endpointPath = "/api/thumbnail.png";
    String clientRequestPath = "/book.png";
    String optionsPath = "/api/thumbnail.{payload.thumbnail.extension}";

    wireMockServer.stubFor(get(urlEqualTo(endpointPath))
        .willReturn(aResponse().withBody(VALID_JSON_RESPONSE_BODY)));

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), clientRequestPath);

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath(optionsPath)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaderPatterns(Collections.singletonList(Pattern.compile(".*")));

    HttpAction tested = new HttpAction(vertx,
        new HttpActionOptions().setEndpointOptions(endpointOptions), ACTION_ALIAS);

    // then
    verifyExecution(tested, clientRequest,
        FRAGMENT.appendPayload("thumbnail", new JsonObject().put("extension", "png")),
        fragmentResult -> assertEquals(SUCCESS_TRANSITION, fragmentResult.getTransition()),
        testContext);
  }

  private HttpAction successAction(Vertx vertx, String responseBody) {
    return getHttpAction(vertx, HttpActionTest.VALID_REQUEST_PATH, responseBody,
        HttpResponseStatus.OK.code(), null);
  }

  private HttpAction errorAction(Vertx vertx, int statusCode, String statusMessage) {
    return getHttpAction(vertx, HttpActionTest.VALID_REQUEST_PATH, null, statusCode, statusMessage);
  }

  private HttpAction getHttpAction(Vertx vertx, String requestPath, String responseBody,
      int statusCode, String statusMessage) {
    wireMockServer.stubFor(get(urlEqualTo(requestPath))
        .willReturn(aResponse()
            .withHeader("responseHeader", "response")
            .withBody(responseBody)
            .withStatus(statusCode)
            .withStatusMessage(statusMessage)));

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath(requestPath)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaders(Collections.singleton("requestHeader"));

    return new HttpAction(vertx,
        new HttpActionOptions().setEndpointOptions(endpointOptions), ACTION_ALIAS);
  }

  private HttpAction getHttpActionWithAdditionalHeaders(Vertx vertx,
      JsonObject additionalHeaders, String expectedHeaderKey, String expectedHeaderValue) {
    wireMockServer.stubFor(get(urlEqualTo(HttpActionTest.VALID_REQUEST_PATH))
        .withHeader(expectedHeaderKey, matching(expectedHeaderValue))
        .willReturn(aResponse()
            .withBody(VALID_JSON_RESPONSE_BODY)));

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath(HttpActionTest.VALID_REQUEST_PATH)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaderPatterns(Collections.singletonList(Pattern.compile(".*")))
        .setAdditionalHeaders(additionalHeaders);

    return new HttpAction(vertx,
        new HttpActionOptions().setEndpointOptions(endpointOptions), ACTION_ALIAS);
  }

  private void verifyExecution(HttpAction tested, ClientRequest clientRequest, Fragment fragment,
      Consumer<FragmentResult> assertions,
      VertxTestContext testContext) throws Throwable {
    tested.apply(new FragmentContext(fragment, clientRequest),
        testContext.succeeding(result -> {
          testContext.verify(() -> assertions.accept(result));
          testContext.completeNow();
        }));

    //then
    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  private ClientRequest prepareClientRequest(MultiMap clientRequestParams,
      MultiMap headers, String clientRequestPath) {
    ClientRequest clientRequest = new ClientRequest();
    clientRequest.setPath(clientRequestPath);
    clientRequest.setHeaders(headers);
    clientRequest.setParams(clientRequestParams);
    return clientRequest;
  }
}