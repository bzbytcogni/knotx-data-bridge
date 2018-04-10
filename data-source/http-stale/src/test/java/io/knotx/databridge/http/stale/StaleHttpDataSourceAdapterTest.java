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
package io.knotx.databridge.http.stale;

import io.knotx.databridge.api.DataSourceAdapterRequest;
import io.knotx.databridge.api.DataSourceAdapterResponse;
import io.knotx.databridge.api.reactivex.AdapterProxy;
import io.knotx.dataobjects.ClientRequest;
import io.knotx.junit.rule.KnotxConfiguration;
import io.knotx.junit.rule.TestVertxDeployer;
import io.reactivex.functions.Consumer;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@RunWith(VertxUnitRunner.class)
public class StaleHttpDataSourceAdapterTest {

  private static final String SERVER_RESPONSE_CONTROL_HEADER = "X-Server-Response-Control";

  private final static String ADAPTER_ADDRESS = "knotx.databridge.http.stale";
  private static final int SERVER_PORT = 4000;

  private static final String CORRECT_RESPONSE_JSON = "{\"data\":\"correct body\"}";
  private static final String SECOND_CORRECT_RESPONSE_JSON = "{\"data\":\"second correct body\"}";
  private static final String THIRD_CORRECT_RESPONSE_JSON = "{\"data\":\"third correct body\"}";
  private static final String INCORRECT_RESPONSE_JSON = "incorrect body";
  private static final JsonObject PARAMS = new JsonObject().put("path", "/content/test");

  //Test Runner Rule of Verts
  private RunTestOnContext vertx = new RunTestOnContext();

  //Test Runner Rule of Knotx
  private TestVertxDeployer knotx = new TestVertxDeployer(vertx);

  //Junit Rule, sets up logger, prepares verts, starts verticles according to the config (supplied in annotation of test method)
  @Rule
  public RuleChain chain = RuleChain.outerRule(vertx).around(knotx);

  /**
   * <pre>
   *  NO. |   ACTION      |   CACHE BEFORE    |   SERVICE RESPONSE   |      CACHE      | ADAPTER
   * RESPONSE
   *  ---------------------------------------------------------------------------------------------------
   *  1.  | Adapter call  |                   |       CORRECT        |      CORRECT    |
   * CORRECT
   * </pre>
   */
  @Test
  @KnotxConfiguration("stale-http-datasource-adapter-test.json")
  public void callAdapter_correctServiceResponse_expectCorrectResponse(TestContext context) {
    DataSourceAdapterRequest adapterRequest = payloadMessage();
    Verticle mockServer = createMockServer(SERVER_PORT, request -> {
      HttpServerResponse response = request.response();
      response.putHeader("content-type", "application/json");
      response.end(CORRECT_RESPONSE_JSON);
    });

    Async async = context.async();

    RxHelper.deployVerticle(Vertx.newInstance(vertx.vertx()), mockServer)
        .subscribe(handler ->
            // call adapter when mock server is ready
            callAdapterWithAssertions(async, adapterRequest,
                adapterResponse -> {
                  // we expect valid response
                  Assert.assertEquals("Expected status code 200 from Adapter!", 200,
                      adapterResponse.getResponse().getStatusCode());
                  Assert.assertEquals("Expected correct value in body!", CORRECT_RESPONSE_JSON,
                      adapterResponse.getResponse().getBody().toString());
                },
                error -> context.fail(error.getMessage())));
  }

  /**
   * <pre>
   *  NO. |   ACTION      |   CACHE BEFORE    |   SERVICE RESPONSE   |      CACHE      | ADAPTER
   * RESPONSE
   *  ---------------------------------------------------------------------------------------------------
   *  1.  | Adapter call  |                   |       INCORRECT      |                 |    ERROR
   * (500)
   * </pre>
   */
  @Test
  @KnotxConfiguration("stale-http-datasource-adapter-test.json")
  public void callAdapter_incorrectServiceResponse_expectErrorResponse(TestContext context) {
    DataSourceAdapterRequest adapterRequest = payloadMessage();
    Verticle mockServer = createMockServer(SERVER_PORT, request -> {
      HttpServerResponse response = request.response();
      response.putHeader("content-type", "application/json");
      response.end(INCORRECT_RESPONSE_JSON);
    });

    Async async = context.async();

    RxHelper.deployVerticle(Vertx.newInstance(vertx.vertx()), mockServer)
        .subscribe(handler ->
            // call adapter when mock server is ready
            callAdapterWithAssertions(async, adapterRequest,
                adapterResponse -> {
                  // we expect invalid response - cache is empty
                  Assert.assertEquals("Expected status code 500 from Adapter!", 500,
                      adapterResponse.getResponse().getStatusCode());
                },
                error -> context.fail(error.getMessage()))
        );
  }

