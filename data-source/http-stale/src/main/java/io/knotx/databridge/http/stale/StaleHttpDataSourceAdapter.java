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


import io.knotx.databridge.api.DataSourceAdapterProxy;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.serviceproxy.ServiceBinder;

public class StaleHttpDataSourceAdapter extends AbstractVerticle {

  private static final Logger LOG = LoggerFactory.getLogger(StaleHttpDataSourceAdapter.class);

  private StaleHttpDataSourceAdapterOptions configuration;

  private MessageConsumer<JsonObject> consumer;

  private ServiceBinder serviceBinder;

  @Override
  public void init(Vertx vertx, Context context) {
    super.init(vertx, context);
    this.configuration = new StaleHttpDataSourceAdapterOptions(config());
  }

  @Override
  public void start() throws Exception {
    LOG.info("Starting <{}> with configuration <{}>", this.getClass().getSimpleName(), configuration);

    serviceBinder = new ServiceBinder(getVertx());
    consumer = serviceBinder.setAddress(configuration.getAddress())
        .register(DataSourceAdapterProxy.class,
            new StaleHttpDataSourceAdapterProxy(new io.vertx.reactivex.core.Vertx(vertx), configuration));
  }

  @Override
  public void stop() throws Exception {
    LOG.info("Stopping <{}>", this.getClass().getSimpleName());
    serviceBinder.unregister(consumer);
  }
}
