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

import io.knotx.engine.api.KnotProxy;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;

public class DataBridgeKnot extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataBridgeKnot.class);

  private DataBridgeKnotOptions options;

  private MessageConsumer<JsonObject> consumer;

  private ServiceBinder serviceBinder;

  @Override
  public void init(Vertx vertx, Context context) {
    super.init(vertx, context);
    this.options = new DataBridgeKnotOptions(config());
  }

  @Override
  public void start() {
    LOGGER.info("Starting <{}>", this.getClass().getSimpleName());

    //register the service proxy on event bus
    serviceBinder = new ServiceBinder(getVertx());
    consumer = serviceBinder
        .setAddress(options.getAddress())
        .register(KnotProxy.class, new DataBridgeKnotProxy(vertx, options));
  }

  @Override
  public void stop() {
    serviceBinder.unregister(consumer);
  }
}
