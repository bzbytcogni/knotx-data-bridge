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
import io.knotx.fragments.handler.api.Knot;
import io.knotx.fragments.handler.api.fragment.FragmentContext;
import io.knotx.fragments.handler.api.fragment.FragmentResult;
import io.reactivex.Single;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;

public class DataBridgeKnot extends AbstractVerticle implements Knot {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataBridgeKnot.class);

  private MessageConsumer<JsonObject> serviceConsumer;
  private ServiceBinder serviceBinder;

  private DataBridgeKnotOptions options;
  private FragmentProcessor snippetProcessor;

  @Override
  public void apply(FragmentContext fragmentContext, Handler<AsyncResult<FragmentResult>> result) {
    Single.just(fragmentContext)
        .doOnSuccess(this::traceFragmentContext)
        .flatMap(eventCtx -> snippetProcessor.processSnippet(eventCtx))
        .subscribe(
            success -> {
              LOGGER.debug("Processing ends with result [{}]", success);
              Future.succeededFuture(success).setHandler(result);
            },
            error -> {
              LOGGER.error("Processing ends with exception!", error);
              Future<FragmentResult> future = Future.failedFuture(error);
              future.setHandler(result);
            });
  }

  private void traceFragmentContext(FragmentContext ctx) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Processing fragment {}", ctx.getFragment().toJson().encodePrettily());
    }
  }

  @Override
  public void init(io.vertx.core.Vertx vertx, Context context) {
    super.init(vertx, context);
    this.options = new DataBridgeKnotOptions(config());
    this.serviceBinder = new ServiceBinder(vertx);
    this.snippetProcessor = new FragmentProcessor(this.vertx, new DataBridgeKnotOptions(config()));
  }

  @Override
  public void start() {
    LOGGER.info("Starting <{}>", this.getClass().getSimpleName());
    serviceConsumer = serviceBinder
        .setAddress(options.getAddress())
        .register(Knot.class, this);
  }

  @Override
  public void stop() {
    LOGGER.info("Stopping <{}>", this.getClass().getSimpleName());
    serviceBinder.unregister(serviceConsumer);
  }

}
