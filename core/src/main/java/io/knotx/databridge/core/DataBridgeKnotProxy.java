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
package io.knotx.databridge.core;

import io.knotx.databridge.core.impl.FragmentProcessor;
import io.knotx.engine.api.FragmentEvent;
import io.knotx.engine.api.FragmentEventContext;
import io.knotx.engine.api.FragmentEventResult;
import io.knotx.engine.api.TraceableKnotOptions;
import io.knotx.engine.api.TraceableKnotProxy;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;

public class DataBridgeKnotProxy extends TraceableKnotProxy {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataBridgeKnotProxy.class);
  public static final String DEFAULT_SUCCESS_TRANSITION = "next";

  private FragmentProcessor snippetProcessor;

  public DataBridgeKnotProxy(Vertx vertx, DataBridgeKnotOptions options) {
    super(new TraceableKnotOptions());
    this.snippetProcessor = new FragmentProcessor(vertx, options);
  }

  @Override
  protected Maybe<FragmentEventResult> execute(FragmentEventContext fragmentContext) {
    return Single.just(fragmentContext)
        .doOnSuccess(this::traceFragmentContext)
        .flatMap(eventCtx -> snippetProcessor.processSnippet(eventCtx))
        .map(this::createSuccessResult)
        .toMaybe();
  }

  @Override
  protected String getAddress() {
    return DataBridgeKnot.EB_ADDRESS;
  }

  private FragmentEventResult createSuccessResult(FragmentEvent event) {
    return new FragmentEventResult(event, DEFAULT_SUCCESS_TRANSITION);
  }

  private void traceFragmentContext(FragmentEventContext ctx) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Processing fragment {}",
          ctx.getFragmentEvent().getFragment().toJson().encodePrettily());
    }
  }
}
