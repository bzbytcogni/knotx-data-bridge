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
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.api.actionlog.ActionLogLevel;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.MultiMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class HttpActionNodeLogTest {

  private static final String ACTION_ALIAS = "httpAction";
  private static final String RAW_BODY = "<html>Hello</html>";
  private static final String APPLICATION_JSON = "application/json";
  private static final String APPLICATION_TEXT = "application/text";
  private static final String JSON = "JSON";
  private static final String JSON_BODY = "{\n" +
      "  \"id\": 21762532,\n" +
      "  \"url\": \"http://knotx.io\",\n" +
      "  \"label\": \"Product\"\n" +
      "}";
  private static final JsonObject EMPTY_JSON = new JsonObject();
  private WireMockServer wireMockServer;

  static Stream<Arguments> succeedingRequestsCasesWithLogLevels() {
    return Stream.of(
        Arguments.of(APPLICATION_JSON, false, null, JSON_BODY, ActionLogLevel.INFO),
        Arguments.of(APPLICATION_TEXT, true, null, JSON_BODY, ActionLogLevel.INFO),
        Arguments.of(APPLICATION_JSON, false, JSON, JSON_BODY, ActionLogLevel.INFO),
        Arguments.of(APPLICATION_JSON, false, null, JSON_BODY, ActionLogLevel.ERROR),
        Arguments.of(APPLICATION_TEXT, true, null, JSON_BODY, ActionLogLevel.ERROR),
        Arguments.of(APPLICATION_JSON, false, JSON, JSON_BODY, ActionLogLevel.ERROR)
    );
  }

  static Stream<Arguments> failingBodyDecodingCasesWithLogLevels() {
    return Stream.of( //Content-Type, forceJson, JSON predicate, Body
        Arguments.of(APPLICATION_JSON, false, null, RAW_BODY, ActionLogLevel.INFO),
        Arguments.of(APPLICATION_TEXT, true, null, RAW_BODY, ActionLogLevel.INFO),
        Arguments.of(APPLICATION_JSON, false, JSON, RAW_BODY, ActionLogLevel.INFO),
        Arguments.of(APPLICATION_JSON, false, null, RAW_BODY, ActionLogLevel.ERROR),
        Arguments.of(APPLICATION_TEXT, true, null, RAW_BODY, ActionLogLevel.ERROR),
        Arguments.of(APPLICATION_JSON, false, JSON, RAW_BODY, ActionLogLevel.ERROR)
    );
  }

  static Stream<Arguments> failingRequestsCasesWithLogLevels() {
    return Stream.of(
        Arguments.of(APPLICATION_TEXT, false, JSON, JSON_BODY, ActionLogLevel.INFO),
        Arguments.of(APPLICATION_TEXT, true, JSON, JSON_BODY, ActionLogLevel.INFO),
        Arguments.of(APPLICATION_TEXT, false, JSON, JSON_BODY, ActionLogLevel.ERROR),
        Arguments.of(APPLICATION_TEXT, true, JSON, JSON_BODY, ActionLogLevel.ERROR)
    );
  }

  @BeforeEach
  void setUp() {
    this.wireMockServer = new WireMockServer(options().dynamicPort());
    this.wireMockServer.start();
  }

  @AfterEach
  void tearDown() {
    this.wireMockServer.stop();
  }

  @ParameterizedTest(name = "expect success transition and action log should contain info level messages")
  @MethodSource("succeedingRequestsCasesWithLogLevels")
  void actionLogShouldContainSuccessfulLogsOnlyWhenInfoLogLevel(String contentType,
      boolean forceJson, String jsonPredicate, String responseBody, ActionLogLevel logLevel,
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    String endpointPath = "/api/info-action-log";

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), endpointPath);
    HttpAction tested = setupTestingInstances(vertx, endpointPath, responseBody, contentType,
        jsonPredicate, forceJson, logLevel);

    verifyExecution(tested, clientRequest, createFragment(), fragmentResult -> {
      assertNotNull(fragmentResult.getNodeLog().getMap().get("logs"));
      JsonObject logs = fragmentResult.getNodeLog().getJsonObject("logs");
      if (ActionLogLevel.INFO.equals(logLevel)) {
        assertNotNull(logs.getJsonObject("request"));
        assertNotNull(logs.getString("rawBody"));
        assertNotNull(logs.getJsonObject("response"));
        assertNull(logs.getJsonObject("error"));
        assertEquals(new JsonObject(JSON_BODY), logs.getJsonObject("result"));
        assertRequestLogs(logs.getJsonObject("request"));
        assertResponseLogs(logs.getJsonObject("response"));
      } else {
        assertTrue(logs.isEmpty());
      }
    }, testContext);
  }

  @ParameterizedTest(name = "Expect request, response, and error logs when decoding exception occurs")
  @MethodSource("failingBodyDecodingCasesWithLogLevels")
  void actionLogShouldContainRequestRawBodyAndError(String contentType, boolean forceJson,
      String jsonPredicate, String responseBody, ActionLogLevel logLevel,
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    String endpointPath = "/api/error-no-response";

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), endpointPath);
    HttpAction tested = setupTestingInstances(vertx, endpointPath, responseBody, contentType,
        jsonPredicate, forceJson, logLevel);

    verifyExecution(tested, clientRequest, new Fragment("type", EMPTY_JSON, "expectedBody"),
        fragmentResult -> {
          assertNotNull(fragmentResult.getNodeLog().getMap().get("logs"));
          JsonObject logs = fragmentResult.getNodeLog().getJsonObject("logs");
          assertNotNull(logs.getJsonObject("request"));
          if (ActionLogLevel.INFO.equals(logLevel)) {
            assertNotNull(logs.getString("rawBody"));
          }
          assertNotNull(logs.getJsonObject("response"));
          assertNotNull(logs.getJsonObject("error"));
          assertNull(logs.getJsonObject("result"));
          assertRequestLogs(logs.getJsonObject("request"));
          assertErrorLogs(logs.getJsonObject("error"));
          assertResponseLogs(logs.getJsonObject("response"));
        }, testContext);
  }

  @ParameterizedTest(name = "Expect request and error logs in node log when service fails")
  @MethodSource("failingRequestsCasesWithLogLevels")
  void actionLogShouldContainRequestAndErrorLogs(String contentType, boolean forceJson,
      String jsonPredicate, String responseBody, ActionLogLevel logLevel,
      VertxTestContext testContext, Vertx vertx) throws Throwable {
    String endpointPath = "/api/exception-no-response";

    ClientRequest clientRequest = prepareClientRequest(MultiMap.caseInsensitiveMultiMap(),
        MultiMap.caseInsensitiveMultiMap(), endpointPath);
    HttpAction tested = setupTestingInstances(vertx, endpointPath, responseBody, contentType,
        jsonPredicate, forceJson, logLevel);

    verifyExecution(tested, clientRequest, createFragment(), fragmentResult -> {
      assertNotNull(fragmentResult.getNodeLog().getJsonObject("logs"));
      JsonObject logs = fragmentResult.getNodeLog().getJsonObject("logs");
      assertNotNull(logs.getJsonObject("request"));
      assertNotNull(logs.getJsonObject("error"));
      assertNull(logs.getJsonObject("response"));
      assertNull(logs.getJsonObject("rawBody"));
      assertNull(logs.getJsonObject("result"));
      assertRequestLogs(logs.getJsonObject("request"));
      assertErrorLogs(logs.getJsonObject("error"));
    }, testContext);
  }

  private HttpAction setupTestingInstances(Vertx vertx, String endpointPath, String body,
      String contentType, String jsonPredicate, boolean forceJson, ActionLogLevel logLevel) {
    Set<String> predicates = new HashSet<>();
    wireMockServer.stubFor(get(urlEqualTo(endpointPath))
        .willReturn(aResponse().withBody(body)
            .withHeader("Content-Type", contentType)));

    EndpointOptions endpointOptions = new EndpointOptions()
        .setPath(endpointPath)
        .setDomain("localhost")
        .setPort(wireMockServer.port())
        .setAllowedRequestHeaderPatterns(Collections.singletonList(Pattern.compile(".*")));

    Optional.ofNullable(jsonPredicate).ifPresent(predicates::add);
    ResponseOptions responseOptions = new ResponseOptions()
        .setPredicates(predicates)
        .setForceJson(forceJson);

    return new HttpAction(vertx,
        new HttpActionOptions()
            .setEndpointOptions(endpointOptions)
            .setResponseOptions(responseOptions),
        ACTION_ALIAS, logLevel);
  }

  private void assertRequestLogs(JsonObject requestLog) {
    assertTrue(requestLog.containsKey("path"));
    assertTrue(requestLog.containsKey("requestHeaders"));
  }

  private void assertErrorLogs(JsonObject errorLog) {
    assertTrue(errorLog.containsKey("className"));
    assertTrue(errorLog.containsKey("message"));
  }

  private void assertResponseLogs(JsonObject responseLog) {
    assertTrue(responseLog.containsKey("httpVersion"));
    assertTrue(responseLog.containsKey("httpMethod"));
    assertTrue(responseLog.containsKey("statusCode"));
    assertTrue(responseLog.containsKey("statusMessage"));
    assertTrue(responseLog.containsKey("headers"));
    assertTrue(responseLog.containsKey("trailers"));
    assertTrue(responseLog.containsKey("requestPath"));
  }

  private void verifyExecution(HttpAction tested, ClientRequest clientRequest, Fragment fragment,
      Consumer<FragmentResult> successAssertions,
      VertxTestContext testContext) throws Throwable {
    tested.apply(new FragmentContext(fragment, clientRequest),
        testContext.succeeding(result -> {
          testContext.verify(() -> successAssertions.accept(result));
          testContext.completeNow();
        }));
    //then
    assertTrue(testContext.awaitCompletion(60, TimeUnit.SECONDS));
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

  private Fragment createFragment() {
    return new Fragment("type", EMPTY_JSON, "expectedBody");
  }

}
