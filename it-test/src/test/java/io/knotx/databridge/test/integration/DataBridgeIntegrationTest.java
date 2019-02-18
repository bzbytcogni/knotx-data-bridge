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

import com.github.tomakehurst.wiremock.WireMockServer;
import io.knotx.databridge.core.DataBridgeKnot;
import io.knotx.engine.api.FragmentEvent;
import io.knotx.engine.api.FragmentEvent.Status;
import io.knotx.engine.api.FragmentEventContext;
import io.knotx.engine.api.FragmentEventResult;
import io.knotx.engine.api.KnotFlow;
import io.knotx.engine.reactivex.api.KnotProxy;
import io.knotx.fragment.Fragment;
import io.knotx.junit5.KnotxApplyConfiguration;
import io.knotx.junit5.KnotxExtension;
import io.knotx.junit5.util.FileReader;
import io.knotx.junit5.wiremock.KnotxWiremock;
import io.knotx.junit5.wiremock.KnotxWiremockExtension;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(KnotxExtension.class)
public class DataBridgeIntegrationTest {

  @KnotxWiremock
  protected WireMockServer mockService;

  @Test
  @KnotxApplyConfiguration("bridgeStack.conf")
  public void callDataBridge_validSnippetFragmentsContextResult(VertxTestContext context,
      Vertx vertx) throws IOException {

    mockDataSource();

    callWithAssertions(context, vertx, "fragment/context_valid_ds.json",
        new KnotFlow(DataBridgeKnot.EB_ADDRESS, Collections.emptyMap()),
        eventResult -> {
          JsonObject payload = eventResult.getFragmentEvent().getFragment().getPayload();
          Assertions.assertEquals(Status.SUCCESS, eventResult.getFragmentEvent().getStatus());
          Assertions.assertTrue(payload.containsKey("_result"));
          Assertions
              .assertEquals(payload.getJsonObject("_result").getString("status"),
                  "success");
        });
  }

  @Test
  @KnotxApplyConfiguration("bridgeStack.conf")
  public void callDataBridge_invalidSnippetFragmentsContextResult(
      VertxTestContext context, Vertx vertx) throws IOException {

    mockFailingDataSource();

    callWithAssertions(context, vertx, "fragment/context_failing_ds.json",
        new KnotFlow(DataBridgeKnot.EB_ADDRESS, Collections.emptyMap()),
        eventResult -> {
          Assertions.assertEquals(Status.FAILURE, eventResult.getFragmentEvent().getStatus());
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
      String fragmentConfigurationPath, KnotFlow flow,
      Consumer<FragmentEventResult> onSuccess) throws IOException {
    FragmentEventContext message = payloadMessage(fragmentConfigurationPath, flow);

    rxProcessWithAssertions(context, vertx, onSuccess, message);
  }

  private void rxProcessWithAssertions(VertxTestContext context, Vertx vertx,
      Consumer<FragmentEventResult> onSuccess, FragmentEventContext payload) {
    KnotProxy service = KnotProxy.createProxy(vertx, DataBridgeKnot.EB_ADDRESS);
    Single<FragmentEventResult> SnippetFragmentsContextSingle = service.rxProcess(payload);

    subscribeToResult_shouldSucceed(context, SnippetFragmentsContextSingle, onSuccess);
  }

  private FragmentEventContext payloadMessage(String fragmentContextPath, KnotFlow flow)
      throws IOException {
    return new FragmentEventContext(new FragmentEvent(fromJsonFile(fragmentContextPath), flow),
        new ClientRequest(), 0);

  }

  private Fragment fromJsonFile(String fragmentContentFile) throws IOException {
    final String fragmentConfig = FileReader.readText(fragmentContentFile);
    return new Fragment("snippet", new JsonObject(fragmentConfig), "");
  }
}
