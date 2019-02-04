package com.acme.datasource.adapter;

import static io.knotx.junit5.util.RequestUtil.subscribeToResult_shouldSucceed;

import io.knotx.databridge.api.DataSourceAdapterRequest;
import io.knotx.databridge.api.DataSourceAdapterResponse;
import io.knotx.junit5.KnotxApplyConfiguration;
import io.knotx.junit5.KnotxExtension;
import io.knotx.reactivex.databridge.api.DataSourceAdapterProxy;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(KnotxExtension.class)
class ExampleDataSourceAdapterProxyTest {

  private static final String KNOTX_BRIDGE_DATASOURCE_CUSTOM = "knotx.bridge.datasource.custom";

  @Test
  @KnotxApplyConfiguration("application.conf")
  public void callCustomDataSourceAdapter_withMessage(
      VertxTestContext context, Vertx vertx) {

    callWithAssertions(context, vertx, "My message",
        response -> {
          Assertions.assertEquals("My message", getMessage(response));
        });
  }

  private void callWithAssertions(
      VertxTestContext context, Vertx vertx, String message,
      Consumer<io.knotx.databridge.api.DataSourceAdapterResponse> onSuccess) {
    DataSourceAdapterRequest request = prepareRequest(message);

    rxProcessWithAssertions(context, vertx, onSuccess, request);
  }


  private void rxProcessWithAssertions(VertxTestContext context, Vertx vertx,
      Consumer<DataSourceAdapterResponse> onSuccess, DataSourceAdapterRequest request) {
    DataSourceAdapterProxy service = DataSourceAdapterProxy.createProxy(vertx,
        KNOTX_BRIDGE_DATASOURCE_CUSTOM);
    Single<DataSourceAdapterResponse> adapterResponse = service.rxProcess(request);

    subscribeToResult_shouldSucceed(context, adapterResponse, onSuccess);
  }

  private DataSourceAdapterRequest prepareRequest(String message) {
    return new DataSourceAdapterRequest()
        .setParams(prepareParams(message));
  }

  private JsonObject prepareParams(String message) {
    return new JsonObject().put("message", message);
  }

  private String getMessage(DataSourceAdapterResponse response) {
    return response.getResponse()
                   .getBody()
                   .toJsonObject()
                   .getString("message");
  }
}
