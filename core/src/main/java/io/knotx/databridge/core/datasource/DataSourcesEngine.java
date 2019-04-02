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
package io.knotx.databridge.core.datasource;

import io.knotx.databridge.api.DataSourceAdapterRequest;
import io.knotx.databridge.api.DataSourceAdapterResponse;
import io.knotx.databridge.core.DataBridgeKnotOptions;
import io.knotx.databridge.core.DataSourceDefinition;
import io.knotx.fragments.handler.api.fragment.FragmentContext;
import io.knotx.reactivex.databridge.api.DataSourceAdapterProxy;
import io.netty.handler.codec.http.HttpStatusClass;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

// TODO define unit tests
public class DataSourcesEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataSourcesEngine.class);

  private static final String RESULT_NAMESPACE_KEY = "_result";
  private static final String RESPONSE_NAMESPACE_KEY = "_response";

  private final DataBridgeKnotOptions options;

  private final Map<String, DataSourceAdapterProxy> adapters;

  public DataSourcesEngine(Vertx vertx, DataBridgeKnotOptions options) {
    this.options = options;
    this.adapters = new HashMap<>();
    this.options.getDataDefinitions().forEach(
        service -> adapters.put(service.getAdapter(),
            DataSourceAdapterProxy.createProxyWithOptions(
                vertx,
                service.getAdapter(),
                this.options.getDeliveryOptions())
        )
    );
  }

  public Single<JsonObject> doServiceCall(DataSourceEntry serviceEntry,
      FragmentContext fragmentContext) {
    DataSourceAdapterRequest adapterRequest = new DataSourceAdapterRequest()
        .setRequest(fragmentContext.getClientRequest())
        .setParams(serviceEntry.getParams());

    return adapters.get(serviceEntry.getAddress())
        .rxProcess(adapterRequest)
        .doOnSuccess(response -> validateStatusCode(response, serviceEntry))
        .map(response -> buildResultObject(adapterRequest, response));
  }

  public DataSourceEntry mergeWithConfiguration(final DataSourceEntry serviceEntry) {
    Optional<DataSourceDefinition> serviceMetadata = options.getDataDefinitions()
        .stream()
        .filter(service -> serviceEntry.getName().matches(service.getName()))
        .findFirst();

    return serviceMetadata.map(
        metadata ->
            new DataSourceEntry(serviceEntry)
                .setAddress(metadata.getAdapter())
                .mergeParams(metadata.getParams())
                .setCacheKey(metadata.getCacheKey()))
        .orElseThrow(() -> {
          LOGGER.error("Missing service configuration for: {}", serviceEntry.getName());
          return new IllegalStateException("Missing service configuration");
        });
  }

  private void validateStatusCode(DataSourceAdapterResponse response,
      DataSourceEntry serviceEntry) {
    int statusCode = response.getResponse().getStatusCode();
    if (HttpStatusClass.valueOf(statusCode) == HttpStatusClass.SERVER_ERROR) {
      throw new IllegalStateException(String
          .format("%s data-source error. Status code returned by adapter is %d",
              serviceEntry.getName(), statusCode));
    }
  }

  private JsonObject buildResultObject(DataSourceAdapterRequest adapterRequest,
      DataSourceAdapterResponse adapterResponse) {
    JsonObject object = new JsonObject();

    String rawData = adapterResponse.getResponse().getBody().toString().trim();

    if (rawData.charAt(0) == '[') {
      object.put(RESULT_NAMESPACE_KEY, new JsonArray(rawData));
    } else if (rawData.charAt(0) == '{') {
      object.put(RESULT_NAMESPACE_KEY, new JsonObject(rawData));
    } else {
      LOGGER.error("Result of [{} {}] neither Json Array nor Json Object: [{}]",
          adapterRequest.getRequest().getMethod(), adapterRequest.getRequest().getPath(),
          StringUtils.abbreviate(rawData, 15));
    }
    object.put(RESPONSE_NAMESPACE_KEY, new JsonObject()
        .put("statusCode", Integer.toString(adapterResponse.getResponse().getStatusCode())));
    return object;
  }
}