  /**
   * <pre>
   *  NO. |   ACTION      |   CACHE BEFORE    |   SERVICE RESPONSE   |      CACHE      | ADAPTER
   * RESPONSE
   *  ---------------------------------------------------------------------------------------------------
   *  1.  | Adapter call  |                   |       ERROR          |                 |    ERROR
   * (500)
   * </pre>
   */
  @Test
  @KnotxConfiguration("stale-http-datasource-adapter-test.json")
  public void callAdapter_errorServiceResponse_expectErrorResponse(TestContext context) {
    DataSourceAdapterRequest adapterRequest = payloadMessage();
    Verticle mockServer = createMockServer(SERVER_PORT, request -> {
      HttpServerResponse response = request.response();
      response.setStatusCode(500);
      response.end("Incorrect Server Error");
    });

    Async async = context.async();

    RxHelper.deployVerticle(Vertx.newInstance(vertx.vertx()), mockServer)
        .subscribe(handler ->
            // call adapter when mock server is ready
            callAdapterWithAssertions(async, adapterRequest,
                adapterResponse -> {
                  // we expect invalid response - cache is empty
                  Assert.assertEquals("Expected status code 500 from Adapter!", 500,
                      adapterResponse.getResponse().getStatusCode());
                },
                error -> context.fail(error.getMessage()))
        );
  }

  /**
   * <pre>
   *  NO. |   ACTION            |   CACHE BEFORE    |   SERVICE RESPONSE   |      CACHE      |
   * ADAPTER RESPONSE
   *  ----------------------------------------------------------------------------------------------------------
   *  1.  | First Adapter call  |                   |       CORRECT        |     CORRECT     |
   * CORRECT
   *  2.  | Second Adapter call |  CORRECT (VALID)  |       NO CALL        |     CORRECT     |
   * CORRECT
   * </pre>
   */
  @Test
  @KnotxConfiguration("stale-http-datasource-adapter-test.json")
  public void callAdapterWithLongTTL_expectCachedResponse(TestContext context) {
    DataSourceAdapterRequest firstAdapterRequest = payloadMessage(ServerResponseControl.CORRECT);
    DataSourceAdapterRequest secondAdapterRequest = payloadMessage(ServerResponseControl.SECOND_CORRECT);

    Async async1 = context.async();
    Async async2 = context.async();

    Verticle mockServer = createMockServer(SERVER_PORT, request -> {
      HttpServerResponse response = request.response();
      response.putHeader("content-type", "application/json");
      ServerResponseControl control = ServerResponseControl
          .valueOf(request.getHeader(SERVER_RESPONSE_CONTROL_HEADER));
      switch (control) {
        case CORRECT:
          response.end(CORRECT_RESPONSE_JSON);
          break;
        case SECOND_CORRECT:
          context.fail("This service should not be called");
          break;
      }
    });

    RxHelper.deployVerticle(Vertx.newInstance(vertx.vertx()), mockServer)
        .subscribe(handler ->
            callAdapterWithAssertions(async1,
                firstAdapterRequest,
                firstResponse ->
                    callAdapterWithAssertions(async2,
                        secondAdapterRequest,
                        secondResponse -> {
                          Assert.assertEquals("Second call: expected status code 200 from Adapter!", 200,
                              secondResponse.getResponse().getStatusCode());
                          Assert.assertEquals("Expected valid correct value in response body from cache!",
                              CORRECT_RESPONSE_JSON,
                              secondResponse.getResponse().getBody().toString());
                        },
                        error -> context.fail(error.getMessage()))
                ,
                error -> context.fail(error.getMessage()))
        );
  }

