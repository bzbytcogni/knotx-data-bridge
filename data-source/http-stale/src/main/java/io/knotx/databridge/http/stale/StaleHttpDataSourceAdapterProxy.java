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
import io.knotx.databridge.http.stale.cache.CacheableAdapterResponse;
import io.knotx.databridge.http.stale.cache.ResponseCache;
import io.knotx.databridge.http.stale.cache.StaleCache;
import io.knotx.databridge.api.reactivex.AbstractDataSourceAdapterProxy;
import io.knotx.databridge.http.common.http.HttpClientFacade;
import io.knotx.dataobjects.ClientResponse;
import io.reactivex.Single;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;

public class StaleHttpDataSourceAdapterProxy extends AbstractDataSourceAdapterProxy {

  private static final Logger LOG = LoggerFactory.getLogger(StaleHttpDataSourceAdapterProxy.class);

  private HttpClientFacade httpClientFacade;

  private StaleCache<String, CacheableAdapterResponse> cache;

  StaleHttpDataSourceAdapterProxy(Vertx vertx, StaleHttpDataSourceAdapterOptions options) {
    this.httpClientFacade = new HttpClientFacade(getWebClient(vertx, options), options);
    this.cache = new ResponseCache(options.getCacheSize(), options.getCacheTtl());
  }

  @Override
  protected Single<DataSourceAdapterResponse> processRequest(DataSourceAdapterRequest request) {
    return Single.just(request)
        .flatMap(req ->
            cache.get(getKey(req))
                // a JSON service response is cached but can be stale
                .map(res -> {
                  LOG.debug("Service response from cache [{}]!", getKey(req));
                  if (res.isExpired()) {
                    // call service and update cache asynchronously
                    LOG.debug("Service reponse is stale, cache refresh [{}]!", getKey(req));
                    callEndpoint(req).subscribe();
                  }
                  // continue and respond with stale or current JSON service response
                  return Single.just(res.getValue());
                })
                // no service response is cached, record service call and cache update
                .orElse(callEndpoint(req)));
  }

  private Single<DataSourceAdapterResponse> callEndpoint(DataSourceAdapterRequest request) {
    return httpClientFacade.process(request, HttpMethod.GET)
        .map(this::validate)
        .map(clientResponse -> new DataSourceAdapterResponse().setResponse(clientResponse))
        .doOnSuccess(response -> {
          cache.put(getKey(request), new CacheableAdapterResponse(response));
          LOG.info("Cache is updated with new value [{}]", getKey(request));
        })
        .doOnError(oneError -> LOG
            .error("Could not get response [{}] from service [{}]", request.getParams(),
                oneError.getMessage()));
  }

  private String getKey(DataSourceAdapterRequest req) {
    return req.getParams().toString();
  }

  private ClientResponse validate(ClientResponse response) {
    String rawData = response.getBody().toString().trim();
    if (rawData.charAt(0) == '[') {
      new JsonArray(rawData);
    } else if (rawData.charAt(0) == '{') {
      new JsonObject(rawData);
    } else {
      throw new IllegalArgumentException(
          "Invalid response format [" + rawData + "], it is not JSON!");
    }
    return response;
  }

  private WebClient getWebClient(Vertx vertx, StaleHttpDataSourceAdapterOptions options) {
    WebClientOptions clientOptions = options.getClientOptions();
    return WebClient.create(vertx, new WebClientOptions(clientOptions));
  }

}
