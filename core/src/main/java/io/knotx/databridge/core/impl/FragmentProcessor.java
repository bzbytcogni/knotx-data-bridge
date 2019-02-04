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
package io.knotx.databridge.core.impl;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import io.knotx.databridge.core.DataBridgeKnotOptions;
import io.knotx.databridge.core.DataBridgeKnotProxy;
import io.knotx.databridge.core.datasource.DataSourceEntry;
import io.knotx.databridge.core.datasource.DataSourcesEngine;
import io.knotx.fragment.HandlerLogEntry;
import io.knotx.fragment.HanlderStatus;
import io.knotx.knotengine.api.SnippetFragmentsContext;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import java.util.concurrent.ExecutionException;

public class FragmentProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(FragmentProcessor.class);

  private final DataSourcesEngine serviceEngine;

  public FragmentProcessor(Vertx vertx, DataBridgeKnotOptions options) {
    this.serviceEngine = new DataSourcesEngine(vertx, options);
  }

  public Single<DataBridgeSnippet> processSnippet(
      final DataBridgeSnippet snippet, SnippetFragmentsContext fragmentsContext) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Processing Handlebars snippet {}", snippet.fragment());
    }
    return Observable.just(snippet)
        .flatMap(DataBridgeSnippet::services)
        .map(serviceEngine::mergeWithConfiguration)
        .doOnNext(this::traceService)
        .flatMap(serviceEntry -> fetchServiceData(snippet, fragmentsContext, serviceEntry))
        .reduce(new JsonObject(), JsonObject::mergeIn)
        .map(results -> applyData(snippet, results))
        .onErrorReturn(e -> handleError(snippet, fragmentsContext, e));
  }


  private Observable<JsonObject> fetchServiceData(DataBridgeSnippet snippet,
      SnippetFragmentsContext fragmentsContext, DataSourceEntry serviceEntry) {
    return fetchServiceData(fragmentsContext, serviceEntry)
        .toObservable()
        .map(serviceEntry::getResultWithNamespaceAsKey)
        .doOnNext(serviceResult -> processStatusCode(serviceResult, snippet, serviceEntry))
        .doOnError(e -> storeErrorInFragmentForService(snippet, serviceEntry.getName(), e));
  }

  private void processStatusCode(JsonObject serviceResult,
      DataBridgeSnippet snippet, DataSourceEntry serviceEntry) {
    int statusCode = serviceEngine.retrieveStatusCode(serviceResult);
    if (isInvalid(statusCode)) {
      storeErrorInFragmentForService(snippet, serviceEntry.getName(),
          new IllegalStateException(String
              .format("%s data-source error. Status code returned by adapter is %d",
                  serviceEntry.getName(), statusCode)));
    }
  }

  private boolean isInvalid(int statusCode) {
    return statusCode >= INTERNAL_SERVER_ERROR.code();
  }

  private Single<JsonObject> fetchServiceData(SnippetFragmentsContext fragmentsContext,
      DataSourceEntry service) {
    LOGGER.debug("Fetching data from service {} {}", service.getAddress(), service.getParams());
    try {
      return fragmentsContext.getCache()
          .get(service.getCacheKey(), () -> {
            LOGGER.debug("Requesting data from adapter {} with params {}", service.getAddress(),
                service.getParams());
            return serviceEngine.doServiceCall(service, fragmentsContext).cache();
          });
    } catch (ExecutionException e) {
      LOGGER.fatal("Unable to get service data {}", e);
      return Single.error(e);
    }
  }

  private DataBridgeSnippet applyData(
      final DataBridgeSnippet snippet,
      JsonObject serviceResult) {
    LOGGER.trace("Applying data to snippet {}", snippet);
    snippet.fragment().context().mergeIn(serviceResult);
    return snippet;
  }

  private void traceService(DataSourceEntry serviceEntry) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Found service call definition: {} {}", serviceEntry.getAddress(),
          serviceEntry.getParams());
    }
  }

  private void storeErrorInFragmentForService(DataBridgeSnippet snippet, String name,
      Throwable e) {
    LOGGER.error("Data Bridge service {} failed. Cause: {}", name, e.getMessage());

    final HandlerLogEntry logEntry = new HandlerLogEntry(DataBridgeKnotProxy.SUPPORTED_FRAGMENT_ID);
    logEntry.error(String.format("Data Bridge service %s failed", name), e.getMessage());
    logEntry.setStatus(HanlderStatus.FAILURE);

    snippet.fragment().getDelegate().appendLog(logEntry);
  }

  private DataBridgeSnippet handleError(DataBridgeSnippet snippet,
      SnippetFragmentsContext fragmentsContext, Throwable t) {
    LOGGER.error(
        "Fragment processing failed. Cause:{}\nRequest:\n{}\nDataBridgeSnippet:\n{}\n",
        t.getMessage(), fragmentsContext.getClientRequest(), snippet);
    storeErrorInFragmentForService(snippet, "databridge", t);
    return snippet;
  }

}
