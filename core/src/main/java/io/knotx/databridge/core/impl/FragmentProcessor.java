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

import io.knotx.databridge.core.DataBridgeKnotOptions;
import io.knotx.databridge.core.DataBridgeKnotProxy;
import io.knotx.databridge.core.datasource.DataSourceEntry;
import io.knotx.databridge.core.datasource.DataSourcesEngine;
import io.knotx.fragment.HandlerLogEntry;
import io.knotx.fragment.HanlderStatus;
import io.knotx.knotengine.api.SnippetFragment;
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

  public Single<DataBridgeFragmentContext> processSnippet(final DataBridgeFragmentContext dataBridgeFragmentContext,
      SnippetFragmentsContext fragmentsContext) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Processing Handlebars snippet {}", dataBridgeFragmentContext.fragment());
    }
    return Observable.just(dataBridgeFragmentContext)
        .flatMap(DataBridgeFragmentContext::services)
        .map(serviceEngine::mergeWithConfiguration)
        .doOnNext(this::traceService)
        .flatMap(serviceEntry ->
            fetchServiceData(serviceEntry, fragmentsContext).toObservable()
                .map(serviceEntry::getResultWithNamespaceAsKey)
                .doOnError(e -> storeErrorInFragment(dataBridgeFragmentContext.fragment(), e,
                    serviceEntry.getName()))
        )
        .reduce(new JsonObject(), JsonObject::mergeIn)
        .map(results -> applyData(dataBridgeFragmentContext, results))
        .onErrorReturn(e -> handleError(dataBridgeFragmentContext, fragmentsContext, e));
  }

  private Single<JsonObject> fetchServiceData(DataSourceEntry service,
      SnippetFragmentsContext fragmentsContext) {
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

  private DataBridgeFragmentContext applyData(final DataBridgeFragmentContext dataBridgeFragmentContext,
      JsonObject serviceResult) {
    LOGGER.trace("Applying data to snippet {}", dataBridgeFragmentContext);
    dataBridgeFragmentContext.fragment().context().mergeIn(serviceResult);
    return dataBridgeFragmentContext;
  }

  private void traceService(DataSourceEntry serviceEntry) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Found service call definition: {} {}", serviceEntry.getAddress(),
          serviceEntry.getParams());
    }
  }

  private void storeErrorInFragment(SnippetFragment fragment, Throwable e, String name) {
    LOGGER.error("Data Bridge service {} failed. Cause: {}", name, e.getMessage());

    final HandlerLogEntry logEntry = new HandlerLogEntry(DataBridgeKnotProxy.SUPPORTED_FRAGMENT_ID);
    logEntry.error(String.format("Data Bridge service %s failed", name), e.getMessage());
    logEntry.setStatus(HanlderStatus.FAILURE);

    fragment.getDelegate().appendLog(logEntry);
  }

  private DataBridgeFragmentContext handleError(DataBridgeFragmentContext dataBridgeFragmentContext,
      SnippetFragmentsContext fragmentsContext, Throwable t) {
    LOGGER.error("Fragment processing failed. Cause:{}\nRequest:\n{}\nDataBridgeFragmentContext:\n{}\n",
        t.getMessage(), fragmentsContext.getClientRequest(), dataBridgeFragmentContext);
    //FIXME
//    dataBridgeFragmentContext.fragment().failure(DataBridgeKnotProxy.SUPPORTED_FRAGMENT_ID, t);
//    if (dataBridgeFragmentContext.fragment().fallback().isPresent()) {
//      return dataBridgeFragmentContext;
//    } else {
//      throw new FragmentProcessingException(String.format("Fragment processing failed in %s", DataBridgeKnotProxy.SUPPORTED_FRAGMENT_ID), t);
//    }
    return dataBridgeFragmentContext;
  }

}
