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
package io.knotx.databridge.adapter.http;

import io.knotx.adapter.AbstractAdapterProxy;
import io.knotx.adapter.common.http.HttpAdapterConfiguration;
import io.knotx.adapter.common.http.HttpClientFacade;
import io.knotx.dataobjects.AdapterRequest;
import io.knotx.dataobjects.AdapterResponse;
import io.knotx.dataobjects.ClientResponse;
import io.knotx.databridge.adapter.http.cache.CacheableAdapterResponse;
import io.knotx.databridge.adapter.http.cache.ResponseCache;
import io.knotx.databridge.adapter.http.cache.StaleCache;
import io.reactivex.Single;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;

public class DataSourceAdapterProxy extends AbstractAdapterProxy {

  private static final Logger LOG = LoggerFactory.getLogger(DataSourceAdapterProxy.class);

  private HttpClientFacade httpClientFacade;

  private StaleCache<String, CacheableAdapterResponse> cache;

  DataSourceAdapterProxy(Vertx vertx, DataSourceAdapterConfiguration conf) {
    this.httpClientFacade = new HttpClientFacade(getWebClient(vertx, conf), conf);
    this.cache = new ResponseCache(conf.getCacheSize(), conf.getCacheTtl());
  }

  @Override
  protected Single<AdapterResponse> processRequest(AdapterRequest request) {
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

  private Single<AdapterResponse> callEndpoint(AdapterRequest request) {
    return httpClientFacade.process(request, HttpMethod.GET)
        .map(this::validate)
        .map(clientResponse -> new AdapterResponse().setResponse(clientResponse))
        .doOnSuccess(response -> {
          cache.put(getKey(request), new CacheableAdapterResponse(response));
          LOG.info("Cache is updated with new value [{}]", getKey(request));
        })
        .doOnError(oneError -> LOG
            .error("Could not get response [{}] from service [{}]", request.getParams(),
                oneError.getMessage()));
  }

  private String getKey(AdapterRequest req) {
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

  private WebClient getWebClient(Vertx vertx, HttpAdapterConfiguration configuration) {
    JsonObject clientOptions = configuration.getClientOptions();
    return clientOptions.isEmpty() ? WebClient.create(vertx) :
        WebClient.create(vertx, new WebClientOptions(clientOptions));
  }

}
