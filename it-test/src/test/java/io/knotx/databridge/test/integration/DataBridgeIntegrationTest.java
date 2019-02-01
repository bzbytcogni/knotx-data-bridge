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
import io.knotx.fragment.Fragment;
import io.knotx.junit5.KnotxApplyConfiguration;
import io.knotx.junit5.KnotxExtension;
import io.knotx.junit5.wiremock.KnotxWiremock;
import io.knotx.junit5.wiremock.KnotxWiremockExtension;

import io.knotx.knotengine.api.SnippetFragmentsContext;
import io.knotx.reactivex.knotengine.api.KnotProxy;
import io.knotx.server.api.context.ClientRequest;
import io.knotx.server.api.context.FragmentsContext;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(KnotxExtension.class)
public class DataBridgeIntegrationTest {

  private final static String CORE_MODULE_EB_ADDRESS = "knotx.knot.databridge";

  @KnotxWiremock
  protected WireMockServer mockService;

  @BeforeEach
  public void before() {
    KnotxWiremockExtension
        .stubForServer(mockService, get(urlMatching("/dataSource/http/.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")));
  }

  @Test
  @KnotxApplyConfiguration("bridgeStack.conf")
  public void callDataBridge_validSnippetFragmentsContextResult(
      VertxTestContext context, Vertx vertx)
      throws IOException, URISyntaxException {

    callWithAssertions(context, vertx, "template-engine/one-snippet-fragment/fragment1.txt",
        SnippetFragmentsContext -> {
          Assertions.assertTrue(
              SnippetFragmentsContext.getFragments().iterator().next().context().containsKey("_result"));
          Assertions.assertEquals(
              SnippetFragmentsContext.getFragments().iterator().next().context().getJsonObject("_result")
                  .getString("result"), "success");
        });
  }

  private void callWithAssertions(
      VertxTestContext context, Vertx vertx, String fragmentPath,
      Consumer<SnippetFragmentsContext> onSuccess) throws IOException, URISyntaxException {
    SnippetFragmentsContext message = payloadMessage(fragmentPath);

    rxProcessWithAssertions(context, vertx, onSuccess, message);
  }

  private void rxProcessWithAssertions(VertxTestContext context, Vertx vertx,
      Consumer<SnippetFragmentsContext> onSuccess, SnippetFragmentsContext payload) {
    KnotProxy service = KnotProxy.createProxy(vertx, CORE_MODULE_EB_ADDRESS);
    Single<SnippetFragmentsContext> SnippetFragmentsContextSingle = service.rxProcess(payload);

    subscribeToResult_shouldSucceed(context, SnippetFragmentsContextSingle, onSuccess);
  }

  private SnippetFragmentsContext payloadMessage(String fragmentPath) throws IOException, URISyntaxException {
    String fragmentContent = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader()
        .getResource(fragmentPath).toURI())));
    return new SnippetFragmentsContext(new FragmentsContext()
        .setClientRequest(new ClientRequest())
        .setFragments(Collections.singletonList(
            new Fragment("snippet", new JsonObject(Collections.singletonMap("knots", "databridge")), fragmentContent))));
  }

}
