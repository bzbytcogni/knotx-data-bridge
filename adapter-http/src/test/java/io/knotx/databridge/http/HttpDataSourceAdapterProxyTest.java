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

import io.knotx.dataobjects.AdapterRequest;
import io.knotx.dataobjects.AdapterResponse;
import io.knotx.dataobjects.ClientRequest;
import io.knotx.junit5.KnotxApplyConfiguration;
import io.knotx.junit5.KnotxExtension;
import io.knotx.junit5.KnotxTestUtils;
import io.knotx.reactivex.proxy.AdapterProxy;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.functions.Consumer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(KnotxExtension.class)
public class HttpDataSourceAdapterProxyTest {

  private final static String ADAPTER_ADDRESS = "knotx.databridge.http";

  @Test
  @KnotxApplyConfiguration("knotx-datasource-http-test.json")
  public void callNonExistingService_expectBadRequestResponse(
      VertxTestContext context, Vertx vertx) {
    callAdapterServiceWithAssertions(context, vertx, "not/existing/service/address",
        adapterResponse -> {
          context
              .verify(() -> Assertions.assertEquals(adapterResponse.getResponse().getStatusCode(),
                  HttpResponseStatus.INTERNAL_SERVER_ERROR.code()));
        },
        context::failNow);
  }

  @Test
  @KnotxApplyConfiguration("knotx-datasource-http-test.json")
  public void callExistingService_expectOKResponseWithServiceDataProvidedByService1(
      VertxTestContext context, Vertx vertx) throws Exception {
    final String expected = KnotxTestUtils.readText("first-response.json");

    callAdapterServiceWithAssertions(context, vertx, "/service/mock/first.json",
        adapterResponse -> {
          context.verify(() -> {
            Assertions.assertEquals(adapterResponse.getResponse().getStatusCode(),
                HttpResponseStatus.OK.code());

            JsonObject serviceResponse = new JsonObject(
                adapterResponse.getResponse().getBody().toString());
            JsonObject expectedResponse = new JsonObject(expected);

            Assertions.assertEquals(serviceResponse, expectedResponse);
          });
        },
        context::failNow);
  }

  private void callAdapterServiceWithAssertions(
      VertxTestContext context,
      Vertx vertx,
      String servicePath,
      Consumer<AdapterResponse> onSuccess,
      Consumer<Throwable> onError) {
    AdapterRequest message = payloadMessage(servicePath);

    AdapterProxy service = AdapterProxy.createProxy(vertx, ADAPTER_ADDRESS);

    service.rxProcess(message)
        .doOnSuccess(onSuccess)
        .subscribe(
            success -> context.completeNow(),
            onError
        );
  }

  private AdapterRequest payloadMessage(String servicePath) {
    return new AdapterRequest().setRequest(new ClientRequest())
        .setParams(new JsonObject().put("path", servicePath));
  }

}
