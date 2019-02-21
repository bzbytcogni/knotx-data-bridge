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
import io.knotx.databridge.core.datasource.DataSourceEntry;
import io.knotx.databridge.core.datasource.DataSourcesEngine;
import io.knotx.engine.api.FragmentEvent;
import io.knotx.engine.api.FragmentEventContext;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;

public class FragmentProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(FragmentProcessor.class);

  private final DataSourcesEngine serviceEngine;

  public FragmentProcessor(Vertx vertx, DataBridgeKnotOptions options) {
    this.serviceEngine = new DataSourcesEngine(vertx, options);
  }

  public Single<FragmentEvent> processSnippet(FragmentEventContext context) {
    return Observable.just(context.getFragmentEvent())
        .map(DataBridgeSnippet::from)
        .flatMap(DataBridgeSnippet::services)
        .map(serviceEngine::mergeWithConfiguration)
        .doOnNext(this::traceService)
        .flatMap(serviceEntry -> fetchServiceData(context, serviceEntry))
        .reduce(new JsonObject(), JsonObject::mergeIn)
        .map(results -> applyData(context.getFragmentEvent(), results));
  }


  private Observable<JsonObject> fetchServiceData(FragmentEventContext fragmentContext,
      DataSourceEntry serviceEntry) {
    return serviceEngine.doServiceCall(serviceEntry, fragmentContext)
        .map(serviceEntry::getResultWithNamespaceAsKey)
        .toObservable();
  }

  private FragmentEvent applyData(final FragmentEvent event, JsonObject serviceResult) {
    LOGGER.trace("Applying data to snippet {}", event);
    event.getFragment().mergeInPayload(serviceResult);
    return event;
  }

  private void traceService(DataSourceEntry serviceEntry) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Found service call definition: {} {}", serviceEntry.getAddress(),
          serviceEntry.getParams());
    }
  }

}
