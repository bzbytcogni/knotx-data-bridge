/*
 * Copyright (C) 2018 Knot.x Project
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
package io.knotx.databridge.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.knotx.junit5.util.RequestUtil.subscribeToResult_shouldFail;
import static io.knotx.junit5.util.RequestUtil.subscribeToResult_shouldSucceed;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.common.collect.Lists;
import io.knotx.databridge.api.DataSourceAdapterRequest;
import io.knotx.databridge.http.common.configuration.HttpDataSourceAdapterOptions;
import io.knotx.databridge.http.common.configuration.HttpDataSourceSettings;
import io.knotx.databridge.http.common.exception.UnsupportedDataSourceException;
import io.knotx.databridge.http.common.http.HttpClientFacade;
import io.knotx.junit5.util.FileReader;
import io.knotx.server.api.context.ClientRequest;
import io.knotx.server.api.context.ClientResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class HttpClientFacadeTest {

  // Configuration
  private static final String DOMAIN = "localhost";
  private static final String PATH = "/services/mock.*";

  // Request payload
  private static final String REQUEST_PATH = "/services/mock/response.json";
  private static final List<Pattern> PATTERNS = Collections
      .singletonList(Pattern.compile("X-test*"));

  @Test
  void whenSupportedStaticPathServiceRequested_expectRequestExecutedAndResponseOKWithBody(
      VertxTestContext context, Vertx vertx) throws Exception {
    // given
    final WebClient mockedWebClient = Mockito.spy(webClient(vertx));
    final JsonObject expectedResponse = new JsonObject(FileReader.readText("service/mock/response.json"));
    final WireMockServer wireMockServer = mockEndpoint(HttpResponseStatus.OK.code(), REQUEST_PATH,
        expectedResponse.encode());
    HttpClientFacade clientFacade = new HttpClientFacade(mockedWebClient,
        getConfiguration(wireMockServer.port()));

    // when
    Single<ClientResponse> result = clientFacade
        .process(payloadMessage(REQUEST_PATH, new ClientRequest()), HttpMethod.GET);

    // then
    subscribeToResult_shouldSucceed(context, result, response -> {
      Assertions.assertEquals(HttpResponseStatus.OK.code(), response.getStatusCode());
      Assertions.assertEquals(expectedResponse, response.getBody().toJsonObject());
      Mockito.verify(mockedWebClient, Mockito.times(1))
          .request(HttpMethod.GET, wireMockServer.port(), DOMAIN, REQUEST_PATH);
      wireMockServer.stop();
    });
  }

  @Test
  void whenSupportedDynamicPathServiceRequested_expectRequestExecutedAndResponseOKWithBody(
      VertxTestContext context, Vertx vertx) throws Exception {
    // given
    final JsonObject expectedResponse = new JsonObject(FileReader.readText("service/mock/response.json"));
    final WireMockServer wireMockServer = mockEndpoint(HttpResponseStatus.OK.code(), REQUEST_PATH,
        expectedResponse.encode());
    final WebClient mockedWebClient = Mockito.spy(webClient(vertx));
    HttpClientFacade clientFacade = new HttpClientFacade(mockedWebClient,
        getConfiguration(wireMockServer.port()));
    final ClientRequest request = new ClientRequest()
        .setParams(MultiMap.caseInsensitiveMultiMap().add("dynamicValue", "response"));

    // when
    Single<ClientResponse> result =
        clientFacade.process(payloadMessage("/services/mock/{param.dynamicValue}.json", request),
            HttpMethod.GET);

    // then
    subscribeToResult_shouldSucceed(context, result, response -> {
      Assertions.assertEquals(HttpResponseStatus.OK.code(), response.getStatusCode());
      Assertions.assertEquals(expectedResponse, response.getBody().toJsonObject());
      Mockito.verify(mockedWebClient, Mockito.times(1))
          .request(HttpMethod.GET, wireMockServer.port(), DOMAIN, REQUEST_PATH);
      wireMockServer.stop();
    });
  }

  @Test
  void whenServiceRequestedWithoutPathParam_expectNoServiceRequestAndBadRequest(
      VertxTestContext context, Vertx vertx) {
    // given
    final WebClient mockedWebClient = Mockito.spy(webClient(vertx));
    HttpClientFacade clientFacade = new HttpClientFacade(mockedWebClient,
        getConfiguration(0));

    // when
    Single<ClientResponse> result = clientFacade
        .process(new DataSourceAdapterRequest(), HttpMethod.GET);

    // then
    subscribeToResult_shouldFail(context, result, error -> {
      Assertions.assertEquals(error.getClass().getSimpleName(),
          IllegalArgumentException.class.getSimpleName());
      Mockito.verify(mockedWebClient, Mockito.times(0))
          .request(ArgumentMatchers.any(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyString(),
              ArgumentMatchers.anyString());
    });
  }

  @Test
  void whenUnsupportedPathServiceRequested_expectNoServiceRequestAndBadRequest(
      VertxTestContext context, Vertx vertx) {
    // given
    final WebClient mockedWebClient = Mockito.spy(webClient(vertx));
    HttpClientFacade clientFacade = new HttpClientFacade(mockedWebClient,
        getConfiguration(0));

    // when
    Single<ClientResponse> result =
        clientFacade
            .process(payloadMessage("/not/supported/path", new ClientRequest()), HttpMethod.GET);

    // then
    subscribeToResult_shouldFail(context, result, error -> {
      Assertions.assertEquals(UnsupportedDataSourceException.class, error.getClass());
      Mockito.verify(mockedWebClient, Mockito.times(0))
          .request(ArgumentMatchers.any(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyString(),
              ArgumentMatchers.anyString());
    });
  }

  @Test
  void whenServiceEmptyResponse_expectNoFailure(VertxTestContext context, Vertx vertx) {
    // given
    final WireMockServer wireMockServer = mockEndpoint(HttpResponseStatus.OK.code(),
        "/services/mock/empty.json", "");
    final WebClient mockedWebClient = Mockito.spy(webClient(vertx));
    HttpClientFacade clientFacade = new HttpClientFacade(mockedWebClient,
        getConfiguration(wireMockServer.port()));

    // when
    Single<ClientResponse> result = clientFacade
        .process(payloadMessage("/services/mock/empty.json", new ClientRequest()), HttpMethod.GET);

    // then
    subscribeToResult_shouldSucceed(context, result, response -> {
      Assertions.assertEquals(HttpResponseStatus.OK.code(), response.getStatusCode());
      Mockito.verify(mockedWebClient, Mockito.times(1))
          .request(HttpMethod.GET, wireMockServer.port(), DOMAIN, "/services/mock/empty.json");
      wireMockServer.stop();
    });
  }

  private WireMockServer mockEndpoint(int statusCode, String requestPath, String body) {
    WireMockServer wireMockServer = new WireMockServer(options().dynamicPort());
    wireMockServer.start();
    wireMockServer.stubFor(get(urlEqualTo(requestPath))
        .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(statusCode)
            .withBody(body)));
    return wireMockServer;
  }

  private WebClient webClient(Vertx vertx) {
    return WebClient.create(vertx);
  }

  private DataSourceAdapterRequest payloadMessage(String servicePath, ClientRequest request) {
    return new DataSourceAdapterRequest().setRequest(request)
        .setParams(new JsonObject().put("path", servicePath));
  }

  private HttpDataSourceAdapterOptions getConfiguration(int port) {
    return new HttpDataSourceAdapterOptions().setServices(getServiceConfigurations(port));
  }

  private List<HttpDataSourceSettings> getServiceConfigurations(int port) {
    return Lists.newArrayList(
        new HttpDataSourceSettings()
            .setPort(port)
            .setDomain(DOMAIN)
            .setPath(PATH)
            .setAllowedRequestHeaderPatterns(PATTERNS));
  }

}
