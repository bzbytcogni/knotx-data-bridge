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
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.knotx.databridge.core.attribute.DataSourceAttribute;
import io.knotx.dataobjects.ClientRequest;
import io.knotx.dataobjects.Fragment;
import io.knotx.dataobjects.KnotContext;
import io.knotx.junit.rule.KnotxConfiguration;
import io.knotx.junit.rule.TestVertxDeployer;
import io.knotx.reactivex.proxy.KnotProxy;
import io.reactivex.functions.Consumer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class DataBridgeIntegrationTest {

  private final static String CORE_MODULE_EB_ADDRESS = "knotx.databridge.core";
  private static final String MOCK_SERVICE_JSON_RESULT_KEY = "result";
  private static final String MOCK_SERVICE_JSON_RESULT_VALUE = "success";
  private static final int MOCK_SERVICE_PORT_NUMBER = 3000;

  //Test Runner Rule of Verts
  private RunTestOnContext vertx = new RunTestOnContext();

  //Test Runner Rule of Knotx
  private TestVertxDeployer knotx = new TestVertxDeployer(vertx);

  //Junit Rule, sets up logger, prepares verts, starts verticles according to the config (supplied in annotation of test method)
  @Rule
  public RuleChain chain = RuleChain.outerRule(vertx).around(knotx);

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(new WireMockConfiguration().port(
      MOCK_SERVICE_PORT_NUMBER));

  @Test
  @KnotxConfiguration("bridgeStack.conf")
  public void callDataBridge_validKnotContextResult(TestContext context)
      throws IOException, URISyntaxException {
    wireMockRule.addStubMapping(stubFor(get(urlEqualTo("/dataSource/http/path/resource.json"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(String
                .format("{\"%s\":\"%s\"}", MOCK_SERVICE_JSON_RESULT_KEY,
                    MOCK_SERVICE_JSON_RESULT_VALUE)))));

    callWithAssertions(context, "template-engine/one-snippet-fragment/fragment1.txt",
        knotContext -> {
          context.assertTrue(
              knotContext.getFragments().iterator().next().context().containsKey("_result"));
          context.assertTrue(
              knotContext.getFragments().iterator().next().context().getJsonObject("_result")
                  .getString(MOCK_SERVICE_JSON_RESULT_KEY).equals(MOCK_SERVICE_JSON_RESULT_VALUE));
        },
        error -> context.fail(error.getMessage()));
  }


  private void callWithAssertions(TestContext context, String fragmentPath,
      Consumer<KnotContext> onSuccess,
      Consumer<Throwable> onError) throws IOException, URISyntaxException {
    KnotContext message = payloadMessage(fragmentPath);
    Async async = context.async();

    KnotProxy service = KnotProxy.createProxy(new Vertx(vertx.vertx()), CORE_MODULE_EB_ADDRESS);
    service.rxProcess(message)
        .doOnSuccess(onSuccess)
        .subscribe(
            success -> async.complete(),
            onError
        );
  }

  private KnotContext payloadMessage(String fragmentPath) throws IOException, URISyntaxException {
    String fragmentContent = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader()
        .getResource(fragmentPath).toURI())));
    return new KnotContext()
        .setClientRequest(new ClientRequest())
        .setFragments(Collections.singletonList(
            Fragment.snippet(Collections.singletonList("databridge"), fragmentContent)));
  }

}
