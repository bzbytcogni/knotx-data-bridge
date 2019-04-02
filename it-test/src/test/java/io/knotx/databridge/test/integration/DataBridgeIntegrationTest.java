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
package io.knotx.databridge.test.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static io.knotx.junit5.util.RequestUtil.subscribeToResult_shouldSucceed;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.knotx.databridge.core.DataBridgeKnotOptions;
import io.knotx.fragment.Fragment;
import io.knotx.fragments.handler.api.fragment.FragmentContext;
import io.knotx.fragments.handler.api.fragment.FragmentResult;
import io.knotx.fragments.handler.reactivex.api.Knot;
import io.knotx.junit5.KnotxApplyConfiguration;
import io.knotx.junit5.KnotxExtension;
import io.knotx.junit5.util.FileReader;
import io.knotx.junit5.util.RequestUtil;
import io.knotx.junit5.wiremock.ClasspathResourcesMockServer;
import io.knotx.junit5.wiremock.KnotxWiremockExtension;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(KnotxExtension.class)
class DataBridgeIntegrationTest {

  @ClasspathResourcesMockServer
  private WireMockServer mockService;

  @Test
  @DisplayName("Expect a success status when HTTP service responds with HTTP 200.")
  @KnotxApplyConfiguration("dataBridgeStack.conf")
  void callDataBridge_validSnippetFragmentsContextResult(VertxTestContext context,
      Vertx vertx) throws IOException {

    mockDataSource();

    callWithAssertions(context, vertx, "fragment/context_valid_ds.json",
        result -> {
          JsonObject payload = result.getFragment().getPayload();
          assertTrue(payload.containsKey("_result"));
          assertEquals(payload.getJsonObject("_result").getString("status"),
              "success");
          assertEquals(FragmentResult.DEFAULT_TRANSITION, result.getTransition());
        });
  }

  @Test
  @DisplayName("Expect a response in the namespace when HTTP service responds with HTTP 200.")
  @KnotxApplyConfiguration("dataBridgeStack.conf")
  void callDataBridgeWithNamespacedValidDS(VertxTestContext context,
      Vertx vertx) throws IOException {

    mockDataSource();

    callWithAssertions(context, vertx, "fragment/context_valid_ds_with_namespace.json",
        result -> {
          JsonObject payload = result.getFragment().getPayload();
          assertTrue(payload.containsKey("namespace"));
          JsonObject namespaceNode = payload.getJsonObject("namespace");
          assertTrue(namespaceNode.containsKey("_result"));
          assertEquals(FragmentResult.DEFAULT_TRANSITION, result.getTransition());
        });
  }

  @Test
  @DisplayName("Expect an error when HTTP service responds with error.")
  @KnotxApplyConfiguration("dataBridgeStack.conf")
  void callDataBridge_invalidSnippetFragmentsContextResult(
      VertxTestContext context, Vertx vertx) throws IOException {

    mockFailingDataSource();

    FragmentContext payload = payloadMessage("fragment/context_failing_ds.json");
    Knot service = Knot.createProxy(vertx, DataBridgeKnotOptions.DEFAULT_ADDRESS);
    Single<FragmentResult> result = service.rxApply(payload);

    RequestUtil.subscribeToResult_shouldFail(context, result, throwable -> {
    });
  }

  private void mockFailingDataSource() {
    KnotxWiremockExtension
        .stubForServer(mockService, get(urlMatching("/dataSource/failing/.*"))
            .willReturn(aResponse()
                .withStatus(500)));
  }

  private void mockDataSource() {
    KnotxWiremockExtension
        .stubForServer(mockService, get(urlMatching("/dataSource/http/.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")));
  }

  private void callWithAssertions(VertxTestContext context, Vertx vertx,
      String fragmentConfigurationPath, Consumer<FragmentResult> onSuccess) throws IOException {
    FragmentContext message = payloadMessage(fragmentConfigurationPath);

    rxProcessWithAssertions(context, vertx, onSuccess, message);
  }

  private void rxProcessWithAssertions(VertxTestContext context, Vertx vertx,
      Consumer<FragmentResult> onSuccess, FragmentContext payload) {
    Knot service = Knot.createProxy(vertx, DataBridgeKnotOptions.DEFAULT_ADDRESS);
    Single<FragmentResult> SnippetFragmentsContextSingle = service.rxApply(payload);

    subscribeToResult_shouldSucceed(context, SnippetFragmentsContextSingle, onSuccess);
  }

  private FragmentContext payloadMessage(String fragmentContextPath)
      throws IOException {
    return new FragmentContext(fromJsonFile(fragmentContextPath), new ClientRequest());

  }

  private Fragment fromJsonFile(String fragmentContentFile) throws IOException {
    final String fragmentConfig = FileReader.readText(fragmentContentFile);
    return new Fragment("snippet", new JsonObject(fragmentConfig), "");
  }
}