  /**
   * <pre>
   *  NO. |   ACTION            |   CACHE BEFORE    |   SERVICE RESPONSE   |      CACHE      |
   * ADAPTER RESPONSE
   *  ----------------------------------------------------------------------------------------------------------
   *  1.  | First Adapter call  |                   |       CORRECT        |     CORRECT     |
   * CORRECT
   *  2.  | Second Adapter call | CORRECT (INVALID) |    SCHEDULE AFTER    |     CORRECT     |
   * CORRECT
   *  3.  |     Cache sync      | CORRECT (INVALID) |    SECOND CORRECT    | SECOND CORRECT  |
   *  4.  | Third Adapter call  |  SECOND CORRECT   |                      | SECOND CORRECT  |
   * SECOND CORRECT
   * </pre>
   */
  @Test
  @KnotxConfiguration("stale-http-datasource-adapte-nottlr-test.json")
  public void callAdapterWithNoTTL_expectCachedResponse(TestContext context)
      throws InterruptedException {
    DataSourceAdapterRequest firstAdapterRequest = payloadMessage(ServerResponseControl.CORRECT);
    DataSourceAdapterRequest secondAdapterRequest = payloadMessage(ServerResponseControl.SECOND_CORRECT);
    DataSourceAdapterRequest thirdAdapterRequest = payloadMessage(ServerResponseControl.THIRD_CORRECT);

    Async async1 = context.async();
    Async async2 = context.async();
    Async asyncCacheSync = context.async();
    Async asyncAfterSync = context.async();

    Verticle mockServer = createMockServer(SERVER_PORT, request -> {
      HttpServerResponse response = request.response();
      response.putHeader("content-type", "application/json");
      ServerResponseControl control = ServerResponseControl
          .valueOf(request.getHeader(SERVER_RESPONSE_CONTROL_HEADER));
      switch (control) {
        case CORRECT:
          response.end(CORRECT_RESPONSE_JSON);
          break;
        case SECOND_CORRECT:
          response.end(SECOND_CORRECT_RESPONSE_JSON);
          ExecutorService executor = Executors.newSingleThreadExecutor();
          executor.submit(asyncCacheSync::complete);
          break;
        case THIRD_CORRECT:
          response.end(THIRD_CORRECT_RESPONSE_JSON);
          break;
      }
    });

    RxHelper.deployVerticle(Vertx.newInstance(vertx.vertx()), mockServer)
        .subscribe(handler ->
            callAdapterWithAssertions(async1,
                firstAdapterRequest,
                firstResponse ->
                    callAdapterWithAssertions(async2,
                        secondAdapterRequest,
                        secondResponse -> {
                          Assert.assertEquals("Second call: expected status code 200 from Adapter!", 200,
                              secondResponse.getResponse().getStatusCode());
                          Assert.assertEquals(
                              "Expected invalid correct value in response body from cache!",
                              CORRECT_RESPONSE_JSON,
                              secondResponse.getResponse().getBody().toString());
                        },
                        error -> context.fail(error.getMessage())),
                error -> context.fail(error.getMessage()))
        );
    asyncCacheSync.handler(handler ->
        callAdapterWithAssertions(asyncAfterSync,
            thirdAdapterRequest,
            thirdResponse -> {
              // we expect value from previous call
              Assert.assertEquals("Third call: expected status code 200 from Adapter!", 200,
                  thirdResponse.getResponse().getStatusCode());
              Assert.assertEquals("Expected second correct value in response body from cache!",
                  SECOND_CORRECT_RESPONSE_JSON,
                  thirdResponse.getResponse().getBody().toString());
            },
            error -> context.fail(error.getMessage()))
    );
  }

  private DataSourceAdapterRequest payloadMessage() {
    return new DataSourceAdapterRequest()
        .setRequest(new ClientRequest())
        .setParams(PARAMS);
  }

  private DataSourceAdapterRequest payloadMessage(ServerResponseControl control) {
    return new DataSourceAdapterRequest()
        .setRequest(new ClientRequest()
            .setHeaders(MultiMap.caseInsensitiveMultiMap()
                .add(SERVER_RESPONSE_CONTROL_HEADER, control.toString())))
        .setParams(PARAMS);
  }

  private void callAdapterWithAssertions(Async async, DataSourceAdapterRequest adapterRequest,
                                         Consumer<DataSourceAdapterResponse> onSuccess, Consumer<Throwable> onError) {

    AdapterProxy adapter = AdapterProxy.createProxy(new Vertx(vertx.vertx()), ADAPTER_ADDRESS);
    adapter.rxProcess(adapterRequest)
        .doOnSuccess(onSuccess)
        .doOnError(onError)
        .doAfterTerminate(async::complete)
        .subscribe();
  }

  private Verticle createMockServer(int port, Handler<HttpServerRequest> handler) {
    return new AbstractVerticle() {

      private HttpServer httpServer;

      @Override
      public void start(Future<Void> startFuture) throws Exception {
        httpServer = vertx.createHttpServer();
        httpServer
            .requestHandler(handler)
            .rxListen(port)
            .toCompletable()
            .subscribe(startFuture::complete);
      }

      @Override
      public void stop(Future<Void> stopFuture) throws Exception {
        stop();
        httpServer.close(handler -> stopFuture.complete());
      }

      @Override
      public io.vertx.core.Vertx getVertx() {
        return super.getVertx();
      }
    };
  }

  enum ServerResponseControl {
    CORRECT, SECOND_CORRECT, THIRD_CORRECT
  }

}
